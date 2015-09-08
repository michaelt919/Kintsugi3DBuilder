#version 330

#define SAMPLE_COUNT 7

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024
#define MAX_LIGHT_COUNT 1024

uniform bool occlusionEnabled;

uniform float weightExponent;
uniform float occlusionBias;
uniform float gamma;
uniform float diffuseRemovalAmount;

uniform mat4 model_view;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

in vec3 fViewPos;
uniform vec3 lightIntensity;
uniform vec3 lightPos;

uniform sampler2DArray imageTextures;
uniform sampler2DArray depthTextures;

uniform sampler2D diffuseMap;
uniform sampler2D normalMap;

uniform bool useDiffuseTexture;
uniform bool useNormalTexture;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform int cameraPoseCount;

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

layout(location = 0) out vec4 fragColor;

float computeSampleWeight(vec3 targetDir, vec3 sampleDir)
{
	return 1.0 / (1.0 - pow(max(0.0, dot(targetDir, sampleDir)), weightExponent)) - 1.0;
}

vec4 getProjTexSample(int index, bool useMipmaps)
{
	vec4 fragPos = cameraPoses[index] * vec4(fPosition, 1.0);
	vec4 projTexCoord = cameraProjections[cameraProjectionIndices[index]] * fragPos;
	projTexCoord = projTexCoord / projTexCoord.w;
	projTexCoord = (projTexCoord + vec4(1)) / 2;
	
	if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1
             || projTexCoord.z < 0 || projTexCoord.z > 1)
	{
		return vec4(0.0);
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = texture(depthTextures, vec3(projTexCoord.xy, index)).r;
			if (projTexCoord.z > imageDepth + occlusionBias)
			{
				// Occluded
				return vec4(0.0);
			}
		}
        
        vec4 color;
        if (useMipmaps)
        {
            color = texture(imageTextures, vec3(projTexCoord.xy, index));
        }
        else
        {
            color = textureLod(imageTextures, vec3(projTexCoord.xy, index), 0.0);
        }
		
		return (color.a < 0.9999 ? 0.0 : 1.0) * vec4(pow(color.rgb, vec3(gamma)), 1.0);
	}
}

float computeGeometricAttenuation(vec3 view, vec3 light, vec3 normal)
{
    vec3 half = normalize(view + light);
    return min(1.0, min(
        2.0 * dot(half, normal) * dot(view, normal) / dot(view, half),
        2.0 * dot(half, normal) * dot(light, normal) / dot(view, half)))
        / (dot(light, normal) * dot(view, normal));
}

vec4 computeMicrofacetDistributionSample(int index, vec3 diffuseAlbedo, vec3 normalDir)
{
    // All in camera space
    vec3 fragmentPos = (cameraPoses[index] * vec4(fPosition, 1.0)).xyz;
    vec3 virtualViewDir = normalize((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz - fragmentPos);
    vec3 sampleViewDir = normalize(-fragmentPos);
    vec3 virtualLightDir = normalize((cameraPoses[index] * vec4(lightPos, 1.0)).xyz - fragmentPos);
    vec3 sampleLightDirUnnorm = lightPositions[lightIndices[index]].xyz - fragmentPos;
    vec3 sampleLightDir = normalize(sampleLightDirUnnorm);
    vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);
    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
    vec3 normalDirCameraSpace = (cameraPoses[index] * vec4(normalDir, 0.0)).xyz;
    
    // Compute sample weight
    float weight = computeSampleWeight(virtualHalfDir, sampleHalfDir);
    
    vec4 sampleColor = getProjTexSample(index, true);
    float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
    float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));

    return nDotV * weight * vec4(max(vec3(0), sampleColor.rgb
            /* * dot(sampleLightDirUnnorm, sampleLightDirUnnorm) / lightIntensities[lightIndices[index]]*/
            - diffuseRemovalAmount * diffuseAlbedo * nDotL)
            / computeGeometricAttenuation(sampleViewDir, sampleLightDir, normalDirCameraSpace), 
        sampleColor.a * nDotL);
}

vec3 computeMicrofacetDistribution(vec3 diffuseAlbedo, vec3 normalDir)
{
	vec4 sum = vec4(0.0);
	for (int i = 0; i < cameraPoseCount; i++)
	{
        sum += computeMicrofacetDistributionSample(i, diffuseAlbedo, normalDir);
	}
	return sum.rgb / sum.a;
}

void main()
{
    vec3 lightDir = lightPos - fPosition;
    vec3 viewDir = fViewPos - fPosition;
    
    vec3 normalDir;
    if (useNormalTexture)
    {
        normalDir = normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0));
    }
    else
    {
        normalDir = normalize(fNormal);
    }
    float nDotL = max(0.0, dot(normalDir, normalize(lightDir)));
    
    vec3 diffuseAlbedo;
    if (useDiffuseTexture)
    {
        diffuseAlbedo = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
    }
    else
    {
        diffuseAlbedo = vec3(0.0);
    }
    
    vec3 specularReflectance;
    if (nDotL > 0.0)
    {
        specularReflectance = computeMicrofacetDistribution(diffuseAlbedo, normalDir) 
            * computeGeometricAttenuation(normalize(viewDir), normalize(lightDir), normalDir);
    }
    
    fragColor = vec4(pow(nDotL /* * lightIntensity / dot(lightDir, lightDir)*/
        * (diffuseAlbedo + specularReflectance), vec3(1 / gamma)), 1.0);
}
