package tetzlaff.ibrelight.rendering.components.lightcalibration;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.components.snap.ViewSnap;

public class LightCalibrationRoot<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final IBRResources<ContextType> resources;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private ViewSnap<ContextType> viewSnapRoot;

    public LightCalibrationRoot(IBRResources<ContextType> resources, SceneModel sceneModel,
                                SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;

        this.viewSnapRoot = new ViewSnap<>(sceneModel, resources.viewSet);
    }

    @Override
    public void initialize() throws Exception
    {
        viewSnapRoot.takeContentRoot(new LightCalibrationContent<>(resources, sceneModel, sceneViewportModel));
        viewSnapRoot.initialize();
    }

    @Override
    public void update() throws Exception
    {
        viewSnapRoot.update();
    }

    @Override
    public void reloadShaders() throws Exception
    {
        viewSnapRoot.reloadShaders();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        viewSnapRoot.draw(framebuffer, cameraViewport);
    }

    @Override
    public void close() throws Exception
    {
        if (viewSnapRoot != null)
        {
            viewSnapRoot.close();
            viewSnapRoot = null;
        }
    }
}
