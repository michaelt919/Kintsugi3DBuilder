#version 330

// A small shader meant to extract the metallicity (b) value from an orm map
// when a metallic map image file does not exist

in vec2 fTexCoord;

uniform sampler2D orm;

layout(location = 2) out vec4 metallicOut; // Meant to be used along side estimateAlbedoORM::framebuffer

void main() {
    float metallicity = texture(orm, fTexCoord).b;

    metallicOut = vec4(vec3(metallicity), 1.0);
}
