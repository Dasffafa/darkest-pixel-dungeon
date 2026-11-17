# 项目可用特效与粒子速查表

> 适用分支：`hat-change`  
> 目录位置：`core/src/main/java/com/egoal/darkestpixeldungeon/effects/` 与 `levels/lighting/`

---

## 1. 新增动态光照与程序化特效（无外部贴图依赖）

| 特效 / 系统 | 类名 / 位置 | 调用方式 | 视觉效果与用途 |
| :--- | :--- | :--- | :--- |
| **彩色像素光照场** | [LightSource.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/levels/lighting/LightSource.kt)、[Level.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/levels/Level.kt) | `Dungeon.level.addTransientLight(LightSource.fromColor(cell, rgb, radius, intensity, duration))` | 墙体阻挡（`losBlocking`）+ 15 Hz 闪烁 + 视野联动；内置 `torch` / `arcane` / `fire` / `frost` / `holy` 预设 |
| **3 层锥形折线激光** | [Beam.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Beam.kt) | `parent.add(Beam.DeathRay(s, e))` / `LightRay` / `HealthRay` / `ThickDeathRay` / `DarkRay` | 3 层共享 22 Hz 抖动脊柱 + `glBlendFuncSeparate` 防发灰 + 自动注入沿途瞬时彩色光源 |
| **假 Bloom 魔法爆炸三件套** | [MagicBurst.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/MagicBurst.kt) | `MagicBurst.spawn(group, cell, color, radius = 16f)` | 过曝纯白核心闪 + 内存径向柔光圆（`glBlendFuncSeparate`）+ 地面弹跳火星（`BouncingSpark`） |
| **喷火技能粒子流** | [FireSprayParticles.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/particles/FireSprayParticles.kt) | `FireSprayParticle.FACTORY`、`FireSparkParticle.FACTORY`、`FireSmokeParticle.FACTORY` | 白黄→橙→红渐变膨胀火团 + 重力火星 + 上升黑烟；`MagicMissile.fire(...)` 已默认接入 |
| **地面焦痕贴花** | [ScorchMark.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/ScorchMark.kt) | `ScorchMark.place(group, cell)` | 在地面生成随机倾角的深炭焦痕，约 `2.6s` 自然淡出 |
| **怪物死亡 GLSL 着色器** | [RadishDeathEffects.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/shaders/RadishDeathEffects.kt) | `RadishDeathShaders.play(target, DeathShaderType.*)`、`RadishDeathShaders.lightEvaporate(target)` | 迁移自 RadishPD 的 10 套死亡着色器（`CUT`/`BURN`/`ACID`/`SINGULARITY`/`NOISE`/`LIGHT_EVAPORATE`/`LIGHT_DISSOLVE`/`ELLIPSE`/`WIPE`/`ALPHA`）；`LIGHT_EVAPORATE` 附带跟随目标的上升 1px 白色光像素粒子流 |

---

## 2. 弹道、射线与几何/精灵特效（`effects/`）

| 特效名称 | 类名 | 常用调用方式 | 适用场景 |
| :--- | :--- | :--- | :--- |
| **法术弹道（带飞行光+命中爆发）** | [MagicMissile.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/MagicMissile.java) | `MagicMissile.blueLight / fire / purpleLight / whiteLight / coldLight / rainbow / electronics / earth / shadow / wool / leaf / force / slowness(...)` | 各类法杖、怪物远程法术（带颜色的弹道已自动联动 `LightMap` 与 `MagicBurst`） |
| **连锁闪电** | [Lightning.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Lightning.java) | `parent.add(Lightning(arcs, callback))` | 闪电法杖、陷阱、多目标电弧连接 |
| **扇形圆弧扫光** | [CircleArc.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/CircleArc.kt) | `CircleArc(nTris, rad).color(rgb, lightMode).show(visual, duration)` | 蓄力、护盾、冷却进度或扇形范围指示 |
| **扩散光环** | [ExpandHalo.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/ExpandHalo.java) / [Halo.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Halo.java) | `ExpandHalo(radius, color)` / `TorchHalo(sprite)` | 光环扩散、火把/精英光圈 |
| **耀斑 / 射线光芒** | [Flare.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Flare.java) | `Flare(nRays, radius).color(rgb, lightMode).show(visual, duration)` | 物品发光、升级、Boss 登场/爆发 |
| **暴击冲击 / 刀光闪烁** | [CriticalShock.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/CriticalShock.java)、[WeaponFlash.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/WeaponFlash.kt)、[KusarigamaSlash.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/KusarigamaSlash.kt) | 实例化并加入 `parent` | 近战暴击、武器闪光、锁镰斩击 |
| **水花溅射 / 涟漪** | [Splash.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Splash.java)、[Ripple.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Ripple.java) | `Splash.at(cell, color, n)` / `GameScene.ripple(cell)` | 药水碎裂、液体飞溅、水面波纹 |
| **伤痕 / 附魔 / 鉴定 / 退化** | [Wound.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Wound.java)、[Enchanting.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Enchanting.java)、[Identification.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Identification.java)、[Degradation.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Degradation.java) | `Wound.hit(ch)` / `Enchanting.show(ch, item)` 等 | 物理受击、装备附魔、鉴定、诅咒退化 |
| **链条 / 推挤 / 换位** | [Chains.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Chains.java)、[Pushing.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Pushing.java)、[Swap.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Swap.java) | `parent.add(Chains(s, e, callback))` 等 | 虚无锁链钩爪、击退、位置交换 |
| **通用气泡图标 / 飘字** | [Speck.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/Speck.java)、[SpellSprite.java](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/SpellSprite.java)、[FloatingText.kt](file:///home/thecatist/darkest-pixel-dungeon/core/src/main/java/com/egoal/darkestpixeldungeon/effects/FloatingText.kt) | `CellEmitter.get(cell).burst(Speck.factory(type), n)`、`SpellSprite.show(...)` | 治疗/星星/气泡/感叹/施法头顶图标/伤害飘字 |

---

## 3. 可复用粒子工厂（`effects/particles/`，配合 `CellEmitter` / `BlobEmitter` / `CharSprite.emitter()`）

| 粒子类 | 工厂入口 | 加色（`lightMode`） | 视觉主题 |
| :--- | :--- | :--- | :--- |
| `FireSprayParticle` | `FireSprayParticle.FACTORY` | 是（分离混合） | 喷火流、高温膨胀火团 |
| `FireSparkParticle` | `FireSparkParticle.FACTORY` | 是（分离混合） | 喷火飞溅火星 |
| `FireSmokeParticle` | `FireSmokeParticle.FACTORY` | 否 | 燃烧黑烟 |
| `FlameParticle` | `FlameParticle.FACTORY` | 是（分离混合） | 常规火焰、火把 |
| `SoulFlameParticle` | `SoulFlameParticle.FACTORY` | 是（分离混合） | 幽蓝灵魂火 |
| `BlastParticle` | `BlastParticle.FACTORY` | 是（分离混合） | 爆炸冲击碎屑 |
| `SparkParticle` | `SparkParticle.FACTORY` / `STATIC` | 是（分离混合） | 电火花、法术余晖 |
| `ElectronParticle` | `ElectronParticle.FACTORY` | 是（分离混合） | 电子/闪电粒子 |
| `EnergyParticle` | `EnergyParticle.FACTORY` | 是（分离混合） | 奥术能量聚散 |
| `ElmoParticle` | `ElmoParticle.FACTORY` | 是（分离混合） | 绿焰/圣艾尔摩之火 |
| `PurpleParticle` | `PurpleParticle.MISSILE` / `BURST` | 是（分离混合） | 紫色解离/虚空粒子 |
| `RainbowParticle` | `RainbowParticle.BURST` | 是（分离混合） | 棱光/彩虹爆发 |
| `ShadowParticle` | `ShadowParticle.MISSILE` / `UP` / `CURSE` | 否 | 暗影、诅咒黑雾 |
| `SmokeParticle` | `SmokeParticle.FACTORY` | 否 | 烟雾、毒气扩散 |
| `BloodParticle` | `BloodParticle.FACTORY` / `BURST` | 否 | 血液飞溅 |
| `PoisonParticle` | `PoisonParticle.MISSILE` / `SPLASH` | 否 | 毒素液滴 |
| `ColdSnowParticles` / `SnowParticle` | `SnowParticle.FACTORY` 等 | 否 | 冰霜雪片、寒气 |
| `EarthParticle` | `EarthParticle.FACTORY` | 否 | 碎石泥土 |
| `LeafParticle` | `LeafParticle.GENERAL` / `LEVEL_SPECIFIC` | 否 | 树叶飘落 |
| `FlowParticle` | `FlowParticle.FACTORY` | 否 | 水流波纹粒子 |
| `ShaftParticle` | `ShaftParticle.FACTORY` | 是（分离混合） | 竖直光柱尘埃 |
| `SacrificialParticle` | `SacrificialParticle.FACTORY` | 是（分离混合） | 祭坛献祭红光 |
| `WebParticle` / `WindParticle` / `WoolParticle` | 各自 `FACTORY` | 否 | 蛛网、风尘、羊毛 |
