/*
 * Darkest Pixel Dungeon
 * Copyright (C) 2018-2026 Darkest Pixel Dungeon contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.egoal.darkestpixeldungeon.effects.shaders

/** Visual-only Shader for world-space snow particles. */
class ColdSnowScript : ResourceNoosaScript("cold-snow") {
    private val spriteSize = uniform("uSpriteSize")
    private val age = uniform("uAge")
    private val lifespan = uniform("uLifespan")
    private val facePhase = uniform("uFacePhase")
    private val depth = uniform("uDepth")
    private val baseScale = uniform("uBaseScale")
    private val particleSize = uniform("uParticleSize")
    private val flipAxis = uniform("uFlipAxis")

    fun prepare(
        width: Float,
        height: Float,
        age: Float,
        lifespan: Float,
        facePhase: Float,
        depth: Float,
        baseScale: Float,
        particleSize: Float,
        axisX: Float,
        axisY: Float
    ) {
        spriteSize.value2f(width, height)
        this.age.value1f(age)
        this.lifespan.value1f(lifespan)
        this.facePhase.value1f(facePhase)
        this.depth.value1f(depth)
        this.baseScale.value1f(baseScale)
        this.particleSize.value1f(particleSize)
        flipAxis.value2f(axisX, axisY)
    }
}
