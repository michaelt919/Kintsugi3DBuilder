#version 330

uniform mat4 model_view;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientColor;
uniform float roughnessScale;
uniform float gamma;

uniform sampler2D diffuse;
uniform sampler2D normal;
uniform sampler2D specular;
uniform sampler2D roughness;

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

layout(location = 0) out vec4 fragColor;

void main()
{
    vec4 diffuseColor = pow(texture(diffuse, fTexCoord), vec4(gamma));
    vec4 specularColor = pow(texture(specular, fTexCoord), vec4(gamma));
    float specularRoughness = texture(roughness, fTexCoord)[0] * roughnessScale;
    vec3 normalDir = normalize((model_view * vec4(texture(normal, fTexCoord).xyz * 2 - vec3(1.0), 0.0)).xyz);

    vec3 viewDir = normalize((model_view * vec4(fPosition, 1.0)).xyz);
    float rDotV = max(0.0, dot(reflect(lightDirection, normalDir), viewDir));
    
    fragColor = vec4(pow((ambientColor + lightColor * max(0.0, dot(lightDirection, normalDir))) * 
        diffuseColor.rgb + exp((rDotV - 1 / rDotV) / (2 * specularRoughness * specularRoughness)) * 
        lightColor * specularColor.rgb, vec3(1 / gamma)), 1.0);
}