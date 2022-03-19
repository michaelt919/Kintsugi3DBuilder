package tetzlaff.ibrelight.rendering.components;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.rendering.LightingResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.components.lit.LitContent;
import tetzlaff.ibrelight.rendering.components.scene.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class StandardScene<ContextType extends Context<ContextType>> extends LitContent<ContextType>
{
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;
    private final IBRResources<ContextType> resources;

    private IBRSubject<ContextType> ibrSubject;
    private LightVisuals<ContextType> lightVisuals;
    private final List<RenderedComponent<ContextType>> otherComponents = new ArrayList<>();

    public StandardScene(IBRResources<ContextType> resources, SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.context;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.resources = resources;
    }

    @Override
    public void initialize() throws Exception
    {
        LightingResources<ContextType> lightingResources = getLightingResources();

        // the actual subject for image-based rendering
        ibrSubject = new IBRSubject<>(resources, lightingResources, sceneModel, sceneViewportModel);
        ibrSubject.initialize();

        // graphics resources for depicting the on-screen representation of lights
        lightVisuals = new LightVisuals<>(context, sceneModel, sceneViewportModel);
        lightVisuals.initialize();

        // Backplate and environment must be first since they aren't depth tested.
        otherComponents.add(new Backplate<>(context, lightingResources, sceneModel));
        otherComponents.add(new Environment<>(context, lightingResources, sceneModel, sceneViewportModel));

        // Foreground components that will be depth tested
        otherComponents.add(new Grid<>(context, sceneModel));
        otherComponents.add(new GroundPlane<>(resources, lightingResources, sceneModel, sceneViewportModel));

        // Run initialization for each additional component
        for (RenderedComponent<ContextType> component : otherComponents)
        {
            component.initialize();
        }
    }

    @Override
    public void reloadShaders() throws Exception
    {
        ibrSubject.reloadShaders();
        lightVisuals.reloadShaders();

        for (RenderedComponent<ContextType> otherComponent : otherComponents)
        {
            otherComponent.reloadShaders();
        }
    }

    @Override
    public void update() throws Exception
    {
        ibrSubject.update();
        lightVisuals.update();

        for (RenderedComponent<ContextType> component : otherComponents)
        {
            component.update();
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        drawInSubdivisions(framebuffer, cameraViewport.getWidth(), cameraViewport.getHeight(), cameraViewport);
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight, CameraViewport cameraViewport)
    {
        // Draw "other components" first, which includes things that ignore the depth test first
        // (environment or backplate)
        otherComponents.forEach(component -> component.draw(framebuffer, cameraViewport));

        // Hole fill color depends on whether in light calibration mode or not.
        ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.0f));

        // Draw the actual object with the model transformation
        ibrSubject.drawInSubdivisions(framebuffer, subdivWidth, subdivHeight, cameraViewport);

        if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
        {
            context.flush();

            // Read buffers here if light widgets are ethereal (i.e. they cannot be clicked and should not be in the ID buffer)
            sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
        }

        lightVisuals.draw(framebuffer, cameraViewport);

        // Finish drawing
        context.flush();

        if (!sceneModel.getLightingModel().areLightWidgetsEthereal()
                && IntStream.range(0, sceneModel.getLightingModel().getLightCount()).anyMatch(sceneModel.getLightingModel()::isLightWidgetEnabled))
        {
            // Read buffers here if light widgets are not ethereal (i.e. they can be clicked and should be in the ID buffer)
            sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
        }
    }

    @Override
    public void close()
    {
        if (ibrSubject != null)
        {
            ibrSubject.close();
        }

        if (lightVisuals != null)
        {
            lightVisuals.close();
        }

        for (RenderedComponent<ContextType> otherComponent : otherComponents)
        {
            try
            {
                otherComponent.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
