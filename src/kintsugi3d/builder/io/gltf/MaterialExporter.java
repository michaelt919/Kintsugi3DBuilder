/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.gltf;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MaterialExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(MaterialExporter.class);

    private static final String TEXTURE_EXPORT_FAILED = "Texture export failed";
    private static final String TEXTURE_EXPORT_FAILED_NAME_METHOD_PARAMETER_COUNT =
        "Texture export failed.  Name: {}  Method: {}  Parameter count: {}";
    private static final String TEXTURE_EXPORT_FAILED_NAME_METHOD_PARAMETER_TYPE =
        "Texture export failed.  Name: {}  Method: {}  Parameter Type: {}";

    private GltfAssetV2 asset;

    private int minLODSize = Integer.MAX_VALUE;
    private String textureFilePrefix = "";
    private String textureFileFormat;

    private TextureResources<?> textureResources;

    private final Map<String, TextureExportSpecification> textures = new HashMap<>(StandardTexture.values().length);

    protected MaterialExporter()
    {
    }

    public final GltfAssetV2 getAsset()
    {
        return asset;
    }

    public final void setAsset(GltfAssetV2 asset)
    {
        this.asset = asset;
    }

    public int getMinLODSize()
    {
        return minLODSize;
    }

    public void setMinLODSize(int minLODSize)
    {
        this.minLODSize = minLODSize;
    }


    public final String getTextureFilePrefix()
    {
        return textureFilePrefix;
    }

    public final void setTextureFilePrefix(String textureFilePrefix)
    {
        this.textureFilePrefix = textureFilePrefix;
    }

    public final String getTextureFileFormat()
    {
        return textureFileFormat;
    }

    public final void setTextureFileFormat(String textureFileFormat)
    {
        this.textureFileFormat = textureFileFormat;
    }
    public TextureResources<?> getTextureResources()
    {
        return textureResources;
    }

    public void setTextureResources(TextureResources<?> textureResources)
    {
        this.textureResources = textureResources;
    }

    public final Iterable<String> getSupportedTextures()
    {
        Collection<String> supportedTextures = new ArrayList<>(StandardTexture.values().length);

        for (Method method : this.getClass().getMethods()) // all methods in the current class and superclasses
        {
            String texName;

            if (method.isAnnotationPresent(CustomTextureExport.class))
            {
                supportedTextures.add(method.getAnnotation(CustomTextureExport.class).value());
            }
            else if (method.isAnnotationPresent(StandardTextureExport.class))
            {
                supportedTextures.add(method.getAnnotation(StandardTextureExport.class).value().texName);
            }
        }

        return supportedTextures;
    }

    public final void apply()
    {
        Collection<String> availableTextures = textureResources.getTextures().keySet();

        for (Method method : this.getClass().getMethods()) // all methods in the current class and superclasses
        {
            String texName;
            boolean requiresAlpha;

            if (method.isAnnotationPresent(CustomTextureExport.class))
            {
                CustomTextureExport annotation = method.getAnnotation(CustomTextureExport.class);
                texName = annotation.value();
                requiresAlpha = annotation.requiresAlpha();
            }
            else if (method.isAnnotationPresent(StandardTextureExport.class))
            {
                StandardTextureExport annotation = method.getAnnotation(StandardTextureExport.class);
                texName = annotation.value().texName;
                requiresAlpha = annotation.requiresAlpha();
            }
            else
            {
                // Method with no applicable annotations; skip
                texName = null;
                requiresAlpha = false;
            }

            if (texName != null && availableTextures.contains(texName) && !textures.containsKey(texName))
            {
                if (method.getParameterCount() == 1)
                {
                    try
                    {
                        TextureInfo texParam = processTextureParam(texName, method, method.getParameterTypes()[0], requiresAlpha);
                        if (texParam != null)
                        {
                            method.invoke(this, texParam);
                        }
                    }
                    catch (IllegalAccessException | InvocationTargetException e)
                    {
                        textureExportFailed(e);
                    }
                }
                else
                {
                    // Do not attempt to assign the texture if the method takes unexpected parameters
                    LOG.error(TEXTURE_EXPORT_FAILED_NAME_METHOD_PARAMETER_COUNT, texName, method, method.getParameterCount());
                }
            }
        }

        Object extras = getExtras();
        if (extras != null)
        {
            asset.getGltf().getMaterials().forEach(material -> material.setExtras(extras));
        }

        finishTextures();

        int baseRes = textureResources.getHeight();
        if (minLODSize < textureResources.getHeight())
        {
            for (var texEntry : textures.entrySet())
            {
                addLodsToTexture(texEntry.getValue().info, texEntry.getKey(), baseRes, minLODSize);
            }
        }
    }

    /**
     * To be overridden by subclass
     * @return
     */
    protected Object getExtras()
    {
        return null;
    }

    /**
     * To be overridden by subclass.
     * Called after all other texture export methods, but before saveTextures.
     */
    protected void finishTextures()
    {
    }

    /**
     * To be overridden by subclass.
     * Called after all other saving textures and LODs.
     */
    protected void postExport()
    {
    }

    public void saveTextures(File outputDirectory)
    {
        textureResources.saveNamedTextures(getSupportedTextures(), textureFileFormat, outputDirectory, textureFilePrefix);
    }

    public void makeLODs(File outputDirectory)
    {
        LODGenerator.getInstance().generateLODs(minLODSize, outputDirectory,
            textures.values().toArray(TextureExportSpecification[]::new));
    }

    private TextureInfo processTextureParam(String texName, Method method, Class<?> paramType, boolean requiresAlpha)
    {
        String format = determineFileFormat(textureFileFormat, requiresAlpha);
        TextureInfo tex = createRelativeTexture(getTextureFilename(texName, format), texName, format);

        if (paramType.isAssignableFrom(TextureInfo.class))
        {
            return tex;
        }
        else if (paramType.isAssignableFrom(MaterialNormalTextureInfo.class))
        {
            return convertTexInfoToNormal(tex);
        }
        else if (paramType.isAssignableFrom(MaterialOcclusionTextureInfo.class))
        {
            return convertTexInfoToOcclusion(tex);
        }
        else
        {
            LOG.error(TEXTURE_EXPORT_FAILED_NAME_METHOD_PARAMETER_TYPE, texName, method, paramType);
            // Do not attempt to assign the texture if the method doesn't take a recognized type.
            return null;
        }
    }

    /**
     * Use this method to add textures outside of the annotation system in subclasses.
     * @param texName
     * @return
     */
    protected TextureInfo addTexture(String texName)
    {
        return addTexture(texName, false);
    }

    /**
     * Use this method to add textures outside of the annotation system in subclasses.
     * @param texName
     * @param requiresAlpha
     * @return
     */
    protected TextureInfo addTexture(String texName, boolean requiresAlpha)
    {
        String format = determineFileFormat(textureFileFormat, requiresAlpha);
        return createRelativeTexture(getTextureFilename(texName, format), texName, format);
    }

    private static void textureExportFailed(ReflectiveOperationException e)
    {
        LOG.warn(TEXTURE_EXPORT_FAILED, e);
    }

    private TextureInfo createRelativeTexture(String uri, String name, String format)
    {
        GlTF gltf = asset.getGltf();

        Image image = new Image();
        image.setUri(uri);
        gltf.addImages(image);
        int imageIndex = gltf.getImages().size() - 1;

        Texture texture = new Texture();
        texture.setSource(imageIndex);
        texture.setName(name);
        gltf.addTextures(texture);
        int textureIndex = gltf.getTextures().size() - 1;

        TextureInfo info = new TextureInfo();
        info.setIndex(textureIndex);

        textures.put(name, new TextureExportSpecification(info, uri, format));

        return info;
    }

    protected static MaterialNormalTextureInfo convertTexInfoToNormal(TextureInfo texInfo)
    {
        MaterialNormalTextureInfo normInfo = new MaterialNormalTextureInfo();

        normInfo.setIndex(texInfo.getIndex());
        normInfo.setTexCoord(texInfo.getTexCoord());
        normInfo.setExtensions(texInfo.getExtensions());
        normInfo.setExtras(texInfo.getExtras());

        return normInfo;
    }

    protected static MaterialOcclusionTextureInfo convertTexInfoToOcclusion(TextureInfo texInfo)
    {
        MaterialOcclusionTextureInfo occlusionInfo = new MaterialOcclusionTextureInfo();

        occlusionInfo.setIndex(texInfo.getIndex());
        occlusionInfo.setTexCoord(texInfo.getTexCoord());
        occlusionInfo.setExtensions(texInfo.getExtensions());
        occlusionInfo.setExtras(texInfo.getExtras());

        return occlusionInfo;
    }

    private String getTextureFilename(String name, String format)
    {
        return TextureResources.getTextureFilename(name, format, textureFilePrefix);
    }

    protected static String determineFileFormat(String preferredFormat, boolean requiresAlpha)
    {
        // If user requested JPEG and alpha is required, force PNG since JPEG doesn't support alpha.
        return requiresAlpha && ("JPEG".equals(preferredFormat) || "JPG".equals(preferredFormat)) ? "PNG" : preferredFormat;
    }

    private void addLodsToTexture(TextureInfo textureInfo, String baseUri, int baseRes, int minRes)
    {
        if (textureInfo != null)
        {
            Texture tex = getTexForInfo(textureInfo);
            GlTF gltf = asset.getGltf();

            TextureExtras extras = new TextureExtras();

            extras.setBaseRes(baseRes);

            String filename = baseUri;
            String extension = "";
            int i = filename.lastIndexOf('.'); //Strip file extension
            if (i > 0)
            {
                extension = filename.substring(i);
                filename = filename.substring(0, i);
            }

            // size = 2048, 1024, 512... minRes
            for (int size = baseRes / 2; size >= minRes; size /= 2)
            {
                Image image = new Image();
                image.setUri(String.format("%s-%d%s", filename, size, extension));
                gltf.addImages(image);
                int imageIndex = gltf.getImages().size() - 1;
                extras.setLodImageIndex(size, imageIndex);
            }

            tex.setExtras(extras);
        }
    }

    private Texture getTexForInfo(TextureInfo info)
    {
        GlTF gltf = asset.getGltf();
        return gltf.getTextures().get(info.getIndex());
    }
}
