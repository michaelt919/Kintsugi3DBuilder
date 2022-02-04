#line 2 4001

uniform sampler2DArray weightMaps;
uniform sampler1DArray basisFunctions;

#ifndef BASIS_COUNT
#define BASIS_COUNT 8
#endif

layout(std140) uniform DiffuseColors
{
    vec4 diffuseColors[BASIS_COUNT];
};

vec3 getMFDEstimate(float nDotH)
{
    vec3 estimate = vec3(0);
    float w = sqrt(max(0.0, acos(nDotH) * 3.0 / PI));

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0] * texture(basisFunctions, vec2(w, b)).rgb;
    }

    return estimate;
}

vec3 getDiffuseEstimate()
{
    vec3 estimate = vec3(0);

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0] * diffuseColors[b].rgb;
    }

    return estimate;
}

vec3 getBRDFEstimate(float nDotH, float geomFactor)
{
    vec3 estimate = vec3(0);
    float w = sqrt(max(0.0, acos(nDotH) * 3.0 / PI));

    for (int b = 0; b < BASIS_COUNT; b++)
    {
        estimate += texture(weightMaps, vec3(fTexCoord, b))[0] * (diffuseColors[b].rgb / PI + texture(basisFunctions, vec2(w, b)).rgb * geomFactor);
    }

    return estimate;
}
