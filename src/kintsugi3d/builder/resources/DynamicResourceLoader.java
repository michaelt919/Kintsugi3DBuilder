/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.resources;

import kintsugi3d.builder.core.DynamicResourceManager;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.rendering.components.RenderingSubject;
import kintsugi3d.builder.resources.project.GraphicsResources;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.ArrayBackedColorImage;
import kintsugi3d.util.EncodableColorImage;
import kintsugi3d.util.EnvironmentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DynamicResourceLoader<ContextType extends Context<ContextType>> implements DynamicResourceManager
{
    private static final Logger LOG = LoggerFactory.getLogger(DynamicResourceLoader.class);
    private final ProgressMonitor progressMonitor;
    private final ContextType context;
    private final GraphicsResources<ContextType> resources;
    private final LightingResources<ContextType> lightingResources;
    private final RenderingSubject<ContextType> subject;

    private boolean newEnvironmentDataAvailable;
    private EnvironmentMap newEnvironmentData;
    private boolean environmentMapUnloadRequested = false;
    private File currentEnvironmentFile;
    private long environmentLastModified;
    private final Object loadEnvironmentLock = new Object();

    private volatile File desiredShaderFile;
    private volatile Map<String, Optional<Object>> shaderDefines;

    private volatile File desiredEnvironmentFile;

    private boolean newBackplateDataAvailable;
    private BufferedImage newBackplateData;
    private boolean backplateUnloadRequested = false;
    private File currentBackplateFile;
    private long backplateLastModified;
    private final Object loadBackplateLock = new Object();

    private boolean newLuminanceEncodingDataAvailable;
    private double[] newLinearLuminanceValues;
    private byte[] newEncodedLuminanceValues;

    private boolean newLightCalibrationAvailable;
    private Vector3 newLightCalibration;

    private volatile File desiredBackplateFile;

    private EncodableColorImage currentEnvironmentMap;

    public DynamicResourceLoader(ProgressMonitor progressMonitor, GraphicsResources<ContextType> resources,
        RenderingSubject<ContextType> subject, LightingResources<ContextType> lightingResources)
    {
        this.progressMonitor = progressMonitor;
        this.context = resources.getContext();
        this.resources = resources;
        this.lightingResources = lightingResources;
        this.subject = subject;
    }

    public void update()
    {
        if (this.environmentMapUnloadRequested)
        {
            lightingResources.takeEnvironmentMap(null);
            this.environmentMapUnloadRequested = false;
        }

        if (this.backplateUnloadRequested)
        {
            lightingResources.takeBackplateTexture(null);
            this.backplateUnloadRequested = false;
        }

        if (this.desiredShaderFile != null)
        {
            this.subject.useFragmentShader(desiredShaderFile);
            this.subject.setExtraFragmentShaderDefines(this.shaderDefines);
            this.subject.reloadShaders();

            this.desiredShaderFile = null;
        }

        if (this.newEnvironmentDataAvailable)
        {
            try
            {
                Cubemap<ContextType> newEnvironmentTexture = null;

                synchronized(loadEnvironmentLock)
                {
                    if (this.newEnvironmentData != null)
                    {
                        EnvironmentMap environmentData = this.newEnvironmentData;
                        this.newEnvironmentData = null;

                        float[][] sides = environmentData.getData();

                        newEnvironmentTexture = context.getTextureFactory().buildColorCubemap(environmentData.getSide())
                            .loadFace(CubemapFace.POSITIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.PX].length / 3, sides[EnvironmentMap.PX]))
                            .loadFace(CubemapFace.NEGATIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.NX].length / 3, sides[EnvironmentMap.NX]))
                            .loadFace(CubemapFace.POSITIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.PY].length / 3, sides[EnvironmentMap.PY]))
                            .loadFace(CubemapFace.NEGATIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.NY].length / 3, sides[EnvironmentMap.NY]))
                            .loadFace(CubemapFace.POSITIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.PZ].length / 3, sides[EnvironmentMap.PZ]))
                            .loadFace(CubemapFace.NEGATIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3,
                                sides[EnvironmentMap.NZ].length / 3, sides[EnvironmentMap.NZ]))
                            .setInternalFormat(ColorFormat.RGB32F)
                            .setMipmapsEnabled(true)
                            .setLinearFilteringEnabled(true)
                            .createTexture();

                        newEnvironmentTexture.setTextureWrap(TextureWrapMode.Repeat, TextureWrapMode.None);
                    }
                }

                if (newEnvironmentTexture != null)
                {
                    lightingResources.takeEnvironmentMap(newEnvironmentTexture);
                }
            }
            catch (RuntimeException e)
            {
                LOG.error("An error has occurred", e);
            }
            catch (Error e)
            {
                LOG.error("An error has occurred", e);
                //noinspection ProhibitedExceptionThrown
                throw e;
            }
            finally
            {
                this.newEnvironmentDataAvailable = false;
                this.progressMonitor.complete();
            }
        }

        if (this.newBackplateDataAvailable)
        {
            try
            {
                Texture2D<ContextType> newBackplateTexture = null;

                synchronized(loadBackplateLock)
                {
                    if (this.newBackplateData != null)
                    {
                        BufferedImage backplateData = this.newBackplateData;
                        this.newBackplateData = null;

                        newBackplateTexture = context.getTextureFactory().build2DColorTextureFromImage(backplateData, true)
                            .setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP)
                            .setLinearFilteringEnabled(true)
                            .setMipmapsEnabled(true)
                            .createTexture();
                    }
                }

                if (newBackplateTexture != null)
                {
                    lightingResources.takeBackplateTexture(newBackplateTexture);
                }
            }
            catch (RuntimeException e)
            {
                LOG.error("An error has occurred", e);
            }
            catch (Error e)
            {
                LOG.error("An error has occurred", e);
                //noinspection ProhibitedExceptionThrown
                throw e;
            }
            finally
            {
                this.newBackplateDataAvailable = false;
            }
        }

        if (this.newLuminanceEncodingDataAvailable)
        {
            if (this.newLinearLuminanceValues != null && this.newEncodedLuminanceValues != null)
            {
                this.resources.updateLuminanceMap(this.newLinearLuminanceValues, this.newEncodedLuminanceValues);
            }
            else
            {
                this.resources.clearLuminanceMap();
            }

            this.newLuminanceEncodingDataAvailable = false;
        }

        if (this.newLightCalibrationAvailable)
        {
            this.resources.updateLightCalibration(this.newLightCalibration);
            this.newLightCalibrationAvailable = false;
        }
    }

    @Override
    public void requestFragmentShader(File shaderFile)
    {
        this.desiredShaderFile = shaderFile;
    }

    @Override
    public void requestFragmentShader(File shaderFile, Map<String, Optional<Object>> extraDefines)
    {
        this.desiredShaderFile = shaderFile;
        this.shaderDefines = extraDefines;
    }

    @Override
    public Optional<EncodableColorImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException
    {
        if (environmentFile == null)
        {
            if (this.lightingResources.getEnvironmentMap() != null)
            {
                this.environmentMapUnloadRequested = true;
            }

            currentEnvironmentMap = null;
            return Optional.empty();
        }
        else if (environmentFile.exists())
        {
            LOG.info("Loading new environment texture.");

            this.desiredEnvironmentFile = environmentFile;
            long lastModified = environmentFile.lastModified();
            boolean readCompleted = false;

            int width = 0;
            int height = 0;
            float[] pixels = null;

            synchronized(loadEnvironmentLock)
            {
                if (Objects.equals(environmentFile, desiredEnvironmentFile) &&
                        (!Objects.equals(environmentFile, currentEnvironmentFile) || lastModified != environmentLastModified))
                {
                    if(this.progressMonitor.isConflictingProcess()){
                        return Optional.empty();
                    }

                    this.progressMonitor.start();
                    this.progressMonitor.setMaxProgress(0.0);
                    this.progressMonitor.setProcessName("Load Environment Map");

                    try
                    {
                        // Use Michael Ludwig's code to convert to a cube map (supports either cross or panorama input)
                        this.newEnvironmentData = EnvironmentMap.createFromHDRFile(environmentFile);
                        this.currentEnvironmentFile = environmentFile;
                        width = newEnvironmentData.getSide() * 4;
                        height = newEnvironmentData.getSide() * 2;
                        pixels = EnvironmentMap.toPanorama(newEnvironmentData.getData(), newEnvironmentData.getSide(), width, height);
                        readCompleted = true;
                    }
                    catch (FileNotFoundException e)
                    {
                        throw e;
                    }
                    catch (IOException e)
                    {
                        LOG.error("Error loading environment map:", e);
                    }
                }
            }

            this.newEnvironmentDataAvailable = this.newEnvironmentDataAvailable || readCompleted;

            if (readCompleted)
            {
                environmentLastModified = lastModified;
                currentEnvironmentMap = new ArrayBackedColorImage(width, height, pixels);
            }

            return Optional.ofNullable(currentEnvironmentMap);
        }
        else
        {
            throw new FileNotFoundException(environmentFile.getPath());
        }
    }

    @Override
    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        if (backplateFile == null && lightingResources.getBackplateTexture() != null)
        {
            this.backplateUnloadRequested = true;
        }
        else if (backplateFile != null && backplateFile.exists())
        {
            LOG.info("Loading new backplate texture.");

            this.desiredBackplateFile = backplateFile;
            long lastModified = backplateFile.lastModified();
            boolean readCompleted = false;

            synchronized(loadBackplateLock)
            {
                if (Objects.equals(backplateFile, desiredBackplateFile) &&
                        (!Objects.equals(backplateFile, currentBackplateFile) || lastModified != backplateLastModified))
                {
                    try
                    {
                        this.newBackplateData = ImageHelper.read(backplateFile).getBufferedImage();
                        this.currentBackplateFile = backplateFile;
                        readCompleted = true;
                    }
                    catch (FileNotFoundException e)
                    {
                        throw e;
                    }
                    catch (IOException e)
                    {
                        LOG.error("Error loading backplate:", e);
                    }
                }
            }

            this.newBackplateDataAvailable = this.newBackplateDataAvailable || readCompleted;

            if (readCompleted)
            {
                backplateLastModified = lastModified;
            }
        }
        else if (backplateFile != null)
        {
            throw new FileNotFoundException(backplateFile.getPath());
        }
    }

    @Override
    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.newLinearLuminanceValues = linearLuminanceValues;
        this.newEncodedLuminanceValues = encodedLuminanceValues;
        this.newLuminanceEncodingDataAvailable = true;
    }

    @Override
    public void clearTonemapping()
    {
        this.newLinearLuminanceValues = null;
        this.newEncodedLuminanceValues = null;
        this.newLuminanceEncodingDataAvailable = true;
    }

    @Override
    public void setLightCalibration(Vector3 lightCalibration)
    {
        this.newLightCalibration = lightCalibration;
        this.newLightCalibrationAvailable = true;
    }
}
