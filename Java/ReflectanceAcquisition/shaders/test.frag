#version 130

//in vec2 fTexCoord;

uniform sampler2D texture0;

void main()
{
	gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);
	//gl_FragColor = texture2D(texture0, fTexCoord.xy) * vec4(0.75, 0.5, 0.25, 1.0);
}