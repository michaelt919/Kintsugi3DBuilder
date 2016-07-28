#ifndef ADJUSTMENT_GLSL
#define ADJUSTMENT_GLSL

#include "../reflectance/reflectance.glsl"

#line 7 2005

#define MIN_ALBEDO    0	// 0.000005		// ~ 1/256 ^ gamma
#define MIN_ROUGHNESS 0	// 0.00390625	// 1/256

uniform sampler2D diffuseEstimate;
uniform sampler2D normalEstimate;
uniform sampler2D specularEstimate;
uniform sampler2D roughnessEstimate;
uniform sampler2D errorTexture;

vec3 getDiffuseColor()
{
    return 0.5 * pow(texture(diffuseEstimate, fTexCoord).rgb, vec3(gamma))
		+ 0.125 * ( pow(textureOffset(diffuseEstimate, fTexCoord, ivec2(0,+1)).rgb, vec3(gamma))
					+ pow(textureOffset(diffuseEstimate, fTexCoord, ivec2(0,-1)).rgb, vec3(gamma))
					+ pow(textureOffset(diffuseEstimate, fTexCoord, ivec2(+1,0)).rgb, vec3(gamma))
					+ pow(textureOffset(diffuseEstimate, fTexCoord, ivec2(-1,0)).rgb, vec3(gamma)) );
}

vec3 getDiffuseNormalVector()
{
    return normalize(
		0.5 * texture(normalEstimate, fTexCoord).xyz * 2 - vec3(1,1,1)
		+ 0.125 * ( textureOffset(normalEstimate, fTexCoord, ivec2(0,+1)).xyz * 2 - vec3(1,1,1)
					+ textureOffset(normalEstimate, fTexCoord, ivec2(0,-1)).xyz * 2 - vec3(1,1,1)
					+ textureOffset(normalEstimate, fTexCoord, ivec2(+1,0)).xyz * 2 - vec3(1,1,1)
					+ textureOffset(normalEstimate, fTexCoord, ivec2(-1,0)).xyz * 2 - vec3(1,1,1) ) );
}

vec3 getSpecularColor()
{
    return 0.5 * pow(texture(specularEstimate, fTexCoord).rgb, vec3(gamma))
		+ 0.125 * ( pow(textureOffset(specularEstimate, fTexCoord, ivec2(0,+1)).rgb, vec3(gamma))
					+ pow(textureOffset(specularEstimate, fTexCoord, ivec2(0,-1)).rgb, vec3(gamma))
					+ pow(textureOffset(specularEstimate, fTexCoord, ivec2(+1,0)).rgb, vec3(gamma))
					+ pow(textureOffset(specularEstimate, fTexCoord, ivec2(-1,0)).rgb, vec3(gamma)) );
}

float getRoughness()
{
    return 0.5 * texture(roughnessEstimate, fTexCoord).r
		+ 0.125 * ( textureOffset(roughnessEstimate, fTexCoord, ivec2(0,+1)).r
					+ textureOffset(roughnessEstimate, fTexCoord, ivec2(0,-1)).r
					+ textureOffset(roughnessEstimate, fTexCoord, ivec2(+1,0)).r
					+ textureOffset(roughnessEstimate, fTexCoord, ivec2(-1,0)).r );
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
		vec3 shadingNormalTS = getDiffuseNormalVector();
		vec3 shadingNormal = tangentToObject * shadingNormalTS;
		
		vec3 prevDiffuseColor = rgbToXYZ(max(vec3(MIN_ALBEDO), getDiffuseColor()));
		vec3 prevSpecularColor = rgbToXYZ(max(vec3(MIN_ALBEDO), getSpecularColor()));
		float prevRoughness = max(MIN_ROUGHNESS, getRoughness());
		float roughnessSquared = prevRoughness * prevRoughness;
		float gammaInv = 1.0 / gamma;
		
		// Partitioned matrix:  [ A B ]
		//						[ C D ]
		mat3   mA = mat3(0);
		mat4x3 mB = mat4x3(0);
		mat3x4 mC = mat3x4(0);
		mat4   mD = mat4(0);
		
		vec3 v1 = vec3(0);
		vec4 v2 = vec4(0);
		
		for (int i = 0; i < viewCount; i++)
		{
			vec3 view = normalize(getViewVector(i));
			float nDotV = max(0, dot(shadingNormal, view));
			
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
				
				vec3 half = normalize(view + light);
				float nDotH = dot(half, shadingNormal);
				
				if (nDotL > 0.0 && nDotH > 0.0)
				{
					float nDotHSquared = nDotH * nDotH;
						
					float q1 = roughnessSquared + (1.0 - nDotHSquared) / nDotHSquared;
					float mfdEval = roughnessSquared / (nDotHSquared * nDotHSquared * q1 * q1);
						
					float q2 = 1.0 + (roughnessSquared - 1.0) * nDotHSquared;
					float mfdDeriv = (1.0 - (roughnessSquared + 1.0) * nDotHSquared) / (q2 * q2 * q2);
					
					float hDotV = max(0, dot(half, view));
					float geomRatio = min(1.0, 2.0 * nDotH * min(nDotV, nDotL) / hDotV) / (4 * nDotV);
					
					vec3 colorScaled = pow(rgbToXYZ(color.rgb / attenuatedLightIntensity), vec3(gammaInv));
					vec3 currentFit = prevDiffuseColor * nDotL + prevSpecularColor * mfdEval * geomRatio;
					vec3 colorResidual = colorScaled - pow(currentFit, vec3(gammaInv));
					
					vec3 innerDeriv = gammaInv * pow(currentFit, vec3(gammaInv - 1));
					mat3 innerDerivMatrix = 
						mat3(vec3(innerDeriv.r, 0, 0),
							vec3(0, innerDeriv.g, 0),
							vec3(0, 0, innerDeriv.b));
					mat3 diffuseDerivs = nDotL * innerDerivMatrix;
					mat3 diffuseDerivsTranspose = transpose(mat3(1) * diffuseDerivs); // Workaround for driver bug
					
					mat3 specularReflectivityDerivs = mfdEval * geomRatio * innerDerivMatrix;
					mat4x3 specularDerivs = mat4x3(
						specularReflectivityDerivs[0],
						specularReflectivityDerivs[1],
						specularReflectivityDerivs[2],
						geomRatio * mfdDeriv * prevSpecularColor * innerDeriv);
					mat3x4 specularDerivsTranspose = transpose(mat3(1) * specularDerivs); // Workaround for driver bug
						
					mA += diffuseDerivsTranspose * diffuseDerivs;
					mB += diffuseDerivsTranspose * specularDerivs;
					mC += specularDerivsTranspose * diffuseDerivs;
					mD += specularDerivsTranspose * specularDerivs;
					
					v1 += diffuseDerivsTranspose * colorResidual;
					v2 += specularDerivsTranspose * colorResidual;
				}
			}
		}
		
		mA += mat3(	vec3(dampingFactor * mA[0][0], 0, 0), 
					vec3(0, dampingFactor * mA[1][1], 0), 
					vec3(0, 0, dampingFactor * mA[2][2]) );
			
		mD += mat4(	vec4(dampingFactor * mD[0][0], 0, 0, 0), 
					vec4(0, dampingFactor * mD[1][1], 0, 0), 
					vec4(0, 0, dampingFactor * mD[2][2], 0),
					vec4(0, 0, 0, dampingFactor * mD[3][3]) );
		
		mat3 mAInverse;
		mat4 schurInverse;
		
		if (determinant(mA) > 0.0) // TODO there might be a better way to do this - make sure damping coefficients are never zero?
		{
			mAInverse = inverse(mA);
			mat4 schurComplement = mD - mC * mAInverse * mB;
			if (determinant(schurComplement) > 0.0)
			{
				schurInverse = inverse(schurComplement);
			}
			else
			{
				schurInverse = mat4(0.0);
			}
		}
		else
		{
			mAInverse = mat3(0.0);
			schurInverse = mat4(0.0);
		}
		
		vec3 diffuseAdj = (mAInverse + mAInverse * mB * schurInverse * mC * mAInverse) * v1 
			- mAInverse * mB * schurInverse * v2;
		
		vec4 specularAdj = -schurInverse * mC * mAInverse * v1 + schurInverse * v2;
		
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
			// diffuseAdj * pow(prevDiffuseColor, vec3(gammaInv - 1.0)) * gammaInv;
		// vec4 specularAdjLinearized = 
			// specularAdj * vec4(pow(prevSpecularColor, vec3(gammaInv - 1.0)) * gammaInv, 1.0);
		// float shiftFraction = min(SHIFT_FRACTION, SHIFT_FRACTION / 
			// sqrt((dot(diffuseAdjLinearized, diffuseAdjLinearized)
				// + dot(specularAdjLinearized, specularAdjLinearized))));
		
		
		
		return ParameterizedFit(
			xyzToRGB(prevDiffuseColor + /* shiftFraction * */diffuseAdj), 
			shadingNormalTS,
			xyzToRGB(prevSpecularColor + /* shiftFraction * */specularAdj.xyz),
			prevRoughness + /* shiftFraction * */specularAdj.w);
	}
}

#endif // ADJUSTMENT_GLSL
