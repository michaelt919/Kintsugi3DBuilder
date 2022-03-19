package tetzlaff.ibrelight.rendering.components;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.rendering.LightingResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.StandardShader;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GroundPlane<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;
    private StandardShader<ContextType> groundPlaneStandardShader;

    private VertexBuffer<ContextType> rectangleVertices;
    private Drawable<ContextType> groundPlaneDrawable;

    public GroundPlane(IBRResources<ContextType> resources, LightingResources<ContextType> lightingResources,
                       SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.context;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.groundPlaneStandardShader = new StandardShader<>(resources, lightingResources, sceneModel);
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        this.rectangleVertices = context.createRectangle();

        groundPlaneStandardShader.initialize(StandardRenderingMode.LAMBERTIAN_SHADED);
        groundPlaneDrawable = context.createDrawable(groundPlaneStandardShader.getProgram());
        groundPlaneDrawable.addVertexBuffer("position", rectangleVertices);
        groundPlaneDrawable.setVertexAttrib("normal", new Vector3(0, 0, 1));
    }

    @Override
    public void update() throws FileNotFoundException
    {
        Map<String, Optional<Object>> defineMap = groundPlaneStandardShader.getPreprocessorDefines();

        // Reloads shaders only if compiled settings have changed.
        if (defineMap.entrySet().stream().anyMatch(
                defineEntry -> !Objects.equals(groundPlaneDrawable.program().getDefine(defineEntry.getKey()), defineEntry.getValue())))
        {
            System.out.println("Updating compiled render settings.");
            reloadShaders();
        }
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        groundPlaneStandardShader.reload(StandardRenderingMode.LAMBERTIAN_SHADED);
        groundPlaneDrawable = context.createDrawable(groundPlaneStandardShader.getProgram());
        groundPlaneDrawable.addVertexBuffer("position", rectangleVertices);
        groundPlaneDrawable.setVertexAttrib("normal", new Vector3(0, 0, 1));
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (sceneModel.getLightingModel().isGroundPlaneEnabled())
        {
            Matrix4 model = sceneModel.getUnscaledMatrix(Matrix4.translate(new Vector3(0, sceneModel.getLightingModel().getGroundPlaneHeight(), 0)))
                .times(Matrix4.rotateX(Math.PI / 2))
                .times(Matrix4.scale(sceneModel.getScale() * sceneModel.getLightingModel().getGroundPlaneSize()));

            groundPlaneStandardShader.setup(model);

            groundPlaneDrawable.program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("SceneObject"));
            groundPlaneDrawable.program().setUniform("defaultDiffuseColor",
                sceneModel.getLightingModel().getGroundPlaneColor().applyOperator(x -> Math.pow(x, 2.2)));
            groundPlaneDrawable.program().setUniform("projection", cameraViewport.getViewportProjection());
            groundPlaneDrawable.program().setUniform("fullProjection", cameraViewport.getFullProjection());

            // Set up camera for ground plane program.
            Matrix4 modelView = cameraViewport.getView().times(model);
            groundPlaneDrawable.program().setUniform("model_view", modelView);
            groundPlaneDrawable.program().setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

            // Disable back face culling since the plane is one-sided.
            context.getState().disableBackFaceCulling();

            // Do first pass at half resolution to off-screen buffer
            groundPlaneDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());

            // Re-enable back face culling
            context.getState().enableBackFaceCulling();
        }
    }

    @Override
    public void close()
    {
        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }

        if (groundPlaneStandardShader != null)
        {
            groundPlaneStandardShader.close();
            groundPlaneStandardShader = null;
        }
    }
}
