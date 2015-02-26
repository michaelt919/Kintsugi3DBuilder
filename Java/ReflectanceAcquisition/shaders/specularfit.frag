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
uniform sampler2D previousError;

uniform float guessSpecularRoughness;
uniform int multisampleRange;
//uniform float multisampleDistanceFactor;
uniform float expectedWeightSum;
uniform float diffuseRemovalFactor;
uniform float specularRoughnessCap;

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

layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 specularRoughness;
layout(location = 2) out vec4 error;
layout(location = 3) out vec4 debug1;
layout(location = 4) out vec4 debug2;
layout(location = 5) out vec4 debug3;
layout(location = 6) out vec4 debug4;
layout(location = 7) out vec4 debug5;

vec4 getColor(int index)
{
    return pow(texture(imageTextures, vec3(fTexCoord, index)), vec4(gamma));
}

vec4 getColorWithOffset(int index, ivec2 offset)
{
    return pow(textureOffset(imageTextures, vec3(fTexCoord, index), offset), vec4(gamma));
}

vec3 getRelativePositionFromDepthBufferWithOffset(int index, ivec2 offset)
{
    vec4 projPos = textureOffset(depthTextures, vec3(fTexCoord, index), offset);
    mat4 proj = cameraProjections[cameraProjectionIndices[index]];
    float z = - proj[3].z / (proj[2].z + 2 * projPos.z - 1);
    float x = - z * (2 * projPos.x - 1 + proj[2].x) / proj[0].x;
    float y = - z * (2 * projPos.y - 1 + proj[2].y) / proj[1].y;
    return vec3(x, y, z);
}

vec3 getViewVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition);
}

vec3 getViewVectorWithOffset(int index, ivec2 offset)
{
    return normalize(transpose(mat3(cameraPoses[index])) * 
        -getRelativePositionFromDepthBufferWithOffset(index, offset));
}

vec3 getLightVector(int index)
{
    // TODO
    return getViewVector(index);
}

vec3 getLightVectorWithOffset(int index, ivec2 offset)
{
    // TODO
    return getViewVectorWithOffset(index, offset);
}

vec4 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord), vec4(gamma));
}

vec4 getDiffuseColorWithOffset(ivec2 offset)
{
    return pow(textureOffset(diffuseEstimate, fTexCoord, offset), vec4(gamma));
}

vec3 getNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 getNormalVectorWithOffset(ivec2 offset)
{
    return normalize(textureOffset(normalEstimate, fTexCoord, offset).xyz * 2 - vec3(1,1,1));
}

vec3 getReflectionVector(vec3 normalVector, vec3 lightVector)
{
    return normalize(2 * dot(lightVector, normalVector) * normalVector - lightVector);
}

float getDistanceByOffset(int index, ivec2 offset)
{
    return distance(fPosition.xyz,
        (cameraPoses[index] * vec4(getRelativePositionFromDepthBufferWithOffset(index, offset), 1.0)).xyz);
}

void main()
{
    vec3 normal = getNormalVector();
    vec4 diffuseColor = getDiffuseColor();
    
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
            vec4 colorRemainder;
            
            if (diffuseColor.a > 0)
            {
                vec3 diffuseContrib = diffuseColor.rgb * max(0, dot(light, normal));
                colorRemainder = vec4(max(vec3(0), 
                    color.rgb - diffuseRemovalFactor * diffuseContrib), color.a);
            }
            else
            {
                colorRemainder = color;
            }
            
            float intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
            
            vec3 reflection = getReflectionVector(normal, light);
            float rDotV = dot(reflection, view);
            
            if (intensity > 0.0 && rDotV > 0.0)
            {
                float u = rDotV - 1 / rDotV;
                
                sA += color.a * pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness)) * 
                    intensity * outerProduct(vec2(u, 1), vec2(u, 1));
                sB += color.a * pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness)) * 
                    intensity * log(intensity) * vec2(u, 1);
                
                specularSum += color.a * pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness))
                    * intensity * colorRemainder.a * vec4(colorRemainder.rgb, 1.0);
                
                for (int j = 1; j < multisampleRange; j++)
                {
                    float weight = 0.25 / (j + 1);
                    ivec2 offset = ivec2(j, 0);
                    for (int k = 0; k < 4*(j+1); k++)
                    {
                        if (offset.x == j && offset.y != -j)
                        {
                            offset.y--;
                        }
                        else if (offset.y == j)
                        {
                            offset.x++;
                        }
                        else if (offset.x == -j)
                        {
                            offset.y++;
                        }
                        else if (offset.y == -j)
                        {
                            offset.x--;
                        }
                        
                        color = getColorWithOffset(i, offset);
                        if (color.a > 0.0)
                        {
                            // float effectiveWeight = weight * multisampleDistanceFactor / 
                                // (getDistanceByOffset(i, offset) + multisampleDistanceFactor);
                            float effectiveWeight = weight;
                            view = getViewVectorWithOffset(i, offset);
                            light = getLightVectorWithOffset(i, offset);
                            normal = getNormalVectorWithOffset(offset);
                            diffuseColor = getDiffuseColorWithOffset(offset);
                            
                            if (diffuseColor.a > 0)
                            {
                                vec3 diffuseContrib = diffuseColor.rgb * max(0, dot(light, normal));
                                colorRemainder = vec4(max(vec3(0), 
                                    color.rgb - diffuseRemovalFactor * diffuseContrib), color.a);
                            }
                            else
                            {
                                colorRemainder = color;
                            }
                            
                            intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
                            
                            reflection = getReflectionVector(normal, light);
                            rDotV = dot(reflection, view);
                            
                            if (intensity > 0.0 && rDotV > 0.0)
                            {
                                u = rDotV - 1 / rDotV;
                                
                                sA += effectiveWeight * color.a * 
                                    pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness)) * 
                                    intensity * outerProduct(vec2(u, 1), vec2(u, 1));
                                sB += effectiveWeight * color.a * 
                                    pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness)) * 
                                    intensity * log(intensity) * vec2(u, 1);
                                
                                specularSum += effectiveWeight * color.a * 
                                    pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness)) * 
                                    intensity * colorRemainder.a * vec4(colorRemainder.rgb, 1.0);
                            }
                        }
                    }
                }
            }
        }
    }
    
    vec3 specularColorPreGamma;
    float roughness;
    if (determinant(sA) == 0.0)
    {
        specularColorPreGamma = vec3(0.0);
        roughness = 0.0;
    }
    else
    {
        vec2 sSolution = inverse(sA) * sB;
        vec3 specularAvg = specularSum.rgb / (specularSum.r + specularSum.g + specularSum.b);
        specularColorPreGamma = min(vec3(1.0), min(1.0, sA[1][1] / expectedWeightSum) * 
            min(3.0, exp(sSolution[1])) * specularAvg);
        roughness = min(1.0, inversesqrt(2 * sSolution[0]));
    }
    
    float sumSqError = 0.0;
    float sumErrorWeights = 0.0;
    normal = getNormalVector();
    diffuseColor = getDiffuseColor();
    for (int i = 0; i < textureCount; i++)
    {
        vec3 view = getViewVector(i);
        vec3 light = getLightVector(i);
        vec3 diffuseContrib = diffuseColor.rgb * max(0, dot(light, normal));
        vec3 reflection = getReflectionVector(normal, light);
        float rDotV = max(0.0, dot(reflection, view));
        vec3 specularContrib = specularColorPreGamma * 
            exp((rDotV - 1 / rDotV) / (2 * roughness * roughness));
        vec4 color = getColor(i);
        vec3 error = min(vec3(1.0), diffuseContrib + specularContrib) - color.rgb;
        sumSqError += color.a * rDotV * dot(error, error);
        sumErrorWeights += color.a * rDotV * 3.0;
        
        // debug1 = vec4(diffuseContrib, 1.0);
        // debug2 = vec4(specularContrib, 1.0);
        // debug3 = vec4(error * error, 1.0);
    }
    error = vec4(vec3(sumSqError / sumErrorWeights), 1.0);
    
    // vec4 previousErrorSample = texture(previousError, fTexCoord);
    // if (previousErrorSample.a > 0.0 && error[0] > previousErrorSample[0])
    // {
        // // Use previous result, since the error got worse
        // discard;
    // }
    // else
    {
        specularColor = vec4(pow(specularColorPreGamma, vec3(1 / gamma)), 1.0);
        if (specularColor.r > 1 / 256.0 || specularColor.g > 1 / 256.0 || specularColor.b > 1 / 256.0)
        {
            specularRoughness = vec4(vec3(roughness / specularRoughnessCap), 1.0);
        }
        else
        {
            specularRoughness = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }
    
    // debug0 = vec4(specularAvg, 1.0);
    // debug1 = vec4(sSolution[0] / 4, (6 + sSolution[1]) / 8, 0.0, 1.0);
    // debug2 = vec4(-sA[0][0], sA[0][1], sA[1][1], 1.0);
    // debug3 = vec4(-sB[0], sB[1], 0.0, 1.0);
    
    // debug4 = vec4((getRelativePositionFromDepthBufferWithOffset(0, ivec2(0)) + vec3(2,2,12)) / 4, 1.0);
    // debug5 = vec4(((cameraPoses[0] * vec4(fPosition, 1.0)).xyz + vec3(2,2,12)) / 4, 1.0);
}
