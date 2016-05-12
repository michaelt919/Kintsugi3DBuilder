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
layout(location = 5) out vec4 uvwMapping;

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
        
        vec3 binormal = normalize( cross( normal, tangent ) );
        mat3 rotationMatrix = transpose( mat3( tangent, binormal, normal ) );
        
        view = rotationMatrix * view;
        light = rotationMatrix * light;
        
        vec3 halfVector = normalize( light + view ); 
        float halfTheta = acos( halfVector.z );
        float halfPhi = atan( halfVector.y , halfVector.x );
        
        float diffTheta = acos( dot( light, halfVector ) );
       
       // rotation about z-axis with negative halfPhi
       	mat3 Rz = mat3( cos( halfPhi ), -sin( halfPhi ), 0,
       					sin( halfPhi ), cos( halfPhi ), 0,
       					0, 0, 1 );
       	
       	// rotation about y-axis with negative halfTheta
       	mat3 Ry = mat3( cos( halfTheta ), 0, sin( halfTheta ),
       					0, 1, 0,
       					-sin( halfTheta ), 0, cos( halfTheta ) );
       
       	
       	// R is rotation matrix to rotate halfVector into north pole
      	mat3 R = Ry * Rz;
       
       	// light vector rotated by matrix R which becomes difference vector
       	vec3 rotatedLight = R * light; 
       	vec3 testVector = R * halfVector;
       	float diffPhi = atan( rotatedLight.y , rotatedLight.x );
       	 
       	// halfTheta : [0, pi/2), diffPhi : [0, pi), diffTheta : [0, pi/2)     
       	// divide by pi to scale it to be between 0 and 1. 
        halfAngles = vec4( 2 * halfTheta / 3.1415927, (halfPhi / 3.1415927 + 1.0) / 2.0, 0, 1.0 );
        differenceAngles = vec4( 2 * diffTheta / 3.1415927, (diffPhi / 3.1415927 + 1.0) / 2.0, 0, 1.0 );
        
        
       	// (u, v, w) mapping
       	float u = sin( halfTheta ) * cos( 2 * diffPhi );
       	float v = sin( halfTheta ) * sin( 2 * diffPhi );
       	float w = 2 * diffTheta / 3.1415927;
       	w = w;
		
        fragColor = vec4(pow(texture(viewImage, projTexCoord.xy).rgb, vec3(2.2)), 1.0);
		//fragColor = vec4(texture(viewImage, projTexCoord.xy).rgb, 1.0 );
		
		if( light.z > 0.5){
			uvwMapping = vec4( u, v, w, 1.0 );
			
			// Gamma correct the n dot l factor
			fragColor = vec4( fragColor.xyz / light.z, 1.0 );
			predictedColor = vec4( vec3((0.5 * light.z + 0.5 * pow(max(halfVector.z, 0.0), 25.0)) / light.z), 1.0);
			//predictedColor = vec4( vec3(0.5) + 0.5 * pow(max(halfVector.z, 0.0), 25.0), 1.0);
			//fragColor = predictedColor;
		}
		else{
			uvwMapping = vec4( 0.0 );
			predictedColor = vec4(0.0);
			fragColor = vec4( 0.0 );
			//fragColor = vec4( 1.0, 0, 0, 0 );
		}
			
       	//projTexCoord = vec4( testVector, 1.0 ); 
		
        // TEST FOR DIFF THETA AND DIFF PHI
        //projTexCoord = vec4( diffTheta, 0, 0, 1 );
        //diffTheta = acos((R*light).z);
        //differenceAngles = vec4( diffTheta, 0, 0, 1 );
        //halfAngles = vec4( acos(rotatedLight.x / sin( diffTheta )), 0, 0, 1 );
		//uvwMapping = vec4( diffPhi, 0, 0, 1 );
		
        //predictedColor = vec4(vec3(max(dot(light, normal), 0.0) + pow(max(dot(normal, halfVector), 0.0), 25.0)), 1.0);
        //predictedColor = vec4( vec3(0.5 * pow(max(halfVector.z, 0.0), 25.0)), 1.0);
		//predictedColor = vec4(vec3(max(light.z, 0.0) + pow(max(halfVector.z, 0.0), 25.0)), 1.0);

	}
}
