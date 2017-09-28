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
    vec4 roughness = texture(roughnessTexture, fTexCoord);
    fragColor = vec4(pow(xyzToRGB(rgbToXYZ(pow(specularColor.rgb, vec3(gamma))) / (4 * roughness.xyz * roughness.xyz)), vec3(1.0 / gamma)),
        specularColor.a * roughness.a);
}
