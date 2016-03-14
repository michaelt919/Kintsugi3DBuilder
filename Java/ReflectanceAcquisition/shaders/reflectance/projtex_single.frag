#version 330

uniform bool occlusionEnabled;
uniform float occlusionBias;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

uniform sampler2D viewImage;
uniform sampler2D depthImage;

uniform mat4 cameraPose;
uniform mat4 cameraProjection;
uniform vec3 lightPosition;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 halfAngleVector;
layout(location = 2) out vec4 projTexCoord;

void main()
{
    projTexCoord = cameraProjection * cameraPose * vec4(fPosition, 1.0);
    projTexCoord /= projTexCoord.w;
    projTexCoord = (projTexCoord + vec4(1)) / 2;
	
	if (projTexCoord.x < 0 || projTexCoord.x > 1 || projTexCoord.y < 0 || projTexCoord.y > 1 ||
            projTexCoord.z < 0 || projTexCoord.z > 1)
	{
		discard;
	}
	else
	{
		if (occlusionEnabled)
		{
			float imageDepth = texture(depthImage, projTexCoord.xy).r;
			if (abs(projTexCoord.z - imageDepth) > occlusionBias)
			{
				// Occluded
				discard;
			}
		}
        
        fragColor = vec4(texture(viewImage, projTexCoord.xy).rgb, 1.0);
        
        vec3 normal = normalize(fNormal);
        vec3 tangent = normalize(fTangent - dot(normal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(normal, fBitangent) * normal 
            - dot(tangent, fBitangent) * tangent);
        
        mat3 tangentToObject = mat3(tangent, bitangent, normal);
        mat3 objectToTangent = transpose(tangentToObject);
        
        vec3 view = normalize(transpose(mat3(cameraPose)) * -cameraPose[3].xyz - fPosition);
        vec3 light = normalize(transpose(mat3(cameraPose))
                        * (lightPosition - cameraPose[3].xyz) - fPosition);
                        
        fragColor = vec4(texture(viewImage, projTexCoord.xy).rgb, light.z);
        halfAngleVector = vec4(objectToTangent * normalize(view + light), dot(light, normal));
	}
}
