#version 330

in vec3 fPosition;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

uniform int objectID;
uniform vec3 color;
uniform float width;
uniform float threshold;
uniform mat4 model_view;
uniform mat4 projection;

void main()
{
    vec4 transformedPosition = projection * model_view * vec4(fPosition, 1);
    transformedPosition /= transformedPosition.w;

    vec4 transformedCirclePosition = projection * model_view * vec4(normalize(fPosition) / 2, 1);
    transformedCirclePosition /= transformedCirclePosition.w;

    vec2 diff = transformedPosition.xy - transformedCirclePosition.xy;

    float intensity = exp((4 * dot(diff, diff)) / width * width) - threshold;

    if (intensity < 0)
    {
        discard;
    }
    else
    {
        fragColor = intensity * vec4(color, 1.0);
        fragObjectID = objectID;
    }
}
