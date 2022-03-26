package tetzlaff.ibrelight.rendering.components.lightcalibration;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.UniformBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.components.IBRSubject;
import tetzlaff.ibrelight.rendering.components.snap.ViewSnapContent;

import java.io.FileNotFoundException;

public class LightCalibrationContent <ContextType extends Context<ContextType>> extends ViewSnapContent<ContextType>
{
    private final ContextType context;
    private final IBRResources<ContextType> resources;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private IBRSubject<ContextType> ibrSubject;

    public LightCalibrationContent(IBRResources<ContextType> resources, SceneModel sceneModel,
                                   SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.context;
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        // the actual subject for image-based rendering
        // No lighting resources since light calibration is effectively unlit shading
        ibrSubject = new IBRSubject<>(resources, null, sceneModel, sceneViewportModel);
        ibrSubject.initialize();
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        ibrSubject.reloadShaders();
    }

    @Override
    public void update() throws FileNotFoundException
    {
        ibrSubject.update();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        drawInSubdivisions(framebuffer, cameraViewport.getWidth(), cameraViewport.getHeight(), cameraViewport);
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight, CameraViewport cameraViewport)
    {
        int primaryLightIndex = this.resources.viewSet.getLightIndex(this.resources.viewSet.getPrimaryViewIndex());

        Vector3 lightPosition = sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3()
                .plus(resources.viewSet.getLightPosition(primaryLightIndex));
        Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());

        // Only draw the IBR subject for light calibration, no other components like backplate, grid, ground plane, etc.

        // Hole fill color depends on whether in light calibration mode or not.
        ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.5f));

        try(UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer())
        {
            viewIndexBuffer.setData(NativeVectorBufferFactory.getInstance()
                .createFromIntArray(false, 1, 1, getSnapViewIndex()));
            ibrSubject.getProgram().setUniformBuffer("ViewIndices", viewIndexBuffer);

            // Draw the actual object, without model transformation for light calibration
            ibrSubject.drawInSubdivisions(framebuffer, subdivWidth, subdivHeight,
                lightTransform.times(cameraViewport.getView()), cameraViewport.getViewportProjection());
        }

        context.flush();

        // Read buffers after rendering just the IBR subject
        sceneViewportModel.refreshBuffers(cameraViewport.getFullProjection(), framebuffer);
    }

    @Override
    public void close() throws Exception
    {
        if (ibrSubject != null)
        {
            ibrSubject.close();
            ibrSubject = null;
        }
    }
}
