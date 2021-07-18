#version 330
#include "PTMfit.glsl"
#include "evaluateBRDF.glsl"
#line 18 0

uniform sampler2D diffuseEstimate;

layout(location = 0) out vec4 fragColor;

void main()
{
    vec3 triangleNormal = normalize(fNormal);
    vec3 tangent = normalize(fTangent - dot(triangleNormal, fTangent) * triangleNormal);
    vec3 bitangent = normalize(fBitangent
        - dot(triangleNormal, fBitangent) * triangleNormal
        - dot(tangent, fBitangent) * tangent);
    mat3 tangentToObject = mat3(tangent, bitangent, triangleNormal);

    vec2 normalDirXY = texture(normalEstimate, fTexCoord).xy * 2 - vec2(1.0);
    vec3 normalDirTS = vec3(normalDirXY, sqrt(1 - dot(normalDirXY, normalDirXY)));
    vec3 normal = tangentToObject * normalDirTS;

    vec3 lightDisplacement = getLightVector();
    vec3 light = normalize(lightDisplacement);
    vec3 view = normalize(getViewVector());
    vec3 halfway = normalize(light + view);
    float nDotH = max(0.0, dot(normal, halfway));
    float nDotL = max(0.0, dot(normal, light));
    float nDotV = max(0.0, dot(normal, view));
    float hDotV = max(0.0, dot(halfway, view));
    float sqrtRoughness = texture(roughnessEstimate, fTexCoord)[0];
    float roughness = sqrtRoughness * sqrtRoughness;
    float geomRatio;

    if (nDotL > 0.0 && nDotV > 0.0)
    {
        float maskingShadowing = geom(roughness, nDotH, nDotV, nDotL, hDotV);
        geomRatio = maskingShadowing / (4 * nDotL * nDotV);
    }
    else if (nDotL > 0.0)
    {
        geomRatio = 0.5 / (roughness * nDotL); // Limit as n dot v goes to zero.
    }

    vec3 incidentRadiance = PI * lightIntensity / dot(lightDisplacement, lightDisplacement);

    if (nDotL > 0.0)
    {
        vec3 brdf = pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma)) / PI + geomRatio * getMFDEstimate(nDotH);
        fragColor = vec4(pow(incidentRadiance * nDotL * brdf, vec3(1.0 / gamma)), 1.0);
    }
    else
    {
        // Limit as n dot l and n dot v both go to zero.
        vec3 mfd = getMFDEstimate(nDotH);
        fragColor = vec4(pow(incidentRadiance * mfd * 0.5 / roughness, vec3(1.0 / gamma)), 1.0);
    }
}
