#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256
#define MAX_LIGHT_POSITION_COUNT 256

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray viewImages;
uniform sampler2DArray depthImages;
uniform int viewCount;

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;

uniform bool computeRoughness;
uniform bool computeNormal;

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
uniform float roughnessScale;

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
    vec4 projTexCoord = cameraProjections[cameraProjectionIndices[index]] * cameraPoses[index] * 
                            vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;
	
	if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1 ||
            projTexCoord.z < 0 || projTexCoord.z > 1)
	{
		return vec4(0);
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = texture(depthImages, vec3(projTexCoord.xy, index)).r;
			if (abs(projTexCoord.z - imageDepth) > occlusionBias)
			{
				// Occluded
				return vec4(0);
			}
		}
        
        return pow(texture(viewImages, vec3(projTexCoord.xy, index)), vec4(gamma));
	}
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

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 removeDiffuse(vec3 originalColor, vec3 diffuseColor, vec3 light, vec3 normal)
{
    vec3 diffuseContrib = diffuseColor * max(0, dot(light, normal));
    float cap = 1.0 - diffuseRemovalAmount * 
                        max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
    return clamp(originalColor - diffuseRemovalAmount * diffuseContrib, 0, cap);
}

bool validateFit(SpecularFit fit)
{
    return ! isinf(fit.color.r) && ! isinf(fit.color.g) && ! isinf(fit.color.b) && 
            ! isnan(fit.color.r) && ! isnan(fit.color.g) && ! isnan(fit.color.b) && 
            ! isinf(fit.normal.x) && ! isinf(fit.normal.y) && ! isinf(fit.normal.z) && 
            ! isnan(fit.normal.x) && ! isnan(fit.normal.y) && ! isnan(fit.normal.z) && 
            ! isinf(fit.roughness) && ! isnan(fit.roughness);
}

SpecularFit clampFit(SpecularFit fit)
{
    fit.color = clamp(fit.color, 0, 1);
    fit.roughness = clamp(fit.roughness, 0, roughnessScale);
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
        vec3 view = getViewVector(i);
        vec4 color = getColor(i);
        float nDotV = dot(geometricNormal, view);
        
        if (color.a * nDotV > 0)
        {
            vec3 light = getLightVector(i);
            vec3 colorRemainder = removeDiffuse(color.rgb, diffuseColor, light, diffuseNormal);
            float intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
            
            vec3 half = normalize(view + light);
            float nDotH = dot(half, diffuseNormal);
            
            if (intensity > 0.0 && nDotH > 0.0)
            {
                float nDotHPower = pow(nDotH, exponent);
                float u = nDotH - 1 / nDotH;
                
                sum += color.a * nDotV * vec4(colorRemainder.rgb, exp((nDotH - 1 / nDotH) / 
                                            (2 * defaultSpecularRoughness * defaultSpecularRoughness)));
                
                a2 += color.a * nDotV * nDotHPower * intensity * outerProduct(vec2(u, 1), vec2(u, 1));
                b2 += color.a * nDotV * nDotHPower * intensity * log(intensity) * vec2(u, 1);
                
                a4 += color.a * nDotV * nDotHPower * intensity * 
                        outerProduct(vec4(half, 1), vec4(half, 1));
                b4 += color.a * nDotV * nDotHPower * intensity * log(intensity) * vec4(half, 1);
            }
        }
    }
    
    vec3 averageColor = sum.rgb / (sum.r + sum.g + sum.b);
    
    SpecularFit fit = SpecularFit(vec3(0.0), 0.0, vec3(0.0));
    float qualitySum = 0.0;
    
    if (computeRoughness && computeNormal)
    {
        vec4 solution4 = inverse(a4) * b4;
        float quality4 = clamp(fit4Weight * abs(determinant(a4)) / 
            (determinantThreshold * determinantThreshold * determinantThreshold), 0.0, 1.0);
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
    }
    
    if (computeRoughness && qualitySum < 1.0)
    {
        vec2 solution2 = inverse(a2) * b2;
        float quality2 = clamp(fit2Weight * abs(determinant(a2)) / determinantThreshold, 
                            0.0, 1.0 - qualitySum);
        
        SpecularFit fit2;
        fit2.color = exp(solution2[1]) * averageColor;
        fit2.roughness = inversesqrt(2 * solution2[0]);
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
    specularRoughness = vec4(vec3(fit.roughness / roughnessScale), 1.0);
}
