#ifndef ADJUSTMENT_GLSL
#define ADJUSTMENT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

#define SHIFT_FRACTION 0.00390625 // 1/256

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

vec3 getSpecularColor()
{
    return pow(texture(specularEstimate, fTexCoord).rgb, vec3(gamma));
}

float getRoughness()
{
    return texture(roughnessEstimate, fTexCoord).r;
}

struct ParameterizedFit
{
	vec3 diffuseColor;
	vec3 specularColor;
	float roughness;
};

ParameterizedFit adjustFit()
{
    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 prevDiffuseColor = getDiffuseColor();
    vec3 prevSpecularColor = getSpecularColor();
    float prevRoughness = getRoughness();
    float roughnessSquared = prevRoughness * prevRoughness;
    float maxLuminance = getMaxLuminance();
	float gammaInv = 1.0 / gamma;
    
	mat3   topLeft = mat4(0);
	mat4x3 topRight = mat4(0);
	mat3x4 bottomLeft = mat4(0);
	mat4   bottomRight = mat4(0);
	
	vec3 vectorTop = vec3(0);
	vec4 vectorBottom = vec4(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
		float nDotV = max(0, dot(diffuseNormal, view));
        
        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
        // Hence, this color corresponds to the reflectance times pi.
        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
		// By adopting the convention that all reflectance values are scaled by pi in this shader,
		// We can avoid division by pi here as well as avoiding the 1/pi factors in the parameterized models.
        vec4 color = getLinearColor(i);
		
        if (color.a > 0 && nDotV > 0 && dot(geometricNormal, view) > 0)
        {
            vec3 lightPreNormalized = getLightVector(i);
            vec3 attenuatedLightIntensity = infiniteLightSources ? 
                getLightIntensity(i) : 
                getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            vec3 light = normalize(lightPreNormalized);
			float nDotL = max(0, dot(light, diffuseNormal));
            
            vec3 half = normalize(view + light);
            float nDotH = dot(half, diffuseNormal);
            
            if (nDotL > 0.0 && nDotH > 0.0)
            {
                float nDotHSquared = nDotH * nDotH;
					
				float q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
				float mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);
				
				float q2 = 1.0 + (roughnessSquared - 1.0) * nDotHSquared;
				float mfdDeriv = 1.0 - (roughnessSq + 1.0) * nDotHSquared / (q2 * q2 * q2);
				
				float hDotV = max(0, dot(half, view));
				float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
				
				vec3 colorScaled = pow(rgbToXYZ(color.rgb / attenuatedLightIntensity), vec3(gammaInv));
				vec3 currentFit = prevDiffuseColor * nDotL + prevSpecularColor * mfdEval * geomRatio;
				vec3 colorResidual = colorScaled - pow(currentFit, vec3(gammaInv));
				
				vec3 innerDeriv = gammaInv * pow(currentFit, vec3(gammaInv - 1));
				float roughnessDeriv = geomRatio * mfdDeriv * prevSpecularColor * innerDeriv;
				mat3 diffuseDerivs = mat3(nDotL * innerDeriv);
				mat4x3 specularDerivs = mat4x3(
					mat3(mfdEval * geomRatio * innerDeriv), 
					geomRatio * mfdDeriv * prevSpecularColor * innerDeriv);
					
				topLeft += transpose(diffuseDerivs) * diffuseDerivs;
				topRight += transpose(diffuseDerivs) * specularDerivs;
				bottomLeft += transpose(specularDerivs) * diffuseDerivs;
				bottmRight += transpose(specularDerivs) * specularDerivs;
				
				vectorTop += transpose(diffuseDerivs) * colorResidual;
				vectorBottom += transpose(specularDerivs) * colorResidual;
            }
        }
    }
	
	mat3 topLeftInverse = inverse(topLeft);
	mat4 topLeftSchurInverse = inverse(bottomRight - bottomLeft * topLeftInverse * topRight);
	
	vec3 diffuseAdj = 
		(topLeftInverse + topLeftInverse * topRight * topLeftSchurInverse * bottomRight * topLeftInverse)
			* vectorTop 
		- topLeftInverse * topRight * topLeftSchurInverse * vectorBottom;
	
	vec4 specularAdj =
		-topLeftSchurInverse * bottomLeft * topLeftInverse * vectorTop 
		+ topLeftSchurInverse * vectorBottom;
		
	float scale = SHIFT_FRACTION / sqrt(dot(diffuseAdj, diffuseAdj) + dot(specularAdj, specularAdj));
	
	return ParameterizedFit(
		prevDiffuseColor + scale * diffuseAdj, 
		prevSpecularColor + scale * specularAdj.rgb,
		prevRoughness + scale * specularAdj.a);
}

#endif // ADJUSTMENT_GLSL
