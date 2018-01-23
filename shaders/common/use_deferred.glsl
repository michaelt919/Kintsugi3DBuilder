#ifndef USE_DEFERRED_GLSL
#define USE_DEFERRED_GLSL

#line 9901 5

in vec2 fTexCoord;

uniform sampler2D positionMap;
uniform sampler2D texCoordMap;
uniform sampler2D normalMap;

vec3 getPosition()
{
    return texture(positionMap, fTexCoord).xyz;
}

vec2 getTexCoord()
{
    return texture(texCoordMap, fTexCoord).xy;
}

vec3 getNormal()
{
    return texture(normalMap, fTexCoord).xyz;
}

#endif // USE_DEFERRED_GLSL
