#version 330

#define SAMPLE_COUNT 8

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
uniform sampler2D testTexture; // TODO don't think this is needed anymore

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

uniform mat4 cameraProj; // TODO don't think this is needed anymore

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
    
    // Insertion sort(ish)
    for (int i = 0; i < cameraPoseCount; i++)
    {
        float weight = getSampleWeight(i);
        if (weight >= weights[0])
        {
            int j = 0;
            int jj = 1;
            
            while(j+1 < SAMPLE_COUNT && weight >= weights[j+1])
            {
                if (jj != j+1) // TODO why is this necessary?
                {
                    return vec4(0);
                }
                weights[j] = weights[j+1];
                indices[j] = indices[j+1];
                jj++;
                j++;
            }
            
            // Insert the new weight & index
            weights[j] = weight;
            indices[j] = i;
        }
    }
    
    // Evaluate the light field
    vec4 sum = vec4(0.0);
	for (int i = 1; i < SAMPLE_COUNT; i++)
	{
		sum += (weights[i] - weights[0]) * getLightFieldSample(indices[i]);
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
