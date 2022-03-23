package tetzlaff.ibrelight.rendering.components.scene;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.rendering.resources.LightingResources;
import tetzlaff.models.BackgroundMode;

import java.io.File;

public class Backplate<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;

    private Program<ContextType> tintedTexProgram;
    private Drawable<ContextType> tintedTexDrawable;
    private VertexBuffer<ContextType> rectangleVertices;

    public Backplate(ContextType context, LightingResources<ContextType> lightingResources, SceneModel sceneModel)
    {
        this.context = context;
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
    }

    @Override
    public void initialize() throws Exception
    {
        this.rectangleVertices = context.createRectangle();

        this.tintedTexProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "texture_tint.frag"))
                .createProgram();

        this.tintedTexDrawable = context.createDrawable(tintedTexProgram);
        this.tintedTexDrawable.addVertexBuffer("position", this.rectangleVertices);
    }

    @Override
    public void reloadShaders() throws Exception
    {

    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (lightingResources.getBackplateTexture() != null && sceneModel.getLightingModel().getBackgroundMode() == BackgroundMode.IMAGE)
        {
            tintedTexDrawable.program().setTexture("tex", lightingResources.getBackplateTexture());
            tintedTexDrawable.program().setUniform("color", sceneModel.getClearColor());

            context.getState().disableDepthTest();
            tintedTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
            context.getState().enableDepthTest();

            // Clear ID buffer again.
            framebuffer.clearIntegerColorBuffer(1, 0, 0, 0, 0);
        }
    }

    @Override
    public void close() throws Exception
    {
        if (tintedTexProgram != null)
        {
            tintedTexProgram.close();
            tintedTexProgram = null;
        }

        if (rectangleVertices != null)
        {
            rectangleVertices.close();
            rectangleVertices = null;
        }
    }
}
