package tetzlaff.ibrelight.rendering.components;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.LightingResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.models.BackgroundMode;

import java.io.File;

public class Environment<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;

    private Program<ContextType> environmentBackgroundProgram;
    private Drawable<ContextType> environmentBackgroundDrawable;
    private VertexBuffer<ContextType> rectangleVertices;

    public Environment(ContextType context, LightingResources<ContextType> lightingResources,
                       SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = context;
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.sceneViewportModel.addSceneObjectType("EnvironmentMap");
    }

    @Override
    public void initialize() throws Exception
    {
        this.rectangleVertices = context.createRectangle();

        this.environmentBackgroundProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                .createProgram();

        this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
        this.environmentBackgroundDrawable.addVertexBuffer("position", this.rectangleVertices);
    }

    @Override
    public void reloadShaders() throws Exception
    {
        Program<ContextType> newEnvironmentBackgroundProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "envbackgroundtexture.frag"))
                .createProgram();

        if (this.environmentBackgroundProgram != null)
        {
            this.environmentBackgroundProgram.close();
        }

        this.environmentBackgroundProgram = newEnvironmentBackgroundProgram;
        this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
        this.environmentBackgroundDrawable.addVertexBuffer("position", rectangleVertices);
    }

    @Override
    public void draw(Framebuffer<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (lightingResources.getEnvironmentMap() != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.ENVIRONMENT_MAP)
        {
            Matrix4 envMapMatrix = sceneModel.getLightingModel().getEnvironmentMapMatrix();

            environmentBackgroundProgram.setUniform("objectID", sceneViewportModel.lookupSceneObjectID("EnvironmentMap"));
            environmentBackgroundProgram.setUniform("useEnvironmentTexture", true);
            environmentBackgroundProgram.setTexture("env", lightingResources.getEnvironmentMap());
            environmentBackgroundProgram.setUniform("model_view", cameraViewport.getView());
            environmentBackgroundProgram.setUniform("projection", cameraViewport.getViewportProjection());
            environmentBackgroundProgram.setUniform("envMapMatrix", envMapMatrix);
            environmentBackgroundProgram.setUniform("envMapIntensity", sceneModel.getClearColor());

            environmentBackgroundProgram.setUniform("gamma",
                    lightingResources.getEnvironmentMap().isInternalFormatCompressed() ||
                            lightingResources.getEnvironmentMap().getInternalUncompressedColorFormat().dataType != ColorFormat.DataType.FLOATING_POINT
                            ? 1.0f : 2.2f);

            context.getState().disableDepthTest();
            environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
            context.getState().enableDepthTest();
        }
    }

    @Override
    public void close() throws Exception
    {
        if (this.environmentBackgroundProgram != null)
        {
            this.environmentBackgroundProgram.close();
            this.environmentBackgroundProgram = null;
        }

        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }
    }
}
