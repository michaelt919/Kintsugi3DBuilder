#version 330

const int NO_TEXTURE_MODE = 0;
const int FULL_TEXTURE_MODE = 1;
const int NORMAL_TEXTURE_ONLY_MODE = 2;

uniform int mode;
uniform mat4 model_view;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientColor;
uniform float roughnessScale;
uniform float gamma;
uniform bool trueBlinnPhong;

uniform sampler2D diffuse;
uniform sampler2D normal;
uniform sampler2D specular;
uniform sampler2D specNormal;
uniform sampler2D roughness;

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
    vec4 diffuseColor = mode != FULL_TEXTURE_MODE ? vec4(0.5, 0.5, 0.5, 1.0) :
        pow(textureAlphaRobust(diffuse, fTexCoord), vec4(gamma));
    vec4 specularColor = mode != FULL_TEXTURE_MODE ? vec4(0.5, 0.5, 0.5, 1.0) :
        pow(textureAlphaRobust(specular, fTexCoord), vec4(gamma));
    float specularRoughness = mode != FULL_TEXTURE_MODE ? 0.125 : 
        textureAlphaRobust(roughness, fTexCoord)[0] * roughnessScale;
    vec3 normalDir = mode == NO_TEXTURE_MODE ? normalize((model_view * vec4(fNormal, 0.0)).xyz) : 
        normalize((model_view * vec4(textureAlphaRobust(normal, fTexCoord).xyz * 2 - vec3(1.0), 0.0)).xyz);

    vec3 specNormalDir = mode == NO_TEXTURE_MODE ? normalize((model_view * vec4(fNormal, 0.0)).xyz) :
        normalize((model_view * vec4(textureAlphaRobust(specNormal, fTexCoord).xyz * 2 - vec3(1.0), 0.0)).xyz);
    vec3 viewDir = normalize(-(model_view * vec4(fPosition, 1.0)).xyz);
    float nDotH = max(0.0, dot(normalize(lightDirection + viewDir), specNormalDir));
    
    fragColor = vec4(pow(
        (ambientColor + lightColor * max(0.0, dot(lightDirection, normalDir))) *  diffuseColor.rgb + 
            (trueBlinnPhong ? 
                pow(nDotH, 1 / (specularRoughness * specularRoughness)) :
                exp((nDotH - 1 / nDotH) / (2 * specularRoughness * specularRoughness)))
            * lightColor * specularColor.rgb, 
        vec3(1 / gamma)), 1.0);
}