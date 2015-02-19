#version 330

#define MAX_CAMERA_POSE_COUNT 256
#define MAX_CAMERA_PROJECTION_COUNT 256

in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;

uniform sampler2DArray imageTextures;
uniform sampler2DArray depthTextures;
uniform int textureCount;
uniform float gamma;

uniform float guessSpecularWeight;
uniform vec3 guessSpecularColor;
uniform float guessSpecularRoughness;
uniform int specularRange;
uniform float expectedWeightSum;

uniform CameraPoses
{
	mat4 cameraPoses[MAX_CAMERA_POSE_COUNT];
};

uniform CameraProjections
{
	mat4 cameraProjections[MAX_CAMERA_PROJECTION_COUNT];
};

uniform CameraProjectionIndices
{
	int cameraProjectionIndices[MAX_CAMERA_POSE_COUNT];
};

layout(location = 0) out vec4 diffuseColor;
layout(location = 1) out vec4 normalMap;
layout(location = 2) out vec4 specularColor;
layout(location = 3) out vec4 specularRoughness;
layout(location = 4) out vec4 debug0;
layout(location = 5) out vec4 debug1;
layout(location = 6) out vec4 debug2;
layout(location = 7) out vec4 debug3;

vec4 getColor(int index)
{
    return pow(texture(imageTextures, vec3(fTexCoord, index)), vec4(gamma));
}

vec4 getColorWithOffset(int index, ivec2 offset)
{
    return pow(textureOffset(imageTextures, vec3(fTexCoord, index), offset), vec4(gamma));
}

vec3 getRelativePositionFromDepthBufferWithOffset(int index, ivec2 offset)
{
    float depth = texture(depthTextures, vec3(fTexCoord, index)).r;
    mat4 proj = cameraProjections[cameraProjectionIndices[index]];
    float z = - proj[3].z / (proj[2].z + 2 * depth - 1);
    float x = - z * (2 * fTexCoord.x - 1 + proj[2].x) / proj[0].x;
    float y = - z * (2 * fTexCoord.y - 1 + proj[2].y) / proj[1].y;
    return vec3(x, y, z);
}

vec3 getViewVector(int index)
{
    return normalize(transpose(mat3(cameraPoses[index])) * -cameraPoses[index][3].xyz - fPosition);
}

vec3 getViewVectorWithOffset(int index, ivec2 offset)
{
    return normalize(transpose(mat3(cameraPoses[index])) * 
        (- cameraPoses[index][3].xyz - getRelativePositionFromDepthBufferWithOffset(index, offset)));
}

vec3 getLightVector(int index)
{
    // TODO
    return getViewVector(index);
}

vec3 getLightVectorWithOffset(int index, ivec2 offset)
{
    // TODO
    return getViewVectorWithOffset(index, offset);
}

vec3 getReflectionVector(vec3 normalVector, vec3 lightVector)
{
    return normalize(2 * dot(lightVector, normalVector) * normalVector - lightVector);
}

void main()
{
    vec4 sumColor = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {   
       vec4 color = getColor(i);
       sumColor += color.a * vec4(color.rgb, 1.0);
    }
    float avgIntensity = (sumColor.r + sumColor.g + sumColor.b) / sumColor.a;
    
    mat4 dA = mat4(0);
    mat4 dB = mat4(0);
    
    vec4 diffuseSum = vec4(0);
    for (int i = 0; i < textureCount; i++)
    {
        vec4 color = getColor(i);
        if (color.a > 0)
        {
            vec4 view = vec4(getViewVector(i), 1.0);
            vec4 light = vec4(getLightVector(i), 1.0);
            
            dA += color.a * outerProduct(light, light);
            dB += color.a * outerProduct(light, vec4(color.rgb, 0.0));
            
            if ((color.r > 0.0 || color.g > 0.0 || color.b > 0.0) && 
                (color.r + color.g + color.b) < avgIntensity)
            {
                diffuseSum += color.a * vec4(color.rgb, 1.0);
            }
        }
    }
    vec3 diffuseAvg = diffuseSum.rgb / (diffuseSum.r + diffuseSum.g + diffuseSum.b);
    
    vec3 simpleWeights = diffuseSum.rgb / max(max(diffuseSum.r, diffuseSum.g), diffuseSum.b);
    vec3 rgbWeights;
    if (guessSpecularWeight > 0)
    {
        vec3 ortho = cross(diffuseAvg, guessSpecularColor);
        if (ortho.x > 0 || ortho.y > 0 || ortho.z > 0)
        {
            mat3 colorBasis = mat3(diffuseAvg, guessSpecularColor, ortho);
            float adjustedFixedSpecWeight = guessSpecularWeight * length(ortho) / 
                (length(diffuseAvg) * length(guessSpecularColor));
            rgbWeights = (adjustedFixedSpecWeight * transpose(inverse(colorBasis))[0] + simpleWeights) / 
                (1 + adjustedFixedSpecWeight);
        }
        else
        {
            rgbWeights = simpleWeights;
        }
    }
    else
    {
        rgbWeights = simpleWeights;
    }
    
    vec4 dSolution = inverse(dA) * dB * vec4(rgbWeights, 0.0);
    float ambientIntensity = dSolution.w;
    float diffuseIntensity = length(dSolution.xyz);
    vec3 normal = normalize(dSolution.xyz);
    normalMap = vec4(normal * 0.5 + vec3(0.5), 1.0);
    //debug0 = vec4(pow(diffuseAvg * dSolution.w, vec3(1 / gamma)), 1.0);
    diffuseColor = vec4(pow(diffuseAvg * diffuseIntensity, vec3(1 / gamma)), 1.0);
    
    vec4 specularSum = vec4(0);
    mat2 sA = mat2(0);
    vec2 sB = vec2(0);
    
    for (int i = 0; i < textureCount; i++)
    {
        vec4 color = getColor(i);
        if (color.a > 0)
        {
            vec3 view = getViewVector(i);
            vec3 light = getLightVector(i);
            
            vec3 diffuseContrib = diffuseColor.rgb * max(0, dot(light, normal));
            vec4 colorRemainder = vec4(max(vec3(0), color.rgb - diffuseContrib), color.a);
            float intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
            
            vec3 reflection = getReflectionVector(normal, light);
            float rDotV = dot(reflection, view);
            
            if (intensity > 0.0 && rDotV > 0.0)
            {
                float u = rDotV - 1 / rDotV;
                
                //sA += color.a * rDotV * outerProduct(vec2(u, 1), vec2(u, 1));
                //sB += color.a * rDotV * log(intensity) * vec2(u, 1);
                
                //sA += color.a * intensity / log(intensity) * outerProduct(vec2(u, 1), vec2(u, 1));
                //sB += color.a * intensity * vec2(u, 1);
                
                //sA += color.a * outerProduct(vec2(u, 1), vec2(u, 1));
                //sB += color.a * log(intensity) * vec2(u, 1);
                
                sA += color.a * pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness)) * 
                    intensity * outerProduct(vec2(u, 1), vec2(u, 1));
                sB += color.a * pow(rDotV, 1 / (guessSpecularRoughness * guessSpecularRoughness)) * 
                    intensity * log(intensity) * vec2(u, 1);
                
                specularSum += color.a * rDotV * intensity * colorRemainder.a * vec4(colorRemainder.rgb, 1.0);
                
                // for (int i = 1; i < specularRange; i++)
                // {
                    // float weight = 1.0; // TODO
                    // ivec2 offset = ivec2(i, 0);
                    // for (int j = 0; j < 4*(i+1); j++)
                    // {
                        // if (offset.x == i && offset.y != -i)
                        // {
                            // offset.y--;
                        // }
                        // else if (offset.y == i)
                        // {
                            // offset.x++;
                        // }
                        // else if (offset.x == -i)
                        // {
                            // offset.y++;
                        // }
                        // else if (offset.y == -i)
                        // {
                            // offset.x--;
                        // }
                        
                        // color = getColorWithOffset(i, offset);
                        // view = getViewVectorWithOffset(i, offset);
                        // light = getLightVectorWithOffset(i, offset);
                        // normal = normal; // TODO
                        
                        // diffuseContrib = diffuseColor.rgb * max(0, dot(light, normal)); // TODO
                        // colorRemainder = vec4(max(vec3(0), color.rgb - diffuseContrib), color.a);
                        // intensity = colorRemainder.r + colorRemainder.g + colorRemainder.b;
                        
                        // reflection = getReflectionVector(normal, light);
                        // rDotV = dot(reflection, view);
                        
                        // if (intensity > 0.0 && rDotV > 0.0)
                        // {
                            // u = rDotV - 1 / rDotV;
                            
                            // //sA += color.a * rDotV * outerProduct(vec2(u, 1), vec2(u, 1));
                            // //sB += color.a * rDotV * log(intensity) * vec2(u, 1);
                            
                            // //sA += color.a * intensity / log(intensity) * outerProduct(vec2(u, 1), vec2(u, 1));
                            // //sB += color.a * intensity * vec2(u, 1);
                            
                            // sA += weight * color.a * outerProduct(vec2(u, 1), vec2(u, 1));
                            // sB += weight * color.a * log(intensity) * vec2(u, 1);
                            
                            // specularSum += colorRemainder.a * vec4(colorRemainder.rgb, 1.0);
                        // }
                    // }
                // }
            }
        }
    }
    
    vec2 sSolution = inverse(sA) * sB;
    vec3 specularAvg = specularSum.rgb / (specularSum.r + specularSum.g + specularSum.b);
    specularColor =  vec4(pow(
        min(1.0, sA[1][1] / expectedWeightSum) * min(3.0, exp(sSolution[1])) * specularAvg,
        vec3(1 / gamma)), 1.0);
    if (specularColor.r > 1 / 256.0 || specularColor.g > 1 / 256.0 || specularColor.b > 1 / 256.0)
    {
        specularRoughness = vec4(vec3(inversesqrt(2 * sSolution[0])), 1.0);
    }
    else
    {
        specularRoughness = vec4(0.0, 0.0, 0.0, 1.0);
    }
    
    debug0 = vec4(specularAvg, 1.0);
    debug1 = vec4(sSolution[0] / 4, (6 + sSolution[1]) / 8, 0.0, 1.0);
    debug2 = vec4(-sA[0][0], sA[0][1], sA[1][1], 1.0);
    debug3 = vec4(-sB[0], sB[1], 0.0, 1.0);
}
