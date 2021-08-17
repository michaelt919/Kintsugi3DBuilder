
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
                Texture2D weights[BASIS_COUNT];
                weights[0] = _Weight0;
                weights[1] = _Weight1;
                weights[2] = _Weight2;
                weights[3] = _Weight3;
                weights[4] = _Weight4;
                weights[5] = _Weight5;
                for (int b = 0; b < BASIS_COUNT; b++)
                {
                    half4 color = SAMPLE_TEXTURE2D(_Weight0, sampler__Weight, TEXCOORD0);
                    pixelColor = pixelColor + color*row[b]；
                    half4 color = SAMPLE_TEXTURE2D(_Weight1, sampler__Weight, TEXCOORD0);
                    pixelColor = pixelColor + color * row[b];
                    half4 color = SAMPLE_TEXTURE2D(_Weight2, sampler__Weight, TEXCOORD0);
                    pixelColor = pixelColor + color * row[b];
                    half4 color = SAMPLE_TEXTURE2D(_Weight3, sampler__Weight, TEXCOORD0);
                    pixelColor = pixelColor + color * row[b];
                    half4 color = SAMPLE_TEXTURE2D(_Weight4, sampler__Weight, TEXCOORD0);
                    pixelColor = pixelColor + color * row[b];
                    half4 color = SAMPLE_TEXTURE2D(_Weight5, sampler__Weight, TEXCOORD0);
                    pixelColor = pixelColor + color * row[b];

                }


                return  pow(pixelColor, float4(1 / 2.2));;
            }

            ENDCG
        }
    }
}
