#ifndef REFLECTANCE_MULTI_AS_SINGLE_GLSL
#define REFLECTANCE_MULTI_AS_SINGLE_GLSL

uniform int viewIndex;

#include "reflectance.glsl"

#line 9 1002

#define infiniteLightSource (infiniteLightSources)
#define cameraPose 			(cameraPoses[viewIndex])
#define lightPosition 		(lightPositions[getLightIndex(viewIndex)].xyz)
#define lightIntensity 		(getLightIntensity(viewIndex).rgb)

vec3 getViewVector()
{
    return getViewVector(viewIndex);
}

vec3 getLightVector()
{
    return getLightVector(viewIndex);
}

vec4 getColor()
{
    return getColor(viewIndex);
}

vec4 getLinearColor()
{
    return linearizeColor(getColor());
}

#endif // REFLECTANCE_MULTI_AS_SINGLE_GLSL
