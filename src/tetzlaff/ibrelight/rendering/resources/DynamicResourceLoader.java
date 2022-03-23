package tetzlaff.ibrelight.rendering.resources;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.DynamicResourceManager;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.util.AbstractImage;
import tetzlaff.util.ArrayBackedImage;
import tetzlaff.util.EnvironmentMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class DynamicResourceLoader<ContextType extends Context<ContextType>> implements DynamicResourceManager
{
    private final LoadingMonitor loadingMonitor;
    private final ContextType context;
    private final IBRResources<ContextType> resources;
    private final LightingResources<ContextType> lightingResources;

    private boolean newEnvironmentDataAvailable;
    private EnvironmentMap newEnvironmentData;
    private boolean environmentMapUnloadRequested = false;
    private File currentEnvironmentFile;
    private long environmentLastModified;
    private final Object loadEnvironmentLock = new Object();

    @SuppressWarnings("FieldCanBeLocal")
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

    @SuppressWarnings("FieldCanBeLocal")
    private volatile File desiredBackplateFile;

    private AbstractImage currentEnvironmentMap;

    public DynamicResourceLoader(LoadingMonitor loadingMonitor, IBRResources<ContextType> resources,
                                 LightingResources<ContextType> lightingResources)
    {
        this.loadingMonitor = loadingMonitor;
        this.context = resources.context;
        this.resources = resources;
        this.lightingResources = lightingResources;
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
                e.printStackTrace();
            }
            finally
            {
                this.newEnvironmentDataAvailable = false;
                this.loadingMonitor.loadingComplete();
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
                e.printStackTrace();
            }
            finally
            {
                this.newBackplateDataAvailable = false;
            }
        }

        if (this.newLuminanceEncodingDataAvailable)
        {
            this.resources.viewSet.setTonemapping(
                this.resources.viewSet.getGamma(),
                this.newLinearLuminanceValues,
                this.newEncodedLuminanceValues);

            this.resources.updateLuminanceMap();

            this.newLuminanceEncodingDataAvailable = false;
        }

        if (this.newLightCalibrationAvailable)
        {
            for (int i = 0; i < resources.viewSet.getLightCount(); i++)
            {
                this.resources.viewSet.setLightPosition(i, newLightCalibration);
            }

            this.resources.updateLightData();
            this.newLightCalibrationAvailable = false;
        }
    }

    @Override
    public Optional<AbstractImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException
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
            System.out.println("Loading new environment texture.");

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
                    this.loadingMonitor.startLoading();
                    this.loadingMonitor.setMaximum(0.0);

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
                        e.printStackTrace();
                    }
                }
            }

            this.newEnvironmentDataAvailable = this.newEnvironmentDataAvailable || readCompleted;

            if (readCompleted)
            {
                environmentLastModified = lastModified;
                currentEnvironmentMap = new ArrayBackedImage(width, height, pixels);
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
            System.out.println("Loading new backplate texture.");

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
                        this.newBackplateData = ImageIO.read(backplateFile);
                        this.currentBackplateFile = backplateFile;
                        readCompleted = true;
                    }
                    catch (FileNotFoundException e)
                    {
                        throw e;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
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
    public void setLightCalibration(Vector3 lightCalibration)
    {
        this.newLightCalibration = lightCalibration;
        this.newLightCalibrationAvailable = true;
    }
}
