#version 330

uniform sampler2D input0;
uniform sampler2D input1;
uniform sampler2D input2;
uniform sampler2D input3;
uniform sampler2D input4;
uniform sampler2D input5;
uniform sampler2D input6;
uniform sampler2D input7;

uniform sampler2D original0;
uniform sampler2D original1;
uniform sampler2D original2;
uniform sampler2D original3;
uniform sampler2D original4;
uniform sampler2D original5;
uniform sampler2D original6;
uniform sampler2D original7;

uniform vec4 defaultColor0;
uniform vec4 defaultColor1;
uniform vec4 defaultColor2;
uniform vec4 defaultColor3;
uniform vec4 defaultColor4;
uniform vec4 defaultColor5;
uniform vec4 defaultColor6;
uniform vec4 defaultColor7;

uniform bool fillAll;

in vec4 fPosition;

layout(location=0) out vec4 output0;
layout(location=1) out vec4 output1;
layout(location=2) out vec4 output2;
layout(location=3) out vec4 output3;
layout(location=4) out vec4 output4;
layout(location=5) out vec4 output5;
layout(location=6) out vec4 output6;
layout(location=7) out vec4 output7;

vec4 fill(sampler2D input, sampler2D original, vec4 defaultColor)
{
    vec2 texCoords = (fPosition.xy + vec2(1)) / 2;
    vec4 central = texture(input, texCoords);
    
    if (central.a >= 1.0)
    {
        // Return the input pixel if it already has an alpha of 1.0
        return central;
    }
    else
    {
        // Check which pixels are valid blending candidates.
        // We don't blend with pixels that originally had an alpha of 0.0
        // to prevent bleeding across boundaries.
        float northMask = textureOffset(original, texCoords, ivec2(0, 1)).a;
        float southMask = textureOffset(original, texCoords, ivec2(0, -1)).a;
        float eastMask = textureOffset(original, texCoords, ivec2(1, 0)).a;
        float westMask = textureOffset(original, texCoords, ivec2(-1, 0)).a;
        
        // If there are no valid candidates, immediately blend with the default color
        if (northMask <= 0.0 && southMask <= 0.0 && eastMask <= 0.0 && westMask <= 0.0)
        {
            return vec4(central.a * central.rgb + (1.0 - central.a) * defaultColor.rgb, 1.0);
        }
        else
        {
            // Sample the neighboring pixels
            vec4 sum = vec4(0.0);
            if (northMask > 0.0)
            {
                vec4 north = textureOffset(input, texCoords, ivec2(0, 1));
                sum += north.a * vec4(north.rgb, 1.0);
            }
            if (southMask > 0.0)
            {
                vec4 south = textureOffset(input, texCoords, ivec2(0, -1));
                sum += south.a * vec4(south.rgb, 1.0);
            }
            if (eastMask > 0.0)
            {
                vec4 east = textureOffset(input, texCoords, ivec2(1, 0));
                sum += east.a * vec4(east.rgb, 1.0);
            }
            if (westMask > 0.0)
            {
                vec4 west = textureOffset(input, texCoords, ivec2(-1, 0));
                sum += west.a * vec4(west.rgb, 1.0);
            }
            
            if (sum.a > (1.0 - central.a))
            {
                return vec4(central.a * central.rgb + (1.0 - central.a) * sum.rgb / sum.a, 1.0);
            }
            else if (fillAll)
            {
                return vec4(central.a * central.rgb + sum.rgb + 
                    (1.0 - central.a - sum.a) * defaultColor.rgb, 1.0);
            }
            else
            {
                return vec4((central.a * central.rgb + sum.rgb) / (central.a + sum.a), central.a + sum.a);
            }
        }
    }
}

void main()
{
    output0 = fill(input0, original0, defaultColor0);
    output1 = fill(input1, original1, defaultColor1);
    output2 = fill(input2, original2, defaultColor2);
    output3 = fill(input3, original3, defaultColor3);
    output4 = fill(input4, original4, defaultColor4);
    output5 = fill(input5, original5, defaultColor5);
    output6 = fill(input6, original6, defaultColor6);
    output7 = fill(input7, original7, defaultColor7);
}
