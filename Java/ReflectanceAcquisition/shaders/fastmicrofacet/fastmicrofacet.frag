#version 330

#define PI 3.1415926535897932384626433832795

#define LIGHT_COUNT 4

uniform mat4 model_view;
uniform mat4 lightMatrices[LIGHT_COUNT];
uniform vec3 lightPositions[LIGHT_COUNT];
uniform vec3 lightColors[LIGHT_COUNT];
uniform vec3 ambientColor;
uniform float gamma;
uniform float shadowBias;

uniform sampler2D diffuse;
uniform sampler2D normal;
uniform sampler2D specular;
uniform sampler2D roughness;
uniform sampler2D shadow;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 fragColor;

vec4 textureAlphaRobust(sampler2D samp, vec2 coords)
{
    ivec2 size = textureSize(samp, 0);
    vec2 scaledCoords = coords * size;
    vec4 sample00 = texelFetch(samp, ivec2(floor(scaledCoords.x), floor(scaledCoords.y)), 0);
    vec4 sample01 = texelFetch(samp, ivec2(floor(scaledCoords.x), ceil(scaledCoords.y)), 0);
    vec4 sample10 = texelFetch(samp, ivec2(ceil(scaledCoords.x), floor(scaledCoords.y)), 0);
    vec4 sample11 = texelFetch(samp, ivec2(ceil(scaledCoords.x), ceil(scaledCoords.y)), 0);
    vec2 weights = scaledCoords - floor(scaledCoords);
    vec4 weighted = weights.x * weights.y * sample11 + weights.x * (1 - weights.y) * sample10 +
                    (1 - weights.x) * weights.y * sample01 + (1 - weights.x) * (1 - weights.y) * sample00;
    return weighted / weighted.a;
}

void main()
{
    vec4 diffuseColor = pow(textureAlphaRobust(diffuse, fTexCoord), vec4(gamma));
    vec4 specularColor = pow(textureAlphaRobust(specular, fTexCoord), vec4(gamma));
    vec3 normalDir = normalize((model_view * 
                vec4(fNormal/*textureAlphaRobust(normal, fTexCoord).xyz * 2 - vec3(1.0)*/, 0.0)).xyz);
    vec3 viewDir = normalize(-(model_view * vec4(fPosition, 1.0)).xyz);
    vec2 roughnessVector = textureAlphaRobust(roughness, fTexCoord).ra;
    float roughness = roughnessVector.x;
    float roughnessAlpha = roughnessVector.y;
    
    // vec4 projTexCoords = lightMatrix * vec4(fPosition, 1.0);
    // projTexCoords /= projTexCoords.w;
    // projTexCoords = (projTexCoords + vec4(1)) / 2;
    // float depth = texture(shadow, projTexCoords.xy).r;
    
    // if (depth + shadowBias < projTexCoords.z)
    // {
        // fragColor = vec4(pow(ambientColor * diffuseColor.rgb, vec3(1 / gamma)), 1.0);
        // return;
    // }
    
    float nDotV = dot(viewDir, normalDir);
    
    if (nDotV < 0.0)
    {
        fragColor = vec4(0,0,0,1);
    }
    else
    {
        float oneMinusNDotV = 1.0 - nDotV;
        float oneMinusNDotVSquared = oneMinusNDotV * oneMinusNDotV;
        
        vec3 accumColor = ambientColor / PI * (diffuseColor.rgb + specularColor.rgb + (vec3(1.0) - diffuseColor.rgb - specularColor.rgb) * oneMinusNDotVSquared * oneMinusNDotVSquared * oneMinusNDotV);
        
        for (int i = 0; i < LIGHT_COUNT; i++)
        {
            vec3 lightDir = normalize((model_view * vec4(lightPositions[i] - fPosition, 0.0)).xyz);
            vec3 halfDir = normalize(lightDir + viewDir);
            
            float nDotL = max(0.0, dot(lightDir, normalDir));
            float nDotH = max(0.0, dot(halfDir, normalDir));
            float hDotV = max(0.0, dot(halfDir, viewDir));
            
            // Diffuse
            accumColor += lightColors[i] * nDotL * diffuseColor.rgb / PI;
        
            if (hDotV > 0.0 && nDotH > 0.0 && roughnessAlpha > 0.0)
            {
                float nDotHSquared = nDotH * nDotH;
                float roughnessSquared = roughness * roughness;
            
                float mfdEval = 
                    //max(0.0, pow(nDotH, 2 / roughnessSquared - 2) / (PI * roughnessSquared));
                    exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared)) 
                        / (PI * nDotHSquared * nDotHSquared * roughnessSquared);
                    
                // ^ See Walter et al. "Microfacet Models for Refraction through Rough Surfaces"
                // for Beckmann to Phong conversion
                    
                float aV = 1.0 / (roughness * sqrt(1.0 - nDotL * nDotL) / nDotL);
                float aVSq = aV * aV;
                float aL = 1.0 / (roughness * sqrt(1.0 - nDotL * nDotL) / nDotL);
                float aLSq = aL * aL;
                    
                float geomAtten = 
                    min(1.0, min(2 * nDotH * nDotV / hDotV, 2 * nDotH * nDotL / hDotV)) / nDotV;
                    //(aV < 1.6 ? 3.535 * aV + 2.181 * aVSq / (1 + 2.276 * aV + 2.577 * aVSq) : 1.0)
                    //    * (aL < 1.6 ? 3.535 * aL + 2.181 * aLSq / (1 + 2.276 * aL + 2.577 * aLSq) : 1.0);
                    // ^ See Walter et al. "Microfacet Models for Refraction through Rough Surfaces"
                    // for this formula
                
                float f0 = dot(specularColor.rgb, vec3(0.2126, 0.7152, 0.0722));
                float sqrtF0 = sqrt(f0);
                float ior = (1 + sqrtF0) / (1 - sqrtF0);
                float g = sqrt(ior*ior + hDotV * hDotV - 1);
                float fresnel = 0.5 * pow(g - hDotV, 2) / pow(g + hDotV, 2)
                    * (1 + pow(hDotV * (g + hDotV) - 1, 2) / pow(hDotV * (g - hDotV) + 1, 2));
                
                vec3 fresnelReflectivity = specularColor.rgb + (vec3(1.0) - specularColor.rgb) *
                        max(0, fresnel - f0) / (1.0 - f0);
                        //(1.0 - hDotV) * (1.0 - hDotV) * (1.0 - hDotV) * 
                        //    (1.0 - hDotV) * (1.0 - hDotV);
                    
                // Specular
                accumColor += lightColors[i] * mfdEval * geomAtten * fresnelReflectivity;
            }
        }
        
        // TODO light attenuation
            
        fragColor = vec4(pow(accumColor * PI, vec3(1 / gamma)), 1.0);
    }
}