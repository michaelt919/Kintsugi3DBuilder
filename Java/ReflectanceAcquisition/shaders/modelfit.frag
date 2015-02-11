#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray textures;
uniform int textureCount;
uniform float gamma;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 ambientEstimate;
layout(location = 1) out vec4 diffuseEstimate;
layout(location = 2) out vec4 normalEstimate;

void main()
{
	vec4 sumColor = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {   
       vec4 color = pow(texture(textures, vec3(fTexCoord, i)), vec4(gamma));
       sumColor += color.a * vec4(color.rgb, 1.0);
    }
    float avgIntensity = (sumColor.r + sumColor.g + sumColor.b) / sumColor.a;
    
    mat4 a = mat4(0);
    mat4 b = mat4(0);
    
    vec4 diffuseSum = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {
        vec4 color = pow(texture(textures, vec3(fTexCoord, i)), vec4(gamma));
        if (color.a > 0)
        {
            vec4 camera = vec4(normalize(transpose(mat3(cameraPoses[i])) * -cameraPoses[i][3].xyz - fPosition), 1.0);
            vec4 light = camera; // TODO
            
            a += color.a * outerProduct(light, light);
            b += color.a * outerProduct(light, vec4(color.rgb, 0.0));
            
            if ((color.r > 0.0 || color.g > 0.0 || color.b > 0.0) && 
                (color.r + color.g + color.b) < avgIntensity)
            {
                diffuseSum += color.a * vec4(color.rgb, 1.0);
            }
        }
    }
    
    vec4 c = inverse(a) * b * vec4(diffuseSum.rgb / max(max(diffuseSum.r, diffuseSum.g), diffuseSum.b), 0.0);
    float ambientIntensity = c.w;
    float diffuseIntensity = length(c.xyz);
    normalEstimate = vec4(c.xyz / diffuseIntensity * 0.5 + vec3(0.5), 1.0);
    vec3 diffuseAvg = diffuseSum.rgb / (diffuseSum.r + diffuseSum.g + diffuseSum.b);
    ambientEstimate = pow(vec4(diffuseAvg * ambientIntensity, 1.0), vec4(1 / gamma));
    diffuseEstimate = pow(vec4(diffuseAvg * diffuseIntensity, 1.0), vec4(1 / gamma));
}
