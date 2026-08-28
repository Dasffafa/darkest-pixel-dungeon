/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2019 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.watabou.noosa;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.watabou.glscripts.Script;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.input.InputHandler;
import com.watabou.input.Keys;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.SystemTime;
import com.watabou.utils.PlatformSupport;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Game implements ApplicationListener {

    public static Game instance;

    public static int dispWidth;
    public static int dispHeight;
    public static int width;
    public static int height;
    public static float density = 1;
    public static String version;
    public static int versionCode;
    public static PlatformSupport platform;

    protected Scene scene;
    protected Scene requestedScene;
    protected boolean requestedReset = true;
    protected SceneChangeCallback onChange;
    protected static Class<? extends Scene> sceneClass;

    protected long now;
    protected long step;

    public static float timeScale = 1f;
    public static float elapsed = 0f;
    public static float timeTotal = 0f;

    protected InputHandler inputHandler;
    private boolean paused;
    private Thread renderThread;

    public Game(Class<? extends Scene> initialScene) {
        this(initialScene, new PlatformSupport());
    }

    public Game(Class<? extends Scene> initialScene, PlatformSupport platformSupport) {
        sceneClass = initialScene;
        instance = this;
        platform = platformSupport;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void create() {
        renderThread = Thread.currentThread();
        density = Gdx.graphics.getDensity();
        dispWidth = Gdx.graphics.getDisplayMode().width;
        dispHeight = Gdx.graphics.getDisplayMode().height;

        inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(inputHandler);
        Gdx.input.setCatchKey(Keys.BACK, true);
        Gdx.input.setCatchKey(Keys.MENU, true);

        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        TextureCache.reload();
        RenderedText.reloadCache();
        Vertexbuffer.refreshAllBuffers();
    }

    @Override
    public void resize(int width, int height) {
        Gdx.gl.glViewport(0, 0, width, height);
        dispWidth = width;
        dispHeight = height;

        if (height != Game.height || width != Game.width) {
            Game.width = width;
            Game.height = height;
            resetScene();
        }
    }

    @Override
    public void render() {
        renderThread = Thread.currentThread();
        if (width == 0 || height == 0) {
            return;
        }

        SystemTime.tick();
        long rightNow = SystemTime.now;
        step = now == 0 ? 0 : rightNow - now;
        now = rightNow;
        step();

        NoosaScript.get().resetCamera();
        NoosaScriptNoLighting.get().resetCamera();
        Gdx.gl.glDisable(Gdx.gl.GL_SCISSOR_TEST);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
        draw();
        Gdx.gl.glFlush();
    }

    @Override
    public void pause() {
        paused = true;
        if (scene != null) {
            scene.pause();
        }
        Script.reset();
        Music.INSTANCE.pause();
        Sample.INSTANCE.pause();
    }

    @Override
    public void resume() {
        paused = false;
        now = 0;
        if (scene != null) {
            scene.resume();
        }
        Music.INSTANCE.resume();
        Sample.INSTANCE.resume();
    }

    @Override
    public void dispose() {
        destroyGame();
        Music.INSTANCE.mute();
        Sample.INSTANCE.reset();
    }

    public void finish() {
        Gdx.app.exit();
    }

    protected void destroyGame() {
        if (scene != null) {
            scene.destroy();
            scene = null;
        }
    }

    public static void resetScene() {
        switchScene(sceneClass);
    }

    public static void switchScene(Class<? extends Scene> scene) {
        switchScene(scene, null);
    }

    public static void switchScene(Class<? extends Scene> scene, SceneChangeCallback callback) {
        sceneClass = scene;
        instance.requestedReset = true;
        instance.onChange = callback;
    }

    public static Scene scene() {
        return instance.scene;
    }

    protected void step() {
        if (requestedReset) {
            requestedReset = false;
            try {
                requestedScene = sceneClass.newInstance();
                switchScene();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        update();
    }

    protected void draw() {
        if (scene != null) {
            scene.draw();
        }
    }

    protected void switchScene() {
        Camera.reset();
        if (scene != null) {
            scene.destroy();
        }
        scene = requestedScene;
        if (onChange != null) onChange.beforeCreate();
        scene.create();
        if (onChange != null) onChange.afterCreate();
        onChange = null;

        elapsed = 0f;
        timeScale = 1f;
        timeTotal = 0f;
    }

    protected void update() {
        elapsed = timeScale * step * 0.001f;
        timeTotal += elapsed;
        inputHandler.processAllEvents();
        scene.update();
        Camera.updateAll();
    }

    public static void vibrate(int milliseconds) {
        Gdx.input.vibrate(milliseconds);
    }

    public static boolean isOnRenderThread() {
        return instance != null && Thread.currentThread() == instance.renderThread;
    }

    public static void runOnRenderThreadAndWait(Runnable runnable) {
        if (isOnRenderThread()) {
            runnable.run();
            return;
        }

        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Gdx.app.postRunnable(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });

        try {
            completed.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the render thread", e);
        }

        Throwable throwable = failure.get();
        if (throwable instanceof RuntimeException) throw (RuntimeException) throwable;
        if (throwable instanceof Error) throw (Error) throwable;
        if (throwable != null) throw new RuntimeException(throwable);
    }

    public interface SceneChangeCallback {
        void beforeCreate();
        void afterCreate();
    }
}
