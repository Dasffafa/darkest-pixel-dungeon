// Adapted from Radish Pixel Dungeon's Dice Mage cut shader (GPL-3.0-or-later).
#ifdef GL_ES
precision highp float;
#endif
varying vec2 vUV;
uniform sampler2D uTex;
uniform vec4 uColorM;
uniform vec4 uColorA;
uniform vec4 uCutLine;
uniform float uCutProgress;
uniform float uCutSide;
uniform vec4 uFrame;
void main() {
    vec4 col = texture2D(uTex, vUV) * uColorM + uColorA;
    vec2 localUV = (vUV - uFrame.xy) / uFrame.zw;
    vec2 start = uCutLine.xy;
    vec2 dir = normalize(uCutLine.zw);
    float d00 = dot(vec2(0.0, 0.0) - start, dir);
    float d10 = dot(vec2(1.0, 0.0) - start, dir);
    float d01 = dot(vec2(0.0, 1.0) - start, dir);
    float d11 = dot(vec2(1.0, 1.0) - start, dir);
    float halfSpan = max(max(d00, d10), max(d01, d11));
    halfSpan = max(halfSpan, -min(min(d00, d10), min(d01, d11)));
    float dist = dot(localUV - start, dir) / halfSpan;
    float edge = abs(dist) - uCutProgress;
    if (edge < 0.0) col.a = 0.0;
    else if (edge < 0.04) col.rgb = mix(col.rgb, vec3(1.0, 0.85, 0.65), 1.0 - edge / 0.04);
    gl_FragColor = col;
}
