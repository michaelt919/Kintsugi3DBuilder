package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;
import javax.swing.*;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.interactive.InteractiveRenderableList;
import tetzlaff.ibr.core.*;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.ReadonlyObjectModel;

// TODO NEWUI replace this class with one that is JavaFX tailored or general-purpose (not Swing)
public class ImageBasedRendererList<ContextType extends Context<ContextType>> 
    extends AbstractListModel<IBRRenderable<ContextType>> implements IBRRenderableListModel<ContextType>
{
    private static final long serialVersionUID = 4167467314632694946L;

    protected final ContextType context;

    private Program<ContextType> program;
    private final InteractiveRenderableList<ContextType, IBRRenderable<ContextType>> renderableList;
    private int effectiveSize;
    private LoadingMonitor loadingMonitor;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;
    private ReadonlySettingsModel settingsModel;

    public ImageBasedRendererList(ContextType context, Program<ContextType> program)
    {
        this.context = context;
        this.renderableList = new InteractiveRenderableList<ContextType, IBRRenderable<ContextType>>();
        this.effectiveSize = 0;

        this.program = program;
    }

    public Program<ContextType> getProgram()
    {
        return this.program;
    }

    public void setProgram(Program<ContextType> program)
    {
        this.program = program;
        for (IBRRenderable<ContextType> ulf : renderableList)
        {
            ulf.setProgram(program);
        }
    }

    @Override
    public void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions)
        throws FileNotFoundException
    {
        // id = vsetFile.getPath()

        this.loadingMonitor.startLoading();

        IBRRenderable<ContextType> newItem =
            new IBRImplementation<ContextType>(id, context, this.getProgram(),
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
                fireIntervalAdded(this, renderableList.size() - 1, renderableList.size() - 1);
            }
        });
        renderableList.add(newItem);
    }

    @Override
    public void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, ReadonlyLoadOptionsModel loadOptions)
        throws FileNotFoundException
    {
        this.loadingMonitor.startLoading();

        IBRRenderable<ContextType> newItem =
            new IBRImplementation<>(id, context, this.getProgram(),
                IBRResources.getBuilderForContext(this.context)
                    .setLoadingMonitor(this.loadingMonitor)
                    .setLoadOptions(loadOptions)
                    .loadAgisoftFiles(xmlFile, meshFile, undistortedImageDirectory));

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
                fireIntervalAdded(this, renderableList.size() - 1, renderableList.size() - 1);
            }
        });
        renderableList.add(newItem);
    }

    @Override
    public int getSize()
    {
        return this.effectiveSize;
    }

    @Override
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
        this.fireContentsChanged(this, -1, -1);
    }

    @Override
    public void setLoadingMonitor(LoadingMonitor loadingMonitor)
    {
        this.loadingMonitor = loadingMonitor;
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
    public void loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException
    {
        this.getSelectedItem().loadEnvironmentMap(environmentMapFile);
    }
}
