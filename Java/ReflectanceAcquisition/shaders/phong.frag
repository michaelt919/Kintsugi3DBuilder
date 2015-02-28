#version 330

uniform mat4 model_view;
uniform vec3 lightPosition;
uniform vec3 lightColor;
uniform vec3 ambientColor;
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
    float specularRoughness = texture(roughness, fTexCoord)[0];
    vec3 normal = normalize(texture(normal, fTexCoord).xyz * 2 - vec3(1.0));
    
    vec3 lightDir = normalize(lightPosition - fPosition);
    float rDotV = max(0.0, -normalize(model_view * vec4(reflect(lightDir, normal), 0.0)).z);
    
    fragColor = vec4(pow((ambientColor + lightColor * max(0.0, dot(lightDir, normal))) * diffuseColor.rgb + 
        exp((rDotV - 1 / rDotV) / (2 * specularRoughness * specularRoughness)) * lightColor * 
        specularColor.rgb, vec3(1 / gamma)), 1.0);
}