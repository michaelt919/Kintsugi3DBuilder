#version 330

#define MAX_CAMERA_POSE_COUNT 1024
#define MAX_CAMERA_PROJECTION_COUNT 1024

uniform bool occlusionEnabled;
uniform float occlusionBias;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec4 fTangent;

uniform sampler2D viewImage;
uniform sampler2D depthImage;

uniform mat4 cameraPose;
uniform mat4 cameraProjection;
// added to take in light position
uniform vec3 lightPosition;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 projTexCoord;
layout(location = 2) out vec4 halfAngles;
layout(location = 3) out vec4 differenceAngles;
layout(location = 4) out vec4 predictedColor;

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
        
        vec3 view = normalize(transpose(mat3(cameraPose)) * -cameraPose[3].xyz - fPosition);
        vec3 normal = normalize(fNormal);
        vec3 tangent = normalize(fTangent.xyz);
        vec3 light = normalize( lightPosition - fPosition );
        fragColor = vec4(texture(viewImage, projTexCoord.xy).rgb, 1.0);
        
        vec3 halfVector = normalize( light + view ); 
        float halfTheta = acos( dot( halfVector, normal ) );
        float diffTheta = acos( dot( view, halfVector ) );
        
        //float halfPhi = acos( dot( tangent, halfVector ) / ( sin( acos( dot( normal, halfVector ) ) ) ) );
        float halfPhi = acos( dot( tangent, halfVector ) / ( sin( halfTheta ) ) );
        float diffPhi = acos( dot( normalize( cross( light, halfVector ) ), normalize( cross( normal, halfVector ) ) ) ); 
              
        float pi = -1.0f;
              
        halfAngles = vec4( halfTheta / acos(pi), halfPhi / acos(pi), 0, 1.0 );
        differenceAngles = vec4( diffTheta / acos(pi), diffPhi / acos(pi), 0, 1.0 );
        
        predictedColor = vec4(vec3(max(dot(light, normal), 0.0) + pow(max(dot(normal, halfVector), 0.0), 25.0)), 1.0);
        
               
        //float dotProductInputVector = dot( light, normal );
        //float inputAngle = acos( dotProductInputVector );
        //float dotProductOutputVector = dot(view, normal );
        //float outputAngle = acos( dotProductOutputVector ); 
	}
}
