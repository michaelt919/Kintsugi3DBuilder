package tetzlaff.ibrelight.rendering.components.scene;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.export.general.GeneralRenderRequestUI;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.resources.IBRResources;

import java.io.File;
import java.io.FileNotFoundException;

public class FocalGrid<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final SceneModel sceneModel;

    private Program<ContextType> solidProgram;
    private Drawable<ContextType> drawable;

    private IBRResources<ContextType> resources;

    public FocalGrid(IBRResources<ContextType> resources, SceneModel sceneModel,
                                   SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.context = resources.context;
        this.resources = resources;
        this.sceneModel = sceneModel;
    }
    public Program<ContextType> getProgram()
    {
        return solidProgram;
    }


    @Override
    public void initialize() throws FileNotFoundException
    {
        this.solidProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "specularfit"), "coc.frag"))
                .createProgram();

        refreshDrawable();

    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {

        Program<ContextType> newSolidProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "specularfit"), "coc.frag"))
                .createProgram();

        if (this.solidProgram != null)
        {
            this.solidProgram.close();
        }

        this.solidProgram = newSolidProgram;

       refreshDrawable();
    }
    private void refreshDrawable()
    {
        this.drawable = context.createDrawable(solidProgram);
        this.drawable.addVertexBuffer("position", this.resources.positionBuffer);

        if (this.resources.normalBuffer != null)
        {
            this.drawable.addVertexBuffer("normal", this.resources.normalBuffer);
        }

        if (this.resources.texCoordBuffer != null)
        {
            this.drawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
        }

        if (this.resources.tangentBuffer != null)
        {
            this.drawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
        }
    }
    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        // Draw grid
        Matrix4 modelView = sceneModel.getModelViewMatrix(cameraViewport.getView());

            this.solidProgram.setUniform("projection", cameraViewport.getViewportProjection());
            this.solidProgram.setUniform("model_view", modelView);
            this.solidProgram.setUniform("color", new Vector4(0.5f, 0.5f, 0.5f, 1.0f));
            this.solidProgram.setUniform("objectID", 0);
            solidProgram.setUniform("distance",sceneModel.getSettingsModel().getFloat("distance"));
            solidProgram.setUniform("aperture",sceneModel.getSettingsModel().getFloat("aperture"));
            solidProgram.setUniform("focal",sceneModel.getSettingsModel().getFloat("focal"));

        this.drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());

    }

    @Override
    public void close()
    {
        if (solidProgram != null)
        {
            solidProgram.close();
            solidProgram = null;
        }

    }
}
