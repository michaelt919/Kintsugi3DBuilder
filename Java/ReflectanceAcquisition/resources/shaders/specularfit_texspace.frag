#version 330

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024
#define MAX_LIGHT_COUNT 1024

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform vec2 minTexCoord;
uniform vec2 maxTexCoord;

uniform sampler2DArray viewImages;
uniform int viewCount;

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

uniform bool computeRoughness;
uniform bool computeNormal;
uniform bool trueBlinnPhong;

uniform float gamma;
uniform bool occlusionEnabled;
uniform float occlusionBias;

uniform float diffuseRemovalAmount;
uniform float specularInfluenceScale;
uniform float determinantThreshold;
uniform float fit4Weight;
uniform float fit2Weight;
uniform float fit1Weight;
uniform vec3 defaultSpecularColor;
uniform float defaultSpecularRoughness;
uniform float specularRoughnessScale;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform LightPositions
{
	vec4 lightPositions[MAX_LIGHT_COUNT];
};

uniform LightIntensities
{
    vec3 lightIntensities[MAX_LIGHT_COUNT];
};

uniform LightIndices
{
	int lightIndices[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 specularColor;
layout(location = 1) out vec4 specularRoughness;
layout(location = 2) out vec4 specularNormalMap;
layout(location = 3) out vec4 debug;

struct SpecularFit
{
    vec3 color;
    float roughness;
    vec3 normal;
};

vec4 getColor(int index)
{
    return pow(texture(viewImages, vec3((fTexCoord - minTexCoord) / (maxTexCoord - minTexCoord), index)), vec4(gamma));
}

vec3 getViewVector(int index)
{
    return transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition;
}

vec3 getLightVector(int index)
{
    return transpose(mat3(cameraPoses[index])) * 
        (lightPositions[lightIndices[index]].xyz - cameraPoses[index][3].xyz) - fPosition;
}

vec3 getLightIntensity(int index)
{
    return lightIntensities[lightIndices[index]];
}

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, vec3 light, vec3 attenuatedLightIntensity, vec3 normal)
{
    vec3 diffuseContrib = diffuseColor * max(0, dot(light, normal)) * attenuatedLightIntensity;
    float cap = 1.0 - diffuseRemovalAmount * max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    vec3 remainder = clamp(originalColor.rgb - diffuseRemovalAmount * diffuseContrib, 0, cap);
    return vec4(remainder, originalColor.a * pow(remainder.r * remainder.g * remainder.b, 1.0 / 3.0));
}

bool validateFit(SpecularFit fit)
{
    return ! isnan(fit.color.r) && ! isnan(fit.color.g) && ! isnan(fit.color.b) && 
            ! isnan(fit.normal.x) && ! isnan(fit.normal.y) && ! isnan(fit.normal.z) && 
            ! isnan(fit.roughness);
}

SpecularFit clampFit(SpecularFit fit)
{
    fit.color = fit.color / max(max(1.0, fit.color.r), max(fit.color.g, fit.color.b));
    fit.roughness = clamp(fit.roughness, 0, specularRoughnessScale);
    return fit;
}

SpecularFit fitSpecular()
{
    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 diffuseColor = getDiffuseColor();
    
    float exponent = 1 / (specularInfluenceScale * specularInfluenceScale);
    
    vec4 sum = vec4(0);
    mat2 a2 = mat2(0);
    vec2 b2 = vec2(0);
    mat4 a4 = mat4(0);
    vec4 b4 = vec4(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
        vec4 color = getColor(i);
        float nDotV = dot(geometricNormal, view);
        
        if (color.a * nDotV > 0)
        {
            vec3 light = getLightVector(i);
            vec3 attenuatedLightIntensity = getLightIntensity(i) / (dot(light, light));
            vec3 lightNormalized = normalize(light);
            
            vec4 colorRemainder = removeDiffuse(color, diffuseColor, lightNormalized, attenuatedLightIntensity, diffuseNormal);
            float intensity = colorRemainder.r / attenuatedLightIntensity.r + 
                                colorRemainder.g / attenuatedLightIntensity.g + 
                                colorRemainder.b / attenuatedLightIntensity,b;
            
            vec3 half = normalize(view + lightNormalized);
            float nDotH = dot(half, diffuseNormal);
            
            if (intensity > 0.0 && nDotH > 0.0)
            {
                float u = nDotH - 1 / nDotH;
                
                sum += color.a * nDotV * vec4(colorRemainder.rgb / attenuatedLightIntensity, 
                        trueBlinnPhong ? 
                            pow(nDotH, 1 / (defaultSpecularRoughness * defaultSpecularRoughness)) :
                            exp((nDotH - 1 / nDotH) / 
                                (2 * defaultSpecularRoughness * defaultSpecularRoughness)));
                
                if (computeRoughness)
                {
                    float nDotHPower = pow(nDotH, exponent);
                
                    a2 += colorRemainder.a * nDotV * nDotHPower * 
                            outerProduct(vec2(u, 1), vec2(u, 1));
                    b2 += colorRemainder.a * nDotV * nDotHPower * log(intensity) * vec2(u, 1);
                    
                    if (computeNormal)
                    {
                        a4 += colorRemainder.a * nDotV * nDotHPower * 
                                outerProduct(vec4(half, 1), vec4(half, 1));
                        b4 += colorRemainder.a * nDotV * nDotHPower * log(intensity) * vec4(half, 1);
                    }
                }
            }
        }
    }
    
    vec3 averageColor = sum.rgb / (sum.r + sum.g + sum.b);
    
    SpecularFit fit = SpecularFit(vec3(0.0), 0.0, vec3(0.0));
    float qualitySum = 0.0;
    
    if (computeRoughness && computeNormal)
    {
        vec4 solution4 = inverse(a4) * b4;
        float quality4 = clamp(fit4Weight * abs(determinant(a4)) * 
                                clamp(dot(normalize(solution4.xyz), geometricNormal), 0, 1) / 
                                (determinantThreshold * determinantThreshold * determinantThreshold), 
                            0.0, 1.0);
        float invRate = inversesqrt(dot(solution4.xyz, solution4.xyz));
        float invSqrtRate = sqrt(invRate);
        
        SpecularFit fit4;
        fit4.color = exp(1.0 / invRate + solution4.w) * averageColor;
        fit4.roughness = (invRate / 2 + 1) * invSqrtRate; // invRate normalizes the solution vector
        fit4.normal = solution4.xyz * invRate;
        
        if (validateFit(fit4))
        {
            fit4 = clampFit(fit4);
        }
        else
        {
            quality4 = 0.0;
        }
        
        if (quality4 > 0.0)
        {
            fit.color += quality4 * fit4.color;
            fit.roughness += quality4 * fit4.roughness;
            fit.normal += quality4 * fit4.normal;
            qualitySum += quality4;
        }
    
        debug = vec4(quality4, 0, 0, 1);
    }
    
    if (computeRoughness && qualitySum < 1.0)
    {
        vec2 solution2 = inverse(a2) * b2;
        float quality2 = clamp(fit2Weight * abs(determinant(a2)) / determinantThreshold, 
                            0.0, 1.0 - qualitySum);
        
        SpecularFit fit2;
        fit2.color = exp(solution2[1]) * averageColor;
        fit2.roughness = inversesqrt(max(0, 2 * solution2[0]));
        fit2.normal = diffuseNormal;
        
        if (validateFit(fit2))
        {
            fit2 = clampFit(fit2);
        }
        else
        {
            quality2 = 0.0;
        }
        
        if (quality2 > 0.0)
        {
            fit.color += quality2 * fit2.color;
            fit.roughness += quality2 * fit2.roughness;
            fit.normal += quality2 * fit2.normal;
            qualitySum += quality2;
        }
    
        debug.g = quality2;
    }
    
    if (qualitySum < 1.0)
    {
        float quality1 = clamp(fit1Weight * sum.a, 0.0, 1.0 - qualitySum);
        
        SpecularFit fit1;
        fit1.color = sum.rgb / sum.a;
        fit1.roughness = defaultSpecularRoughness;
        fit1.normal = diffuseNormal;
        
        if (validateFit(fit1))
        {
            fit1 = clampFit(fit1);
        }
        else
        {
            quality1 = 0.0;
        }
        
        if (quality1 > 0.0)
        {
            fit.color += quality1 * fit1.color;
            fit.roughness += quality1 * fit1.roughness;
            fit.normal += quality1 * fit1.normal;
            qualitySum += quality1;
        }
    
        debug.b = quality1;
    }
    
    if (qualitySum < 1.0)
    {
        fit.color += (1.0 - qualitySum) * defaultSpecularColor;
        fit.roughness += (1.0 - qualitySum) * defaultSpecularRoughness;
        fit.normal += (1.0 - qualitySum) * diffuseNormal;
    }
    
    return fit;
}

void main()
{
    SpecularFit fit = fitSpecular();
    specularColor = vec4(pow(fit.color, vec3(1 / gamma)), 1.0);
    specularNormalMap = vec4(fit.normal * 0.5 + vec3(0.5), 1.0);
    specularRoughness = vec4(vec3(fit.roughness / specularRoughnessScale), 1.0);
}
