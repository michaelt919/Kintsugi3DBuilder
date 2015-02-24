#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray imageTextures;
uniform sampler2DArray depthTextures;
uniform int textureCount;
uniform float gamma;

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularColorEstimate;
uniform sampler2D roughnessEstimate;

uniform float guessSpecularWeight;
uniform vec3 guessSpecularColor;
uniform float specularRemovalFactor;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform CameraProjections
{
	mat4 cameraProjections[MAX_CAMERA_PROJECTION_COUNT];
};

uniform CameraProjectionIndices
{
	int cameraProjectionIndices[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normalMap;
layout(location = 2) out vec4 debug0;
layout(location = 3) out vec4 debug1;
layout(location = 4) out vec4 debug2;
layout(location = 5) out vec4 debug3;
layout(location = 6) out vec4 debug4;
layout(location = 7) out vec4 debug5;

vec4 getColor(int index)
{
    return pow(texture(imageTextures, vec3(fTexCoord, index)), vec4(gamma));
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
    return normalize(2 * dot(lightVector, normalVector) * normalVector - lightVector);
}

vec4 getSpecularColor()
{
    return pow(texture(specularColorEstimate, fTexCoord), vec4(gamma));
}

float getSpecularRoughness()
{
    return texture(roughnessEstimate, fTexCoord)[0];
}

vec4 getPreviousDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord), vec4(gamma));
}

vec3 getPreviousNormalVector()
{
    return texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1);
}

void main()
{
    vec4 prevDiffuseColor = getPreviousDiffuseColor();
    vec3 prevNormal = getPreviousNormalVector();
    vec4 specularColor = getSpecularColor();
    float specularRoughness = getSpecularRoughness();

    vec4 sumColor = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {   
       vec4 color = getColor(i);
       sumColor += color.a * vec4(color.rgb, 1.0);
    }
    float avgIntensity = (sumColor.r + sumColor.g + sumColor.b) / sumColor.a;
    
    mat4 dA = mat4(0);
    mat4 dB = mat4(0);
    
    vec4 diffuseSum = prevDiffuseColor.a * vec4(prevDiffuseColor.rgb, 1.0);
    for (int i = 0; i < textureCount; i++)
    {
        vec4 color = getColor(i);
        if (color.a > 0)
        {
            vec4 light = vec4(getLightVector(i), 1.0);
            vec4 colorRemainder;
            
            if (prevDiffuseColor.a > 0)
            {
                vec3 view = getViewVector(i);
                vec3 refl = getReflectionVector(prevNormal, light.xyz);
                float rDotV = dot(refl, view.xyz);
                vec3 specularContrib = specularColor.rgb * 
                    exp((rDotV - 1 / rDotV) / (2 * specularRoughness * specularRoughness));
                colorRemainder = vec4(max(vec3(0), 
                    color.rgb - specularRemovalFactor * specularContrib), color.a);
            }
            else if ((color.r > 0.0 || color.g > 0.0 || color.b > 0.0) && 
                (color.r + color.g + color.b) < avgIntensity)
            {
                // First fitting iteration only
                colorRemainder = color;
                diffuseSum += color.a * vec4(color.rgb, 1.0);
            }
            
            dA += color.a * outerProduct(light, light);
            dB += color.a * outerProduct(light, vec4(color.rgb, 0.0));
        }
    }
    vec3 diffuseAvg = diffuseSum.rgb / (diffuseSum.r + diffuseSum.g + diffuseSum.b);
    
    vec3 simpleWeights = diffuseSum.rgb / max(max(diffuseSum.r, diffuseSum.g), diffuseSum.b);
    vec3 rgbWeights;
    //float diffuseRemovalMult;
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
            //diffuseRemovalMult = adjustedFixedSpecWeight / (1 + adjustedFixedSpecWeight);
        }
        else
        {
            rgbWeights = simpleWeights;
            //diffuseRemovalMult = 0.0;
        }
    }
    else
    {
        rgbWeights = simpleWeights;
        //diffuseRemovalMult = 0.0;
    }
    
    vec4 dSolution = inverse(dA) * dB * vec4(rgbWeights, 0.0);
    float ambientIntensity = dSolution.w;
    float diffuseIntensity = length(dSolution.xyz);
    vec3 normal = normalize(dSolution.xyz);
    normalMap = vec4(normal * 0.5 + vec3(0.5), 1.0);
    diffuseColor = vec4(pow(diffuseAvg * diffuseIntensity, vec3(1 / gamma)), 1.0);
    
    debug0 = vec4(pow(diffuseAvg * dSolution.w, vec3(1 / gamma)), 1.0);
}
