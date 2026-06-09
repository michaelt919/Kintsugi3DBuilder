#ifndef VIEW_TEXTURE_COMMON
#define VIEW_TEXTURE_COMMON

#line 5 3200

in vec2 fTexCoord;

layout(location = 0) out vec4 fragColor;

#ifndef SAMPLE_VIEW_TEX
#define SAMPLE_VIEW_TEX(VIEW_TEX_UV) ( vec4(VIEW_TEX_UV, 0.0, 1.0) )
#endif

void main()
{
    fragColor = SAMPLE_VIEW_TEX(fTexCoord);
}

#endif
