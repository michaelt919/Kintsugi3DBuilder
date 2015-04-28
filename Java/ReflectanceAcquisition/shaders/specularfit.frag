#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256
#define MAX_LIGHT_POSITION_COUNT 256

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

uniform bool computeRoughness;
uniform bool computeNormal;
uniform bool useViewSetNormal;
uniform float specularInfluenceScale;
uniform int multisampleRange;
//uniform float multisampleDistanceFactor;
uniform vec3 defaultSpecularColor;
uniform float defaultSpecularRoughness;
uniform float weightSumThreshold;
uniform float determinantThreshold;
uniform float determinantExponent;
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

uniform LightPositions
{
	vec4 lightPositions[MAX_LIGHT_POSITION_COUNT];
};

uniform LightIndices
{
	int lightIndices[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 specularRoughness;
layout(location = 2) out vec4 specularNormal;
layout(location = 3) out vec4 error;
layout(location = 4) out vec4 debug1;
layout(location = 5) out vec4 debug2;
layout(location = 6) out vec4 debug3;
layout(location = 7) out vec4 debug4;

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
    return normalize(transpose(mat3(cameraPoses[index])) * 
        (lightPositions[lightIndices[index]].xyz - cameraPoses[index][3].xyz) - fPosition);
}

vec3 getLightVectorWithOffset(int index, ivec2 offset)
{
    return normalize(transpose(mat3(cameraPoses[index])) * 
        (lightPositions[lightIndices[index]].xyz - cameraPoses[index][3].xyz - 
            getRelativePositionFromDepthBufferWithOffset(index, offset)));
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
    
    vec3 specularColorPreGamma;
    float roughness;
    vec3 sNormal = normal;
    float alpha;
    
    if (useViewSetNormal)
    {
        vec4 halfVectorSum = vec4(0);
        
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
                    float cap = 1.0 - diffuseRemovalFactor * 
                        max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
                    colorRemainder = vec4(clamp(color.rgb - diffuseRemovalFactor * diffuseContrib,
                        vec3(0), vec3(cap)), color.a);
                }
                else
                {
                    colorRemainder = color;
                }
                
                vec3 half = normalize(light + view);
                float nDotH = dot(normal, half);
                
                if (colorRemainder.r > 0.0 || colorRemainder.g > 0.0 || colorRemainder.b > 0.0)
                {
                    halfVectorSum += (colorRemainder.r + colorRemainder.g + colorRemainder.b) * 
                        vec4(half, 1.0);
                }
            }
        }
    
        sNormal = halfVectorSum.xyz / halfVectorSum.w;
    }
    
    if (computeRoughness)
    {
        if (!computeNormal)
        {
            vec4 specularSum = vec4(0);
            mat2 sA = mat2(0);
            vec2 sB = vec2(0);
            float weightSum = 0.0;
            
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
                        float cap = 1.0 - diffuseRemovalFactor * 
                            max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
                        colorRemainder = vec4(clamp(color.rgb - diffuseRemovalFactor * diffuseContrib,
                            vec3(0), vec3(cap)), color.a);
                    }
                    else
                    {
                        colorRemainder = color;
                    }
                    
                    float intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
                    
                    vec3 half = normalize(light + view);
                    float nDotH = dot(sNormal, half);
                    
                    if (intensity > 0.0 && nDotH > 0.0)
                    {
                        float u = nDotH - 1 / nDotH;
                        
                        sA += color.a * pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale)) 
                            * intensity * outerProduct(vec2(u, 1), vec2(u, 1));
                        sB += color.a * pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale)) 
                            * intensity * log(intensity) * vec2(u, 1);
                        
                        specularSum += color.a * 
                            pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale))
                            * intensity * vec4(colorRemainder.rgb, 1.0);
                            
                        weightSum += color.a * 
                            pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale));
                        
                        for (int j = 1; j <= multisampleRange; j++)
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
                                        vec3 diffuseContrib = diffuseColor.rgb *
                                            max(0, dot(light, normal));
                                        float cap = 1.0 - max(diffuseContrib.r, 
                                            max(diffuseContrib.g, diffuseContrib.b));
                                        colorRemainder = vec4(clamp(
                                            color.rgb - diffuseRemovalFactor * diffuseContrib,
                                            vec3(0), vec3(cap)), color.a);
                                    }
                                    else
                                    {
                                        colorRemainder = color;
                                    }
                                    
                                    intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
                                    
                                    half = normalize(light + view);
                                    nDotH = dot(normal, half);
                                    
                                    if (intensity > 0.0 && nDotH > 0.0)
                                    {
                                        u = nDotH - 1 / nDotH;
                                        
                                        sA += effectiveWeight * color.a * 
                                            pow(nDotH, 
                                                1 / (specularInfluenceScale * specularInfluenceScale)) 
                                            * intensity * outerProduct(vec2(u, 1), vec2(u, 1));
                                        sB += effectiveWeight * color.a * 
                                            pow(nDotH, 
                                                1 / (specularInfluenceScale * specularInfluenceScale)) 
                                            * intensity * log(intensity) * vec2(u, 1);
                                        
                                        specularSum += effectiveWeight * color.a * 
                                            pow(nDotH, 
                                                1 / (specularInfluenceScale * specularInfluenceScale)) 
                                            * intensity * colorRemainder.a * vec4(colorRemainder.rgb, 1.0);
                                            
                                        weightSum += effectiveWeight * color.a * 
                                            pow(nDotH, 1 / (specularInfluenceScale * 
                                            specularInfluenceScale));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            vec2 sSolution = inverse(sA) * sB;
            vec3 specularAvg = specularSum.rgb / (specularSum.r + specularSum.g + specularSum.b);
            float scaledWeightSum = min(1.0, sA[1][1] / weightSumThreshold);
            
            specularColorPreGamma = scaledWeightSum * min(vec3(1.0), exp(sSolution[1]) * specularAvg) + 
                                            (1 - scaledWeightSum) * defaultSpecularColor;
            roughness = scaledWeightSum * min(specularRoughnessCap, inversesqrt(2 * sSolution[0])) + 
                                (1 - scaledWeightSum) * defaultSpecularRoughness;
            alpha = min(1.0, scaledWeightSum * 
                        pow(abs(determinant(sA)) / (sA[1][1] * determinantThreshold), determinantExponent)
                            + (1 - scaledWeightSum));
            // alpha = min(1.0, (exp(determinantExponent * abs(determinant(sA)) / determinantThreshold) - 1) /
                                // (exp(determinantExponent) - 1));
                                
            debug1 = vec4(specularAvg, 1.0);
            debug3 = vec4(vec3(scaledWeightSum), 1.0);
        }
        else
        {
            vec4 specularSum = vec4(0);
            mat4 sA = mat4(0);
            vec4 sB = vec4(0);
            float weightSum = 0.0;
            
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
                        float cap = 1.0 - diffuseRemovalFactor * 
                            max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
                        colorRemainder = vec4(clamp(color.rgb - diffuseRemovalFactor * diffuseContrib,
                            vec3(0), vec3(cap)), color.a);
                    }
                    else
                    {
                        colorRemainder = color;
                    }
                    
                    float intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
                    
                    vec3 half = normalize(view + light);
                    float nDotH = dot(half, normal);
                    
                    if (intensity > 0.0 && nDotH > 0.0)
                    {
                        sA += color.a * pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale)) 
                            * intensity * outerProduct(vec4(half, 1), vec4(half, 1));
                        sB += color.a * pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale)) 
                            * intensity * log(intensity) * vec4(half, 1);
                        
                        specularSum += color.a * 
                            pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale))
                            * intensity * vec4(colorRemainder.rgb, 1.0);
                            
                        weightSum += color.a * 
                            pow(nDotH, 1 / (specularInfluenceScale * specularInfluenceScale));
                    }
                }
            }
            
            vec4 sSolution = inverse(sA) * sB;
            vec3 specularAvg = specularSum.rgb / (specularSum.r + specularSum.g + specularSum.b);
            float scaledWeightSum = min(1.0, sA[3][3] / weightSumThreshold);
            
            float invRate = inversesqrt(dot(sSolution.xyz, sSolution.xyz));
            specularColorPreGamma = scaledWeightSum * 
                min(vec3(1.0), exp(1.0 / invRate + sSolution.w) * specularAvg) + 
                (1 - scaledWeightSum) * defaultSpecularColor;
            float invSqrtRate = sqrt(invRate);
            roughness = scaledWeightSum * min(specularRoughnessCap, (invRate / 2 + 1) * invSqrtRate) +
                (1 - scaledWeightSum) * defaultSpecularRoughness;
            alpha = min(1.0, scaledWeightSum * 
                        pow(abs(determinant(sA)) / (sA[3][3] * determinantThreshold), determinantExponent)
                        + (1 - scaledWeightSum));
            sNormal = scaledWeightSum * sSolution.xyz * invRate + (1 - scaledWeightSum) * normal;
                                
            debug1 = vec4(specularAvg, 1.0);
            debug3 = vec4(vec3(scaledWeightSum), 1.0);
        }
    }
    else
    {
        vec4 specularSum = vec4(0);
        vec4 halfVectorSum = vec4(0);
        
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
                    float cap = 1.0 - diffuseRemovalFactor * 
                        max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
                    colorRemainder = vec4(clamp(color.rgb - diffuseRemovalFactor * diffuseContrib,
                        vec3(0), vec3(cap)), color.a);
                }
                else
                {
                    colorRemainder = color;
                }
                
                vec3 half = normalize(light + view);
                float nDotH = dot(sNormal, half);
                
                if ((colorRemainder.r > 0.0 || colorRemainder.g > 0.0 || colorRemainder.b > 0.0) && 
                    nDotH > 0.0)
                {
                    specularSum += colorRemainder.a * 
                        vec4(colorRemainder.rgb, exp((nDotH - 1 / nDotH) / 
                            (2 * defaultSpecularRoughness * defaultSpecularRoughness)));
                        
                    halfVectorSum += (colorRemainder.r + colorRemainder.g + colorRemainder.b) * 
                        vec4(half, 1.0);
                }
            }
        }
        
        float scaledWeightSum = min(1.0, specularSum.a / weightSumThreshold);
        
        specularColorPreGamma = scaledWeightSum * min(vec3(1.0), specularSum.rgb / specularSum.a) + 
                                        (1 - scaledWeightSum) * defaultSpecularColor;
        roughness = defaultSpecularRoughness;
        alpha = 1.0;
        
        debug1 = vec4(normalize(specularSum.rgb), 1.0);
        debug3 = vec4(vec3(scaledWeightSum), 1.0);
        debug4 = vec4((halfVectorSum.xyz / halfVectorSum.w) / 2 + vec3(0.5), 1.0);
    }
    
    if (isinf(specularColorPreGamma.r) || isnan(specularColorPreGamma.r) || 
        isinf(specularColorPreGamma.g) || isnan(specularColorPreGamma.g) || 
        isinf(specularColorPreGamma.b) || isnan(specularColorPreGamma.b) ||
        isinf(roughness) || isnan(roughness))
    {
        specularColorPreGamma = defaultSpecularColor;
        roughness = defaultSpecularRoughness;
        alpha = 0.0;
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
        vec3 half = normalize(view + light);
        float nDotH = max(0.0, dot(sNormal, half));
        vec3 specularContrib = specularColorPreGamma * 
            exp((nDotH - 1 / nDotH) / (2 * roughness * roughness));
        vec4 color = getColor(i);
        vec3 error = min(vec3(1.0), diffuseContrib + specularContrib) - color.rgb;
        sumSqError += color.a * nDotH * dot(error, error);
        sumErrorWeights += color.a * nDotH * 3.0;
        
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
        specularColor = vec4(pow(specularColorPreGamma, vec3(1 / gamma)), alpha);
        specularNormal = vec4(sNormal * 0.5 + vec3(0.5), computeNormal ? alpha : 1.0);
        // if (specularColor.r > 1 / 256.0 || specularColor.g > 1 / 256.0 || specularColor.b > 1 / 256.0)
        {
            specularRoughness = vec4(vec3(roughness / specularRoughnessCap), alpha);
        }
        // else
        // {
            // specularRoughness = vec4(1.0);
        // }
    }
    
   // debug2 = vec4(lightPositions[0].xyz * 0.5 + vec3(0.5), 1.0);
    debug2 = vec4(vec3(alpha), 1.0);
    
    // debug0 = vec4(specularAvg, 1.0);
    // debug1 = vec4(sSolution[0] / 4, (6 + sSolution[1]) / 8, 0.0, 1.0);
    // debug2 = vec4(-sA[0][0], sA[0][1], sA[1][1], 1.0);
    // debug3 = vec4(-sB[0], sB[1], 0.0, 1.0);
    
    // debug4 = vec4((getRelativePositionFromDepthBufferWithOffset(0, ivec2(0)) + vec3(2,2,12)) / 4, 1.0);
    // debug5 = vec4(((cameraPoses[0] * vec4(fPosition, 1.0)).xyz + vec3(2,2,12)) / 4, 1.0);
}
