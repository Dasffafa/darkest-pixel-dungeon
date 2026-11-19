#ifdef GL_ES
  precision highp float;
#endif

varying vec2 vUV;
uniform sampler2D uTex;
uniform float uAlpha;
uniform vec4 uFrame;

void main() {
  vec4 col = texture2D(uTex, vUV);
  if (col.a > 0.0) {
    vec2 ratioPos = (vUV - uFrame.xy) / uFrame.zw;
    float distFromCenter = abs(ratioPos.x - 0.5) * 2.0;
    if (distFromCenter <= uAlpha) {
      float lum = dot(col.rgb, vec3(0.299, 0.587, 0.114));
      vec3 gold = mix(vec3(0.45, 0.28, 0.02), vec3(1.0, 0.88, 0.25), clamp(lum * 1.4, 0.0, 1.0));
      col.rgb = gold;
    }
  }
  gl_FragColor = col;
}
