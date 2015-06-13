#version 330

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024
#define MAX_LIGHT_POSITION_COUNT 1024

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray viewImages;
uniform sampler2DArray depthImages;
uniform int viewCount;
uniform float gamma;

uniform bool occlusionEnabled;
uniform float occlusionBias;

uniform float delta;
uniform int iterations;
uniform float determinantThreshold;
uniform float fit1Weight;
uniform float fit3Weight;

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

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normalMap;
layout(location = 2) out vec4 ambient;
layout(location = 3) out vec4 debug;

struct DiffuseFit
{
    vec3 color;
    vec3 normal;
    vec3 ambient;
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

bool validateFit(DiffuseFit fit)
{
    return ! isnan(fit.color.r) && ! isnan(fit.color.g) && ! isnan(fit.color.b) &&
            ! isinf(fit.color.r) && ! isinf(fit.color.g) && ! isinf(fit.color.b) &&
            ! isnan(fit.normal.x) && ! isnan(fit.normal.y) && ! isnan(fit.normal.z) &&
            ! isinf(fit.normal.x) && ! isinf(fit.normal.y) && ! isinf(fit.normal.z) &&
            ! isnan(fit.ambient.r) && ! isnan(fit.ambient.g) && ! isnan(fit.ambient.b) &&
            ! isinf(fit.ambient.r) && ! isinf(fit.ambient.g) && ! isinf(fit.ambient.b);
}

DiffuseFit fitDiffuse()
{
    vec3 geometricNormal = normalize(fNormal);
    
    DiffuseFit fit = DiffuseFit(vec3(0), vec3(0), vec3(0));
    
    for (int k = 0; k < iterations; k++)
    {
        //mat4 a = mat4(0);
        //mat4 b = mat4(0);
        mat3 a = mat3(0);
        mat3 b = mat3(0);
        vec4 weightedSum = vec4(0.0);
        float nDotLSum = 0.0;
        
        for (int i = 0; i < viewCount; i++)
        {
            vec3 view = getViewVector(i);
            vec4 color = getColor(i);
            float nDotV = dot(geometricNormal, view);
            if (color.a * nDotV > 0)
            {
                //vec4 light = vec4(getLightVector(i), 1.0);
                vec3 light = getLightVector(i);
                
                float weight = color.a * nDotV;
                if (k != 0)
                {
                    vec3 error = color.rgb - fit.color * dot(fit.normal, light);
                    weight *= exp(-dot(error,error)/(2*delta*delta));
                }
                    
                a += weight * outerProduct(light, light);
                //b += color.a * nDotV * outerProduct(light, vec4(color.rgb, 0.0));
                b += weight * outerProduct(light, color.rgb);
                weightedSum += weight * vec4(color.rgb, 1.0);
                nDotLSum += weight * max(0, dot(geometricNormal, light));
            }
        }
        
        vec3 averageColor = weightedSum.rgb / (weightedSum.r + weightedSum.g + weightedSum.b);
        mat3 m = inverse(a) * b;
        vec3 rgbFit = vec3(length(m[0]), length(m[1]), length(m[2]));
        vec3 rgbWeights = weightedSum.rgb / rgbFit;
        
        if (rgbFit.r == 0.0)
        {
            rgbWeights.r = 0.0;
        }
        if (rgbFit.g == 0.0)
        {
            rgbWeights.g = 0.0;
        }
        if (rgbFit.b == 0.0)
        {
            rgbWeights.b = 0.0;
        }
        
        if (rgbWeights.r > rgbWeights.g && rgbWeights.r > rgbWeights.b)
        {
            // Red dominates
            rgbWeights *= rgbFit.r / weightedSum.r;
        }
        else if (rgbWeights.g > rgbWeights.b)
        {
            // Green dominates
            rgbWeights *= rgbFit.g / weightedSum.g;
        }
        else
        {
            // Blue dominates
            rgbWeights *= rgbFit.b / weightedSum.b;
        }
        
        //vec4 solution = m * vec4(rgbWeights, 0.0);
        vec3 solution = m * rgbWeights;
        
        float intensity = length(solution.xyz);
        //float ambientIntensity = solution.w;
    
        float fit3Quality = clamp(fit3Weight * determinant(a) * 
                                    clamp(dot(normalize(solution.xyz), geometricNormal), 0, 1)  / 
                                    (weightedSum.a * determinantThreshold), 0.0, 1.0);
        
        fit.color = clamp(averageColor * intensity, 0, 1) * fit3Quality + 
                        clamp(weightedSum.rgb / nDotLSum, 0, 1) * 
                            clamp(fit1Weight * nDotLSum, 0, 1 - fit3Quality);
        fit.normal = normalize(solution.xyz) * fit3Quality + fNormal * (1 - fit3Quality);
        debug = vec4(fit3Quality, clamp(fit1Weight * nDotLSum, 0, 1 - fit3Quality), 0.0, 1.0);
    }
    
    if (!validateFit(fit))
    {
        fit.color = vec3(0.0);
        fit.normal = vec3(0.0);
        fit.ambient = vec3(0.0);
    }
    
    return fit;
}

void main()
{
    DiffuseFit fit = fitDiffuse();
    diffuseColor = vec4(pow(fit.color, vec3(1 / gamma)), 1.0);
    normalMap = vec4(fit.normal * 0.5 + vec3(0.5), 1.0);
    ambient = vec4(pow(fit.ambient, vec3(1 / gamma)), 1.0);
}
