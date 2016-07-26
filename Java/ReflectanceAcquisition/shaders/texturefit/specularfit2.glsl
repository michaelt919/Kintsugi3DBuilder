#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

uniform sampler2D normalEstimate;
uniform sampler2D roughnessEstimate;

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

float getRoughness()
{
    return texture(roughnessEstimate, fTexCoord).r;
}

struct ParameterizedFit
{
	vec3 diffuseColor;
	vec3 normal;
	vec3 specularColor;
	float roughness;
};

ParameterizedFit fitSpecular()
{
    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    float roughness = getRoughness();
    float roughnessSquared = roughness * roughness;
    float maxLuminance = getMaxLuminance();
    
    vec4 sum = vec4(0);
	mat2 a = mat2(0);
	vec4 diffuseWeightedSum = vec4(0);
	vec4 specularWeightedSum = vec4(0);
    
    for (int i = 0; i < viewCount; i++)
    {
        vec3 view = normalize(getViewVector(i));
        
        // Values of 1.0 for this color would correspond to the expected reflectance
        // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
        // Hence, this color corresponds to the reflectance times pi.
        // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
		// By adopting the convention that all reflectance values are scaled by pi in this shader,
		// We can avoid division by pi here as well as avoiding the 1/pi factors in the parameterized models.
        vec4 color = getLinearColor(i);
        
        if (color.a * dot(view, geometricNormal) > 0)
        {
            vec3 lightPreNormalized = getLightVector(i);
            vec3 attenuatedLightIntensity = infiniteLightSources ? 
                getLightIntensity(i) : 
                getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            vec3 light = normalize(lightPreNormalized);
			float nDotL = max(0, dot(light, diffuseNormal));
			float nDotV = max(0, dot(diffuseNormal, view));
            
            vec3 half = normalize(view + light);
            float nDotH = dot(half, diffuseNormal);
            
            if (nDotV > 0 && nDotH > 0.0)
            {
                float nDotHSquared = nDotH * nDotH;
                
                // float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared)) 
                    // / (roughnessSquared * nDotHSquared * nDotHSquared);
					
				float q = roughnessSquared + (1 - nDotHSquared) / nDotHSquared;
				float mfdEval =  roughnessSquared / (nDotHSquared * nDotHSquared * q * q);
				
				float hDotV = max(0, dot(half, view));
					
				float diffuseWeight = color.a * nDotL;
				float specularWeight = color.a * mfdEval / (4 * nDotV) * min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV);
				
				vec3 colorScaled = color.a * color.rgb / attenuatedLightIntensity;
				
				vec2 weights = vec2(diffuseWeight, specularWeight);
				a += outerProduct(weights, weights);
				diffuseWeightedSum += diffuseWeight * vec4(colorScaled, diffuseWeight);
				specularWeightedSum += specularWeight * vec4(colorScaled, specularWeight);
            }
        }
    }
	
	mat2x3 solution = transpose(inverse(a) * 
		transpose(mat2x3(diffuseWeightedSum.rgb, specularWeightedSum.rgb)));
	vec3 diffuseColorA = clamp(solution[0], vec3(0.0), diffuseWeightedSum.rgb);
	vec3 specularColorA = clamp(solution[1], vec3(0.0), 16.0 * roughnessSquared * specularWeightedSum.rgb);
	
	vec3 diffuseColorB = clamp(diffuseWeightedSum.rgb / diffuseWeightedSum.a, 
							vec3(0.0), diffuseWeightedSum.rgb);
	vec3 specularColorB = clamp(specularWeightedSum.rgb / specularWeightedSum.a, 
							vec3(0.0), specularWeightedSum.rgb);
	
	vec3 finalDiffuseColor = sign(diffuseColorA) * 
			(sign(specularColorA) * diffuseColorA + (1.0 - sign(specularColorA)) * diffuseColorB);
			
	vec3 finalSpecularColor = sign(specularColorA) * 
			(sign(diffuseColorA) * specularColorA + (1.0 - sign(diffuseColorA)) * specularColorB);
	
    // Dividing by the sum of weights to get the weighted average.
    // We'll put a lower cap of 1/m^2 on the alpha we divide by so that noise doesn't get amplified
    // for texels where there isn't enough information at the specular peak.
    //return ParameterizedFit(finalDiffuseColor, diffuseNormal, finalSpecularColor, roughness);
	return ParameterizedFit(vec3(0.5), geometricNormal, vec3(0.25), 0.25);
}

#endif // SPECULARFIT_GLSL
