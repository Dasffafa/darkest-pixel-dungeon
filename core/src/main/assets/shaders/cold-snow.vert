// Darkest Pixel Dungeon; GPL-3.0-or-later.
#ifdef GL_ES
precision highp float;
#endif
uniform mat4 uCamera;
uniform mat4 uModel;
uniform vec2 uSpriteSize;
uniform float uFacePhase;
uniform float uDepth;
uniform float uBaseScale;
uniform float uParticleSize;
uniform vec2 uFlipAxis;
attribute vec4 aXYZW;
attribute vec2 aUV;
varying vec2 vUV;
void main() {
    float rawFacing = cos(uFacePhase);
    float facing = rawFacing < 0.0 ? min(rawFacing, -0.025) : max(rawFacing, 0.025);
    float perspective = uParticleSize * uBaseScale * (0.72 + uDepth * 0.48) * 0.6;
    vec4 local = aXYZW;
    vec2 center = uSpriteSize * 0.5;
    vec2 centered = local.xy - center;
    vec2 axis = normalize(uFlipAxis);
    vec2 alongAxis = axis * dot(centered, axis);
    vec2 acrossAxis = centered - alongAxis;
    local.xy = (alongAxis + acrossAxis * facing) * perspective + center;
    gl_Position = uCamera * uModel * local;
    vUV = aUV;
}
