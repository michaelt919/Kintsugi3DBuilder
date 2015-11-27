#version 330

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024
#define MAX_LIGHT_COUNT 1024

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray viewImages;
uniform sampler2DArray depthImages;
uniform int viewCount;
uniform float gamma;

uniform bool occlusionEnabled;
uniform float occlusionBias;
uniform bool infiniteLightSources;

uniform float delta;
uniform int iterations;

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

layout(location = 0) out vec4 lightPosition;
layout(location = 1) out vec4 lightIntensity;

struct LightFit
{
    vec3 position;
    float intensity;
    float quality;
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

bool validateFit(LightFit fit)
{
    return  ! isnan(fit.position.x) && ! isnan(fit.position.y) && ! isnan(fit.position.z) &&
            ! isinf(fit.position.x) && ! isinf(fit.position.y) && ! isinf(fit.position.z) &&
            ! isnan(fit.intensity)  && ! isnan(fit.intensity) &&
            ! isinf(fit.quality)    && ! isinf(fit.quality);
}

LightFit fitLight()
{
    vec3 normal = normalize(fNormal);
    
    LightFit fit = LightFit(vec3(0), 0, 0);
    float weightedIntensitySum;
    float weightSum;
    
    for (int k = 0; k < iterations; k++)
    {
        weightedIntensitySum = 0;
        weightSum = 0;
    
        mat3 a = mat3(0);
        vec3 b = vec3(0);
        
        for (int i = 0; i < viewCount; i++)
        {
            vec3 viewNormal = (cameraPoses[i] * vec4(normal, 0.0)).xyz;
            vec3 surfacePosition = (cameraPoses[i] * vec4(fPosition, 1.0)).xyz;
            float nDotV = dot(viewNormal, normalize(-surfacePosition));
            vec4 color = getColor(i);
            
            if (color.a * nDotV > 0)
            {
                float lightSqr = dot(surfacePosition, surfacePosition);
                vec3 scaledNormal = viewNormal * inversesqrt(lightSqr) / lightSqr;
                vec3 sampleVector = vec3(scaledNormal.xy, -dot(scaledNormal, surfacePosition));
                float intensity = dot(color.rgb, vec3(1));
                
                float weight = color.a * nDotV;
                if (k != 0)
                {
                    float error = intensity - fit.intensity * dot(viewNormal, fit.position) / lightSqr;
                    weight *= exp(-error*error/(2*delta*delta));
                }
                    
                a += weight * outerProduct(sampleVector, sampleVector);
                b += weight * intensity * sampleVector;
                weightedIntensitySum += weight * intensity;
                weightSum += weight;
            }
        }
        
        vec3 solution = inverse(a) * b;
        fit.position = vec3(solution.xy, 0.0) / solution.z;
        fit.intensity = solution.z;
        fit.quality = clamp(solution.z * determinant(a) / weightSum, 0.0, 1.0);
    }
    
    if (!validateFit(fit))
    {
        fit.position = vec3(0.0);
        fit.intensity = 0.0;
        fit.quality = 0.0;
    }
    else
    {
        // ratio of outgoing light to target intensity value 
        // (so that the diffuse albedo map ends up in an appropriate range)
        fit.intensity *= weightSum / weightedIntensitySum;
    }
    
    return fit;
}

void main()
{
    LightFit fit = fitLight();
    lightPosition = vec4(fit.position, fit.quality);
    lightIntensity = vec4(vec3(fit.intensity), fit.quality);
}
