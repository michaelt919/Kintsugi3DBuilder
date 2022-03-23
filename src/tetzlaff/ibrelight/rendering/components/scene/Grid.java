package tetzlaff.ibrelight.rendering.components.scene;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;

import java.io.File;
import java.io.FileNotFoundException;

public class Grid<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final SceneModel sceneModel;

    private Program<ContextType> solidProgram;
    private VertexBuffer<ContextType> gridVertices;
    private Drawable<ContextType> gridDrawable;

    public Grid(ContextType context, SceneModel sceneModel)
    {
        this.context = context;
        this.sceneModel = sceneModel;
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        this.solidProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
                .createProgram();

        float[] grid = new float[252];
        for (int i = 0; i < 21; i++)
        {
            grid[i * 12] = i * 0.1f - 1.0f;
            grid[i * 12 + 1] = 0;
            grid[i * 12 + 2] = 1;

            grid[i * 12 + 3] = i * 0.1f - 1.0f;
            grid[i * 12 + 4] = 0;
            grid[i * 12 + 5] = -1;

            grid[i * 12 + 6] = 1;
            grid[i * 12 + 7] = 0;
            grid[i * 12 + 8] = i * 0.1f - 1.0f;

            grid[i * 12 + 9] = -1;
            grid[i * 12 + 10] = 0;
            grid[i * 12 + 11] = i * 0.1f - 1.0f;
        }

        this.gridVertices = context.createVertexBuffer()
                .setData(NativeVectorBufferFactory.getInstance()
                        .createFromFloatArray(3, 84, grid));

        this.gridDrawable = context.createDrawable(this.solidProgram);
        this.gridDrawable.addVertexBuffer("position", gridVertices);
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {

        Program<ContextType> newSolidProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
                .createProgram();

        if (this.solidProgram != null)
        {
            this.solidProgram.close();
        }

        this.solidProgram = newSolidProgram;

        this.gridDrawable = context.createDrawable(this.solidProgram);
        this.gridDrawable.addVertexBuffer("position", gridVertices);
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        // Draw grid
        if (sceneModel.getSettingsModel().getBoolean("is3DGridEnabled"))
        {
            this.solidProgram.setUniform("projection", cameraViewport.getViewportProjection());
            this.solidProgram.setUniform("model_view", cameraViewport.getView().times(Matrix4.scale(sceneModel.getScale())));
            this.solidProgram.setUniform("color", new Vector4(0.5f, 0.5f, 0.5f, 1.0f));
            this.solidProgram.setUniform("objectID", 0);
            this.gridDrawable.draw(PrimitiveMode.LINES, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());
        }
    }

    @Override
    public void close()
    {
        if (solidProgram != null)
        {
            solidProgram.close();
            solidProgram = null;
        }

        if (gridVertices != null)
        {
            gridVertices.close();
            gridVertices = null;
        }
    }
}
