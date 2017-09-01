package tetzlaff.gl.material;

import java.io.File;
import java.io.IOException;
import java.util.*;

import tetzlaff.gl.vecmath.Vector3;

public class Material 
{
    private String name;

    private Vector3 ambient;
    private Vector3 diffuse;
    private Vector3 specular;
    private Vector3 emission;
    private float exponent;
    private float roughness;
    private float opacity;
    private float translucency;
    private float metallic;
    private Vector3 sheen;
    private Vector3 clearcoat;
    private float clearcoatRoughness;

    private MaterialColorMap ambientMap;
    private MaterialColorMap diffuseMap;
    private MaterialColorMap specularMap;
    private MaterialColorMap emissionMap;
    private MaterialScalarMap exponentMap;
    private MaterialScalarMap roughnessMap;
    private MaterialScalarMap opacityMap;
    private MaterialScalarMap transparencyMap;
    private MaterialScalarMap translucencyMap;
    private MaterialScalarMap metallicMap;
    private MaterialColorMap sheenMap;
    private MaterialScalarMap anisotropyMap;
    private MaterialScalarMap anisotropyRotationMap;
    private MaterialBumpMap bumpMap;
    private MaterialTextureMap normalMap;
    private MaterialScalarMap displacementMap;

    public Material(String name)
    {
        this.name = name;

        this.ambient = new Vector3(0.0f);
        this.diffuse = new Vector3(0.0f);
        this.specular = new Vector3(0.0f);
        this.exponent = 0.0f;
        this.roughness = 1.0f;
        this.opacity = 1.0f;
        this.emission = new Vector3(0.0f);
        this.metallic = 0.0f;
        this.sheen = new Vector3(0.0f);
        this.clearcoat = new Vector3(0.0f);
        this.clearcoatRoughness = 0.0f;
    }

    public static Dictionary<String, Material> loadFromMTLFile(File mtlFile) throws IOException
    {
        Dictionary<String, Material> materials = new Hashtable<>();
        Material currentMaterial = null;
        boolean ambientSet = false;

        if (mtlFile.exists())
        {
            try(Scanner scanner = new Scanner(mtlFile))
            {
                while(scanner.hasNext())
                {
                    String id = scanner.next();

                    if ("newmtl".equals(id) && scanner.hasNext())
                    {
                        currentMaterial = new Material(scanner.next());
                        materials.put(currentMaterial.getName(), currentMaterial);
                    }
                    else if (currentMaterial != null)
                    {
                        try
                        {
                            switch(id.toLowerCase())
                            {
                                case "ka":
                                    currentMaterial.setAmbient(new Vector3(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
                                    ambientSet = true;
                                    break;
                                case "kd":
                                    currentMaterial.setDiffuse(new Vector3(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
                                    break;
                                case "ks":
                                    currentMaterial.setSpecular(new Vector3(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
                                    break;
                                case "ke":
                                    currentMaterial.setEmission(new Vector3(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
                                    break;
                                case "d":
                                    currentMaterial.setOpacity(scanner.nextFloat());
                                    break;
                                case "tr":
                                    currentMaterial.setOpacity(1.0f - scanner.nextFloat());
                                    break;
                                case "tf":
                                    currentMaterial.setTranslucency(scanner.nextFloat());
                                    break;
                                case "ns":
                                    currentMaterial.setExponent(scanner.nextFloat());
                                    break;
                                case "pr":
                                    currentMaterial.setRoughness(scanner.nextFloat());
                                    break;
                                case "pm":
                                    currentMaterial.setMetallic(scanner.nextFloat());
                                    break;
                                case "ps":
                                    currentMaterial.setSheen(new Vector3(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
                                    break;
                                case "pc":
                                    currentMaterial.setClearcoat(new Vector3(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));
                                    break;
                                case "pcr":
                                    currentMaterial.setClearcoatRoughness(scanner.nextFloat());
                                    break;
                                case "map_ka":
                                    MaterialColorMap ambientMap = parseColorMapStatement(scanner);
                                    currentMaterial.setAmbientMap(ambientMap);
                                    break;
                                case "map_kd":
                                    MaterialColorMap diffuseMap = parseColorMapStatement(scanner);
                                    currentMaterial.setDiffuseMap(diffuseMap);
                                    break;
                                case "map_ks":
                                    MaterialColorMap specularMap = parseColorMapStatement(scanner);
                                    currentMaterial.setSpecularMap(specularMap);
                                    break;
                                case "map_ke":
                                    MaterialColorMap emissionMap = parseColorMapStatement(scanner);
                                    currentMaterial.setEmissionMap(emissionMap);
                                    break;
                                case "map_d":
                                    MaterialScalarMap opacityMap = parseScalarMapStatement(scanner);
                                    if (opacityMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        opacityMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setOpacityMap(opacityMap);
                                    currentMaterial.setTransparencyMap(null);
                                    break;
                                case "map_tr":
                                    MaterialScalarMap transparencyMap = parseScalarMapStatement(scanner);
                                    if (transparencyMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        transparencyMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setTransparencyMap(transparencyMap);
                                    currentMaterial.setOpacityMap(null);
                                    break;
                                case "map_tf":
                                    MaterialScalarMap translucencyMap = parseScalarMapStatement(scanner);
                                    if (translucencyMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        translucencyMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setTransparencyMap(translucencyMap);
                                    currentMaterial.setOpacityMap(null);
                                    break;
                                case "map_ns":
                                    MaterialScalarMap exponentMap = parseScalarMapStatement(scanner);
                                    if (exponentMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        exponentMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setExponentMap(exponentMap);
                                    currentMaterial.setRoughnessMap(null);
                                    break;
                                case "map_pr":
                                    MaterialScalarMap roughnessMap = parseScalarMapStatement(scanner);
                                    if (roughnessMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        roughnessMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setRoughnessMap(roughnessMap);
                                    currentMaterial.setExponentMap(null);
                                    break;
                                case "map_pm":
                                    MaterialScalarMap metallicMap = parseScalarMapStatement(scanner);
                                    if (metallicMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        metallicMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setMetallicMap(metallicMap);
                                    break;
                                case "map_ps":
                                    MaterialColorMap sheenMap = parseColorMapStatement(scanner);
                                    currentMaterial.setSheenMap(sheenMap);
                                    break;
                                case "aniso":
                                    MaterialScalarMap anisotropyMap = parseScalarMapStatement(scanner);
                                    if (anisotropyMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        anisotropyMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setAnisotropyMap(anisotropyMap);
                                    break;
                                case "anisor":
                                    MaterialScalarMap anisotropyRotationMap = parseScalarMapStatement(scanner);
                                    if (anisotropyRotationMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        anisotropyRotationMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setAnisotropyRotationMap(anisotropyRotationMap);
                                    break;
                                case "bump":
                                    MaterialBumpMap bumpMap = parseBumpMapStatement(scanner);
                                    if (bumpMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        bumpMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setBumpMap(bumpMap);
                                    break;
                                case "disp":
                                    MaterialScalarMap displacementMap = parseScalarMapStatement(scanner);
                                    if (displacementMap.getChannel() == MaterialTextureChannel.Unspecified)
                                    {
                                        displacementMap.setChannel(MaterialTextureChannel.Luminance);
                                    }
                                    currentMaterial.setDisplacementMap(displacementMap);
                                    break;
                                case "normal":
                                    MaterialTextureMap normalMap = parseTextureStatement(scanner);
                                    currentMaterial.setNormalMap(normalMap);
                                    break;
                            }
                        }
                        catch(InputMismatchException e)
                        {
                            e.printStackTrace();
                            scanner.nextLine();
                        }
                        catch(NoSuchElementException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    if (scanner.hasNextLine())
                    {
                        // Always advance to the next line.
                        scanner.nextLine();
                    }
                }
            }
        }

        if (currentMaterial != null)
        {
            if(!ambientSet)
            {
                currentMaterial.setAmbient(currentMaterial.getDiffuse());
            }

            if (currentMaterial.getAmbientMap() == null)
            {
                currentMaterial.setAmbientMap(currentMaterial.getDiffuseMap());
            }
        }

        return materials;
    }

    private static void processTextureOption(Scanner scanner, MaterialTextureMap texture, String option)
    {
        try
        {
            // Option statement
            switch(option.toLowerCase())
            {
                case "-mm":
                    texture.setBase(scanner.nextFloat());
                    if (scanner.hasNextFloat())
                    {
                        texture.setGain(scanner.nextFloat());
                    }
                    break;
                case "-o":
                    float x = scanner.nextFloat();
                    float y;
                    float z;
                    if (scanner.hasNextFloat())
                    {
                        y = scanner.nextFloat();
                        if (scanner.hasNextFloat())
                        {
                            z = scanner.nextFloat();
                        }
                        else
                        {
                            z = 0.0f;
                        }
                    }
                    else
                    {
                        y = 0.0f;
                        z = 0.0f;
                    }
                    texture.setOffset(new Vector3(x, y, z));
                    break;
                case "-s":
                    float sx = scanner.nextFloat();
                    float sy;
                    float sz;
                    if (scanner.hasNextFloat())
                    {
                        sy = scanner.nextFloat();
                        if (scanner.hasNextFloat())
                        {
                            sz = scanner.nextFloat();
                        }
                        else
                        {
                            sz = 1.0f;
                        }
                    }
                    else
                    {
                        sy = 1.0f;
                        sz = 1.0f;
                    }
                    texture.setScale(new Vector3(sx, sy, sz));
                    break;
                case "-clamp":
                    if ("on".equals(scanner.next()))
                    {
                        texture.setClampingRequired(true);
                    }
                    break;
                default:
                    // If nothing else, read past the next token.
                    scanner.next();
                    break;
            }
        }
        catch(InputMismatchException e)
        {
            e.printStackTrace();
            scanner.next();
        }
    }

    private static MaterialTextureMap parseTextureStatement(Scanner scanner)
    {
        MaterialTextureMap texture = new MaterialTextureMap();

        while(texture.getMapName() == null)
        {
            while(scanner.hasNextFloat())
            {
                // Skip past additional numeric options
                scanner.nextFloat();
            }

            String nextPart = scanner.next();
            if(nextPart.startsWith("-"))
            {
                processTextureOption(scanner, texture, nextPart);
            }
            else
            {
                // The filename
                texture.setMapName(nextPart);
            }
        }

        return texture;
    }

    private static MaterialColorMap parseColorMapStatement(Scanner scanner)
    {
        MaterialColorMap texture = new MaterialColorMap();

        while(texture.getMapName() == null)
        {
            while(scanner.hasNextFloat())
            {
                // Skip past additional numeric options
                scanner.nextFloat();
            }

            String nextPart = scanner.next();
            if(nextPart.startsWith("-"))
            {
                if ("-cc".equals(nextPart))
                {
                    if ("on".equals(scanner.next()))
                    {
                        texture.setGammaCorrectionRequired(true);
                    }
                }
                else
                {
                    processTextureOption(scanner, texture, nextPart);
                }
            }
            else
            {
                // The filename
                texture.setMapName(nextPart);
            }
        }

        return texture;
    }

    private static MaterialScalarMap parseScalarMapStatement(Scanner scanner)
    {
        MaterialScalarMap texture = new MaterialScalarMap();

        while(texture.getMapName() == null)
        {
            while(scanner.hasNextFloat())
            {
                // Skip past additional numeric options
                scanner.nextFloat();
            }

            String nextPart = scanner.next();
            if(nextPart.startsWith("-"))
            {
                if ("-imfchan".equals(nextPart))
                {
                    switch(scanner.next().toLowerCase())
                    {
                        case "r":
                            texture.setChannel(MaterialTextureChannel.Red);
                            break;
                        case "g":
                            texture.setChannel(MaterialTextureChannel.Green);
                            break;
                        case "b":
                            texture.setChannel(MaterialTextureChannel.Blue);
                            break;
                        case "m":
                        case "a":
                            texture.setChannel(MaterialTextureChannel.Alpha);
                            break;
                        case "l":
                        case "y":
                            texture.setChannel(MaterialTextureChannel.Luminance);
                            break;
                        case "z":
                        case "d":
                            texture.setChannel(MaterialTextureChannel.Depth);
                            break;
                    }
                }
                else
                {
                    processTextureOption(scanner, texture, nextPart);
                }
            }
            else
            {
                // The filename
                texture.setMapName(nextPart);
            }
        }

        return texture;
    }

    private static MaterialBumpMap parseBumpMapStatement(Scanner scanner)
    {
        MaterialBumpMap texture = new MaterialBumpMap();

        while(texture.getMapName() == null)
        {
            while(scanner.hasNextFloat())
            {
                // Skip past additional numeric options
                scanner.nextFloat();
            }

            String nextPart = scanner.next();
            if(nextPart.startsWith("-"))
            {
                if ("-bm".equals(nextPart))
                {
                    if (scanner.hasNextFloat())
                    {
                        texture.setBumpMultiplier(scanner.nextFloat());
                    }
                    else
                    {
                        scanner.next(); // unexpected format
                    }
                }
                else if ("-imfchan".equals(nextPart))
                {
                    switch(scanner.next().toLowerCase())
                    {
                        case "r":
                            texture.setChannel(MaterialTextureChannel.Red);
                            break;
                        case "g":
                            texture.setChannel(MaterialTextureChannel.Green);
                            break;
                        case "b":
                            texture.setChannel(MaterialTextureChannel.Blue);
                            break;
                        case "m":
                        case "a":
                            texture.setChannel(MaterialTextureChannel.Alpha);
                            break;
                        case "l":
                        case "y":
                            texture.setChannel(MaterialTextureChannel.Luminance);
                            break;
                        case "z":
                        case "d":
                            texture.setChannel(MaterialTextureChannel.Depth);
                            break;
                    }
                }
                else
                {
                    processTextureOption(scanner, texture, nextPart);
                }
            }
            else
            {
                // The filename
                texture.setMapName(nextPart);
            }
        }

        return texture;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Vector3 getAmbient()
    {
        return this.ambient;
    }

    public void setAmbient(Vector3 ambient)
    {
        this.ambient = ambient;
    }

    public Vector3 getDiffuse()
    {
        return this.diffuse;
    }

    public void setDiffuse(Vector3 diffuse)
    {
        this.diffuse = diffuse;
    }

    public Vector3 getSpecular()
    {
        return this.specular;
    }

    public void setSpecular(Vector3 specular)
    {
        this.specular = specular;
    }

    public float getExponent()
    {
        return this.exponent;
    }

    public void setExponent(float exponent)
    {
        this.exponent = exponent;
        this.roughness = (float)Math.sqrt(2.0 / (exponent + 2));
    }

    public float getRoughness()
    {
        return this.roughness;
    }

    public void setRoughness(float roughness)
    {
        this.roughness = roughness;
        this.exponent = 2.0f / (roughness * roughness) - 2;
    }

    public Vector3 getEmission()
    {
        return this.emission;
    }

    public void setEmission(Vector3 emission)
    {
        this.emission = emission;
    }

    public float getMetallic()
    {
        return this.metallic;
    }

    public void setMetallic(float metallic)
    {
        this.metallic = metallic;
    }

    public Vector3 getSheen()
    {
        return this.sheen;
    }

    public void setSheen(Vector3 sheen)
    {
        this.sheen = sheen;
    }

    public Vector3 getClearcoat()
    {
        return this.clearcoat;
    }

    public void setClearcoat(Vector3 clearcoat)
    {
        this.clearcoat = clearcoat;
    }

    public float getClearcoatRoughness()
    {
        return this.clearcoatRoughness;
    }

    public void setClearcoatRoughness(float clearcoatRoughness)
    {
        this.clearcoatRoughness = clearcoatRoughness;
    }

    public float getOpacity()
    {
        return this.opacity;
    }

    public void setOpacity(float opacity)
    {
        this.opacity = opacity;
    }

    public float getTransparency()
    {
        return 1.0f - this.opacity;
    }

    public void setTransparency(float transparency)
    {
        this.opacity = 1.0f - opacity;
    }

    public float getTranslucency()
    {
        return translucency;
    }

    public void setTranslucency(float translucency)
    {
        this.translucency = translucency;
    }

    public MaterialColorMap getAmbientMap()
    {
        return this.ambientMap;
    }

    public void setAmbientMap(MaterialColorMap ambientMap)
    {
        this.ambientMap = ambientMap;
    }

    public MaterialColorMap getDiffuseMap()
    {
        return this.diffuseMap;
    }

    public void setDiffuseMap(MaterialColorMap diffuseMap)
    {
        this.diffuseMap = diffuseMap;
    }

    public MaterialColorMap getSpecularMap()
    {
        return this.specularMap;
    }

    public void setSpecularMap(MaterialColorMap specularMap)
    {
        this.specularMap = specularMap;
    }

    public MaterialScalarMap getExponentMap()
    {
        return this.exponentMap;
    }

    public void setExponentMap(MaterialScalarMap exponentMap)
    {
        this.exponentMap = exponentMap;
    }

    public MaterialScalarMap getRoughnessMap()
    {
        return this.roughnessMap;
    }

    public void setRoughnessMap(MaterialScalarMap roughnessMap)
    {
        this.roughnessMap = roughnessMap;
    }

    public MaterialScalarMap getOpacityMap()
    {
        return opacityMap;
    }

    public void setOpacityMap(MaterialScalarMap opacityMap)
    {
        this.opacityMap = opacityMap;
    }

    public MaterialScalarMap getTransparencyMap()
    {
        return transparencyMap;
    }

    public void setTransparencyMap(MaterialScalarMap transparencyMap)
    {
        this.transparencyMap = transparencyMap;
    }

    public MaterialScalarMap getTranslucencyMap()
    {
        return translucencyMap;
    }

    public void setTranslucencyMap(MaterialScalarMap translucencyMap)
    {
        this.translucencyMap = translucencyMap;
    }

    public MaterialColorMap getEmissionMap()
    {
        return this.emissionMap;
    }

    public void setEmissionMap(MaterialColorMap emissionMap)
    {
        this.emissionMap = emissionMap;
    }

    public MaterialScalarMap getMetallicMap()
    {
        return this.metallicMap;
    }

    public void setMetallicMap(MaterialScalarMap metallicMap)
    {
        this.metallicMap = metallicMap;
    }

    public MaterialColorMap getSheenMap()
    {
        return this.sheenMap;
    }

    public void setSheenMap(MaterialColorMap sheenMap)
    {
        this.sheenMap = sheenMap;
    }

    public MaterialScalarMap getAnisotropyMap()
    {
        return this.anisotropyMap;
    }

    public void setAnisotropyMap(MaterialScalarMap anisotropyMap)
    {
        this.anisotropyMap = anisotropyMap;
    }

    public MaterialScalarMap getAnisotropyRotationMap()
    {
        return this.anisotropyRotationMap;
    }

    public void setAnisotropyRotationMap(MaterialScalarMap anisotropyRotationMap)
    {
        this.anisotropyRotationMap = anisotropyRotationMap;
    }

    public MaterialBumpMap getBumpMap()
    {
        return this.bumpMap;
    }

    public void setBumpMap(MaterialBumpMap bumpMap)
    {
        this.bumpMap = bumpMap;
    }

    public MaterialTextureMap getNormalMap()
    {
        return this.normalMap;
    }

    public void setNormalMap(MaterialTextureMap normalMap)
    {
        this.normalMap = normalMap;
    }

    public MaterialScalarMap getDisplacementMap()
    {
        return this.displacementMap;
    }

    public void setDisplacementMap(MaterialScalarMap displacmentMap)
    {
        this.displacementMap = displacmentMap;
    }
}
