package com.watabou.noosa

import com.watabou.glscripts.Script
import com.watabou.glwrap.Uniform

class LightingScript : NoosaScript() {

    val uQuantizeSteps: Uniform = uniform("uQuantizeSteps")

    override fun lighting(rm: Float, gm: Float, bm: Float, am: Float, ra: Float, ga: Float, ba: Float, aa: Float) {
        // Light color is baked directly into the lightmap texture.
    }

    override fun use() {
        resetCamera()
        super.use()
        uQuantizeSteps.value1f(quantizeSteps)
    }

    override fun shader(): String = SHADER

    companion object {
        @JvmField
        var quantizeSteps: Float = 0f

        @JvmStatic
        fun get(): LightingScript = Script.use(LightingScript::class.java)

        private const val SHADER =
            "uniform mat4 uCamera;\n" +
            "uniform mat4 uModel;\n" +
            "attribute vec4 aXYZW;\n" +
            "attribute vec2 aUV;\n" +
            "varying vec2 vUV;\n" +
            "void main() {\n" +
            "  gl_Position = uCamera * uModel * aXYZW;\n" +
            "  vUV = aUV;\n" +
            "}\n" +
            "//\n" +
            "#ifdef GL_ES\n" +
            "  precision mediump float;\n" +
            "#endif\n" +
            "varying vec2 vUV;\n" +
            "uniform sampler2D uTex;\n" +
            "uniform float uQuantizeSteps;\n" +
            "void main() {\n" +
            "  vec4 c = texture2D(uTex, vUV);\n" +
            "  if (uQuantizeSteps > 0.5) {\n" +
            "    c.rgb = floor(c.rgb * uQuantizeSteps + 0.5) / uQuantizeSteps;\n" +
            "  }\n" +
            "  gl_FragColor = c;\n" +
            "}\n"
    }
}
