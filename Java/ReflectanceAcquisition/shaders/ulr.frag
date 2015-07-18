#version 330

#define SAMPLE_COUNT 7

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024

uniform bool occlusionEnabled;

uniform float weightExponent;
uniform float occlusionBias;
uniform float gamma;

uniform mat4 model_view;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

in vec3 fViewPos;

uniform sampler2DArray imageTextures;
uniform sampler2DArray depthTextures;

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

layout(location = 0) out vec4 fragColor;

float computeSampleWeight(vec3 cameraPos, vec3 samplePos, vec3 fragmentPos)
{
	return 1.0 / (1.0 - pow(max(0.0, dot(normalize(samplePos - fragmentPos), 
		normalize(cameraPos - fragmentPos))), weightExponent)) - 1.0;
}

float getSampleWeight(int index)
{
    return computeSampleWeight((cameraPoses[index] * vec4(fViewPos, 1.0)).xyz, vec3(0.0), 
        (cameraPoses[index] * vec4(fPosition, 1.0)).xyz);
}

vec4 getLightFieldSample(int index)
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
        
        vec4 color = texture(imageTextures, vec3(projTexCoord.xy, index));
		
		return (color.a < 0.9999 ? 0.0 : 1.0) * vec4(pow(color.rgb, vec3(gamma)), 1.0);
	}
}

vec4 getLightFieldSampleNoMipmap(int index)
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
        
        vec4 color = textureLod(imageTextures, vec3(projTexCoord.xy, index), 1.0);
		
		return (color.a < 0.9999 ? 0.0 : 1.0) * vec4(pow(color.rgb, vec3(gamma)), 1.0);
	}
}

vec4 computeLightFieldBuehler()
{
    float weights[SAMPLE_COUNT];
    int indices[SAMPLE_COUNT];
    
    // Initialization
    for (int i = 0; i < SAMPLE_COUNT; i++)
    {
        weights[i] = -1.0 / 0.0;
        indices[i] = -1;
    }
    
    // Partial heapsort
    for (int i = 0; i < cameraPoseCount; i++)
    {
        float weight = getSampleWeight(i);
        if (weight >= weights[0]) // Decide if the new view goes in the heap
        {
            // Replace the min node in the heap with the new one
            weights[0] = weight;
            indices[0] = i;
            
            int currentIndex = 0;
            int minIndex = -1;
            
            while (currentIndex != -1)
            {
                // The two "children" in the heap
                int leftIndex = 2*currentIndex+1;
                int rightIndex = 2*currentIndex+2;
                
                // Find the smallest of the current node, and its left and right children
                if (leftIndex < SAMPLE_COUNT && weights[leftIndex] < weights[currentIndex])
                {
                    minIndex = leftIndex;
                }
                else
                {
                    minIndex = currentIndex;
                }
                
                if (rightIndex < SAMPLE_COUNT && weights[rightIndex] < weights[minIndex])
                {
                    minIndex = rightIndex;
                }
                
                // If a child is smaller than the current node, then swap
                if (minIndex != currentIndex)
                {
                    float weightTmp = weights[currentIndex];
                    int indexTmp = indices[currentIndex];
                    weights[currentIndex] = weights[minIndex];
                    indices[currentIndex] = indices[minIndex];
                    weights[minIndex] = weightTmp;
                    indices[minIndex] = indexTmp;
                
                    currentIndex = minIndex;
                }
                else
                {
                    currentIndex = -1; // Signal to quit
                }
            }
        }
    }
    
    // Evaluate the light field
    // Because of the min-heap property, weights[0] should be the smallest weight
    vec4 sum = vec4(0.0);
	for (int i = 1; i < SAMPLE_COUNT; i++)
	{
		sum += (weights[i] - weights[0]) * getLightFieldSampleNoMipmap(indices[i]);
	}
    
	return pow(sum / sum.a, vec4(1 / gamma));
}

vec4 computeLightField()
{
	vec4 sum = vec4(0.0);
	for (int i = 0; i < cameraPoseCount; i++)
	{
		sum += getSampleWeight(i) * getLightFieldSample(i);
	}
	return pow(sum / sum.a, vec4(1 / gamma));
}

void main()
{
    fragColor = computeLightField();
	//fragColor = computeLightFieldBuehler();
}
