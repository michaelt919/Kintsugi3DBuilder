package tetzlaff.ibrelight.rendering;

import java.io.*;
import java.util.AbstractList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.interactive.InteractiveRenderableList;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRenderableListModel;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ReadonlyLoadOptionsModel;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.ReadonlyObjectModel;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.AbstractImage;

// TODO NEWUI replace this class with one that is JavaFX tailored or general-purpose (not Swing)
public class ImageBasedRendererList<ContextType extends Context<ContextType>>
    extends AbstractList<IBRRenderable<ContextType>>
    implements IBRRenderableListModel<ContextType>
{
    private final ContextType context;

    private final InteractiveRenderableList<ContextType, IBRRenderable<ContextType>> renderableList;
    private int effectiveSize;
    private LoadingMonitor loadingMonitor;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;
    private ReadonlySettingsModel settingsModel;

    public ImageBasedRendererList(ContextType context)
    {
        this.context = context;
        this.renderableList = new InteractiveRenderableList<>();
        this.effectiveSize = 0;
    }

    private void handleMissingFiles(Exception e)
    {
        e.printStackTrace();
        if (loadingMonitor != null)
        {
            loadingMonitor.loadingFailed(e);
        }
    }

    @Override
    public void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions)
    {
        this.loadingMonitor.startLoading();

        try
        {
            IBRRenderable<ContextType> newItem =
                new IBRImplementation<>(id, context, null,
                    IBRResources.getBuilderForContext(this.context)
                        .setLoadingMonitor(this.loadingMonitor)
                        .setLoadOptions(loadOptions)
                        .loadVSETFile(vsetFile));

            newItem.setObjectModel(this.objectModel);
            newItem.setCameraModel(this.cameraModel);
            newItem.setLightingModel(this.lightingModel);
            newItem.setSettingsModel(this.settingsModel);

            newItem.setLoadingMonitor(new LoadingMonitor()
            {
                @Override
                public void startLoading()
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.startLoading();
                    }
                }

                @Override
                public void setMaximum(double maximum)
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.setMaximum(maximum);
                    }
                }

                @Override
                public void setProgress(double progress)
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.setProgress(progress);
                    }
                }

                @Override
                public void loadingComplete()
                {
                    renderableList.setSelectedItem(newItem);
                    effectiveSize = renderableList.size();
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.loadingComplete();
                    }
                }

                @Override
                public void loadingFailed(Exception e)
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.loadingFailed(e);
                    }
                }
            });
            renderableList.add(newItem);
        }
        catch (FileNotFoundException e)
        {
            handleMissingFiles(e);
        }
    }

    @Override
    public void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, String primaryViewName,
        ReadonlyLoadOptionsModel loadOptions)
    {
        this.loadingMonitor.startLoading();

        try
        {
            IBRRenderable<ContextType> newItem =
                new IBRImplementation<>(id, context, null,
                    IBRResources.getBuilderForContext(this.context)
                        .setLoadingMonitor(this.loadingMonitor)
                        .setLoadOptions(loadOptions)
                        .loadAgisoftFiles(xmlFile, meshFile, undistortedImageDirectory)
                        .setPrimaryView(primaryViewName));

            newItem.setObjectModel(this.objectModel);
            newItem.setCameraModel(this.cameraModel);
            newItem.setLightingModel(this.lightingModel);
            newItem.setSettingsModel(this.settingsModel);

            newItem.setLoadingMonitor(new LoadingMonitor()
            {
                @Override
                public void startLoading()
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.startLoading();
                    }
                }

                @Override
                public void setMaximum(double maximum)
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.setMaximum(maximum);
                    }
                }

                @Override
                public void setProgress(double progress)
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.setProgress(progress);
                    }
                }

                @Override
                public void loadingComplete()
                {
                    renderableList.setSelectedItem(newItem);
                    effectiveSize = renderableList.size();
                    double primaryViewDistance = newItem.getResources().getPrimaryViewDistance();

                    Vector3 lightIntensity = new Vector3((float)(primaryViewDistance * primaryViewDistance));

                    for (int i = 0; i < newItem.getActiveViewSet().getLightCount(); i++)
                    {
                        newItem.getActiveViewSet().setLightIntensity(i, lightIntensity);
                    }

                    newItem.getActiveViewSet().setInfiniteLightSources(false);

                    newItem.getResources().updateLightData();
                    newItem.reloadShaders();

                    if (loadingMonitor != null)
                    {
                        loadingMonitor.loadingComplete();
                    }
                }

                @Override
                public void loadingFailed(Exception e)
                {
                    if (loadingMonitor != null)
                    {
                        loadingMonitor.loadingFailed(e);
                    }
                }
            });
            renderableList.add(newItem);
        }
        catch(FileNotFoundException|XMLStreamException e)
        {
            handleMissingFiles(e);
        }
    }

    public IBRRenderable<ContextType> getElementAt(int index)
    {
        return renderableList.get(index);
    }

    @Override
    public IBRRenderable<ContextType> getSelectedItem()
    {
        return renderableList.getSelectedItem();
    }

    @Override
    public void setSelectedItem(Object item)
    {
        if (item == null)
        {
            renderableList.setSelectedItem(null);
        }
        else
        {
            for (int i = 0; i < renderableList.size(); i++)
            {
                if (Objects.equals(renderableList.get(i), item))
                {
                    renderableList.setSelectedIndex(i);
                }
            }
        }
    }

    @Override
    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
    }

    @Override
    public DoubleUnaryOperator getLuminanceEncodingFunction()
    {
        return this.getSelectedItem().getActiveViewSet().getLuminanceEncoding().encodeFunction;
    }

    @Override
    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.getSelectedItem().setTonemapping(linearLuminanceValues, encodedLuminanceValues);
    }

    @Override
    public void applyLightCalibration()
    {
        this.getSelectedItem().applyLightCalibration();
    }

    public InteractiveRenderable<ContextType> getRenderable()
    {
        return renderableList;
    }

    @Override
    public void setObjectModel(ReadonlyObjectModel objectModel)
    {
        this.objectModel = objectModel;
        for (IBRRenderable<?> renderable : renderableList)
        {
            renderable.setObjectModel(objectModel);
        }
    }

    @Override
    public void setCameraModel(ReadonlyCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
        for (IBRRenderable<?> renderable : renderableList)
        {
            renderable.setCameraModel(cameraModel);
        }
    }

    @Override
    public void setLightingModel(ReadonlyLightingModel lightingModel)
    {
        this.lightingModel = lightingModel;
        for (IBRRenderable<?> renderable : renderableList)
        {
            renderable.setLightingModel(lightingModel);
        }
    }

    @Override
    public void setSettingsModel(ReadonlySettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
        for (IBRRenderable<?> renderable : renderableList)
        {
            renderable.setSettingsModel(settingsModel);
        }
    }

    @Override
    public Optional<AbstractImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        return this.getSelectedItem().loadEnvironmentMap(environmentMapFile);
    }

    @Override
    public void loadBackplate(File backplateFile) throws FileNotFoundException
    {
        this.getSelectedItem().loadBackplate(backplateFile);
    }

    @Override
    public void saveToVSETFile(File vsetFile) throws IOException
    {
        try (OutputStream stream = new FileOutputStream(vsetFile))
        {
            this.getSelectedItem().getActiveViewSet().writeVSETFileToStream(stream, vsetFile.getParentFile().toPath());
        }
    }

    @Override
    public void unload()
    {
        if (this.getSelectedItem() != null)
        {
            this.renderableList.remove(this.renderableList.getSelectedIndex());
            this.renderableList.setSelectedItem(null);
        }
    }

    @Override
    public int size()
    {
        return this.effectiveSize;
    }

    @Override
    public IBRRenderable<ContextType> get(int index)
    {
        return renderableList.get(index);
    }

    @Override
    public int getSelectedIndex()
    {
        return renderableList.getSelectedIndex();
    }

    @Override
    public void setSelectedIndex(int index)
    {
        renderableList.setSelectedIndex(index);
    }
}
