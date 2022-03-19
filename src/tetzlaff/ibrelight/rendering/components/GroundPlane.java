package tetzlaff.ibrelight.rendering.components;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.rendering.LightingResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.StandardShader;

import java.io.FileNotFoundException;

public class GroundPlane<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;
    private StandardShader<ContextType> groundPlaneStandardShader;

    private VertexBuffer<ContextType> rectangleVertices;
    Drawable<ContextType> groundPlaneDrawable;

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
    public void reloadShaders() throws FileNotFoundException
    {
        groundPlaneStandardShader.reload(StandardRenderingMode.LAMBERTIAN_SHADED);
        groundPlaneDrawable = context.createDrawable(groundPlaneStandardShader.getProgram());
        groundPlaneDrawable.addVertexBuffer("position", rectangleVertices);
        groundPlaneDrawable.setVertexAttrib("normal", new Vector3(0, 0, 1));
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 fullProjection,
                     Matrix4 viewportProjection, int x, int y, int width, int height)
    {
        if (sceneModel.getLightingModel().isGroundPlaneEnabled())
        {
            groundPlaneStandardShader.setup(
                sceneModel.getUnscaledMatrix(Matrix4.translate(new Vector3(0, sceneModel.getLightingModel().getGroundPlaneHeight(), 0)))
                    .times(Matrix4.rotateX(Math.PI / 2))
                    .times(Matrix4.scale(sceneModel.getScale() * sceneModel.getLightingModel().getGroundPlaneSize())));

            groundPlaneDrawable.program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("SceneObject"));
            groundPlaneDrawable.program().setUniform("defaultDiffuseColor", sceneModel.getLightingModel().getGroundPlaneColor());
            groundPlaneDrawable.program().setUniform("projection", viewportProjection);
            groundPlaneDrawable.program().setUniform("fullProjection", fullProjection);

            Matrix4 model = sceneModel.getUnscaledMatrix(Matrix4.translate(new Vector3(0, sceneModel.getLightingModel().getGroundPlaneHeight(), 0)))
                    .times(Matrix4.rotateX(Math.PI / 2))
                    .times(Matrix4.scale(sceneModel.getScale() * sceneModel.getLightingModel().getGroundPlaneSize()));

            // Set up camera for ground plane program.
            groundPlaneDrawable.program().setUniform("model_view", view.times(model));
            groundPlaneDrawable.program().setUniform("viewPos", view.quickInverse(0.01f).getColumn(3).getXYZ());

            // Disable back face culling since the plane is one-sided.
            context.getState().disableBackFaceCulling();

            // Do first pass at half resolution to off-screen buffer
            groundPlaneDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer, x, y, width, height);

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
