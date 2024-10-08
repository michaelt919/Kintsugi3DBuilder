package kintsugi3d.builder.rendering.components.scene.camera;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.BaseScene;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.rendering.components.lightcalibration.CameraFrustum;
import kintsugi3d.builder.rendering.components.lightcalibration.CameraVisual;
import kintsugi3d.builder.rendering.components.snap.ViewSelection;
import kintsugi3d.builder.rendering.components.snap.ViewSelectionImpl;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector4;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CameraWidgetGroup<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private int cameraIndex;

    private ViewSelection selection;

    private IBRResourcesImageSpace<ContextType> resources;
    private SceneModel sceneModel;
    private SceneViewportModel sceneViewportModel;

    private CameraVisual<ContextType> cameraVisual;
    private CameraFrustum<ContextType> cameraFrustum;

    public CameraWidgetGroup(IBRResourcesImageSpace<ContextType> resources,
                             SceneModel sceneModel, SceneViewportModel sceneViewportModel)
    {
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;

        selection = new ViewSelectionImpl(resources.getViewSet(), sceneModel){
            @Override
            public int getSelectedViewIndex()
            {
                return cameraIndex;
            }
        };

        cameraVisual = new CameraVisual<>(resources, sceneViewportModel);
        cameraVisual.setViewSelection(selection);
        cameraFrustum = new CameraFrustum<>(resources, sceneViewportModel);
        cameraFrustum.setViewSelection(selection);
    }

    @Override
    public void initialize()
    {
        cameraVisual.initialize();
        cameraFrustum.initialize();
    }

    @Override
    public void reloadShaders()
    {
        cameraVisual.reloadShaders();
        cameraFrustum.reloadShaders();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (true) //TODO
        {
            for (cameraIndex = 0; cameraIndex < resources.getViewSet().getCameraPoseCount(); cameraIndex++)
            {
                cameraVisual.draw(framebuffer, cameraViewport);
                cameraFrustum.draw(framebuffer, cameraViewport);
            }
        }
    }

    @Override
    public void close()
    {
        cameraVisual.close();
        cameraFrustum.close();
    }
}
