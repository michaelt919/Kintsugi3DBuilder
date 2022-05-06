#version 330
in vec3 fPosition;
in vec2 fTexCoord;
in vec3 fNormal;
in vec3 fTangent;
in vec3 fBitangent;

#include <shaders/colorappearance/colorappearance_multi_as_single.glsl>
#include <shaders/colorappearance/imgspace.glsl>

uniform float aperture;
uniform float focal;
uniform float distance;

out vec4 colormask;

void main() {



    vec3 viewVector = getViewVector();
    float distanceObject= length(viewVector);


    float coc=
    ((aperture*(abs(distance-distanceObject))/
    (distanceObject))*(focal/(distance-focal)));


    if(coc>0.25) {
        colormask=vec4(1,1,1,1);
    }
    else  {
        colormask=vec4(0,0,0,1);

    }

    //colormask=vec4(coc,coc,coc,1);

}
