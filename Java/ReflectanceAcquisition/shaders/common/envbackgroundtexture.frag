#version 330

uniform sampler2D env;
in vec2 fTexCoord;
out vec4 fragColor;

uniform mat4 model_view;
uniform mat4 projection;

uniform mat4 envMapMatrix;

#define PI 3.1415926535897932384626433832795

void main()
{
	vec4 unprojected = inverse(projection) * vec4(fTexCoord * 2 - vec2(1), 0, 1);

	vec3 viewDir = 
		normalize((envMapMatrix * inverse(model_view) * vec4(unprojected.xyz / unprojected.w, 0.0)).xyz);

	vec2 texCoords = vec2(atan(-viewDir.x, -viewDir.z) / 2, asin(viewDir.y)) / PI + vec2(0.5);
		
	// To prevent seams when the texture wraps around
	
	// OpenGL 4.0 version
	// float lod1 = textureQueryLod(env, texCoords).y;
	// float lod2 = textureQueryLod(env, mod(texCoords + vec2(0.5, 0.0), 1.0) - vec2(0.5, 0.0)).y;
		
    // fragColor = textureLod(env, texCoords, min(lod1, lod2));
	
	// Alternative that doesn't require OpenGL 4:
	vec4 color1 = texture(env, texCoords);
	vec4 color2 = texture(env, mod(texCoords + vec2(0.5, 0.0), 1.0) - vec2(0.5, 0.0));
	fragColor = mix(color1, color2, 2.0 * abs(texCoords.x - 0.5));
}