#version 330

uniform mat4 model_view;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec4 fTangent;

in vec3 fViewPos;

//uniform mat4 cameraPose;
//uniform vec3 lightPosition;

layout(location = 0) out vec4 fragColor;

uniform int constantCount;

uniform Constants
{
	vec4 constants[4];
};

uniform Lambdas
{
	vec4 lambdas[2000];
};

uniform Thetas
{
	vec4 thetas[2000];
};

void main()
{
	vec3 lightPosition = vec3(10, 0, 0);
	vec3 view = normalize(fViewPos - fPosition);
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
	float diffPhi = atan( rotatedLight.y , rotatedLight.x );
	 
	// halfTheta : [0, pi/2), diffPhi : [0, pi), diffTheta : [0, pi/2)     
	// divide by pi to scale it to be between 0 and 1. 
	vec4 halfAngles = vec4( 2 * halfTheta / 3.1415927, (halfPhi / 3.1415927 + 1.0) / 2.0, 0, 1.0 );
	vec4 differenceAngles = vec4( 2 * diffTheta / 3.1415927, (diffPhi / 3.1415927 + 1.0) / 2.0, 0, 1.0 );
	
	// (u, v, w) mapping
	float u = sin( halfTheta ) * cos( 2 * diffPhi );
	float v = sin( halfTheta ) * sin( 2 * diffPhi );
	float w = 2 * diffTheta / 3.1415927;
	
	vec3 uvwMapping = vec3( u, v, w );
	vec3 firstPart = constants[0].xyz + u * constants[1].xyz + v * constants[2].xyz + w * constants[3].xyz;
	
	vec3 ambient = vec3(0, 0, 0);
	vec3 diffuseProduct = vec3(0.5, 0.5, 0);
	vec3 specularProduct = vec3(0.6, 0.6, 0.5);
	float kd = max( light.z, 0.0 );
	float ks = pow(max( halfVector.z, 0.0 ), 20);
	vec3 diffuse = kd * diffuseProduct;
	vec3 specular = ks * specularProduct;
	
	// TODO: pass in the number of sample points instead of hard coding to 20
	int N = 2000;
	vec3 sum = firstPart;
	for (int i = 0; i < N; i++)
	{
		sum += length(uvwMapping - thetas[i].xyz) * lambdas[i].xyz;
	}
	
	//fragColor = vec4( constants[2].xyz /2 + vec3(0.5) ,1);
	//fragColor = vec4(firstPart/2 + vec3(0.5), 1);
	fragColor = vec4( vec3(1) * (u+0.5), 1);
	//fragColor = vec4( vec3(1) * diffPhi, 1 );
}
