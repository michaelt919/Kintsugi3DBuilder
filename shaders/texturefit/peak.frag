#version 330

uniform sampler2D specularTexture;
uniform sampler2D roughnessTexture;

#include "../colorappearance/linearize.glsl"

#line 11 0

in vec2 fTexCoord;
out vec4 fragColor;

void main() 
{
    vec4 specularColor = texture(specularTexture, fTexCoord);
    vec4 sqrtroughness = texture(roughnessTexture, fTexCoord);
    vec3 roughness = sqrtroughness.xyz * sqrtroughness.xyz;
    fragColor = vec4(pow(max(vec3(0.0), xyzToRGB(rgbToXYZ(pow(specularColor.rgb, vec3(gamma))) / (4 * roughness * roughness))), vec3(1.0 / gamma)),
        specularColor.a * sqrtroughness.a);
}
