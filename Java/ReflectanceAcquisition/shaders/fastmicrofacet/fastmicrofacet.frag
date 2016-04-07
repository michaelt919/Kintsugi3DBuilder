#version 330

#define PI 3.1415926535897932384626433832795

uniform mat4 model_view;
uniform mat4 lightMatrix;
uniform vec3 lightPosition;
uniform vec3 lightColor;
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
                vec4(textureAlphaRobust(normal, fTexCoord).xyz * 2 - vec3(1.0), 0.0)).xyz);
    vec3 viewDir = normalize(-(model_view * vec4(fPosition, 1.0)).xyz);
    vec3 lightDirection = normalize((model_view * vec4(lightPosition - fPosition, 0.0)).xyz);
    float nDotH = max(0.0, dot(normalize(lightDirection + viewDir), normalDir));
    float roughness = textureAlphaRobust(roughness, fTexCoord).r;
    
    // vec4 projTexCoords = lightMatrix * vec4(fPosition, 1.0);
    // projTexCoords /= projTexCoords.w;
    // projTexCoords = (projTexCoords + vec4(1)) / 2;
    // float depth = texture(shadow, projTexCoords.xy).r;
    
    // if (depth + shadowBias < projTexCoords.z)
    // {
        // fragColor = vec4(pow(ambientColor * diffuseColor.rgb, vec3(1 / gamma)), 1.0);
        // return;
    // }
    
    float mfdEval = 0.0;
    
    if (nDotH > 0.0)
    {
        float nDotHSquared = nDotH * nDotH;
        float roughnessSquared = roughness * roughness;
    
        mfdEval = exp((nDotHSquared - 1.0) / (nDotHSquared * roughnessSquared)) 
            / (nDotHSquared * nDotHSquared);
    }
        
    // TODO light attenuation
        
    fragColor = vec4(pow(
        (ambientColor + lightColor * max(0.0, dot(lightDirection, normalDir))) * diffuseColor.rgb / PI + 
            mfdEval * lightColor * specularColor.rgb, 
        vec3(1 / gamma)), 1.0);
}