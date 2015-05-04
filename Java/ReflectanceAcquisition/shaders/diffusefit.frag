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
uniform sampler2D specularColorEstimate;
uniform sampler2D specularNormalEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D previousError;

uniform float delta;
uniform int minDiffuseSamples;

uniform float guessSpecularWeight;
uniform float guessSpecularOrthoExp;
uniform vec3 guessSpecularColor;
uniform float specularRemovalFactor;
uniform float specularRoughnessCap;
uniform float determinantThreshold;

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

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normalMap;
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

vec3 getViewVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition);
}

vec3 getLightVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * 
        (lightPositions[lightIndices[index]].xyz - cameraPoses[index][3].xyz) - fPosition);
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
    return specularRoughnessCap * texture(roughnessEstimate, fTexCoord)[0];
}

vec4 getPreviousDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord), vec4(gamma));
}

vec3 getPreviousNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 getSpecularNormalVector()
{
    return normalize(texture(specularNormalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

void main()
{
    vec4 prevDiffuseColor = getPreviousDiffuseColor();
    vec3 prevNormal = getPreviousNormalVector();
    vec4 specularColor = getSpecularColor();
    vec3 specNormal = getSpecularNormalVector();
    float specularRoughness = getSpecularRoughness();

    vec4 sumColor = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {
        vec4 color = getColor(i);
        sumColor += color.a * vec4(color.rgb, 1.0);
    }
    float avgIntensity = (sumColor.r + sumColor.g + sumColor.b) / sumColor.a;
    
    vec3 normal = vec3(0);
    vec3 diffuseColorPreGamma = vec3(0);
    bool firstIteration = true;
    int skipCount = -1, lastSkipCount = -2;
    while(skipCount > lastSkipCount)
    {
        lastSkipCount = skipCount;
        skipCount = 0;
        int ignoreCount = 0;
    
        //mat4 dA = mat4(0);
        //mat4 dB = mat4(0);
        mat3 dA = mat3(0);
        mat3 dB = mat3(0);
        float weightSum = 0;
        
        vec4 diffuseSum = prevDiffuseColor.a * vec4(prevDiffuseColor.rgb, 1.0);
        for (int i = 0; i < textureCount; i++)
        {
            vec4 color = getColor(i);
            if (color.a > 0)
            {
                //vec4 light = vec4(getLightVector(i), 1.0);
                vec3 light = getLightVector(i);
                
                if (prevDiffuseColor.a == 0.0 || dot(light, prevNormal) > 0)
                {
                    vec4 colorRemainder;
                    
                    if (prevDiffuseColor.a > 0)
                    {
                        vec3 view = getViewVector(i);
                        vec3 half = normalize(view + light);
                        float nDotH = max(0.0, dot(half, specNormal));
                        vec3 specularContrib = specularColor.rgb * 
                            exp((nDotH - 1 / nDotH) / (2 * specularRoughness * specularRoughness));
                        colorRemainder = vec4(max(vec3(0), 
                            color.rgb - specularRemovalFactor * specularContrib), color.a);
                    }
                    else 
                    {
                        // First global fitting (diffuse+specular) iteration only
                        colorRemainder = color;
                    }
                    
                    if (firstIteration || 
                        colorRemainder.r + colorRemainder.g + colorRemainder.b <= 
                            (diffuseColorPreGamma.r + diffuseColorPreGamma.g + diffuseColorPreGamma.b) 
                                * dot(normal, light) + delta)
                    {
                        dA += colorRemainder.a * outerProduct(light, light);
                        //dB += colorRemainder.a * outerProduct(light, vec4(colorRemainder.rgb, 0.0));
                        dB += colorRemainder.a * outerProduct(light, colorRemainder.rgb);
                        
                        weightSum += colorRemainder.a;
                        
                        if (prevDiffuseColor.a == 0.0 && // First global fitting iteration only
                            (color.r > 0.0 || color.g > 0.0 || color.b > 0.0) && 
                            (color.r + color.g + color.b) < avgIntensity)
                        {
                            diffuseSum += color.a * vec4(color.rgb, 1.0);
                        }
                    }
                    else
                    {
                        skipCount++;
                    }
                }
                else
                {
                    ignoreCount++;
                }
            }
            else
            {
                ignoreCount++;
            }
        }
        
        if (!firstIteration && skipCount + ignoreCount + minDiffuseSamples > textureCount)
        {
            skipCount = lastSkipCount;
            // Use the result of the previous iteration
        }
        else
        {
            vec3 diffuseAvg = diffuseSum.rgb / (diffuseSum.r + diffuseSum.g + diffuseSum.b);
            
            mat3 dM = inverse(dA) * dB;
            vec3 componentFit = vec3(length(dM[0]), length(dM[1]), length(dM[2]));
            float simpleWeightScale;
            if (diffuseSum.r > diffuseSum.g && diffuseSum.r > diffuseSum.b)
            {
                // Red dominates
                simpleWeightScale = componentFit.r / diffuseSum.r;
            }
            else if (diffuseSum.g > diffuseSum.b)
            {
                // Green dominates
                simpleWeightScale = componentFit.g / diffuseSum.g;
            }
            else
            {
                // Blue dominates
                simpleWeightScale = componentFit.b / diffuseSum.b;
            }
            vec3 componentFitInv = 1.0 / componentFit;
            if (componentFit.r == 0.0)
            {
                componentFitInv.r = 0.0;
            }
            if (componentFit.g == 0.0)
            {
                componentFitInv.g = 0.0;
            }
            if (componentFit.b == 0.0)
            {
                componentFitInv.b = 0.0;
            }
            vec3 simpleWeights = simpleWeightScale * diffuseSum.rgb * componentFitInv;
            
            vec3 rgbWeights;
            //float diffuseRemovalMult;
            if (guessSpecularWeight > 0)
            {
                vec3 ortho = cross(normalize(diffuseSum.rgb), normalize(guessSpecularColor));
                if (ortho.x > 0 || ortho.y > 0 || ortho.z > 0)
                {
                    mat3 colorBasis = mat3(diffuseAvg, guessSpecularColor, ortho);
                    float adjustedSpecGuessWeight = guessSpecularWeight * 
                        pow(length(ortho), guessSpecularOrthoExp);
                    vec3 basisWeights = transpose(inverse(colorBasis))[0];
                    vec3 scaledBasisWeights = basisWeights / 
                        max(max(basisWeights.r, basisWeights.g), basisWeights.b);
                    rgbWeights = (adjustedSpecGuessWeight * scaledBasisWeights + simpleWeights) / 
                        (1 + adjustedSpecGuessWeight);
                    //diffuseRemovalMult = adjustedSpecGuessWeight / (1 + adjustedSpecGuessWeight);
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
        
            float scaledDeterminant = determinant(dA) / (weightSum * determinantThreshold);
            
            //vec4 dSolution = inverse(dA) * dB * vec4(rgbWeights, 0.0);
            vec3 dSolution = dM * rgbWeights;
            //float ambientIntensity = dSolution.w;
            float diffuseIntensity = length(dSolution.xyz);
            normal = normalize(dSolution.xyz) * clamp(scaledDeterminant, 0.0, 1.0) + fNormal * clamp(1 - scaledDeterminant, 0.0, 1.0);
            diffuseColorPreGamma = diffuseAvg * diffuseIntensity;
        
            debug1 = vec4(rgbWeights, 1.0);
            debug2 = vec4(vec3(diffuseIntensity), 1.0);
            debug3 = vec4(diffuseAvg, 1.0);
            debug4 = vec4(vec3(float(skipCount) / float(textureCount-ignoreCount)), 1.0);
            debug5 = vec4(vec3(float(ignoreCount) / float(textureCount)), 1.0);
        }
            
        firstIteration = false;
    }
    
    float alpha;
    if (isnan(diffuseColorPreGamma.r) || isnan(diffuseColorPreGamma.g) || isnan(diffuseColorPreGamma.b) ||
        isinf(diffuseColorPreGamma.r) || isinf(diffuseColorPreGamma.g) || isinf(diffuseColorPreGamma.b) ||
        isnan(normal.x) || isnan(normal.y) || isnan(normal.z) ||
        isinf(normal.x) || isinf(normal.y) || isinf(normal.z))
    {
        diffuseColorPreGamma = vec3(0.0);
        normal = vec3(0.0);
        alpha = 0.0;
    }
    else
    {
        alpha = 1.0;
    }
    
    //debug1 = vec4(pow(diffuseAvg * dSolution.w, vec3(1 / gamma)), 1.0);
    
    float sumSqError = 0.0;
    for (int i = 0; i < textureCount; i++)
    {
        vec3 view = getViewVector(i);
        vec3 light = getLightVector(i);
        vec3 diffuseContrib = diffuseColorPreGamma.rgb * max(0, dot(light, normal));
        vec3 half = normalize(view + light);
        float nDotH = max(0.0, dot(normal, half));
        vec3 specularContrib = specularColor.rgb * 
            exp((nDotH - 1 / nDotH) / (2 * specularRoughness * specularRoughness));
        vec4 color = getColor(i);
        vec3 error = min(vec3(1.0), diffuseContrib + specularContrib) - color.rgb;
        sumSqError += dot(error, error);
    }
    error = vec4(vec3(sumSqError / (3 * textureCount)), 1.0);
    
   // normal = normal * (1 - clamp(sumSqError / (3 * textureCount) * 100.0, 0, 1));
    
    // vec4 previousErrorSample = texture(previousError, fTexCoord);
    // if (previousErrorSample.a > 0.0 && error[0] > previousErrorSample[0])
    // {
        // // Use previous result, since the error got worse
        // diffuseColor = texture(diffuseEstimate, fTexCoord);
        // normalMap = texture(normalEstimate, fTexCoord);
    // }
    // else
    {
        diffuseColor = vec4(pow(diffuseColorPreGamma, vec3(1 / gamma)), alpha);
        normalMap = vec4(normal * 0.5 + vec3(0.5), alpha);
    }
    
    //debug1 = vec4(lightPositions[0].xyz * 0.5 + vec3(0.5), 1.0);
}
