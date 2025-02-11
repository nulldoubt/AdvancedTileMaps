#ifdef GL_ES
    #define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif
in LOWP vec4 v_color;
in vec2 v_texCoords;
in vec2 v_worldPosition;

uniform sampler2D u_texture;
uniform sampler2D u_overlay;
uniform float u_scale;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(u_texture, v_texCoords);
    vec2 overlayCoords = v_worldPosition * u_scale;
    vec4 finalColor = mix(baseColor, texture(u_overlay, vec2(overlayCoords.x, 1.0 - overlayCoords.y)), floor(baseColor.r));
    fragColor = v_color * finalColor;
}
