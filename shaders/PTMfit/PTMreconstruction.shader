
Shader "Custom/PTMreconstruction"
{
    Properties{
        _WeightMap("Weights", 2DArray) = "" {}
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
            sampler2D _WeightMap;
            TEXTURE2D_ARRAY(_Weights);

            v2f vertexFunc(appdata IN) {
                v2f OUT;
                OUT.position = UnityObjectToClipPos(IN.vertex);
                OUT.uv = IN.uv;

                return OUT;
            }
            using System.IO;
            using UnityEngine;
            using UnityEditor;
            static void Create2DFromImages(int index)
            {
                /*string firstImage = EditorUtility.OpenFilePanel("Select the first texture in the array", "Assets", "png");*/
                string Image= EditorUtility.OpenFilePanel

                if (firstImage == null) // Operation cancelled
                {
                    return;
                }

                firstImage = AbsolutePathToAssetPath(firstImage);

                int firstTextureIndex = 0;

                Regex regex = new Regex("[0-9]+");

                // Turn the file name into a formatting string.
                string filePattern = regex.Replace(firstImage, (Match m) =>
                {
                    if (m.Success && !m.NextMatch().Success) // Final match
                    {
                        // Remember the starting index (probably either 0 or 1).
                        firstTextureIndex = int.Parse(m.Value);

                        // Replace number in the original file name with a format item.
                        // We actually use string.Format to create the formatting string.
                        return "{0," + m.Value.Length + ":D" + m.Value.Length + "}";
                    }
                    else // Not the final match
                    {
                        // No change
                        return m.Value;
                    }
                });

                int maxSlices = 32;

                // Load the first image
                Debug.Log("Loading " + firstImage);
                Texture2D tex = AssetDatabase.LoadAssetAtPath<Texture2D>(firstImage);

                if (tex != null)
                {
                    // Create the texture array
                    Texture2DArray textureArray = new Texture2DArray(tex.width, tex.height, maxSlices, TextureFormat.RGB24, false);
                    textureArray.SetPixels(tex.GetPixels(0), 0, 0);

                    // Load the other images
                    for (int i = 1; i < maxSlices && tex != null; i++)
                    {
                        string filename = string.Format(filePattern, firstTextureIndex + i);
                        tex = AssetDatabase.LoadAssetAtPath<Texture2D>(filename);
                        Debug.Log("Loading " + filename);
                        if (tex != null)
                        {
                            textureArray.SetPixels(tex.GetPixels(0), i, 0);
                        }
                        else
                        {
                            Debug.Log("File not found; finishing.");
                        }
                    }
                    textureArray.Apply();

                    // Turn the file name for the first image into a file name for the texture array
                    string assetPath = regex.Replace(firstImage, (Match m) =>
                    {
                        if (m.Success && !m.NextMatch().Success) // Final match
                        {
                            // Remember the starting index (probably either 0 or 1).
                            firstTextureIndex = int.Parse(m.Value);

                            // Replace number in the original file name with "arr"
                            return "arr";
                        }
                        else // Not the final match
                        {
                            // No change
                            return m.Value;
                        }
                    });

                    // Change the file extension to .asset.
                    assetPath = assetPath.Replace(".png", ".asset");

                    // Save the texture array
                    AssetDatabase.CreateAsset(textureArray, assetPath);
                    Debug.Log("Saved asset to " + assetPath);
                }
                else
                {
                    Debug.Log("File not found; finishing.");
                }
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

                float3 weights[BASIS_COUNT];
                for (int b = 0; b < BASIS_COUNT; b++)
                {
                    //weights[b] = texture(_WeightMap, float3(TEXCOORD0, b)).xyz;
                    weight[b] =SAMPLE_TEXTURE2D_ARRAY(_Weights, linear_clamp_sampler_Weights, TEXCOORD0, b);

                    pixelColor = pixelColor + float4(weights[b] * row[b], 0.0);
                }

                return  pow(pixelColor, float4(1 / 2.2));;
            }

            ENDCG
        }
    }
}
