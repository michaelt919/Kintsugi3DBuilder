#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray textures;
uniform int textureCount;
uniform float gamma;

uniform float guessSpecularWeight;
uniform vec3 guessSpecularColor;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normalMap;
layout(location = 2) out vec4 specularColor;
layout(location = 3) out vec4 specularRoughness;
layout(location = 4) out vec4 ambientColor;

vec4 getColor(int index)
{
    return pow(texture(textures, vec3(fTexCoord, index)), vec4(gamma));
}

vec3 getViewVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition);
}

vec3 getLightVector(int index)
{
    // TODO
    return getViewVector(index);
}

vec3 getReflectionVector(vec3 normalVector, vec3 lightVector)
{
    return 2 * dot(lightVector, normalVector) * normalVector - lightVector;
}

void main()
{
	vec4 sumColor = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {   
       vec4 color = getColor(i);
       sumColor += color.a * vec4(color.rgb, 1.0);
    }
    float avgIntensity = (sumColor.r + sumColor.g + sumColor.b) / sumColor.a;
    
    mat4 dA = mat4(0);
    mat4 dB = mat4(0);
    
    vec4 diffuseSum = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {
        vec4 color = getColor(i);
        if (color.a > 0)
        {
            vec4 view = vec4(getViewVector(i), 1.0);
            vec4 light = vec4(getLightVector(i), 1.0);
            
            dA += color.a * outerProduct(light, light);
            dB += color.a * outerProduct(light, vec4(color.rgb, 0.0));
            
            if ((color.r > 0.0 || color.g > 0.0 || color.b > 0.0) && 
                (color.r + color.g + color.b) < avgIntensity)
            {
                diffuseSum += color.a * vec4(color.rgb, 1.0);
            }
        }
    }
    vec3 diffuseAvg = diffuseSum.rgb / (diffuseSum.r + diffuseSum.g + diffuseSum.b);
    
    vec3 simpleWeights = diffuseSum.rgb / max(max(diffuseSum.r, diffuseSum.g), diffuseSum.b);
    vec3 rgbWeights;
    if (guessSpecularWeight > 0)
    {
        vec3 ortho = cross(diffuseAvg, guessSpecularColor);
        if (ortho.x > 0 || ortho.y > 0 || ortho.z > 0)
        {
            mat3 colorBasis = mat3(diffuseAvg, guessSpecularColor, ortho);
            float adjustedFixedSpecWeight = guessSpecularWeight * length(ortho) / 
                (length(diffuseAvg) * length(guessSpecularColor));
            rgbWeights = (adjustedFixedSpecWeight * transpose(inverse(colorBasis))[0] + simpleWeights) / 
                (1 + adjustedFixedSpecWeight);
        }
        else
        {
            rgbWeights = simpleWeights;
        }
    }
    else
    {
        rgbWeights = simpleWeights;
    }
    
    vec4 dSolution = inverse(dA) * dB * vec4(rgbWeights, 0.0);
    float ambientIntensity = dSolution.w;
    float diffuseIntensity = length(dSolution.xyz);
    vec3 normal = dSolution.xyz / diffuseIntensity;
    normalMap = vec4(normal * 0.5 + vec3(0.5), 1.0);
    ambientColor = vec4(pow(diffuseAvg * dSolution.w, vec3(1 / gamma)), 1.0);
    diffuseColor = vec4(pow(diffuseAvg * diffuseIntensity, vec3(1 / gamma)), 1.0);
    
    vec4 specularSum = vec4(0);
    mat2 sA = mat2(0);
    vec2 sB = vec2(0);
    
    for (int i = 0; i < textureCount; i++)
    {
        vec4 color = getColor(i);
        if (color.a > 0)
        {
            vec3 view = getViewVector(i);
            vec3 light = getLightVector(i);
            
            vec3 diffuseContrib = diffuseColor.rgb * max(0, dot(light, normal));
            vec4 colorRemainder = vec4(max(vec3(0), color.rgb - diffuseContrib), color.a);
            float intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
            
            if (intensity > 0.0)
            {
                vec3 reflection = getReflectionVector(normal, light);
                float rDotV = dot(reflection, view);
                if (rDotV > 0.0)
                {
                    float x = rDotV - 1 / rDotV;
                    //sA += color.a * intensity / log(intensity) * outerProduct(vec2(x, 1), vec2(x, 1));
                    //sB += color.a * intensity * vec2(x, 1);
                    sA += color.a * outerProduct(vec2(x, 1), vec2(x, 1));
                    sB += color.a * log(intensity) * vec2(x, 1);
                    specularSum += colorRemainder.a * vec4(colorRemainder.rgb, 1.0);
                }
            }
        }
    }
    
    vec2 sSolution = inverse(sA) * sB;
    vec3 specularAvg = specularSum.rgb / (diffuseSum.r + diffuseSum.g + diffuseSum.b);
    specularColor = vec4(pow(exp(sSolution[1]) * specularAvg, vec3(1 / gamma)), 1.0);
    specularRoughness = vec4(vec3(inversesqrt(2*sSolution[0])), 1.0);
}
