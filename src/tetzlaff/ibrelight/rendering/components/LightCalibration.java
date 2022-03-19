package tetzlaff.ibrelight.rendering.components;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.UniformBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.rendering.LightingResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;

import java.io.FileNotFoundException;

public class LightCalibration<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final IBRResources<ContextType> resources;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private IBRSubject<ContextType> ibrSubject;

    public LightCalibration(IBRResources<ContextType> resources, SceneModel sceneModel,
                            SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.context;
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    public void initialize() throws FileNotFoundException
    {
        // the actual subject for image-based rendering
        // No lighting resources since light calibration is effectively unlit shading
        ibrSubject = new IBRSubject<>(resources, null, sceneModel, sceneViewportModel);
        ibrSubject.initialize();
    }

    public void update() throws FileNotFoundException
    {
        ibrSubject.update();
    }

    public void reloadShaders() throws FileNotFoundException
    {
        ibrSubject.reloadShaders();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        drawInSubdivisions(framebuffer, cameraViewport.getWidth(), cameraViewport.getHeight(), cameraViewport);
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight, Matrix4 view, Matrix4 projection)
    {
        int primaryLightIndex = this.resources.viewSet.getLightIndex(this.resources.viewSet.getPrimaryViewIndex());

        Vector3 lightPosition = sceneModel.getSettingsModel().get("currentLightCalibration", Vector2.class).asVector3()
                .plus(resources.viewSet.getLightPosition(primaryLightIndex));
        Matrix4 lightTransform = Matrix4.translate(lightPosition.negated());

        Matrix4 viewInverse = view.quickInverse(0.01f);
        float maxSimilarity = Float.NEGATIVE_INFINITY;
        int snapViewIndex = -1;

        // View will be overridden for light calibration so that it snaps to specific views
        Matrix4 viewSnap = null;

        for(int i = 0; i < this.resources.viewSet.getCameraPoseCount(); i++)
        {
            Matrix4 candidatePose = this.resources.viewSet.getCameraPose(i);
            Matrix4 candidateView = candidatePose.times(sceneModel.getFullModelMatrix().quickInverse(0.01f));
            float similarity = viewInverse.times(Vector4.ORIGIN).getXYZ()
                    .dot(candidateView.quickInverse(0.01f).times(Vector4.ORIGIN).getXYZ());

            if (similarity > maxSimilarity)
            {
                maxSimilarity = similarity;
                viewSnap = candidateView;
                snapViewIndex = i;
            }
        }

        assert viewSnap != null; // Should be non-null if there are any camera poses since initially maxSimilarity is -infinity

        // Only draw the IBR subject for light calibration, no other components like backplate, grid, ground plane, etc.

        // Hole fill color depends on whether in light calibration mode or not.
        ibrSubject.getProgram().setUniform("holeFillColor", new Vector3(0.5f));

        try(UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer())
        {
            viewIndexBuffer.setData(NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, 1, snapViewIndex));
            ibrSubject.getProgram().setUniformBuffer("ViewIndices", viewIndexBuffer);

            // Draw the actual object, without model transformation for light calibration
            ibrSubject.drawInSubdivisions(framebuffer, subdivWidth, subdivHeight, lightTransform.times(viewSnap), projection);
        }

        context.flush();

        // Read buffers after rendering just the IBR subject
        sceneViewportModel.refreshBuffers(projection, framebuffer);
    }

    @Override
    public void close()
    {
        if (ibrSubject != null)
        {
            ibrSubject.close();
        }
    }
}
