
Shader "Custom/PTMreconstruction"
{
    Properties{
        //_WeightMap("Weights", 2DArray) = "" {}
        _Weight0("Weight0", 2D) = "" {}
        _Weight1("Weight1", 2D) = "" {}
        _Weight2("Weight2", 2D) = "" {}
        _Weight3("Weight3", 2D) = "" {}
        _Weight4("Weight4", 2D) = "" {}
        _Weight5("Weight5", 2D) = "" {}
    }
    SubShader{
        Pass{
            CGPROGRAM
            #pragma vertex vertexFunc
            #pragma fragment fragmentFunc
            #include "UnityCG.cginc"

            struct appdata {
                float4 vertex:POSITION;
                float2 uv:TEXCOORD0;
            };

            struct v2f {
                float4 position:SV_POSITION;
                float2 uv:TEXCOORD0;
            };

            fixed4 _Color;

            v2f vertexFunc(appdata IN) {
                v2f OUT;
                OUT.position = UnityObjectToClipPos(IN.vertex);
                OUT.uv = IN.uv;

                return OUT;
            }
            

            fixed4 fragmentFunc(v2f IN) :SV_Target{
                int BASIS_COUNT = 6;
                fixed4 pixelColor = (0,0,0,1);
                float u = _WorldSpaceLightPos0.x;
                float v = _WorldSpaceLightPos0.y;
                float w = _WorldSpaceLightPos0.z;
                float row[BASIS_COUNT];
                row[0] = 1.0f;
                row[1] = u;
                row[2] = v;
                row[3] = w;
                row[4] = u * u;
                row[5] = u * v;

                half4 color0 = 2*SAMPLE_TEXTURE2D(_Weight0, sampler__Weight0, TEXCOORD0)- (1, 1, 1, 1);
                pixelColor = pixelColor + color0*row[0]；
                
                half4 color1 = 2*SAMPLE_TEXTURE2D(_Weight1, sampler__Weight1, TEXCOORD0)- (1, 1, 1, 1);
                pixelColor = pixelColor + color1*row[1]；

                half4 color2 = 2*SAMPLE_TEXTURE2D(_Weight2, sampler__Weight2, TEXCOORD0)- (1, 1, 1, 1);
                pixelColor = pixelColor + color2*row[2]；

                half4 color3 = 2*SAMPLE_TEXTURE2D(_Weight3, sampler__Weight3, TEXCOORD0)- (1, 1, 1, 1);
                pixelColor = pixelColor + color3*row[3]；
                
                half4 color4 = 2*SAMPLE_TEXTURE2D(_Weight4, sampler__Weight4, TEXCOORD0)- (1, 1, 1, 1);
                pixelColor = pixelColor + color4*row[4]；

                half4 color5 = 2*SAMPLE_TEXTURE2D(_Weight5, sampler__Weight5, TEXCOORD0)- (1, 1, 1, 1);
                pixelColor = pixelColor + color5*row[5]；

                return  pixelColor;
            }

            ENDCG
        }
    }
}
