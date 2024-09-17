package kintsugi3d.builder.rendering.components.scene.camera;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.components.ShaderComponent;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector4;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CameraWidgetGroup<ContextType extends Context<ContextType>> extends ShaderComponent<ContextType>
{
    private final IBRResourcesImageSpace<ContextType> resources;
    private final SceneViewportModel sceneViewportModel;
    private final SceneModel sceneModel;

    public CameraWidgetGroup(ContextType context, IBRResourcesImageSpace<ContextType> resources,
                             SceneModel sceneModel, SceneViewportModel sceneViewportModel)
    {
        super(context);
        this.resources = resources;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        if (true) //TODO
        {
            this.getDrawable().program().setUniform("projection", cameraViewport.getViewportProjection());
            this.getDrawable().program().setUniform("color", new Vector4(0.0f, 1.0f, 0.0f, 1.0f));
            this.getDrawable().program().setUniform("objectID", 0);


            for (int i = 0; i < resources.getViewSet().getCameraPoseCount(); i++)
            {
                Matrix4 model = sceneModel.getUnscaledMatrix(resources.getViewSet().getCameraPoseInverse(i))
                    .times(Matrix4.scale(sceneModel.getScale()));

                this.getDrawable().program().setUniform("model_view", cameraViewport.getView().times(model));
                this.getDrawable().draw(PrimitiveMode.LINES, cameraViewport.ofFramebuffer(framebuffer));
            }
        }
    }

    @Override
    protected ProgramObject<ContextType> createProgram(ContextType context) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "solid.frag"))
            .createProgram();
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        float[] arrow = {
            0, 0, 0,
            0, 0, -1,
            0, 0, -1,
            0, 0.1f, -0.9f,
        };

        return Map.of("position", context.createVertexBuffer()
            .setData(NativeVectorBufferFactory.getInstance()
                .createFromFloatArray(3, 4, arrow)));
    }
}
