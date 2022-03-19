package tetzlaff.ibrelight.rendering;

import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.SceneModel;

import java.io.File;
import java.io.FileNotFoundException;

public class LightingResources<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final ContextType context;
    private final SceneModel sceneModel;

    private Program<ContextType> shadowProgram;

    private Texture3D<ContextType> shadowMaps;
    private FramebufferObject<ContextType> shadowFramebuffer;
    private Drawable<ContextType> shadowDrawable;

    private Texture2D<ContextType> backplateTexture;
    private Cubemap<ContextType> environmentMap;
    private FramebufferObject<ContextType> screenSpaceDepthFBO;


    public LightingResources(ContextType context, SceneModel sceneModel)
    {
        this.context = context;
        this.sceneModel = sceneModel;
    }

    public void initialize() throws FileNotFoundException
    {
        shadowProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File(new File(new File("shaders"), "common"), "depth.vert"))
                .addShader(ShaderType.FRAGMENT, new File(new File(new File("shaders"), "common"), "depth.frag"))
                .createProgram();

        shadowDrawable = context.createDrawable(shadowProgram);

        shadowMaps = createShadowMaps();
        shadowFramebuffer = context.buildFramebufferObject(2048, 2048)
                .addDepthAttachment()
                .createFramebufferObject();

        this.screenSpaceDepthFBO = context.buildFramebufferObject(512, 512)
                .addDepthAttachment(DepthAttachmentSpec.createFixedPointWithPrecision(16).setLinearFilteringEnabled(true))
                .createFramebufferObject();
    }

    public Texture2D<ContextType> getBackplateTexture()
    {
        return backplateTexture;
    }

    /**
     * Assumes ownership of the backplate texture
     * @param backplateTexture
     */
    public void takeBackplateTexture(Texture2D<ContextType> backplateTexture)
    {
        if (this.backplateTexture != null)
        {
            this.backplateTexture.close();
        }

        this.backplateTexture = backplateTexture;
    }

    public Cubemap<ContextType> getEnvironmentMap()
    {
        return this.environmentMap;
    }

    /**
     * Assumes ownership of the environment map resource
     * @param newEnvironmentMap
     */
    public void takeEnvironmentMap(Cubemap<ContextType> newEnvironmentMap)
    {
        if (this.environmentMap != null)
        {
            this.environmentMap.close();
        }

        this.environmentMap = newEnvironmentMap;
    }

    /**
     * Used for generating shadow maps.
     * Does not take ownership of this buffer.
     * @param positionBuffer
     */
    public void setPositionBuffer(VertexBuffer<ContextType> positionBuffer)
    {
        shadowDrawable.addVertexBuffer("position", positionBuffer);
    }

    public Matrix4 getLightProjection(int lightIndex)
    {
        Matrix4 lightMatrix = sceneModel.getLightModelViewMatrix(lightIndex);

        Vector4 lightDisplacement = lightMatrix.times(sceneModel.getCentroid().asPosition());
        float lightDist = lightDisplacement.getXYZ().length();
        float lookAtDist = lightDisplacement.getXY().length();

        float radius = (float)
                (sceneModel.getOrientation()
                        .times(new Vector3(0.5f * sceneModel.getScale()))
                        .length() / Math.sqrt(3));

        float fov;
        float farPlane;
        float nearPlane;

        if (sceneModel.getLightingModel().isGroundPlaneEnabled())
        {
            fov = 2.0f * (float)Math.asin(Math.min(0.99, (sceneModel.getScale() + lookAtDist) / lightDist));
            farPlane = lightDist + 2 * sceneModel.getScale();
            nearPlane = Math.max((lightDist + radius) / 32.0f, lightDist - 2 * radius);
        }
        else
        {
            fov = 2.0f * (float)Math.asin(Math.min(0.99, (radius + lookAtDist) / lightDist));
            farPlane = lightDist + radius;
            nearPlane = Math.max(farPlane / 1024.0f, lightDist - 2 * radius);
        }

        // Limit fov by the light's spot size.
        float spotFOV = 2.0f * sceneModel.getLightingModel().getLightPrototype(lightIndex).getSpotSize();
        fov = Math.min(fov, spotFOV);

        return Matrix4.perspective(fov, 1.0f, nearPlane, farPlane);
    }

    public Texture3D<ContextType> getShadowMaps()
    {
        return shadowMaps;
    }

    public void refreshShadowMaps()
    {
        // Too many lights; need to re-allocate shadow maps
        if (shadowMaps.getDepth() < sceneModel.getLightingModel().getLightCount())
        {
            shadowMaps.close();
            shadowMaps = null;
            shadowMaps = createShadowMaps();
        }

        for (int lightIndex = 0; lightIndex < sceneModel.getLightingModel().getLightCount(); lightIndex++)
        {
            drawShadowMaps(lightIndex);
        }
    }

    private Texture3D<ContextType> createShadowMaps()
    {
        return context.getTextureFactory().build2DDepthTextureArray(2048, 2048, sceneModel.getLightingModel().getLightCount())
                .setInternalPrecision(32)
                .setFloatingPointEnabled(true)
                .createTexture();
    }

    private void drawShadowMaps(int lightIndex)
    {
        Matrix4 lightProj = getLightProjection(lightIndex);

        shadowProgram.setUniform("projection", lightProj);

        FramebufferAttachment<ContextType> attachment = shadowMaps.getLayerAsFramebufferAttachment(lightIndex);

        shadowFramebuffer.setDepthAttachment(attachment);
        shadowFramebuffer.clearDepthBuffer();

        shadowProgram.setUniform("model_view", sceneModel.getLightModelViewMatrix(lightIndex));
        shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
    }

    public Texture2D<ContextType> getScreenSpaceDepthTexture()
    {
        return screenSpaceDepthFBO.getDepthAttachmentTexture();
    }

    public void refreshScreenSpaceDepthFBO(boolean expectingModelTransform, Matrix4 view, Matrix4 projection)
    {
        context.getState().disableBackFaceCulling();

        shadowProgram.setUniform("projection", projection);
        screenSpaceDepthFBO.clearDepthBuffer();

        Matrix4 modelView = expectingModelTransform ?
                sceneModel.getModelViewMatrix(view) : sceneModel.getCameraPoseFromViewMatrix(view);

        shadowProgram.setUniform("model_view", modelView);
        shadowProgram.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());
        shadowDrawable.draw(PrimitiveMode.TRIANGLES, screenSpaceDepthFBO);

        context.getState().enableBackFaceCulling();
    }

    @Override
    public void close()
    {
        if (this.backplateTexture != null)
        {
            this.backplateTexture.close();
            this.backplateTexture = null;
        }

        if (this.environmentMap != null)
        {
            this.environmentMap.close();
            this.environmentMap = null;
        }

        if (shadowMaps != null)
        {
            shadowMaps.close();
            shadowMaps = null;
        }

        if (shadowFramebuffer != null)
        {
            shadowFramebuffer.close();
            shadowFramebuffer = null;
        }

        if (shadowProgram != null)
        {
            shadowProgram.close();
            shadowProgram = null;
        }

        if (screenSpaceDepthFBO != null)
        {
            screenSpaceDepthFBO.close();
            screenSpaceDepthFBO = null;
        }
    }
}
