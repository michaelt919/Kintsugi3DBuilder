#ifndef SPECULARFIT_GLSL
#define SPECULARFIT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2004

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D roughnessEstimate;

vec3 getDiffuseColor()
{
    return pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma));
}

vec3 getDiffuseNormalVector()
{
    return normalize(texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1));
}

float getRoughness()
{
    return texture(roughnessEstimate, fTexCoord).r;
}

// vec4 removeDiffuse(vec4 originalColor, vec3 diffuseColor, float maxLuminance,
    // vec3 light, vec3 attenuatedLightIntensity, vec3 normal)
// {
    // float nDotL = max(0, dot(light, normal));
    // if (nDotL == 0.0)
    // {
        // return vec4(0);
    // }
    // else
    // {
        // vec3 diffuseContrib = diffuseColor * nDotL * attenuatedLightIntensity;
        // float cap = maxLuminance - max(diffuseContrib.r, max(diffuseContrib.g, diffuseContrib.b));
        // vec3 remainder = clamp(originalColor.rgb - diffuseContrib, 0, cap);
        // return vec4(remainder, originalColor.a);
    // }
// }

struct TwoColorFit
{
	vec3 specularColor;
	vec3 diffuseColor;
};

TwoColorFit fitSpecular()
{
    vec3 geometricNormal = normalize(fNormal);
    vec3 diffuseNormal = getDiffuseNormalVector();
    vec3 originalDiffuseColor = getDiffuseColor();
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
    return TwoColorFit(finalSpecularColor, finalDiffuseColor);
}

// TwoColorFit fitSpecular()
// {
    // vec3 geometricNormal = normalize(fNormal);
    // vec3 diffuseNormal = getDiffuseNormalVector();
    // vec3 originalDiffuseColor = getDiffuseColor();
    // float roughness = getRoughness();
    // float roughnessSquared = roughness * roughness;
    // float maxLuminance = getMaxLuminance();
    
    // vec4 sum = vec4(0);
	// mat2 a = mat2(0);
	// vec4 diffuseWeightedSum = vec4(0);
	// vec4 specularWeightedSum = vec4(0);
    
    // for (int i = 0; i < viewCount; i++)
    // {
        // vec3 view = normalize(getViewVector(i));
        
        // // Values of 1.0 for this color would correspond to the expected reflectance
        // // for an ideal diffuse reflector (diffuse albedo of 1), which is a reflectance of 1 / pi.
        // // Hence, this color corresponds to the reflectance times pi.
        // // Both the Phong model and the Cook Torrance with a Beckmann distribution also have a 1/pi factor.
		// // By adopting the convention that all reflectance values are scaled by pi in this shader,
		// // We can avoid division by pi here as well as avoiding the 1/pi factors in the parameterized models.
        // vec4 color = getLinearColor(i);
        
        // if (color.a * dot(view, geometricNormal) > 0)
        // {
            // vec3 lightPreNormalized = getLightVector(i);
            // vec3 attenuatedLightIntensity = infiniteLightSources ? 
                // getLightIntensity(i) : 
                // getLightIntensity(i) / (dot(lightPreNormalized, lightPreNormalized));
            // vec3 light = normalize(lightPreNormalized);
			// float nDotL = max(0, dot(light, diffuseNormal));
			// float nDotV = max(0, dot(diffuseNormal, view));
            
            // vec3 half = normalize(view + light);
            // float nDotH = dot(half, diffuseNormal);
            
            // if (nDotV > 0 && nDotH > 0.0)
            // {
                // float nDotHSquared = nDotH * nDotH;
                
                // // float mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared)) 
                    // // / (roughnessSquared * nDotHSquared * nDotHSquared);
					
				// float q = roughnessSquared + (1 - nDotHSquared) / nDotHSquared;
				// float mfdEval =  roughnessSquared / (nDotHSquared * nDotHSquared * q * q);
				
				// float hDotV = max(0, dot(half, view));
					
				// float diffuseWeight = color.a * nDotL;
				// float specularWeight = color.a * mfdEval / (4 * nDotV) * min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV);
				
				// vec3 colorXYZ = rgbToXYZ(nDotV * color.rgb / attenuatedLightIntensity);
				
				// // Pseudo-LAB color space
				// vec3 colorLAB = color.a * vec3(colorXYZ.y, 
					// 5 * (colorXYZ.x - colorXYZ.y) * inversesqrt(colorXYZ.y), 
					// 2 * (colorXYZ.y - colorXYZ.z) * inversesqrt(colorXYZ.y));
				
				// vec2 weights = vec2(diffuseWeight, specularWeight);				
				// a += outerProduct(weights, weights);
				// diffuseWeightedSum += diffuseWeight * vec4(colorLAB, diffuseWeight);
				// specularWeightedSum += specularWeight * vec4(colorLAB, specularWeight);
            // }
        // }
    // }
	
	// mat2x3 solution = transpose(inverse(a) * 
		// transpose(mat2x3(diffuseWeightedSum.xyz, specularWeightedSum.xyz)));
	// vec3 diffuseOptionA = clamp(solution[0], vec3(0.0), diffuseWeightedSum.xyz);
	// vec3 specularOptionA = clamp(solution[1], vec3(0.0), 16.0 * roughnessSquared * specularWeightedSum.xyz);
	
	// vec3 diffuseOptionB = clamp(diffuseWeightedSum.xyz / diffuseWeightedSum.w, 
							// vec3(0, -abs(diffuseWeightedSum.yz)), abs(diffuseWeightedSum.xyz));
	// vec3 specularOptionB = clamp(specularWeightedSum.xyz / specularWeightedSum.w, 
							// vec3(0, -abs(specularWeightedSum.yz)), abs(specularWeightedSum.xyz));
			
	// vec3 diffuseOptionA_RGB = 
		// max(vec3(0.0), xyzToRGB(vec3(diffuseOptionA.x) 
					// + vec3(0.2 * diffuseOptionA.y * sqrt(diffuseOptionA.x), 0, 
						// -0.5 * diffuseOptionA.z * sqrt(diffuseOptionA.x))));
					
	// vec3 diffuseOptionB_RGB = 
		// max(vec3(0.0), xyzToRGB(vec3(diffuseOptionB.x) 
					// + vec3(0.2 * diffuseOptionB.y * sqrt(diffuseOptionB.x), 0, 
						// -0.5 * diffuseOptionB.z * sqrt(diffuseOptionB.x))));
		
	// vec3 specularOptionA_RGB = 
		// max(vec3(0.0), xyzToRGB(vec3(specularOptionA.x) 
					// + vec3(0.2 * specularOptionA.y * sqrt(specularOptionA.x), 0, 
						// -0.5 * specularOptionA.z * sqrt(specularOptionA.x))));
					
	// vec3 specularOptionB_RGB = 
		// max(vec3(0.0), xyzToRGB(vec3(specularOptionB.x) 
					// + vec3(0.2 * specularOptionB.y * sqrt(specularOptionB.x), 0, 
						// -0.5 * specularOptionB.z * sqrt(specularOptionB.x))));
	
	// vec3 finalDiffuseColor = sign(diffuseOptionA_RGB) * 
		// (sign(specularOptionA_RGB) * diffuseOptionA_RGB 
			// + (1.0 - sign(specularOptionA_RGB)) * diffuseOptionB_RGB);
			
	// vec3 finalSpecularColor = sign(specularOptionA_RGB) * 
		// (sign(diffuseOptionA_RGB) * specularOptionA_RGB 
			// + (1.0 - sign(diffuseOptionA_RGB)) * specularOptionB_RGB);
	
    // // Dividing by the sum of weights to get the weighted average.
    // // We'll put a lower cap of 1/m^2 on the alpha we divide by so that noise doesn't get amplified
    // // for texels where there isn't enough information at the specular peak.
    // return TwoColorFit(finalSpecularColor, finalDiffuseColor);
// }

#endif // SPECULARFIT_GLSL
