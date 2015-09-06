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

vec3 getLightVector(int index)
{
    return transpose(mat3(cameraPoses[index])) * 
        (lightPositions[lightIndices[index]].xyz - cameraPoses[index][3].xyz) - fPosition;
}

vec3 getLightIntensity(int index)
{
    return lightIntensities[lightIndices[index]];
}

float computeSampleWeight(vec3 targetDir, vec3 sampleDir)
{
	return 1.0 / (1.0 - pow(max(0.0, dot(targetDir, sampleDir)), weightExponent)) - 1.0;
}

float getHalfwayFieldSampleWeight(int index)
{
    // All in camera space
    vec3 fragmentPos = (cameraPoses[index] * vec4(fPosition, 1.0)).xyz;
    vec3 virtualViewDir = normalize((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz - fragmentPos);
    vec3 sampleViewDir = normalize(-fragmentPos);
    vec3 virtualLightDir = normalize((cameraPoses[index] * vec4(lightPos, 1.0)).xyz - fragmentPos);
    vec3 sampleLightDir = normalize(lightPositions[lightIndices[index]].xyz - fragmentPos);
    return computeSampleWeight(
        normalize(virtualViewDir + virtualLightDir),
        normalize(sampleViewDir + sampleLightDir));
}

vec4 getLightFieldSample(int index, bool useMipmaps)
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

vec3 getDiffuseColor(vec3 normal, vec3 light)
{
    return pow(texture(diffuseMap, fTexCoord), vec4(gamma)).rgb * max(0, dot(normal, light));
}

vec3 getDiffuseColor(vec3 light)
{
    if (useDiffuseTexture)
    {
        if (useNormalTexture)
        {
            return diffuseRemovalAmount * 
                getDiffuseColor(normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0)), light);
        }
        else
        {
            return diffuseRemovalAmount * getDiffuseColor(normalize(fNormal), light);
        }
    }
    else
    {
        return vec3(0); // assume metallic
    }
}

vec4 extractSpecular(int index, vec4 color)
{
    vec3 light = getLightVector(index);
    vec3 threshold = color.a * getDiffuseColor(normalize(light));
    return vec4(max(vec3(0), color.rgb /* * dot(light, light) / getLightIntensity(index)*/ - threshold),
        color.a);
}

vec3 computeHalfwayField()
{
	vec4 sum = vec4(0.0);
	for (int i = 0; i < cameraPoseCount; i++)
	{
        sum += getHalfwayFieldSampleWeight(i) * extractSpecular(i, getLightFieldSample(i, true));
	}
	return sum.rgb / sum.a;
}

void main()
{
    vec3 lightDir = lightPos - fPosition;
    float nDotL;
    if (useNormalTexture)
    {
        nDotL = max(0.0, dot(normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0)), 
                                normalize(lightDir)));
    }
    else
    {
        nDotL = max(0.0, dot(normalize(fNormal), normalize(lightDir)));
    }
    if (useDiffuseTexture)
    {
        fragColor = vec4(pow(nDotL * 
            (pow(texture(diffuseMap, fTexCoord), vec4(gamma)).rgb + computeHalfwayField())
            /* * lightIntensity / dot(lightDir, lightDir)*/, vec3(1 / gamma)), 1.0);
    }
    else
    {
        fragColor = vec4(pow(nDotL * computeHalfwayField()
            /* * lightIntensity / dot(lightDir, lightDir)*/, vec3(1 / gamma)), 1.0);
    }
}
