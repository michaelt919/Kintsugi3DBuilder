
#ifndef BASIS_GLSL
#define BASIS_GLSL

#include <subject/subject.glsl>


uniform vec3 defaultDiffuseColor;

#ifndef DEFAULT_DIFFUSE_COLOR
#if SPECULAR_TEXTURE_ENABLED
#define DEFAULT_DIFFUSE_COLOR (vec3(0.0))
#else
#define DEFAULT_DIFFUSE_COLOR (defaultDiffuseColor)
#endif // !SPECULAR_TEXTURE_ENABLED
#endif // DEFAULT_DIFFUSE_COLOR

#ifndef DEFAULT_SPECULAR_COLOR
#if DIFFUSE_TEXTURE_ENABLED
#define DEFAULT_SPECULAR_COLOR (vec3(0.0))
#else
#define DEFAULT_SPECULAR_COLOR (vec3(0.04))
#endif // DIFFUSE_TEXTURE_ENABLED
#endif // DEFAULT_SPECULAR_COLOR

#ifndef DEFAULT_SPECULAR_ROUGHNESS
#define DEFAULT_SPECULAR_ROUGHNESS (0.1); // TODO pass in a default?
#endif

#include <colorappearance/material.glsl>
#include <specularfit/evaluateBRDF.glsl>


#line 49 3102

vec3 global(ViewingParameters v, Material m)
{
    return getEnvironmentDiffuse(v.normalDir) * m.occlusion * min(vec3(1.0), m.diffuseColor + m.specularColor);
}

vec3 emissive()
{
    return vec3(texture(weightMaps, vec3(fTexCoord, 0)).r * vec4(0,0,1,1));
}

vec3 specular(LightingParameters l, Material m)
{
    #if FRESNEL_EFFECT_ENABLED
    // Multiply by PI since the fit was done in a divided-by-pi space in terms of diffuse albedo,
    // but we implicitly do our real-time calculations in a pre-multiplied by pi space (i.e. no division by pi for diffuse).
    vec3 mfdFresnelBase = PI * getBRDFEstimate(l.nDotH, 1.0); // set G to 1.0 since masking / shading is handled by subjectMain
    return fresnel(mfdFresnelBase, vec3(getLuminance(mfdFresnelBase) / getLuminance(m.specularColor)), l.hDotV);
    #else // !FRESNEL_EFFECT_ENABLED
    return PI * getBRDFEstimate(l.nDotH, 1.0); // set G to 1.0 since masking / shading is handled by subjectMain
    #endif // FRESNEL_EFFECT_ENABLED
}

vec3 diffuse(LightingParameters l, Material m)
{
    return m.diffuseColor;
}

//#include "subjectMain.glsl"
#include <subject/subjectMain.glsl>

#endif // BASIS_GLSL