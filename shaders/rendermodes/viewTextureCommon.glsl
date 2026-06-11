#ifndef VIEW_TEXTURE_COMMON
#define VIEW_TEXTURE_COMMON

#include <subject/overlayModes.glsl>
#line 5 3200

uniform sampler2DArray weightMaps;

in vec2 fTexCoord;

layout(location = 0) out vec4 fragColor;

#ifndef SAMPLE_VIEW_TEX
#define SAMPLE_VIEW_TEX(VIEW_TEX_UV) ( vec4(VIEW_TEX_UV, 0.0, 1.0) )
#endif

void main()
{
    vec4 overlay;

#if OVERLAY_MODE == OVERLAY_MODE_WEIGHTMAP
    vec4 weightmapTex = texture(weightMaps, vec3(fTexCoord, OVERLAY_WEIGHTMAP_INDEX));
    overlay =  weightmapTex.r * vec4(1.0, 0.0, 1.0, 0.0);
#else
    overlay = vec4(0.0);
#endif

    fragColor = overlay + SAMPLE_VIEW_TEX(fTexCoord);
}

#endif
