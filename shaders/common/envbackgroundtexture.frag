#version 330

in vec2 fTexCoord;
layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

uniform int objectID;
uniform samplerCube env;
uniform mat4 model_view;
uniform mat4 projection;
uniform mat4 envMapMatrix;
uniform vec3 envMapIntensity;
uniform float gamma;

#define PI 3.1415926535897932384626433832795

void main()
{
    vec4 unprojected = inverse(projection) * vec4(fTexCoord * 2 - vec2(1), 0, 1);

    vec3 viewDir =
        normalize((envMapMatrix * inverse(model_view) * vec4(unprojected.xyz / unprojected.w, 0.0)).xyz);

    fragColor = vec4(envMapIntensity * pow(texture(env, viewDir).rgb, vec3(1.0 / gamma)), 1.0);

    // Use this version for a blurred background
    //fragColor = vec4(envMapIntensity * pow(textureLod(env, viewDir, 3).rgb, vec3(1.0 / gamma)), 1.0);

    fragObjectID = objectID;
}