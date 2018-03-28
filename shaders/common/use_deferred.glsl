#ifndef USE_DEFERRED_GLSL
#define USE_DEFERRED_GLSL

#line 5 9901

uniform sampler2D positionMap;
uniform sampler2D normalMap;

vec3 getPosition(vec2 texCoord)
{
    return texture(positionMap, texCoord).xyz;
}

vec3 getNormal(vec2 texCoord)
{
    vec3 normal = texture(normalMap, texCoord).xyz;

    if (normal == vec3(0))
    {
        return vec3(0);
    }
    else
    {
        return normalize(normal);
    }
}

#endif // USE_DEFERRED_GLSL
