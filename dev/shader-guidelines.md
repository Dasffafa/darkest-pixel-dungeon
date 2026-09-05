# 跟随地图图格或 Sprite 的 Shader 编写注意事项

本文记录 Darkest Pixel Dungeon 在 Noosa/LibGDX 渲染管线中编写世界空间 Shader
时必须遵守的约束。目标是让效果跟随地图拖动、角色移动、Camera 缩放和震动，
而不是意外固定在屏幕或 UI 坐标中。

## 1. 先确定效果所属空间

| 类型 | 坐标来源 | 推荐入口 |
|---|---|---|
| 跟随角色或怪物 | `CharSprite` 的 `x/y` 与 `uModel` | `SpriteShaderEffect` |
| 跟随固定地图格 | `DungeonTilemap.tileToWorld(cell)` | 地图视觉层中的 `Group`/粒子 |
| 整张地图滤镜 | 世界层 `FrameBuffer` | `MapFilter`/`FilteredMapGroup` |
| UI 效果 | `uiCamera` | UI 自己的 Visual/Group |

不要用屏幕像素坐标模拟地图坐标，也不要把图格粒子挂到 UI Camera。全屏地图滤镜
只适合处理已经渲染完成的地图画面，不适合替代需要与单个图格保持关系的粒子。

## 2. 世界空间顶点变换

跟随图格或 Sprite 的顶点 Shader 必须同时使用 `uModel` 和 `uCamera`：

```glsl
uniform mat4 uCamera;
uniform mat4 uModel;

void main() {
    gl_Position = uCamera * uModel * localPosition;
}
```

- `uModel` 提供对象位置、原点、基础旋转和缩放。
- `uCamera` 提供地图滚动、跟随角色、缩放与震动。
- 省略 `uModel` 会让多个 Sprite 使用同一局部位置。
- 省略或长期缓存 `uCamera` 会让效果看起来固定在屏幕上。

## 3. Camera 矩阵是可变对象

Noosa 的 `Camera.matrix` 会在原数组上更新。只比较 `Camera` 对象引用无法判断矩阵
是否因拖动或跟随角色而变化。

资源型 Shader 应继承 `ResourceNoosaScript`。该基类在每次绘制前调用
`resetCamera()`，强制 `NoosaScript.camera()` 重新上传矩阵：

```kotlin
override fun camera(camera: Camera?) {
    resetCamera()
    super.camera(camera)
}
```

新增世界空间 Shader 不要绕过这一入口。若创建其他 Shader 基类，也必须提供等价的
逐帧 Camera Uniform 刷新机制。

## 4. 跟随地图格的效果

图格效果应保存格子编号，并在创建时转换为世界坐标：

```kotlin
private val point = DungeonTilemap.tileToWorld(cell)
```

控制器应加入 `Level.addVisuals()` 返回的地图视觉 `Group`，或者 GameScene 中其他明确
属于世界层的 Group。不要加入使用 `uiCamera` 的控件组。

建议由图格控制器负责：

- `Dungeon.visible[cell]` 可见性判断；
- 粒子生成频率和回收；
- 世界空间初始位置；
- 离开场景后的统一销毁。

粒子的下落和风偏可以继续在 CPU 更新 `x/y`。Shader 只处理翻面、形变、颜色和
透明度时，粒子仍然与地图格保持确定关系。

## 5. 跟随 Sprite 的效果

角色和怪物效果通过 `CharSprite.shaderEffect` 挂载：

```kotlin
sprite.shaderEffect = SomeSpriteEffect()
```

`SpriteShaderEffect.update()` 用于推进时间和动画参数，`prepare()` 在绘制前上传
Uniform。自定义 Shader 必须兼容 Noosa 的标准字段：

- `uCamera`
- `uModel`
- `uTex`
- `aXYZW`
- `aUV`

如果需要保留 Sprite 原有的染色与透明度，还应声明和使用 `uColorM`、`uColorA`：

```glsl
vec4 color = texture2D(uTex, vUV) * uColorM + uColorA;
```

Sprite 图集效果应通过 `Image.frame()` 上传当前 UV 范围，不能假设纹理坐标总是
`0..1`。否则动画换帧后，局部裁切、溶解或噪声位置会跳动。

## 6. 围绕对象中心进行形变

直接缩放 `aXYZW.xy` 会以左上角为原点，造成翻面时位置漂移。应先移动到局部中心，
完成形变后再移回：

```glsl
vec2 center = uSpriteSize * 0.5;
vec2 p = local.xy - center;
// transform p
local.xy = p + center;
```

沿任意倾斜轴翻面时，将局部坐标拆成轴向和垂直轴向分量：

```glsl
vec2 axis = normalize(uFlipAxis);
vec2 alongAxis = axis * dot(p, axis);
vec2 acrossAxis = p - alongAxis;
p = alongAxis + acrossAxis * cos(uPhase);
```

每个实例可以在生成时随机一次 `uFlipAxis`，不要每帧随机，否则会产生抖动。

## 7. Uniform 与绘制顺序

`Image.draw()` 的主要顺序是：

1. 选择并启用 Script；
2. 绑定纹理；
3. 上传 Camera；
4. 上传 Model 和颜色；
5. 绘制 Quad。

效果的 `prepare()` 必须在对应 Script 已经启用后写 Uniform。不要从 Actor 线程直接
调用 OpenGL；跨线程安装或切换效果应使用 `Game.runOnRenderThreadAndWait()` 或已有的
渲染线程调度入口。

通过 `SpriteBatch`、FrameBuffer 合成等其他渲染管线绘制后，必须清除 Noosa 的
Program 和 Texture 绑定缓存，否则后续对象可能错误地跳过 `glUseProgram` 或纹理绑定：

```kotlin
Script.clearBinding()
Texture.clear()
```

## 8. 生命周期与资源恢复

- Shader 源码放在 `core/src/main/assets/shaders`。
- 使用 `Gdx.files.internal()` 加载，保证 Android/Desktop 路径一致。
- 场景销毁时释放独占的 FrameBuffer、SpriteBatch 和 ShaderProgram。
- `Script.reset()` 后，基于 `Script.use()` 的 Shader 应允许重新创建。
- 暂停恢复、窗口缩放和场景重建后必须重新验证 Camera、纹理和 Uniform。
- Shader 编译失败必须记录完整日志；全屏滤镜应回退到可用的直接或恒等绘制路径。

## 9. 验收清单

世界空间效果至少验证：

1. 拖动地图时，效果和原图格/Sprite 保持相对位置。
2. 角色移动导致 Camera 跟随时，效果不固定在屏幕上。
3. 放大缩小时，位置、中心与形变比例正确。
4. Camera 震动时，效果和地图同步震动。
5. Sprite 换动画帧后，UV 局部效果不跳动。
6. 效果关闭后恢复原始颜色、透明度和绘制路径。
7. Android 与 Desktop 的方向、透明混合和 GLSL 编译结果一致。
8. 暂停恢复和重新进入场景后不丢失 Shader 或纹理。

当前参考实现：

- `effects/shaders/ResourceNoosaScript.kt`
- `effects/shaders/ColdSnowScript.kt`
- `effects/particles/ColdSnowParticles.kt`
- `effects/shaders/CutDeathEffect.kt`
- `effects/shaders/FilteredMapGroup.kt`
