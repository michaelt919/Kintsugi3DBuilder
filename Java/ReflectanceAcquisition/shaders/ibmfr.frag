#version 330

#define SAMPLE_COUNT 7
#define PACKED_INDEX_VERTEX_COUNT 7;
#define MAX_VIRTUAL_LIGHT_COUNT 4;

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

uniform isampler2DArray viewIndexTextures;

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

int[SAMPLE_COUNT] readViewIndices()
{
    ivec4 prevViewIndices0 = texture(viewIndexTextures, vec3(fTexCoord, 0));
    ivec4 prevViewIndices1 = texture(viewIndexTextures, vec3(fTexCoord, 1));

    return int[SAMPLE_COUNT](
        prevViewIndices0.x,
        prevViewIndices0.y,
        prevViewIndices0.z,
        prevViewIndices0.w,
        prevViewIndices1.x,
        prevViewIndices1.y,
        prevViewIndices1.z
    );
    
    // ivec2 texSize = textureSize(viewIndexTextures, 0).xy;
    // vec2 texCoordExact = texSize * fTexCoord;
    // vec2 texCoordFloor = floor(texCoordExact);
    // vec2 texCoordDelta = texCoordExact-texCoordFloor;
    
    // ivec2 coordsA, coordsB, coordsC, coordsD;
    // if (texCoordDelta.x > 0.5)
    // {
        // if (texCoordDelta.y > 0.5)
        // {
            // coordsA = ivec2(texCoordFloor + ivec2(1,1));
            // coordsD = ivec2(texCoordFloor);
            
            // if (texCoordDelta.x > texCoordDelta.y)
            // {
                // coordsB = ivec2(texCoordFloor + ivec2(1,0));
                // coordsC = ivec2(texCoordFloor + ivec2(0,1));
            // }
            // else
            // {
                // coordsB = ivec2(texCoordFloor + ivec2(0,1));
                // coordsC = ivec2(texCoordFloor + ivec2(1,0));
            // }
        // }
        // else
        // {
            // coordsA = ivec2(texCoordFloor + ivec2(1,0));
            // coordsD = ivec2(texCoordFloor + ivec2(0,1));
            
            // if (texCoordDelta.x > 1.0 - texCoordDelta.y)
            // {
                // coordsB = ivec2(texCoordFloor + ivec2(1,1));
                // coordsC = ivec2(texCoordFloor + ivec2(0,0));
            // }
            // else
            // {
                // coordsB = ivec2(texCoordFloor + ivec2(0,0));
                // coordsC = ivec2(texCoordFloor + ivec2(1,1));
            // }
        // }
    // }
    // else
    // {
        // if (texCoordDelta.y > 0.5)
        // {
            // coordsA = ivec2(texCoordFloor + ivec2(0,1));
            // coordsD = ivec2(texCoordFloor + ivec2(1,0));
            
            // if (texCoordDelta.x > 1.0 - texCoordDelta.y)
            // {
                // coordsB = ivec2(texCoordFloor + ivec2(1,1));
                // coordsC = ivec2(texCoordFloor);
            // }
            // else
            // {
                // coordsB = ivec2(texCoordFloor);
                // coordsC = ivec2(texCoordFloor + ivec2(1,1));
            // }
        // }
        // else
        // {
            // coordsA = ivec2(texCoordFloor);
            // coordsD = ivec2(texCoordFloor + ivec2(1,1));
            
            // if (texCoordDelta.x > texCoordDelta.y)
            // {
                // coordsB = ivec2(texCoordFloor + ivec2(1,0));
                // coordsC = ivec2(texCoordFloor + ivec2(0,1));
            // }
            // else
            // {
                // coordsB = ivec2(texCoordFloor + ivec2(0,1));
                // coordsC = ivec2(texCoordFloor + ivec2(1,0));
            // }
        // }
    // }

    // ivec4 prevViewIndices0A = texelFetch(viewIndexTextures, ivec3(coordsA, 0), 0);
    // ivec4 prevViewIndices1A = texelFetch(viewIndexTextures, ivec3(coordsA, 1), 0);
    
    // ivec4 prevViewIndices0B = texelFetch(viewIndexTextures, ivec3(coordsB, 0), 0);
    // ivec4 prevViewIndices1B = texelFetch(viewIndexTextures, ivec3(coordsB, 1), 0);
    
    // ivec4 prevViewIndices0C = texelFetch(viewIndexTextures, ivec3(coordsC, 0), 0);
    // ivec4 prevViewIndices1C = texelFetch(viewIndexTextures, ivec3(coordsC, 1), 0);
    
    // ivec4 prevViewIndices0D = texelFetch(viewIndexTextures, ivec3(coordsD, 0), 0);
    // ivec4 prevViewIndices1D = texelFetch(viewIndexTextures, ivec3(coordsD, 1), 0);

    // return int[SAMPLE_COUNT](
        // prevViewIndices0A.x >= 0 ? prevViewIndices0A.x :
        // prevViewIndices0B.x >= 0 ? prevViewIndices0B.x :
        // prevViewIndices0C.x >= 0 ? prevViewIndices0C.x : prevViewIndices0D.x,
        // prevViewIndices0A.y >= 0 ? prevViewIndices0A.y :
        // prevViewIndices0B.y >= 0 ? prevViewIndices0B.y :
        // prevViewIndices0C.y >= 0 ? prevViewIndices0C.y : prevViewIndices0D.y,
        // prevViewIndices0A.z >= 0 ? prevViewIndices0A.z :
        // prevViewIndices0B.z >= 0 ? prevViewIndices0B.z :
        // prevViewIndices0C.z >= 0 ? prevViewIndices0C.z : prevViewIndices0D.z,
        // prevViewIndices0A.w >= 0 ? prevViewIndices0A.w :
        // prevViewIndices0B.w >= 0 ? prevViewIndices0B.w :
        // prevViewIndices0C.w >= 0 ? prevViewIndices0C.w : prevViewIndices0D.w,
        // prevViewIndices1A.x >= 0 ? prevViewIndices1A.x :
        // prevViewIndices1B.x >= 0 ? prevViewIndices1B.x :
        // prevViewIndices1C.x >= 0 ? prevViewIndices1C.x : prevViewIndices1D.x,
        // prevViewIndices1A.y >= 0 ? prevViewIndices1A.y :
        // prevViewIndices1B.y >= 0 ? prevViewIndices1B.y :
        // prevViewIndices1C.y >= 0 ? prevViewIndices1C.y : prevViewIndices1D.y,
        // prevViewIndices1A.z >= 0 ? prevViewIndices1A.z :
        // prevViewIndices1B.z >= 0 ? prevViewIndices1B.z :
        // prevViewIndices1C.z >= 0 ? prevViewIndices1C.z : prevViewIndices1D.z
    // );
}

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

vec4 computeMicrofacetDistributionSample(int index, vec3 diffuseAlbedo, vec3 normalDir, bool useMipmaps)
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
    
    vec4 sampleColor = getProjTexSample(index, useMipmaps);
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
        sum += computeMicrofacetDistributionSample(i, diffuseAlbedo, normalDir, true);
	}
	return sum.rgb / sum.a;
}

vec3 computeMicrofacetDistributionFast(vec3 diffuseAlbedo, vec3 normalDir)
{
    int viewIndices[SAMPLE_COUNT] = readViewIndices();
    
    vec4 sum = vec4(0.0);
	for (int i = 1; i < SAMPLE_COUNT; i++)
	{
        if (viewIndices[i] >= 0 && viewIndices[i] < MAX_CAMERA_POSE_COUNT)
        {
            sum += computeMicrofacetDistributionSample(viewIndices[i], diffuseAlbedo, normalDir, false);
        }
	}
    
    if (sum.a > 0.0)
    {
        return sum.rgb / sum.a;
    }
    else
    {
        return vec3(0);
    }
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
        specularReflectance = computeMicrofacetDistributionFast(diffuseAlbedo, normalDir) 
            * computeGeometricAttenuation(normalize(viewDir), normalize(lightDir), normalDir);
    }
    
    fragColor = vec4(pow(nDotL /* * lightIntensity / dot(lightDir, lightDir)*/
        * (diffuseAlbedo + specularReflectance), vec3(1 / gamma)), 1.0);
}
