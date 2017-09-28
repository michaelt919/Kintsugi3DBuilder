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
uniform sampler2D peakEstimate;
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

// All vectors should be in tangent space
// Result needs to be multiplied by specular reflectivity and derivative due to gamma
vec2 computeSpecularNormalDerivs(vec3 normal, vec3 light, vec3 view, vec3 halfway,
    float hDotV, float nDotL, float nDotV, float nDotH, float nDotHSq, float mSq, float geom)
{
    float a = (mSq - 1) * nDotHSq;
    float b = a + 1;
    float c = 1 - 3 * a;
    float d = normal.z * nDotV * nDotV * b * b * b;

    if (geom < 1)
    {
        vec3 mask;
        float nDotM;
        if (nDotL < nDotV)
        {
            mask = light;
            nDotM = nDotL;
        }
        else
        {
            mask = view;
            nDotM = nDotV;
        }

        return mSq / (2 * d * hDotV) *
            (nDotV * (normal.z * (halfway.xy * nDotM * c + mask.xy * nDotH * b)
                        - normal.xy * (halfway.z * nDotM * c + mask.z * nDotH * b))
                + nDotM * nDotH * b * (normal.xy * view.z - normal.z * view.xy));
    }
    else
    {
        return mSq / (4 * d) *
            (4 * (mSq - 1) * nDotH * nDotV * (normal.xy * halfway.z - normal.z * halfway.xy)
                + b * (normal.xy * view.z - normal.z * view.xy));
    }
}

struct ParameterizedFit
{
    vec3 diffuseColor;
    vec3 normal;
    vec3 specularColor;
    vec3 roughness;
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

        // // Partitioned matrix:  [ A B C ]
        // //                        [ D E F ]
        // //                        [ G H I ]
        // mat3   mA = mat3(0);
        // mat4x3 mB = mat4x3(0);
        // mat2x3 mC = mat2x3(0);
        // mat3x4 mD = mat3x4(0);
        // mat4   mE = mat4(0);
        // mat2x4 mF = mat2x4(0);
        // mat3x2 mG = mat3x2(0);
        // mat4x2 mH = mat4x2(0);
        // mat2   mI = mat2(0);

        // vec3 v1 = vec3(0);
        // vec4 v2 = vec4(0);
        // vec2 v3 = vec2(0);

        mat3 m = mat3(0.0);
        vec3 v = vec3(0.0);

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

                if (nDotL > 0.0 /* && nDotH > sqrt(0.5)*/)
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
                    // mat3 outerDerivMatrix =
                        // mat3(vec3(outerDeriv.r, 0, 0),
                            // vec3(0, outerDeriv.g, 0),
                            // vec3(0, 0, outerDeriv.b));
                    // mat3 diffuseDerivs = nDotL * outerDerivMatrix;
                    // mat3 diffuseDerivsTranspose = transpose(mat3(1) * diffuseDerivs); // Workaround for driver bug

                    vec3 diffuseCompensation = vec3(0);//max(vec3(0), sign(prevDiffuseColor));
//                    mat3 diffuseCompensationMatrix =
//                        mat3(vec3(diffuseCompensation.r, 0, 0),
//                             vec3(0, diffuseCompensation.g, 0),
//                             vec3(0, 0, diffuseCompensation.b));

                    // mat3 specularReflectivityDerivs =
                        // roughnessSquared
                            // * (mfdEval * geomRatio
                                // - diffuseCompensationMatrix * roughnessSquared / 2 * nDotL)
                            // * outerDerivMatrix;

                    // mat4x3 specularDerivs = mat4x3(
                        // specularReflectivityDerivs[0],
                        // specularReflectivityDerivs[1],
                        // specularReflectivityDerivs[2],
                        // (geomRatio * mfdDerivOverRoughnessSquared - diffuseCompensation * nDotL)
                            // * prevSpecularColor * outerDeriv);
                    // mat3x4 specularDerivsTranspose = transpose(mat3(1) * specularDerivs); // Workaround for driver bug

                    mat2x3 normalDerivs =
                        outerProduct(prevDiffuseColor * outerDeriv,
                            lightTS.xy - lightTS.z * shadingNormalTS.xy / shadingNormalTS.z) +
                        outerProduct(prevSpecularColor * outerDeriv,
                            computeSpecularNormalDerivs(shadingNormalTS, lightTS, viewTS, halfTS,
                                hDotV, nDotL, nDotV, nDotH, nDotHSquared, roughnessSquared, geom));
                    // mat3x2 normalDerivsTranspose = transpose(mat3(1) * normalDerivs);

                    float weight = nDotV; //clamp(1 / (1 - nDotHSquared), 0, 1000000);

                    // mA += weight * diffuseDerivsTranspose * diffuseDerivs;
                    // mB += weight * diffuseDerivsTranspose * specularDerivs;
                    // mC += weight * diffuseDerivsTranspose * normalDerivs;
                    // mD += weight * specularDerivsTranspose * diffuseDerivs;
                    // mE += weight * specularDerivsTranspose * specularDerivs;
                    // mF += weight * specularDerivsTranspose * normalDerivs;
                    // mG += weight * normalDerivsTranspose * diffuseDerivs;
                    // mH += weight * normalDerivsTranspose * specularDerivs;
                    // mI += weight * normalDerivsTranspose * normalDerivs;

                    // v1 += weight * diffuseDerivsTranspose * colorResidual;
                    // v2 += weight * specularDerivsTranspose * colorResidual;
                    // v3 += weight * normalDerivsTranspose * colorResidual;


                    mat3 derivs = mat3(
                        (geomRatio * mfdDerivOverRoughnessSquared - diffuseCompensation * nDotL)
                            * prevSpecularColor * outerDeriv,
                        normalDerivs[0], normalDerivs[1]);
                    mat3 derivsTranspose = transpose(mat3(1) * derivs); // Workaround for driver bug

                    m += weight * derivsTranspose * derivs;
                    v += weight * derivsTranspose * colorResidual;
                }
            }
        }

        // mA += mat3(    vec3(dampingFactor * max(mA[0][0], MIN_DIAGONAL), 0, 0),
                    // vec3(0, dampingFactor * max(mA[1][1], MIN_DIAGONAL), 0),
                    // vec3(0, 0, dampingFactor * max(mA[2][2], MIN_DIAGONAL)) );

        // mE += mat4(    vec4(dampingFactor * max(mE[0][0], MIN_DIAGONAL), 0, 0, 0),
                    // vec4(0, dampingFactor * max(mE[1][1], MIN_DIAGONAL), 0, 0),
                    // vec4(0, 0, dampingFactor * max(mE[2][2], MIN_DIAGONAL), 0),
                    // vec4(0, 0, 0, dampingFactor * max(mE[3][3], MIN_DIAGONAL)) );

        // mI += mat2( vec2(dampingFactor * max(mI[0][0], MIN_DIAGONAL), 0),
                    // vec2(0, dampingFactor * max(mI[1][1], MIN_DIAGONAL)) );

        // mat3 mAInverse = mat3(0); // Setting this to the zero matrix disables diffuse fitting
            // //inverse(mA);
        // mat4 schurComplementE_ABD = mE - mD * mAInverse * mB;
        // mat4 schurInverseE_ABD = inverse(schurComplementE_ABD);

        // mat2x4 mZ = schurInverseE_ABD * (mF - mD * mAInverse * mC);
        // mat2x3 mY = mAInverse * (mC - mB * mZ);

        // mat2 schurComplementI = mI - mG * mY - mH * mZ;
        // mat2 schurInverseI = inverse(schurComplementI);

        // vec2 normalAdj = vec2(0);
        // vec4 specularAdj = vec4(0);
        // vec3 diffuseAdj = vec3(0);

        // if
        // (
            // //determinant(mA) > 0.0 &&
                // // ^ diffuse determinant -- comment out if diffuse fitting is disabled
            // determinant(schurComplementE_ABD) > 0.0 // specular determinant
            // // && determinant(schurComplementI) > 0.0
                // // ^ normal determinant - comment out if normal fitting is disabled
        // )
        // {
            // vec4 specularAdjInit = schurInverseE_ABD * (v2 - mD * mAInverse * v1);
            // vec3 diffuseAdjInit = mAInverse * (v1 - mB * specularAdjInit);

            // // Commenting out the following line disables normal fitting:
            // //normalAdj = schurInverseI * (v3 - mG * diffuseAdjInit - mH * specularAdjInit);

            // specularAdj = specularAdjInit - mZ * normalAdj;
            // diffuseAdj = diffuseAdjInit - mY * normalAdj;
        // }

        // mat3 testIdentity1 = (mAInverse + mAInverse * mB * schurInverse * mC * mAInverse) * mA
            // - mAInverse * mB * schurInverse * mC;

        // mat4x3 testZero1 = (mAInverse + mAInverse * mB * schurInverse * mC * mAInverse) * mB
            // - mAInverse * mB * schurInverse * mD;

        // mat3x4 testZero2 = -schurInverse * mC * mAInverse * mA + schurInverse * mC;

        // mat4x4 testIdentity2 = -schurInverse * mC * mAInverse * mB + schurInverse * mD;

        // vec3 testColor = vec3(1,0,0) * (length(testIdentity1[0]-vec3(1,0,0))
                // + length(testIdentity1[1]-vec3(0,1,0))
                // + length(testIdentity1[2]-vec3(0,0,1)))
            // + vec3(0,1,0) * (length(testIdentity2[0]-vec4(1,0,0,0))
                // + length(testIdentity2[1]-vec4(0,1,0,0))
                // + length(testIdentity2[2]-vec4(0,0,1,0))
                // + length(testIdentity2[3]-vec4(0,0,0,1)))
            // + vec3(0,0,1) * (length(testZero1[0]) + length(testZero1[1]) + length(testZero1[2])
                // + length(testZero1[3]) + length(testZero2[0]) + length(testZero2[1])
                // + length(testZero2[2]));



        // // Attempt to linearize the adjustment scale
        // vec3 diffuseAdjLinearized =
            // diffuseAdj * pow(prevDiffuseColor, vec3(fittingGammaInv - 1.0)) * fittingGammaInv;
        // vec4 specularAdjLinearized =
          // specularAdj * vec4(pow(prevSpecularColor, vec3(fittingGammaInv - 1.0)) * fittingGammaInv, 1.0);
        // float shiftFraction = min(SHIFT_FRACTION, SHIFT_FRACTION /
            // sqrt((dot(diffuseAdjLinearized, diffuseAdjLinearized)
                // + dot(specularAdjLinearized, specularAdjLinearized))));


        mat3 mDamped = m + mat3(vec3(dampingFactor * max(m[0][0], MIN_DIAGONAL), 0, 0),
                            vec3(0, dampingFactor * max(m[1][1], MIN_DIAGONAL), 0),
                            vec3(0, 0, dampingFactor * max(m[2][2], MIN_DIAGONAL)) );

        vec3 adjustment = inverse(mDamped) * v;

        vec3 diffuseAdj = vec3(0);
        vec4 specularAdj = vec4(0.0, 0.0, 0.0, adjustment[0]);
        vec2 normalAdj = vec2(adjustment[1], adjustment[2]);


        if (isinf(normalAdj.x) || isnan(normalAdj.x) || isinf(normalAdj.y) || isnan(normalAdj.y)
            || isinf(diffuseAdj.x) || isnan(diffuseAdj.x) || isinf(diffuseAdj.y) || isnan(diffuseAdj.y)
            || isinf(diffuseAdj.z) || isnan(diffuseAdj.z)
            || isinf(specularAdj.x) || isnan(specularAdj.x) || isinf(specularAdj.y) || isnan(specularAdj.y)
            || isinf(specularAdj.z) || isnan(specularAdj.z) || isinf(specularAdj.w) || isnan(specularAdj.w))
        {
            return ParameterizedFit(
                xyzToRGB(prevDiffuseColor),
                shadingNormalTS,
                xyzToRGB(prevSpecularColor),
                roughness);
        }
        else
        {
            float newRoughnessSquared = max(0.0, roughnessSquared + /* shiftFraction * */specularAdj.w);
//            vec3 newSpecularColor =
//                max((prevSpecularColor / roughnessSquared + /* shiftFraction * */specularAdj.xyz) * newRoughnessSquared, vec3(0));
            vec3 newSpecularColorRGB = pow(texture(peakEstimate, fTexCoord).rgb, vec3(gamma)) * (4 * newRoughnessSquared);

//            diffuseAdj =
//                prevSpecularColor * roughnessSquared / 2
//                    - newSpecularColor * newRoughnessSquared / 2;

            vec2 newNormalXY = shadingNormalTS.xy + normalAdj;

            return ParameterizedFit(
                xyzToRGB(prevDiffuseColor + /* shiftFraction * */diffuseAdj),
                vec3(newNormalXY, sqrt(max(0.0, 1 - dot(newNormalXY, newNormalXY)))),
                //xyzToRGB(prevSpecularColor + /* shiftFraction * */specularAdj.xyz),
                newSpecularColorRGB,
                vec3(clamp(sqrt(newRoughnessSquared), MIN_ROUGHNESS, MAX_ROUGHNESS)));
        }
    }
}

#endif // ADJUSTMENT_GLSL
