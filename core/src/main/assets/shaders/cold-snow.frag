// Darkest Pixel Dungeon; GPL-3.0-or-later.
#ifdef GL_ES
precision highp float;
#endif
varying vec2 vUV;
uniform sampler2D uTex;
uniform float uAge;
uniform float uLifespan;
void main() {
    vec4 texel = texture2D(uTex, vUV);
    float progress = clamp(uAge / max(uLifespan, 0.001), 0.0, 1.0);
    float fadeIn = smoothstep(0.0, 0.18, progress);
    float fadeOut = 1.0 - smoothstep(0.35, 1.0, progress);
    float fade = fadeIn * fadeOut;
    gl_FragColor = vec4(0.949, 0.941, 1.0, texel.a * fade);
}
