#version 400

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2D texture0;
uniform mat4 texMatrix;

void main()
{
	vec4 projPos = texMatrix * vec4(fPosition, 1.0);
	projPos = projPos / projPos.w;
	
	vec2 texCoord = vec2((projPos.x / 2 + 0.5), (-projPos.y / 2 + 0.5));
	
	if (texCoord.x < 0 || texCoord.x > 1 || texCoord.y < 0 || texCoord.y > 1)
	{
		gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
	}
	else
	{
		gl_FragColor = texture2D(texture0, texCoord.xy);
	}
}