#version 330

const int NO_TEXTURE_MODE = 0;
const int FULL_TEXTURE_MODE = 1;
const int PLASTIC_TEXTURE_MODE = 2;
const int METALLIC_TEXTURE_MODE = 3;
const int DIFFUSE_TEXTURE_MODE = 4;
const int NORMAL_TEXTURE_ONLY_MODE = 5;
const int DIFFUSE_NO_SHADING_MODE = 6;

uniform int mode;
uniform mat4 model_view;
uniform mat4 lightMatrix;
//uniform vec3 lightDirection;
uniform vec3 lightPosition;
uniform vec3 lightColor;
uniform vec3 ambientColor;
uniform float specularScale;
uniform float roughnessScale;
uniform float gamma;
uniform float shadowBias;
uniform bool trueBlinnPhong;

uniform sampler2D diffuse;
uniform sampler2D normal;
uniform sampler2D specular;
uniform sampler2D specNormal;
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
    if (mode == DIFFUSE_NO_SHADING_MODE)
    {
        fragColor = textureAlphaRobust(diffuse, fTexCoord);
    }
    else
    {
        vec4 diffuseColor = mode == NO_TEXTURE_MODE || mode == NORMAL_TEXTURE_ONLY_MODE ? 
            vec4(0.5, 0.5, 0.5, 1.0) : pow(textureAlphaRobust(diffuse, fTexCoord), vec4(gamma));
        vec4 specularColor;
        if (mode == PLASTIC_TEXTURE_MODE || mode == NORMAL_TEXTURE_ONLY_MODE || mode == NO_TEXTURE_MODE)  
        {
            specularColor = specularScale * vec4(1.0);
        }
        else if (mode == DIFFUSE_TEXTURE_MODE)
        {
            specularColor = specularScale * vec4(0.0, 0.0, 0.0, 1.0);
        }
        else if (mode == METALLIC_TEXTURE_MODE)
        {
            specularColor = specularScale * diffuseColor;
        }
        else
        {
            specularColor = specularScale * pow(textureAlphaRobust(specular, fTexCoord), vec4(gamma));
        }
        float specularRoughness = (mode == NO_TEXTURE_MODE || mode == NORMAL_TEXTURE_ONLY_MODE ? 
            1.0 : textureAlphaRobust(roughness, fTexCoord)[0]) * roughnessScale;
        vec3 normalDir = mode == NO_TEXTURE_MODE ? 
            normalize((model_view * vec4(fNormal, 0.0)).xyz) : 
            normalize((model_view * 
                vec4(textureAlphaRobust(normal, fTexCoord).xyz * 2 - vec3(1.0), 0.0)).xyz);

        vec3 specNormalDir = mode == NO_TEXTURE_MODE ? normalize((model_view * vec4(fNormal, 0.0)).xyz) :
            normalize((model_view * 
                vec4(textureAlphaRobust(specNormal, fTexCoord).xyz * 2 - vec3(1.0), 0.0)).xyz);
        vec3 viewDir = normalize(-(model_view * vec4(fPosition, 1.0)).xyz);
        vec3 lightDirection = normalize((model_view * vec4(lightPosition - fPosition, 0.0)).xyz);
        float nDotH = max(0.0, dot(normalize(lightDirection + viewDir), specNormalDir));
        
        // if (mode == FULL_TEXTURE_MODE)
        // {
            // vec4 projTexCoords = lightMatrix * vec4(fPosition, 1.0);
            // projTexCoords /= projTexCoords.w;
            // projTexCoords = (projTexCoords + vec4(1)) / 2;
            // float depth = texture(shadow, projTexCoords.xy).r;
            
            // if (depth + shadowBias < projTexCoords.z)
            // {
                // fragColor = vec4(pow(ambientColor * diffuseColor.rgb, vec3(1 / gamma)), 1.0);
                // return;
            // }
        // }
        
        fragColor = vec4(pow(
            (ambientColor + lightColor * max(0.0, dot(lightDirection, normalDir))) *  diffuseColor.rgb + 
                (trueBlinnPhong ? 
                    pow(nDotH, 1 / (specularRoughness * specularRoughness)) :
                    exp((nDotH - 1 / nDotH) / (2 * specularRoughness * specularRoughness)))
                * lightColor * specularColor.rgb, 
            vec3(1 / gamma)), 1.0);
    }
}