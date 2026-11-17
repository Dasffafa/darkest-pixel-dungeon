package com.egoal.darkestpixeldungeon.levels.lighting

import com.egoal.darkestpixeldungeon.levels.Level

data class AmbientLightProfile(
    val r: Float,
    val g: Float,
    val b: Float
) {
    fun scaled(factor: Float): AmbientLightProfile =
        AmbientLightProfile(r * factor, g * factor, b * factor)

    companion object {
        fun forLevel(depth: Int, feeling: Level.Feeling): AmbientLightProfile {
            val base = when {
                depth <= 0 -> AmbientLightProfile(0.65f, 0.68f, 0.72f) // village
                depth <= 5 -> AmbientLightProfile(0.30f, 0.38f, 0.46f) // sewers: damp cool cyan-blue
                depth <= 10 -> AmbientLightProfile(0.28f, 0.30f, 0.38f) // prison: cold slate blue
                depth <= 15 -> AmbientLightProfile(0.36f, 0.29f, 0.24f) // caves: warm earthy brown
                depth <= 20 -> AmbientLightProfile(0.35f, 0.32f, 0.28f) // city: muted bronze
                else -> AmbientLightProfile(0.36f, 0.22f, 0.28f)        // halls: dim crimson-violet
            }
            return if (feeling == Level.Feeling.DARK) base.scaled(0.55f) else base
        }
    }
}

data class LightSource(
    var cell: Int,
    var colorR: Float,
    var colorG: Float,
    var colorB: Float,
    var radius: Float,
    var intensity: Float = 1f,
    var flickerAmp: Float = 0f,
    var flickerRadius: Float = 0f,
    var flickerSpeed: Float = 4f,
    var duration: Float = -1f,
    var blocksLos: Boolean = true,
    var phase: Float = (cell * 0.73f) % 6.28318f
) {
    var elapsed: Float = 0f

    val isTransient: Boolean
        get() = duration >= 0f

    val isExpired: Boolean
        get() = duration >= 0f && elapsed >= duration

    fun currentScale(): Float {
        if (duration <= 0f) return 1f
        return (1f - elapsed / duration).coerceIn(0f, 1f)
    }

    companion object {
        fun torch(cell: Int): LightSource = LightSource(
            cell = cell,
            colorR = 1.0f,
            colorG = 0.64f,
            colorB = 0.28f,
            radius = 3.6f,
            intensity = 0.95f,
            flickerAmp = 0.07f,
            flickerRadius = 0.18f,
            flickerSpeed = 4.5f
        )

        fun arcane(cell: Int, duration: Float = -1f): LightSource = LightSource(
            cell = cell,
            colorR = 0.35f,
            colorG = 0.68f,
            colorB = 1.0f,
            radius = 3.5f,
            intensity = 0.85f,
            flickerAmp = 0.05f,
            flickerRadius = 0.12f,
            flickerSpeed = 3.2f,
            duration = duration
        )

        fun fire(cell: Int, duration: Float = 0.35f): LightSource = LightSource(
            cell = cell,
            colorR = 1.0f,
            colorG = 0.50f,
            colorB = 0.15f,
            radius = 3.2f,
            intensity = 1.0f,
            duration = duration
        )

        fun frost(cell: Int, duration: Float = 0.35f): LightSource = LightSource(
            cell = cell,
            colorR = 0.35f,
            colorG = 0.82f,
            colorB = 1.0f,
            radius = 3.0f,
            intensity = 0.9f,
            duration = duration
        )

        fun holy(cell: Int, duration: Float = 0.35f): LightSource = LightSource(
            cell = cell,
            colorR = 1.0f,
            colorG = 0.92f,
            colorB = 0.55f,
            radius = 3.4f,
            intensity = 0.95f,
            duration = duration
        )

        fun fromColor(cell: Int, rgb: Int, radius: Float = 2.8f, intensity: Float = 0.85f, duration: Float = 0.3f): LightSource {
            val r = ((rgb shr 16) and 0xFF) / 255f
            val g = ((rgb shr 8) and 0xFF) / 255f
            val b = (rgb and 0xFF) / 255f
            return LightSource(
                cell = cell,
                colorR = r,
                colorG = g,
                colorB = b,
                radius = radius,
                intensity = intensity,
                duration = duration
            )
        }
    }
}
