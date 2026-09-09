[English](./README.md)

## Darkest Pixel Dungeon(dpd)

一款地牢肉鸽（roguelike）RPG，包含随机生成的关卡、物品、敌人和陷阱。截图见下方。

基于 [pixel dungeon](https://github.com/watabou/pixel-dungeon) 与 [shattered pixel dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) 的源代码。

查看 [darkest pixel dungeon wiki](https://pixeldungeon.fandom.com/wiki/Mod-Darkest_Pixel_Dungeon)

下载 [最新版本](https://github.com/egoal/darkest-pixel-dungeon/releases)

翻译帮助 [transifex](https://www.transifex.com/darkest-pixel-dungeon-localization/)

---

当前特性与亮点：
* 压力系统（pressure system）。
* 天赋/特技机制（perk mechanics）。
* 重做的伤害流程（damage proc）。
* 新职业：女巫（Sorceress）、放逐者（Exile）。
* 新敌人。
* 新神器、装备及许多其他物品。
* 新 NPC 与任务。

背景音乐来自（致谢）[yet another pixel dungeon](https://github.com/ConsideredHamster/YetAnotherPixelDungeon)

---

开发日志（中文）在 [dev/dev-notes](./dev/dev-notes)

欢迎联系我，中英文皆可。

---

截图：

![](dev/screenshots/00.png)
![](dev/screenshots/05.png)

---

## 编译

### Desktop Gradle 任务

### 正常运行

```bash
./gradlew :desktop:run
```

通过 LWJGL3 启动桌面版，并自动注入版本信息。

### 调试模式运行

```bash
./gradlew :desktop:runDebug
```

设置系统属性：

```
dpd.debug=true
```

在该选项开启期间创建的英雄会启用调试物品。

### 构建可执行 JAR

```bash
./gradlew :desktop:release
```

生成包含运行依赖的可执行 fat JAR：

```
desktop/build/libs/darkest-pixel-dungeon-<version>-desktop.jar
```

可以通过以下命令启动：

```bash
java -jar desktop/build/libs/darkest-pixel-dungeon-<version>-desktop.jar
```

### 构建 Windows 独立发行包

```bash
./gradlew :desktop:packrWindows
```

该任务会：

1. 下载 Windows x64 Temurin JDK 21。
2. 使用 jdeps 分析所需 Java 模块。
3. 使用 jlink 创建裁剪后的 Windows JRE。
4. 使用 Packr 生成 Windows 可执行文件。

产物位于：

```
desktop/build/packr/dist/
```

如果该任务由于网络问题下载持续失败，请从 [Adoptium Temurin 官方下载](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64) 手动下载 Windows x64 JDK 21 压缩包，并放到 `desktop/build/packr/windows-jdk.zip`。作者使用的版本是 Windows JDK 21 的 zip 压缩包。注意，无论你使用什么操作系统，都应下载 Windows 版 JDK，因为该任务是为交叉编译 Windows 发行版而设计的。

---

### Android Studio 编译

### 前置条件

- 安装 Android Studio 与 Android SDK（JDK 由 Android Studio 自带或已配置）。
- 原生 LibGDX `.so` 库会在构建时自动拷贝，无需手动处理。


### 运行 debug APK（default-apk）

1. 用 Android Studio 打开项目根目录（Gradle 项目，Android 模块名为 `android`）。
2. 准备模拟器（AVD）或连接已开启 USB 调试的真机。
3. 顶部工具栏选择 `android` 运行配置。
4. 点击 Run（绿色三角）。Android Studio 会执行 `:android:assembleDebug`，用调试密钥自动签名并安装应用（`com.egoal.darkestpixeldungeon`）。
5. Debug APK 产物位于 `android/build/outputs/apk/debug/android-debug.apk`。

### 使用自己的签名编译新版本

项目未为 release 预配置签名，请使用 Android Studio 的签名向导：

1. 菜单 **Build → Generate Signed App Bundle or APK...**。
2. 选择 **Android App Bundle**（用于上架 Google Play）或 **APK**。
3. 选择你的 keystore：选中已有 `.jks`/`.keystore` 并填写 store password、key alias、key password；或点击 **Create new...** 新建一个。
4. build variant 选择 **release**。
5. 完成后产物位于：
   - App Bundle：`android/build/outputs/bundle/release/android-release.aab`
   - APK：`android/build/outputs/apk/release/android-release.apk`

版本名/版本号取自根目录 `build.gradle` 中的 `rootProject.ext.appVersionName` / `rootProject.ext.appVersionCode` —— 发布新版本前先更新它们。