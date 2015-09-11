#version 330

#define SAMPLE_COUNT 7

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024
#define MAX_LIGHT_COUNT 1024

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform vec3 lightIntensity;
uniform vec3 lightPos;

uniform float weightExponent;
uniform mat4 model_view;

uniform sampler2D normalMap;
uniform bool useNormalTexture;

uniform isampler2DArray viewIndexTextures;

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

layout(location = 0) out ivec4 viewIndices0;
layout(location = 1) out ivec4 viewIndices1;

int[SAMPLE_COUNT] readViewIndices()
{
    ivec2 texCoords = ivec2(round(gl_FragCoord.xy));
    ivec4 prevViewIndices0 = texelFetch(viewIndexTextures, ivec3(texCoords, 0), 0);
    ivec4 prevViewIndices1 = texelFetch(viewIndexTextures, ivec3(texCoords, 1), 0);

    return int[SAMPLE_COUNT](
        prevViewIndices0.x,
        prevViewIndices0.y,
        prevViewIndices0.z,
        prevViewIndices0.w,
        prevViewIndices1.x,
        prevViewIndices1.y,
        prevViewIndices1.z
    );
}

void writeViewIndices(int[SAMPLE_COUNT] viewIndices)
{
    viewIndices0 = ivec4(viewIndices[0], viewIndices[1], viewIndices[2], viewIndices[3]);
    viewIndices1 = ivec4(viewIndices[4], viewIndices[5], viewIndices[6], -1);
}

float computeSampleWeight(vec3 targetDir, vec3 sampleDir)
{
	return 1.0 / (1.0 - pow(max(0.0, dot(targetDir, sampleDir)), weightExponent)) - 1.0;
}

void heapify(inout int indices[SAMPLE_COUNT], inout float weights[SAMPLE_COUNT], int startingIndex)
{
    int currentIndex = startingIndex;
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

void updateViewIndices(inout int indices[SAMPLE_COUNT])
{
    float weights[SAMPLE_COUNT];
    
    bool viewsUsed[MAX_CAMERA_POSE_COUNT];
    for (int i = 0; i < cameraPoseCount; i++)
    {
        viewsUsed[i] = false;
    }
    
    vec3 normalDir;
    if (useNormalTexture)
    {
        normalDir = normalize(texture(normalMap, fTexCoord).xyz * 2 - vec3(1.0));
    }
    else
    {
        normalDir = normalize(fNormal);
    }
    
    vec3 viewPos = transpose(mat3(model_view)) * -model_view[3].xyz;

    // Initialization
    for (int i = 0; i < SAMPLE_COUNT; i++)
    {
        // if (indices[i] >= 0 && indices[i] < MAX_CAMERA_POSE_COUNT)
        // {
            // // All in camera space
            // vec3 fragmentPos = (cameraPoses[i] * vec4(fPosition, 1.0)).xyz;
            // vec3 virtualViewDir = normalize((cameraPoses[i] * vec4(viewPos, 1.0)).xyz - fragmentPos);
            // vec3 sampleViewDir = normalize(-fragmentPos);
            // vec3 virtualLightDir = normalize((cameraPoses[i] * vec4(lightPos, 1.0)).xyz - fragmentPos);
            // vec3 sampleLightDirUnnorm = lightPositions[lightIndices[i]].xyz - fragmentPos;
            // vec3 sampleLightDir = normalize(sampleLightDirUnnorm);
            // vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);
            // vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
            // vec3 normalDirCameraSpace = (cameraPoses[i] * vec4(normalDir, 0.0)).xyz;
            
            // float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
            // float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
            
            // // Compute sample weight
            // weights[i] = computeSampleWeight(virtualHalfDir, sampleHalfDir) * nDotL * nDotV;
            
            // viewsUsed[indices[i]] = true;
        // }
        // else
        {
            weights[i] = -1.0 / 0.0;
            indices[i] = -1;
        }
    }
    
    // // Build heap
    // for (int i = SAMPLE_COUNT / 2 - 1; i >= 0; i--)
    // {
        // heapify(indices, weights, i);
    // }
    
    // Partial heapsort
    for (int i = 0; i < 1; i++)
    {
        if (!viewsUsed[i])
        {
            // All in camera space
            vec3 fragmentPos = (cameraPoses[i] * vec4(fPosition, 1.0)).xyz;
            vec3 virtualViewDir = normalize((cameraPoses[i] * vec4(viewPos, 1.0)).xyz - fragmentPos);
            vec3 sampleViewDir = normalize(-fragmentPos);
            vec3 virtualLightDir = normalize((cameraPoses[i] * vec4(lightPos, 1.0)).xyz - fragmentPos);
            vec3 sampleLightDirUnnorm = lightPositions[lightIndices[i]].xyz - fragmentPos;
            vec3 sampleLightDir = normalize(sampleLightDirUnnorm);
            vec3 virtualHalfDir = normalize(virtualViewDir + virtualLightDir);
            vec3 sampleHalfDir = normalize(sampleViewDir + sampleLightDir);
            vec3 normalDirCameraSpace = (cameraPoses[i] * vec4(normalDir, 0.0)).xyz;
            
            float nDotL = max(0, dot(normalDirCameraSpace, sampleLightDir));
            float nDotV = max(0, dot(normalDirCameraSpace, sampleViewDir));
            
            // Compute sample weight
            float weight = computeSampleWeight(virtualHalfDir, sampleHalfDir) * nDotL * nDotV;
        
            if (weight >= weights[0]) // Decide if the new view goes in the heap
            {
                // Replace the min node in the heap with the new one
                weights[0] = weight;
                indices[0] = i;
                
                heapify(indices, weights, 0);
            }
        }
    }
}

void main()
{
    int[] indices = readViewIndices();
    updateViewIndices(indices);
    writeViewIndices(indices);
}
