package tetzlaff.ibrelight.rendering.components;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.ibrelight.core.StandardRenderingMode;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.rendering.LightingResources;
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
    private final LightingResources<ContextType> lightingResources;

    private StandardShader<ContextType> standardShader;
    private Drawable<ContextType> drawable;

    private UniformBuffer<ContextType> weightBuffer;

    private StandardRenderingMode lastCompiledRenderingMode = StandardRenderingMode.IMAGE_BASED;

    public IBRSubject(IBRResources<ContextType> resources, LightingResources<ContextType> lightingResources,
                      SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.resources = resources;
        this.lightingResources = lightingResources;
        this.context = resources.context;
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.standardShader = new StandardShader<>(resources, lightingResources, sceneModel);
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        standardShader.initialize(this.sceneModel.getSettingsModel() == null ?
            StandardRenderingMode.IMAGE_BASED : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class));

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


    public void updateCompiledSettings()
    {
        Map<String, Optional<Object>> defineMap = getPreprocessorDefines();

        StandardRenderingMode renderingMode =
                this.sceneModel.getSettingsModel() == null ? StandardRenderingMode.IMAGE_BASED : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class);

        if (renderingMode != lastCompiledRenderingMode ||
                defineMap.entrySet().stream().anyMatch(
                        defineEntry -> !Objects.equals(drawable.program().getDefine(defineEntry.getKey()), defineEntry.getValue())))
        {
            try
            {
                System.out.println("Updating compiled render settings.");
                standardShader.reload(renderingMode);
            }
            catch (RuntimeException|FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        standardShader.reload(this.sceneModel.getSettingsModel() == null ?
            StandardRenderingMode.IMAGE_BASED : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class));

        this.lastCompiledRenderingMode = renderingMode;

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
    public void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 fullProjection,
                     Matrix4 viewportProjection, int x, int y, int width, int height)
    {
        try(UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer()) {

            // After the ground plane, use a gray color for anything without a texture map.
            drawable.program().setUniform("defaultDiffuseColor", new Vector3(0.125f));

            context.getState().disableBackFaceCulling();

            standardShader.setup();
            drawable.program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("IBRObject"));

            if (lightCalibrationMode) {
                drawable.program().setUniform("holeFillColor", new Vector3(0.5f));
                viewIndexBuffer.setData(NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, 1, snapViewIndex));
                drawable.program().setUniformBuffer("ViewIndices", viewIndexBuffer);
            } else {
                drawable.program().setUniform("holeFillColor", new Vector3(0.0f));
            }

            drawable.program().setTexture("screenSpaceDepthBuffer", lightingResources.getScreenSpaceDepthTexture());

            Matrix4 modelView = lightCalibrationMode ? sceneModel.getCameraPoseFromViewMatrix(view) : sceneModel.getModelViewMatrix(view);

            setupModelView(drawable.program(), view);

            drawable.program().setUniform("projection", viewportProjection);
            drawable.program().setUniform("fullProjection", fullProjection);

            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, x, y, width, height);

            context.getState().enableBackFaceCulling();
        }
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
