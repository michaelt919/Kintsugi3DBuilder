#version 330

#define SAMPLE_COUNT 7

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024
#define MAX_LIGHT_COUNT 1024

#define MAX_VIRTUAL_LIGHT_COUNT 4

uniform bool occlusionEnabled;
uniform bool shadowTestingEnabled;

uniform float weightExponent;
uniform float occlusionBias;
uniform float gamma;
uniform float fresnelStrength;
uniform bool infiniteLightSources;

uniform sampler1D luminanceMap;

uniform mat4 model_view;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

in vec3 fViewPos;
uniform vec3 lightIntensity[MAX_VIRTUAL_LIGHT_COUNT];
uniform vec3 lightPos[MAX_VIRTUAL_LIGHT_COUNT];
uniform int virtualLightCount;

uniform sampler2DArray imageTextures;
uniform sampler2DArray depthTextures;
uniform sampler2DArray shadowTextures;

//uniform isampler2DArray viewIndexTextures;

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

uniform ShadowMatrices
{
    mat4 shadowMatrices[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 fragColor;

// int[SAMPLE_COUNT] readViewIndices()
// {
    // ivec4 prevViewIndices0 = texture(viewIndexTextures, vec3(fTexCoord, 0));
    // ivec4 prevViewIndices1 = texture(viewIndexTextures, vec3(fTexCoord, 1));

    // return int[SAMPLE_COUNT](
        // prevViewIndices0.x,
        // prevViewIndices0.y,
        // prevViewIndices0.z,
        // prevViewIndices0.w,
        // prevViewIndices1.x,
        // prevViewIndices1.y,
        // prevViewIndices1.z
    // );
    
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
//}

float fresnel(vec3 viewDir, vec3 halfwayDir)
{
    float fresnelStrength = 0.95;
    return (1 - fresnelStrength) + 
            fresnelStrength * pow(clamp(1 - dot(viewDir, halfwayDir), 0, 1), 5.0);
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
            else if (shadowTestingEnabled)
            {
                vec4 shadowTexCoord = shadowMatrices[index] * vec4(fPosition, 1.0);
                shadowTexCoord /= shadowTexCoord.w;
                shadowTexCoord = (shadowTexCoord + vec4(1)) / 2;
                
                if (shadowTexCoord.x < 0 || shadowTexCoord.x > 1 || 
                    shadowTexCoord.y < 0 || shadowTexCoord.y > 1 ||
                    shadowTexCoord.z < 0 || shadowTexCoord.z > 1)
                {
                    return vec4(0);
                }
                else
                {
                    float shadowImageDepth = texture(shadowTextures, vec3(shadowTexCoord.xy, index)).r;
                    if (abs(shadowTexCoord.z - shadowImageDepth) > occlusionBias)
                    {
                        // Occluded
                        return vec4(0);
                    }
                }
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
		
        //return (color.a < 0.9999 ? 0.0 : 1.0) * vec4(pow(color.rgb, vec3(gamma)), 1.0);
        
        vec3 colorGC = pow(color.rgb, vec3(gamma));
        float scale = colorGC.r + colorGC.g + colorGC.b < 0.001 ? 0.0 : texture(luminanceMap, (colorGC.r + colorGC.g + colorGC.b) / 3).r * 3 / (colorGC.r + colorGC.g + colorGC.b);
        return (color.a < 0.9999 ? 0.0 : 1.0) * vec4(colorGC.rgb * scale, 1.0);
        
        // return (color.a < 0.9999 ? 0.0 : 1.0) * 
            // vec4(texture(luminanceMap, color.r).r, 
                // texture(luminanceMap, color.g).r, 
                // texture(luminanceMap, color.b).r, 
                // 1.0);
	}
}

float computeGeometricAttenuation(vec3 view, vec3 light, vec3 normal)
{
    vec3 halfAngle = normalize(view + light);
    return min(1.0, min(
        2.0 * dot(halfAngle, normal) * dot(view, normal) / dot(view, halfAngle),
        2.0 * dot(halfAngle, normal) * dot(light, normal) / dot(view, halfAngle)))
        / (dot(light, normal) * dot(view, normal));
}

vec4[MAX_VIRTUAL_LIGHT_COUNT] computeSample(int index, vec3 diffuseColor, 
        vec3 normalDir, bool useMipmaps)
{
    vec4 sampleColor = getProjTexSample(index, useMipmaps);
    vec4 result[MAX_VIRTUAL_LIGHT_COUNT];
    
    // All in camera space
    vec3 fragmentPos = (cameraPoses[index] * vec4(fPosition, 1.0)).xyz;
    vec3 virtualViewDir = normalize((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz - fragmentPos);
    vec3 sampleViewDir = normalize(-fragmentPos);
    vec3 sampleLightDirUnnorm = lightPositions[lightIndices[index]].xyz - fragmentPos;
    vec3 sampleLightDir = normalize(sampleLightDirUnnorm);
    vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
    vec3 normalDirCameraSpace = (cameraPoses[index] * vec4(normalDir, 0.0)).xyz;
    
    float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
    float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
    
    vec3 diffuseContrib = diffuseColor * nDotL;
    float invLightAtten = (infiniteLightSources ? 1.0 : dot(sampleLightDirUnnorm, sampleLightDirUnnorm));
    float maxSpecular = invLightAtten - max(max(diffuseContrib.r, diffuseContrib.g), diffuseContrib.b);
    
    vec3 specularResidual = min(vec3(maxSpecular), sampleColor.rgb * invLightAtten - diffuseContrib);
    
    vec4 precomputedSample = nDotV * vec4(specularResidual
        //    / lightIntensities[lightIndices[index]] // TODO uncomment this
            / computeGeometricAttenuation(sampleViewDir, sampleLightDir, normalDirCameraSpace)
            / fresnel(sampleViewDir, sampleHalfDir), 
        sampleColor.a * nDotL);
        
    for (int lightPass = 0; lightPass < MAX_VIRTUAL_LIGHT_COUNT; lightPass++)
    {
        vec3 virtualLightDir = normalize((cameraPoses[index] * vec4(lightPos[lightPass], 1.0)).xyz - fragmentPos);
        vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);

        // Compute sample weight
        float weight = computeSampleWeight(virtualHalfDir, sampleHalfDir);
        result[lightPass] = weight * precomputedSample;
    }
    
    return result;
}

vec3[MAX_VIRTUAL_LIGHT_COUNT] computeMicrofacetDistributions(
        vec3 diffuseColor, vec3 normalDir)
{
	vec4[MAX_VIRTUAL_LIGHT_COUNT] sums;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        sums[i] = vec4(0.0);
    }
    
	for (int i = 0; i < cameraPoseCount; i++)
	{
        vec4[MAX_VIRTUAL_LIGHT_COUNT] microfacetSample = 
            computeSample(i, diffuseColor, normalDir, true);
        
        for (int j = 0; j < MAX_VIRTUAL_LIGHT_COUNT; j++)
        {
            sums[j] += microfacetSample[j];
        }
	}
    
    vec3[MAX_VIRTUAL_LIGHT_COUNT] results;
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT; i++)
    {
        results[i] = sums[i].rgb / sums[i].a;
    }
	return results;
}

// vec3 computeMicrofacetDistributionsFast(vec3 diffuseColor, vec3 normalDir)
// {
    // int viewIndices[SAMPLE_COUNT] = readViewIndices();
    
    // vec4 sum = vec4(0.0);
	// for (int i = 1; i < SAMPLE_COUNT; i++)
	// {
        // if (viewIndices[i] >= 0 && viewIndices[i] < MAX_CAMERA_POSE_COUNT)
        // {
            // // TODO multiple light sources
            // sum += computeSample(viewIndices[i], diffuseColor, normalDir, false)[0];
        // }
	// }
    
    // if (sum.a > 0.0)
    // {
        // return sum.rgb / sum.a;
    // }
    // else
    // {
        // return vec3(0);
    // }
// }

void main()
{
    vec3 viewDir = normalize(fViewPos - fPosition);
    
    vec3 normalDir;
    if (useNormalTexture)
    {
        normalDir = normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0));
    }
    else
    {
        normalDir = normalize(fNormal);
    }
    
    vec3 diffuseColor;
    if (useDiffuseTexture)
    {
        diffuseColor = pow(texture(diffuseMap, fTexCoord).rgb, vec3(gamma));
    }
    else
    {
        diffuseColor = vec3(0.0);
    }
    
    vec3[] microfacetDistributions = computeMicrofacetDistributions(
            diffuseColor, normalDir);
    vec3 reflectance = vec3(0.0);
    
    for (int i = 0; i < MAX_VIRTUAL_LIGHT_COUNT && i < virtualLightCount; i++)
    {
        vec3 lightDirUnNorm = lightPos[i] - fPosition;
        vec3 lightDir = normalize(lightDirUnNorm);
        float nDotL = max(0.0, dot(normalDir, lightDir));
        
        if (nDotL > 0.0)
        {
            reflectance += nDotL * (diffuseColor + 
                microfacetDistributions[i]
                    * computeGeometricAttenuation(viewDir, lightDir, normalDir)
                    * fresnel(viewDir, normalize(viewDir + lightDir) /* halfway */))
                * lightIntensity[i]
                * (infiniteLightSources ? 1.0 : 1.0 / 
                    dot(lightDirUnNorm, lightDirUnNorm));
        }
    }
    
    fragColor = vec4(pow(reflectance, vec3(1 / gamma)), 1.0);
}
