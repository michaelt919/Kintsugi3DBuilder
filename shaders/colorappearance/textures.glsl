#line 2 1020

#ifndef DIFFUSE_TEXTURE_ENABLED
#define DIFFUSE_TEXTURE_ENABLED 0
#endif

#ifndef SPECULAR_TEXTURE_ENABLED
#define SPECULAR_TEXTURE_ENABLED 0
#endif

#ifndef ROUGHNESS_TEXTURE_ENABLED
#define ROUGHNESS_TEXTURE_ENABLED 0
#endif

#ifndef NORMAL_TEXTURE_ENABLED
#define NORMAL_TEXTURE_ENABLED 0
#endif

#ifndef TANGENT_SPACE_NORMAL_MAP
#define TANGENT_SPACE_NORMAL_MAP 1
#endif

#if DIFFUSE_TEXTURE_ENABLED
uniform sampler2D diffuseMap;
#endif

#if SPECULAR_TEXTURE_ENABLED
uniform sampler2D specularMap;
#endif

#if ROUGHNESS_TEXTURE_ENABLED
uniform sampler2D roughnessMap;
#endif

#if NORMAL_TEXTURE_ENABLED
uniform sampler2D normalMap;

vec3 getNormal(vec2 texCoord)
{
    vec2 normalXY = texture(normalMap, texCoord).xy * 2 - 1;
    return vec3(normalXY, 1.0 - dot(normalXY, normalXY));
}
#endif
