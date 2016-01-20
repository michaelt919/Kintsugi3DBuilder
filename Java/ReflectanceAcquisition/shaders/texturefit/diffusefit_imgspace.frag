#version 330

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normalMap;
layout(location = 2) out vec4 ambient;
layout(location = 3) out vec4 debug;

#include "../reflectance/reflectance.glsl"
#include "../reflectance/imgspace.glsl"
#include "../texturefit/diffusefit.glsl"

#line 17 0

void main()
{
    DiffuseFit fit = fitDiffuse();
    diffuseColor = vec4(pow(fit.color, vec3(1 / gamma)), 1.0);
    normalMap = vec4(fit.normal * 0.5 + vec3(0.5), 1.0);
    ambient = vec4(pow(fit.ambient, vec3(1 / gamma)), 1.0);
}
