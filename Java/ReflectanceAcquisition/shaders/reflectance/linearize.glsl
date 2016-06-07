#ifndef LINEARIZE_GLSL
#define LINEARIZE_GLSL

#line 5 1200

uniform sampler1D luminanceMap;
uniform bool useLuminanceMap;

uniform float gamma;

float getLuminance(vec3 rgbColor)
{
    // linearized sRGB to CIE-Y
    return dot(rgbColor, vec3(0.2126, 0.7152, 0.0722));
}

float getMaxLuminance()
{
    if (useLuminanceMap)
    {
        return texture(luminanceMap, 1.0).r;
    }
    else
    {
        return 1.0;
    }
}

float getMaxTonemappingScale()
{
	return 5.0;
}

vec3 linearizeColor(vec3 nonlinearColor)
{
    if (useLuminanceMap)
    {
        if (nonlinearColor.r <= 0.0 && nonlinearColor.g <= 0.0 && nonlinearColor.b <= 0.0)
        {
            return vec3(0);
        }
        else
        {
            // Step 1: remove gamma correction
            vec3 colorGamma = pow(nonlinearColor, vec3(gamma));
            
            // Step 2: convert to CIE luminance
            // Clamp to 1 so that the ratio computed in step 3 is well defined
            // if the luminance value somehow exceeds 1.0
            float luminanceNonlinear = getLuminance(colorGamma);
			
			float maxLuminance = getMaxLuminance();
			
			if (luminanceNonlinear > 1.0)
			{
				return colorGamma * maxLuminance;
			}
			else
			{
				// Step 3: determine the ratio between the linear and nonlinear luminance
				// Reapply gamma correction to the single luminance value
				float scale = min(getMaxTonemappingScale() * maxLuminance, texture(luminanceMap, pow(luminanceNonlinear, 1.0 / gamma)).r / luminanceNonlinear);
					
				// Step 4: return the color, scaled to have the correct luminance,
				// but the original saturation and hue.
				return colorGamma * scale;
			}
        }
    }
    else
    {
        return pow(nonlinearColor, vec3(gamma));
    }
}

vec4 linearizeColor(vec4 nonlinearColor)
{
    return vec4(linearizeColor(nonlinearColor.rgb), nonlinearColor.a);
}

#endif // LINEARIZE_GLSL