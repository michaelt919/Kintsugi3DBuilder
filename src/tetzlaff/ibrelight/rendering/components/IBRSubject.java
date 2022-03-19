package tetzlaff.ibrelight.rendering.components;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.CameraViewport;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.ibrelight.rendering.resources.LightingResources;
import tetzlaff.ibrelight.rendering.SceneViewportModel;
import tetzlaff.ibrelight.rendering.StandardShader;
import tetzlaff.ibrelight.util.KNNViewWeightGenerator;
import tetzlaff.util.ShadingParameterMode;

import java.io.FileNotFoundException;
import java.util.AbstractList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class IBRSubject<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;
    private final IBRResources<ContextType> resources;

    private StandardShader<ContextType> standardShader;
    private Drawable<ContextType> drawable;

    private UniformBuffer<ContextType> weightBuffer;

    private StandardRenderingMode lastCompiledRenderingMode = StandardRenderingMode.IMAGE_BASED;

    public IBRSubject(IBRResources<ContextType> resources, LightingResources<ContextType> lightingResources,
                      SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.resources = resources;
        this.context = resources.context;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.sceneViewportModel.addSceneObjectType("IBRObject");
        this.standardShader = new StandardShader<>(resources, lightingResources, sceneModel);
    }

    public Program<ContextType> getProgram()
    {
        return standardShader.getProgram();
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        standardShader.initialize(this.sceneModel.getSettingsModel() == null ?
            StandardRenderingMode.IMAGE_BASED : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class));
        refreshDrawable();
    }

    @Override
    public void update() throws FileNotFoundException
    {
        Map<String, Optional<Object>> defineMap = standardShader.getPreprocessorDefines();

        // Reloads shaders only if compiled settings have changed.
        if (getExpectedRenderingMode() != lastCompiledRenderingMode ||
            defineMap.entrySet().stream().anyMatch(
                defineEntry -> !Objects.equals(drawable.program().getDefine(defineEntry.getKey()), defineEntry.getValue())))
        {
            System.out.println("Updating compiled render settings.");
            reloadShaders();
        }
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        StandardRenderingMode renderingMode = getExpectedRenderingMode();

        // Force reload shaders
        standardShader.reload(renderingMode);
        refreshDrawable();

        this.lastCompiledRenderingMode = renderingMode;
    }

    private void refreshDrawable()
    {
        this.drawable = context.createDrawable(standardShader.getProgram());
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

    private StandardRenderingMode getExpectedRenderingMode()
    {
        return this.sceneModel.getSettingsModel() == null ?  StandardRenderingMode.IMAGE_BASED
            : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class);
    }

    private void setupModelView(Program<ContextType> p, Matrix4 modelView)
    {
        p.setUniform("model_view", modelView);
        p.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

        if (!this.sceneModel.getSettingsModel().getBoolean("relightingEnabled") && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")
                && this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM)
        {
            if (weightBuffer == null)
            {
                weightBuffer = context.createUniformBuffer();
            }
            weightBuffer.setData(this.generateViewWeights(modelView)); // TODO modelView might not be the right matrix?
            p.setUniformBuffer("ViewWeights", weightBuffer);
        }
    }

    private NativeVectorBuffer generateViewWeights(Matrix4 targetView)
    {
        float[] viewWeights = //new PowerViewWeightGenerator(settings.getWeightExponent())
            new KNNViewWeightGenerator(4)
                .generateWeights(resources,
                    new AbstractList<Integer>()
                    {
                        @Override
                        public Integer get(int index)
                        {
                            return index;
                        }

                        @Override
                        public int size()
                        {
                            return resources.viewSet.getCameraPoseCount();
                        }
                    },
                    targetView);

        return NativeVectorBufferFactory.getInstance().createFromFloatArray(1, viewWeights.length, viewWeights);
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        context.getState().disableBackFaceCulling();

        // After the ground plane, use a gray color for anything without a texture map.
        drawable.program().setUniform("defaultDiffuseColor", new Vector3(0.125f));

        standardShader.setup();
        drawable.program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("IBRObject"));

        Matrix4 modelView = sceneModel.getModelViewMatrix(cameraViewport.getView());

        setupModelView(drawable.program(), modelView);

        drawable.program().setUniform("projection", cameraViewport.getViewportProjection());
        drawable.program().setUniform("fullProjection", cameraViewport.getFullProjection());

        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());

        context.getState().enableBackFaceCulling();
    }

    @Override
    public void close()
    {
        if (standardShader != null)
        {
            standardShader.close();
            standardShader = null;
        }

        if (weightBuffer != null)
        {
            weightBuffer.close();
            weightBuffer = null;
        }
    }
}
