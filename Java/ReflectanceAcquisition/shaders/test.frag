#version 130

uniform sampler2D texture0;

void main()
{
	gl_FragColor = texture2D(texture0, gl_TexCoord[0].xy) * vec4(0.75, 0.5, 0.25, 1.0);
}