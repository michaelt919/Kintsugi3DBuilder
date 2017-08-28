#version 330

in vec3 fPosition;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out int fragObjectID;

uniform int objectID;
uniform vec3 color;
uniform float width;
uniform float threshold;
uniform float maxAngle;
uniform mat4 model_view;
uniform mat4 projection;

void main()
{
    if (abs(atan(fPosition.y, fPosition.x)) > maxAngle)
    {
        discard;
    }
    else
    {
        vec2 thinDirection = normalize(transpose(model_view)[2].xy);
        vec2 thickDirection = vec2(-thinDirection.y, thinDirection.x);

        float cosTheta = normalize(model_view[2].xyz).z;
        vec2 diff = fPosition.xy - normalize(fPosition.xyz).xy / 2;
        vec2 diffScaled = dot(diff, thinDirection) * abs(cosTheta) * thinDirection
            + dot(diff, thickDirection) * max(0.125, abs(cosTheta)) * thickDirection;

        float intensity = exp(-4 * dot(diffScaled, diffScaled) / (width * width)) - threshold;

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
}
