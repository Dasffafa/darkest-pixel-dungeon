package dpdtest

import com.badlogic.gdx.ApplicationLogger
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.scenes.InterlevelScene
import com.watabou.noosa.Game
import com.watabou.utils.PlatformSupport
import org.lwjgl.glfw.GLFW

class HarnessGame(platformSupport: PlatformSupport) : DarkestPixelDungeon(platformSupport) {

    private var windowHandle: Long = 0L

    override fun create() {
        sceneClass = BootScene::class.java
        windowHandle = (Gdx.graphics as Lwjgl3Graphics).window.windowHandle
        GLFW.glfwSetWindowAttrib(windowHandle, GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE)
        checkWindowHidden("create()")

        val audioClass = Gdx.audio?.javaClass?.name ?: "null"
        if (!audioClass.lowercase().contains("mock")) {
            Result.finish(Result.Verdict.UNSAFE, "audio is not disabled: Gdx.audio is $audioClass", null)
        }

        Gdx.app.applicationLogger = object : ApplicationLogger {
            override fun log(tag: String, message: String) {
                Log.info("[$tag] $message")
            }

            override fun log(tag: String, message: String, exception: Throwable) {
                Log.info("[$tag] $message\n${Log.stackOf(exception)}")
            }

            override fun error(tag: String, message: String) {
                Log.error("[$tag] $message")
                if (tag == "dpd" && message.contains("Unhandled exception")) {
                    Result.finish(Result.Verdict.CRASH, "game reported unhandled error: $message", null)
                }
            }

            override fun error(tag: String, message: String, exception: Throwable) {
                Log.error("[$tag] $message\n${Log.stackOf(exception)}")
                Result.finish(Result.Verdict.CRASH, "game reported exception: $exception", Log.stackOf(exception))
            }

            override fun debug(tag: String, message: String) {
                Log.info("[$tag DEBUG] $message")
            }

            override fun debug(tag: String, message: String, exception: Throwable) {
                Log.info("[$tag DEBUG] $message\n${Log.stackOf(exception)}")
            }
        }

        super.create()

        Log.info("game created: version=${Game.version} versionCode=${Game.versionCode}")
    }

    private fun checkWindowHidden(where: String) {
        if (windowHandle == 0L) return
        val visible = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE
        val focused = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE
        val iconified = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE
        if (visible || focused || iconified) {
            GLFW.glfwHideWindow(windowHandle)
            Result.finish(
                Result.Verdict.UNSAFE,
                "window became visible/focused/iconified at $where",
                Log.dumpAllThreads()
            )
        }
    }

    override fun render() {
        checkWindowHidden("frame ${Log.frame}")
        if (scene is GameScene && !Game.switchingScene()) {
            Game.timeScale = timeScale
        } else if (scene !is InterlevelScene) {
            Game.timeScale = 1f
        }
        Log.frame++
        super.render()
    }

    companion object {
        @Volatile
        @JvmField
        var bootReady: Boolean = false

        @Volatile
        @JvmField
        var timeScale: Float = 1f
    }
}
