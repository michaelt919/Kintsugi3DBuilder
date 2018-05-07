#version 330

in vec2 fTexCoord;

layout(location = 0) out vec4 residual;

uniform vec2 minTexCoord;
uniform vec2 maxTexCoord;

#include "../common/use_deferred.glsl"

#define fPosition ( getPosition(fTexCoord) )

#include "../colorappearance/colorappearance_multi_as_single.glsl"
#include "../colorappearance/imgspace.glsl"
#include "resid.glsl"

#line 24 0

void main()
{
    vec3 normal = getNormal(fTexCoord);
    if (normal == vec3(0))
    {
        discard;
    }
    else
    {
        residual = computeResidual(mix(minTexCoord, maxTexCoord, fTexCoord), normal);
    }
}
