package dpdtest

import com.watabou.input.Keys
import com.watabou.noosa.Game
import com.watabou.noosa.Scene
import java.util.concurrent.atomic.AtomicReference

class H {
    @Volatile
    private var renderThread: Thread? = null

    fun noteRenderThread(t: Thread?) {
        renderThread = t
    }

    fun frames(count: Int) {
        val target = Log.frame + count.coerceAtLeast(1)
        val deadline = System.nanoTime() + 30_000_000_000L
        while (Log.frame < target) {
            if (System.nanoTime() > deadline) {
                throw HarnessError("timed out waiting for $count render frames")
            }
            Thread.sleep(2)
        }
    }

    fun <T> onRender(block: () -> T): T {
        val ref = AtomicReference<T>()
        val err = AtomicReference<Throwable?>()
        Game.runOnRenderThreadAndWait {
            try {
                ref.set(block())
            } catch (t: Throwable) {
                err.set(t)
            }
        }
        err.get()?.let { throw it }
        return ref.get()
    }

    fun switchScene(sceneClass: Class<out Scene>) {
        onRender {
            Game.switchScene(sceneClass)
        }
        val deadline = System.nanoTime() + 15_000_000_000L
        while (!sceneClass.isInstance(Game.scene()) || Game.switchingScene()) {
            if (System.nanoTime() > deadline) {
                throw HarnessError("timed out switching to ${sceneClass.simpleName}")
            }
            Thread.sleep(5)
        }
        frames(3)
    }

    fun pressKey(keyCode: Int) {
        onRender {
            Keys.event.dispatch(Keys.Key(keyCode, true))
        }
        frames(1)
        onRender {
            Keys.event.dispatch(Keys.Key(keyCode, false))
        }
        frames(1)
    }

    fun check(condition: Boolean, message: String) {
        if (condition) {
            Result.recordPass(message)
        } else {
            Result.recordFail(message)
            throw CheckFailed(message)
        }
    }

    fun note(message: String) {
        Result.note(message)
    }
}
