#version 130

in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2D texture0;

void main()
{
	 gl_FragColor = //(vec4(0.3) + 0.7*texture2D(texture0, fTexCoord.xy)) * 
		 //vec4(0.75 * fTexCoord.x, 0.5 * fTexCoord.y, 0.25, 1.0) *
		 vec4(0.75, 0.5, 0.25, 1.0) *
		 vec4(vec3(normalize(fNormal).z), 1.0f);
}