#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

uniform bool occlusionEnabled;
uniform float occlusionBias;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

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

layout(location = 0) out vec4 ambientEstimate;
layout(location = 1) out vec4 diffuseEstimate;
layout(location = 2) out vec4 normalEstimate;

void main()
{
	mat4 a = mat4(0);
    vec4 b = vec4(0);
    vec4 sumColor = vec4(0);
    
    for (int i = 0; i < cameraPoseCount; i++)
    {
        vec4 fragPos = cameraPoses[i] * vec4(fPosition, 1.0);
        vec4 projPos = cameraProjections[cameraProjectionIndices[i]] * fragPos;
        projPos = projPos / projPos.w;
	
        vec2 texCoord = vec2(projPos.x / 2 + 0.5, projPos.y / 2 + 0.5);
	
        if (texCoord.x >= 0 && texCoord.x <= 1 && texCoord.y >= 0 && texCoord.y <= 1)
        {
            if (occlusionEnabled)
            {
                float imageDepth = 2*texture(depthTextures, vec3(texCoord.xy, i)).x - 1;
                if (abs(projPos.z - imageDepth) > occlusionBias)
                {
                    // Occluded
                    continue;
                }
            }
        
            vec4 light = vec4(normalize(transpose(mat3(cameraPoses[i])) * -cameraPoses[i][3].xyz - fPosition), 1.0);
            vec4 color = texture(imageTextures, vec3(texCoord.xy, i));
            if ((color.r > 0.0 || color.g > 0.0 || color.b > 0.0) && 
                color.r < 1.0 && color.g < 1.0 && color.b < 1.0)
            {
                float intensity = color.r + color.g + color.b;
                a += color.a * outerProduct(light, light);
                b += color.a * intensity * light;
                sumColor += color.a * vec4(color.rgb, intensity);
            }
        }
    }
    
    vec4 c = inverse(a) * b;
    float ambientIntensity = c.w;
    float diffuseIntensity = length(c.xyz);
    normalEstimate = vec4(c.xyz / diffuseIntensity * 0.5 + vec3(0.5), 1.0);
    vec3 averageColor = sumColor.rgb / sumColor.a;
    ambientEstimate = vec4(averageColor * ambientIntensity, 1.0);
    diffuseEstimate = vec4(averageColor * diffuseIntensity, 1.0);
}
