#ifndef LIGHTFIT_GLSL
#define LIGHTFIT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2001

uniform float delta;
uniform int iterations;

struct LightFit
{
    vec3 position;
    float intensity;
    float quality;
};

bool validateFit(LightFit fit)
{
    return  ! isnan(fit.position.x) && ! isnan(fit.position.y) && ! isnan(fit.position.z) &&
            ! isinf(fit.position.x) && ! isinf(fit.position.y) && ! isinf(fit.position.z) &&
            ! isnan(fit.intensity)  && ! isnan(fit.intensity) &&
            ! isinf(fit.quality)    && ! isinf(fit.quality);
}

LightFit fitLight()
{
    vec3 normal = normalize(fNormal);
    
    LightFit fit = LightFit(vec3(0), 0, 0);
    float weightedIntensitySum;
    float weightSum;
    
    for (int k = 0; k < iterations; k++)
    {
        weightedIntensitySum = 0;
        weightSum = 0;
    
        mat3 a = mat3(0);
        vec3 b = vec3(0);
        
        for (int i = 0; i < viewCount; i++)
        {
            vec3 viewNormal = (cameraPoses[i] * vec4(normal, 0.0)).xyz;
            vec3 surfacePosition = (cameraPoses[i] * vec4(fPosition, 1.0)).xyz;
            float nDotV = dot(viewNormal, normalize(-surfacePosition));
            vec4 color = getColor(i);
            
            if (color.a * nDotV > 0)
            {
                float lightSqr = dot(surfacePosition, surfacePosition);
                vec3 scaledNormal = viewNormal * inversesqrt(lightSqr) / lightSqr;
                vec3 sampleVector = vec3(scaledNormal.xy, -dot(scaledNormal, surfacePosition));
                float intensity = dot(color.rgb, vec3(1));
                
                float weight = color.a * nDotV;
                if (k != 0)
                {
                    float error = intensity - fit.intensity * dot(viewNormal, fit.position) / lightSqr;
                    weight *= exp(-error*error/(2*delta*delta));
                }
                    
                a += weight * outerProduct(sampleVector, sampleVector);
                b += weight * intensity * sampleVector;
                weightedIntensitySum += weight * intensity;
                weightSum += weight;
            }
        }
        
        vec3 solution = inverse(a) * b;
        fit.position = vec3(solution.xy, 0.0) / solution.z;
        fit.intensity = solution.z;
        fit.quality = clamp(solution.z * determinant(a) / weightSum, 0.0, 1.0);
    }
    
    if (!validateFit(fit))
    {
        fit.position = vec3(0.0);
        fit.intensity = 0.0;
        fit.quality = 0.0;
    }
    else
    {
        // ratio of outgoing light to target intensity value 
        // (so that the diffuse albedo map ends up in an appropriate range)
        fit.intensity *= weightSum / weightedIntensitySum;
    }
    
    return fit;
}

#endif // LIGHTFIT_GLSL
