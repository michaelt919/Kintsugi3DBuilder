#version 330

uniform sampler2D specularTexture;
uniform sampler2D roughnessTexture;

uniform float gamma;

in vec2 fTexCoord;
out vec4 fragColor;

void main() 
{
    vec4 specularColor = texture(specularTexture, fTexCoord);
    vec4 roughness = texture(roughnessTexture, fTexCoord);
    fragColor = vec4(specularColor.rgb * pow(2 * roughness.rgb, vec3(-2.0 / gamma)), specularColor.a * roughness.a);
}
