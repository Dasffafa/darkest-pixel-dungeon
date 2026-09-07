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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.watabou.noosa;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.watabou.glwrap.Matrix;
import com.watabou.glwrap.Quad;

import java.nio.FloatBuffer;
import java.util.HashMap;

/** LibGDX FreeType-backed text renderer, adapted from Shattered b985ed3b. */
public class RenderedText extends Image {

    private BitmapFont font;
    private int size;
    private String text;

    public RenderedText() { }
    public RenderedText(int size) { this.size = size; }
    public RenderedText(String text, int size) { this.size = size; text(text); }

    public void text(String text) { this.text = text; measure(); }
    public String text() { return text; }
    public void size(int size) { this.size = size; measure(); }
    public float baseLine() { return size * scale.y; }

    private void measure() {
        if (!Game.isOnRenderThread()) {
            Game.runOnRenderThreadAndWait(this::measure);
            return;
        }
        measureOnRenderThread();
    }

    private synchronized void measureOnRenderThread() {
        if (text == null || text.isEmpty()) {
            text = "";
            width = height = 0;
            visible = false;
            return;
        }
        visible = true;
        font = Game.platform.getFont(size, text);
        GlyphLayout layout = new GlyphLayout(font, text);
        width = layout.width;
        height = size * 1.375f;
    }

    @Override
    protected void updateMatrix() {
        super.updateMatrix();
        Matrix.translate(matrix, 0, Math.round((baseLine() * 0.1f) / scale.y));
    }

    @Override
    public synchronized void draw() {
        if (!visible || font == null) return;
        updateMatrix();
        TextRenderBatch.current = this;
        font.draw(TextRenderBatch.INSTANCE, text, 0, 0);
    }

    public static void clearCache() { Game.platform.resetFonts(); }
    public static void reloadCache() { Game.platform.resetFonts(); }
    public static void setFont(String asset) { Game.platform.setFont(asset); }

    private static class TextRenderBatch implements Batch {
        private static final TextRenderBatch INSTANCE = new TextRenderBatch();
        private static RenderedText current;
        private final float[] vertices = new float[16];
        private final HashMap<Integer, FloatBuffer> buffers = new HashMap<Integer, FloatBuffer>();

        @Override
        public void draw(Texture texture, float[] source, int offset, int count) {
            FloatBuffer target = buffers.get(count / 20);
            if (target == null) {
                target = Quad.createSet(count / 20);
                buffers.put(count / 20, target);
            }
            target.position(0);
            for (int i = offset; i < offset + count; i += 20) {
                vertices[0]=source[i]; vertices[1]=source[i+1]; vertices[2]=source[i+3]; vertices[3]=source[i+4];
                vertices[4]=source[i+5]; vertices[5]=source[i+6]; vertices[6]=source[i+8]; vertices[7]=source[i+9];
                vertices[8]=source[i+10]; vertices[9]=source[i+11]; vertices[10]=source[i+13]; vertices[11]=source[i+14];
                vertices[12]=source[i+15]; vertices[13]=source[i+16]; vertices[14]=source[i+18]; vertices[15]=source[i+19];
                target.put(vertices);
            }
            target.position(0);
            NoosaScript script = NoosaScript.get();
            texture.bind();
            com.watabou.glwrap.Texture.clear();
            script.camera(current.camera());
            script.uModel.valueM4(current.matrix);
            script.lighting(current.rm, current.gm, current.bm, current.am,
                    current.ra, current.ga, current.ba, current.aa);
            script.drawQuadSet(target, count / 20);
        }

        public void begin() { } public void end() { } public void flush() { } public void dispose() { }
        public void setColor(Color c) { } public void setColor(float r,float g,float b,float a) { }
        public Color getColor() { return Color.WHITE; } public void setPackedColor(float c) { }
        public float getPackedColor() { return Color.WHITE_FLOAT_BITS; }
        public void disableBlending() { } public void enableBlending() { }
        public void setBlendFunction(int s,int d) { }
        public void setBlendFunctionSeparate(int sc,int dc,int sa,int da) { }
        public int getBlendSrcFunc() { return 0; } public int getBlendDstFunc() { return 0; }
        public int getBlendSrcFuncAlpha() { return 0; } public int getBlendDstFuncAlpha() { return 0; }
        public Matrix4 getProjectionMatrix() { return null; } public Matrix4 getTransformMatrix() { return null; }
        public void setProjectionMatrix(Matrix4 m) { } public void setTransformMatrix(Matrix4 m) { }
        public void setShader(ShaderProgram s) { } public ShaderProgram getShader() { return null; }
        public boolean isBlendingEnabled() { return true; } public boolean isDrawing() { return true; }
        public void draw(Texture t,float x,float y,float ox,float oy,float w,float h,float sx,float sy,float r,int a,int b,int c,int d,boolean fx,boolean fy) { }
        public void draw(Texture t,float x,float y,float w,float h,int a,int b,int c,int d,boolean fx,boolean fy) { }
        public void draw(Texture t,float x,float y,int a,int b,int c,int d) { }
        public void draw(Texture t,float x,float y,float w,float h,float u,float v,float u2,float v2) { }
        public void draw(Texture t,float x,float y) { } public void draw(Texture t,float x,float y,float w,float h) { }
        public void draw(TextureRegion r,float x,float y) { } public void draw(TextureRegion r,float x,float y,float w,float h) { }
        public void draw(TextureRegion r,float x,float y,float ox,float oy,float w,float h,float sx,float sy,float rot) { }
        public void draw(TextureRegion r,float x,float y,float ox,float oy,float w,float h,float sx,float sy,float rot,boolean cw) { }
        public void draw(TextureRegion r,float w,float h,Affine2 transform) { }
    }
}
