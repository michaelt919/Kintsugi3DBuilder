#ifndef ADJUSTMENT_GLSL
#define ADJUSTMENT_GLSL

#include "../colorappearance/colorappearance.glsl"

#line 7 2005

#define MIN_ALBEDO     0.000005        // ~ 1/256 ^ gamma
#define MIN_ROUGHNESS  0.00390625    // 1/256
#define MAX_ROUGHNESS  0.70710678 // sqrt(1/2)
#define MIN_DIAGONAL  0.000001 

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D errorTexture;

uniform float fittingGamma;

vec3 getDiffuseColor()
{
    vec4 center = texture(diffuseEstimate, fTexCoord);
    vec4 up = textureOffset(diffuseEstimate, fTexCoord, ivec2(0,+1));
    vec4 down = textureOffset(diffuseEstimate, fTexCoord, ivec2(0,-1));
    vec4 left = textureOffset(diffuseEstimate, fTexCoord, ivec2(+1,0));
    vec4 right = textureOffset(diffuseEstimate, fTexCoord, ivec2(-1,0));

    vec4 weightedSum = 0.5 * center.a * vec4(pow(center.rgb, vec3(gamma)), 1.0)
        + 0.125 * (up.a * vec4(pow(up.rgb, vec3(gamma)), 1.0)
            + down.a * vec4(pow(down.rgb, vec3(gamma)), 1.0)
            + left.a * vec4(pow(left.rgb, vec3(gamma)), 1.0)
            + right.a * vec4(pow(right.rgb, vec3(gamma)), 1.0));

    //return weightedSum.rgb / weightedSum.a;
    return pow(center.rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    vec4 center = texture(normalEstimate, fTexCoord);
    vec4 up = textureOffset(normalEstimate, fTexCoord, ivec2(0,+1));
    vec4 down = textureOffset(normalEstimate, fTexCoord, ivec2(0,-1));
    vec4 left = textureOffset(normalEstimate, fTexCoord, ivec2(+1,0));
    vec4 right = textureOffset(normalEstimate, fTexCoord, ivec2(-1,0));

    vec3 weightedSum = 0.5 * center.a * (center.xyz * 2 - vec3(1,1,1))
        + 0.125 * (up.a * (up.xyz * 2 - vec3(1,1,1))
            + down.a * (down.xyz * 2 - vec3(1,1,1))
            + left.a * (left.xyz * 2 - vec3(1,1,1))
            + right.a * (right.xyz * 2 - vec3(1,1,1)));

    //return normalize(weightedSum.xyz);
    return normalize(center.xyz * 2 - vec3(1,1,1));
}

vec3 getSpecularColor()
{
    vec4 center = texture(specularEstimate, fTexCoord);
    vec4 up = textureOffset(specularEstimate, fTexCoord, ivec2(0,+1));
    vec4 down = textureOffset(specularEstimate, fTexCoord, ivec2(0,-1));
    vec4 left = textureOffset(specularEstimate, fTexCoord, ivec2(+1,0));
    vec4 right = textureOffset(specularEstimate, fTexCoord, ivec2(-1,0));

    vec4 weightedSum = 0.5 * center.a * vec4(pow(center.rgb, vec3(gamma)), 1.0)
        + 0.125 * (up.a * vec4(pow(up.rgb, vec3(gamma)), 1.0)
            + down.a * vec4(pow(down.rgb, vec3(gamma)), 1.0)
            + left.a * vec4(pow(left.rgb, vec3(gamma)), 1.0)
            + right.a * vec4(pow(right.rgb, vec3(gamma)), 1.0));

    //return weightedSum.rgb / weightedSum.a;
    return pow(center.rgb, vec3(gamma));
}

float getRoughness()
{
    vec4 center = texture(roughnessEstimate, fTexCoord);
    vec4 up = textureOffset(roughnessEstimate, fTexCoord, ivec2(0,+1));
    vec4 down = textureOffset(roughnessEstimate, fTexCoord, ivec2(0,-1));
    vec4 left = textureOffset(roughnessEstimate, fTexCoord, ivec2(+1,0));
    vec4 right = textureOffset(roughnessEstimate, fTexCoord, ivec2(-1,0));

    vec2 weightedSum = 0.5 * center.a * vec2(center.r, 1.0)
        + 0.125 * (up.a * vec2(up.r, 1.0) + down.a * vec2(down.r, 1.0)
            + left.a * vec2(left.r, 1.0) + right.a * vec2(right.r, 1.0));

    //return weightedSum.x / weightedSum.y;
    return center.r;
}

struct ParameterizedFit
{
    vec3 diffuseColor;
    vec3 normal;
    vec3 specularColor;
    float roughness;
};

ParameterizedFit adjustFit()
{
    float dampingFactor = texture(errorTexture, fTexCoord).x;

    if (dampingFactor == 0.0)
    {
        discard;
    }
    else
    {
        vec3 normal = normalize(fNormal);

        vec3 tangent = normalize(fTangent - dot(normal, fTangent));
        vec3 bitangent = normalize(fBitangent
            - dot(normal, fBitangent) * normal 
            - dot(tangent, fBitangent) * tangent);
            
        mat3 tangentToObject = mat3(tangent, bitangent, normal);
        mat3 objectToTangent = transpose(mat3(1) * tangentToObject); // Workaround for driver bug
        vec3 shadingNormalTS = getDiffuseNormalVector();
        vec3 shadingNormal = tangentToObject * shadingNormalTS;

        vec3 prevDiffuseColor = rgbToXYZ(max(vec3(MIN_ALBEDO), getDiffuseColor()));
        vec3 prevSpecularColor = rgbToXYZ(max(vec3(MIN_ALBEDO), getSpecularColor()));
        float roughness = max(MIN_ROUGHNESS, getRoughness());
        float roughnessSquared = roughness * roughness;
        float fittingGammaInv = 1.0 / fittingGamma;

        float m = 0.0;
        float v = 0.0;

        for (int i = 0; i < viewCount; i++)
        {
            vec3 view = normalize(getViewVector(i));
            float nDotV = max(0, dot(shadingNormal, view));
            vec3 viewTS = objectToTangent * view;

            // Values of 1.0 for this color would correspond to the expected reflectance
            // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
            // Hence, this color corresponds to the reflectance times pi.
            // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
            // By adopting the convention that all reflectance values are scaled by pi in this shader,
            // We can avoid division by pi here as well as avoiding the 1/pi factors in the parameterized models.
            vec4 color = getLinearColor(i);

            if (color.a > 0 && nDotV > 0 && dot(normal, view) > 0)
            {
                vec3 lightPreNormalized = getLightVector(i);
                vec3 attenuatedLightIntensity = infiniteLightSources ?
                    getLightIntensity(i) :
                    getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
                vec3 light = normalize(lightPreNormalized);
                float nDotL = max(0, dot(light, shadingNormal));
                vec3 lightTS = objectToTangent * light;

                vec3 halfway = normalize(view + light);
                float nDotH = dot(halfway, shadingNormal);
                vec3 halfTS = objectToTangent * halfway;

                if (nDotL > 0.0 && nDotH > sqrt(0.5))
                {
                    // An implicit change of variables is done here.
                    // The reflectivity variable being fit is actually the ratio between
                    // the fresnel reflectivity and the roughness squared (i.e. u=F/m^2)
                    // The roughness squared itself is treated a "variable" (i.e. v=m^2)
                    // The diffuse compensation is intended to ensure that the reflectance
                    // away from specular remains constant.
                    // An implication of this is that as the halfway angle approaches 45 degrees,
                    // the derivatives should approach zero.

                    float nDotHSquared = nDotH * nDotH;

                    float q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
                    float mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);

                    float q2 = 1.0 + (roughnessSquared - 1.0) * nDotHSquared;
                    // The following evaluation actually computes the derivative of
                    // the microfacet distribution times m^2, with respect to "m^2",
                    // and then as if the whole quantity were divided by m^2.
                    float mfdDerivOverRoughnessSquared = 2 * (1 - nDotHSquared) / (q2 * q2 * q2);

                    float hDotV = max(0, dot(halfway, view));
                    float geom = 1.0; //min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV);
                    float geomRatio = geom / (4 * nDotV);

                    vec3 colorScaled = pow(rgbToXYZ(color.rgb / attenuatedLightIntensity),
                        vec3(fittingGammaInv));
                    vec3 currentFit = prevDiffuseColor * nDotL + prevSpecularColor * mfdEval * geomRatio;
                    vec3 colorResidual = colorScaled - pow(currentFit, vec3(fittingGammaInv));

                    vec3 outerDeriv = fittingGammaInv * pow(currentFit, vec3(fittingGammaInv - 1));

                    vec3 diffuseCompensation = vec3(0);

                    float weight = 1.0;//clamp(1 / (1 - nDotHSquared), 0, 1000000);

                    vec3 derivs = (geomRatio * mfdDerivOverRoughnessSquared - diffuseCompensation * nDotL) * prevSpecularColor * outerDeriv;

                    m += weight * dot(derivs, derivs);
                    v += weight * dot(derivs, colorResidual);
                }
            }
        }

        float mDamped = m + dampingFactor * max(m, MIN_DIAGONAL);

        float adjustment = v / mDamped;

        if (isinf(adjustment) || isnan(adjustment))
        {
            return ParameterizedFit(
                xyzToRGB(prevDiffuseColor),
                shadingNormalTS,
                xyzToRGB(prevSpecularColor),
                roughness);
        }
        else
        {
            float newRoughnessSquared = max(0.0, roughnessSquared + /* shiftFraction * */adjustment);
            vec3 newSpecularColor = max(
                (prevSpecularColor / roughnessSquared + /* shiftFraction * */adjustment)
                    * newRoughnessSquared,
                vec3(0));


            vec3 diffuseAdj =
                vec3(0.0);
                // prevSpecularColor * roughnessSquared / 2
                    // - newSpecularColor * newRoughnessSquared / 2;

            vec2 newNormalXY = shadingNormalTS.xy;

            return ParameterizedFit(
                xyzToRGB(prevDiffuseColor + /* shiftFraction * */diffuseAdj),
                shadingNormalTS,
                //xyzToRGB(prevSpecularColor + /* shiftFraction * */specularAdj.xyz),
                xyzToRGB(newSpecularColor),
                clamp(sqrt(newRoughnessSquared), MIN_ROUGHNESS, MAX_ROUGHNESS));
        }
    }
}

#endif // ADJUSTMENT_GLSL
