package tetzlaff.texturefit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.SimpleLoadOptionsModel;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources;

class TextureFitResources<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final int SHADOW_MAP_FAR_PLANE_CUSHION = 2; // TODO decide where this should be defined

    private static final double FITTING_GAMMA = 2.2; // TODO make this configurable from the interface
    private static final boolean BRUTE_FORCE_NORMAL_COMPUTATION = false;

    private final ContextType context;
    private final File vsetFile;
    private final File objFile;
    private final TextureFitParameters param;

    private File imageDir;
    private File maskDir;
    private File rescaleDir;
    private File tmpDir;
    private IBRResources<ContextType> resources;

    private Program<ContextType> depthRenderingProgram;
    private Program<ContextType> projTexProgram;
    private Program<ContextType> lightFitProgram;
    private Program<ContextType> diffuseFitProgram;
    private Program<ContextType> specularFitProgram;
    private Program<ContextType> adjustFitProgram;
    private Program<ContextType> errorCalcSimpleProgram;
    private Program<ContextType> diffuseDebugProgram;
    private Program<ContextType> specularDebugProgram;
    private Program<ContextType> textureRectProgram;

    private VertexBuffer<ContextType> positionBuffer;
    private VertexBuffer<ContextType> texCoordBuffer;
    private VertexBuffer<ContextType> normalBuffer;
    private VertexBuffer<ContextType> tangentBuffer;
    private Vector3 center;

    private UniformBuffer<ContextType> lightPositionBuffer;
    private UniformBuffer<ContextType> lightIntensityBuffer;
    private UniformBuffer<ContextType> shadowMatrixBuffer;

    private String materialFileName;
    private String materialName;

    private ViewSet viewSet;

    private Program<ContextType> errorCalcProgram;
    private Program<ContextType> holeFillProgram;
    private Program<ContextType> finalizeProgram;
    private Program<ContextType> peakIntensityProgram;

    private Texture3D<ContextType> viewTextures;
    private Texture3D<ContextType> depthTextures;
    private Texture3D<ContextType> shadowTextures;

    private PeakIntensityEstimator<ContextType> peakIntensityEstimator;
    private SpecularPeakFit<ContextType> specularPeakFit;

    TextureFitResources(ContextType context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, TextureFitParameters param)
    {
        this.context = context;
        this.vsetFile = vsetFile;
        this.objFile = objFile;
        this.imageDir = imageDir;
        this.maskDir = maskDir;
        this.rescaleDir = rescaleDir;
        this.param = param;
    }

    @Override
    public void close()
    {
        if (viewTextures != null)
        {
            viewTextures.close();
        }

        if (depthTextures != null)
        {
            depthTextures.close();
        }

        if (shadowTextures != null)
        {
            shadowTextures.close();
        }

        if (lightPositionBuffer != null)
        {
            lightPositionBuffer.close();
        }

        if (lightIntensityBuffer != null)
        {
            lightIntensityBuffer.close();
        }

        if (shadowMatrixBuffer != null)
        {
            shadowMatrixBuffer.close();
        }

        if (projTexProgram != null)
        {
            projTexProgram.close();
        }

        if (diffuseFitProgram != null)
        {
            diffuseFitProgram.close();
        }

        if (diffuseDebugProgram != null)
        {
            diffuseDebugProgram.close();
        }

        if (specularDebugProgram != null)
        {
            specularDebugProgram.close();
        }

        if (depthRenderingProgram != null)
        {
            depthRenderingProgram.close();
        }

        if (lightFitProgram != null)
        {
            lightFitProgram.close();
        }

        if (specularFitProgram != null)
        {
            specularFitProgram.close();
        }

        if (adjustFitProgram != null)
        {
            adjustFitProgram.close();
        }

        if (errorCalcProgram != null)
        {
            errorCalcProgram.close();
        }

        if (errorCalcSimpleProgram != null)
        {
            errorCalcSimpleProgram.close();
        }

        if (textureRectProgram != null)
        {
            textureRectProgram.close();
        }

        if (holeFillProgram != null)
        {
            holeFillProgram.close();
        }

        if (finalizeProgram != null)
        {
            finalizeProgram.close();
        }

        if (peakIntensityProgram != null)
        {
            peakIntensityProgram.close();
        }

        if (resources != null)
        {
            resources.close();
        }

        if (positionBuffer != null)
        {
            positionBuffer.close();
        }

        if (normalBuffer != null)
        {
            normalBuffer.close();
        }

        if (texCoordBuffer != null)
        {
            texCoordBuffer.close();
        }

        if (tangentBuffer != null)
        {
            tangentBuffer.close();
        }
    }



    String getMaterialFileName()
    {
        return materialFileName;
    }

    String getMaterialName()
    {
        return materialName;
    }

    ViewSet getViewSet()
    {
        return viewSet;
    }

    Program<ContextType> getErrorCalcProgram()
    {
        return errorCalcProgram;
    }

    Program<ContextType> getHoleFillProgram()
    {
        return holeFillProgram;
    }

    Program<ContextType> getFinalizeProgram()
    {
        return finalizeProgram;
    }

    Program<ContextType> getPeakIntensityProgram()
    {
        return peakIntensityProgram;
    }

    Texture3D<ContextType> getViewTextures()
    {
        return viewTextures;
    }

    Texture3D<ContextType> getDepthTextures()
    {
        return depthTextures;
    }

    Texture3D<ContextType> getShadowTextures()
    {
        return shadowTextures;
    }

    PeakIntensityEstimator<ContextType> getPeakIntensityEstimator()
    {
        return peakIntensityEstimator;
    }

    SpecularPeakFit<ContextType> getSpecularPeakFit()
    {
        return specularPeakFit;
    }

    VertexGeometry getGeometry()
    {
        return resources.geometry;
    }

    void compileShaders() throws IOException
    {
        System.out.println("Loading and compiling shader programs...");
        Date timestamp = new Date();

        depthRenderingProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "depth.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "common", "depth.frag").toFile())
            .createProgram();

        projTexProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCE", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "colorappearance", "projtex_single.frag").toFile())
            .createProgram();

        lightFitProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit",
                param.isImagePreprojectionUseEnabled() ? "lightfit_texspace.frag" : "lightfit_imgspace.frag").toFile())
            .createProgram();

        diffuseFitProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit",
                param.isImagePreprojectionUseEnabled() ? "diffusefit_texspace.frag" : "diffusefit_imgspace.frag").toFile())
            .createProgram();

        specularFitProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit",
                //"debug.frag").toFile())
                param.isImagePreprojectionUseEnabled() ? "specularfit_texspace.frag" : "specularfit_imgspace.frag").toFile())
            .createProgram();

        adjustFitProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "texturefit",
                //"adjustfit_debug.frag").toFile())
                param.isImagePreprojectionUseEnabled() ? "adjustfit_texspace.frag" : "adjustfit_imgspace.frag").toFile())
            .createProgram();

        errorCalcProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "texturefit",
                //"errorcalc_debug.frag").toFile())
                param.isImagePreprojectionUseEnabled() ? "errorcalc_texspace.frag" : "errorcalc_imgspace.frag").toFile())
            .createProgram();

        errorCalcSimpleProgram = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "texturefit", "errorcalc_simple.frag").toFile())
            .createProgram();

        diffuseDebugProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "colorappearance", "projtex_multi.frag").toFile())
            .createProgram();

        specularDebugProgram = resources.getIBRShaderProgramBuilder()
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", param.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", param.isCameraVisibilityTestEnabled())
            .define("SHADOW_TEST_ENABLED", false)
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "texturefit", "specularresid_imgspace.frag").toFile())
            .createProgram();

        textureRectProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "common", "texture.frag").toFile())
            .createProgram();

        holeFillProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "texturefit", "holefill.frag").toFile())
            .createProgram();

        finalizeProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "texturefit", "finalize.frag").toFile())
            .createProgram();

        peakIntensityProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "texturefit", "peak.frag").toFile())
            .createProgram();

        peakIntensityEstimator = new PeakIntensityEstimator<>(context, viewSet);
        peakIntensityEstimator.compileShaders(param.isCameraVisibilityTestEnabled(), false);

        specularPeakFit = new SpecularPeakFit<>(context, this::setupCommonShaderInputs, param.isCameraVisibilityTestEnabled(), false,
            viewSet, param.getTextureSubdivision());

        System.out.println("Shader compilation completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    }

    @FunctionalInterface
    private interface TextureSpaceCallback<ContextType extends Context<ContextType>>
    {
        void execute(Framebuffer<ContextType> framebuffer, int subdivisionRow, int subdivisionCol);
    }

    private FramebufferObject<ContextType> projectIntoTextureSpace(
        Program<ContextType> program, int viewIndex, int textureSize, int textureSubdivision, boolean useExistingTextureArray,
        TextureSpaceCallback<ContextType> callback) throws IOException
    {
        try(FramebufferObject<ContextType> mainFBO =
            context.buildFramebufferObject(textureSize / textureSubdivision, textureSize / textureSubdivision)
                .addColorAttachments(ColorFormat.RGBA32F, 3)
                .createFramebufferObject())
        {
            Drawable<ContextType> drawable = context.createDrawable(program);

            drawable.addVertexBuffer("position", positionBuffer);
            drawable.addVertexBuffer("texCoord", texCoordBuffer);
            drawable.addVertexBuffer("normal", normalBuffer);
            drawable.addVertexBuffer("tangent", tangentBuffer);

            drawable.program().setUniform("gamma", param.getGamma());

            if (resources.getLuminanceMap() == null)
            {
                drawable.program().setTexture("luminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
            }
            else
            {
                drawable.program().setTexture("luminanceMap", resources.getLuminanceMap());
            }

            int width;
            int height;

            if (useExistingTextureArray && viewTextures != null)
            {
                if (param.isCameraVisibilityTestEnabled() && depthTextures != null)
                {
                    drawable.program().setTexture("depthImages", depthTextures);
                    drawable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());

                    if (shadowTextures != null && shadowMatrixBuffer != null)
                    {
                        drawable.program().setTexture("shadowImages", shadowTextures);
                        drawable.program().setUniformBuffer("ShadowMatrices", shadowMatrixBuffer);
                    }
                }

                drawable.program().setUniformBuffer("CameraPoses", resources.cameraPoseBuffer);
                drawable.program().setUniformBuffer("CameraProjections", resources.cameraProjectionBuffer);
                drawable.program().setUniformBuffer("CameraProjectionIndices", resources.cameraProjectionIndexBuffer);
                drawable.program().setUniformBuffer("LightIndices", resources.lightIndexBuffer);
                drawable.program().setUniformBuffer("LightPositions", this.lightPositionBuffer);
                drawable.program().setUniformBuffer("LightIntensities", this.lightIntensityBuffer);

                drawable.program().setTexture("viewImages", viewTextures);
                drawable.program().setUniform("viewIndex", viewIndex);

                width = viewTextures.getWidth();
                height = viewTextures.getHeight();
            }
            else
            {
                drawable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());

                drawable.program().setUniform("cameraPose", viewSet.getCameraPose(viewIndex));
                drawable.program().setUniform("cameraProjection",
                    viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
                        .getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));

                boolean enableShadowTest = param.isCameraVisibilityTestEnabled();

                Vector3 lightPosition = viewSet.getLightPosition(viewSet.getLightIndex(viewIndex));
                drawable.program().setUniform("lightIntensity", viewSet.getLightIntensity(viewSet.getLightIndex(viewIndex)));
                drawable.program().setUniform("lightPosition", lightPosition);

                File imageFile = new File(imageDir, viewSet.getImageFileName(viewIndex));
                if (!imageFile.exists())
                {
                    String[] filenameParts = viewSet.getImageFileName(viewIndex).split("\\.");
                    filenameParts[filenameParts.length - 1] = "png";
                    String pngFileName = String.join(".", filenameParts);
                    imageFile = new File(imageDir, pngFileName);
                }

                ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> textureBuilder;

                if (maskDir == null)
                {
                    textureBuilder = context.getTextureFactory().build2DColorTextureFromFile(imageFile, true);
                }
                else
                {
                    File maskFile = new File(maskDir, viewSet.getImageFileName(viewIndex));
                    if (!maskFile.exists())
                    {
                        String[] filenameParts = viewSet.getImageFileName(viewIndex).split("\\.");
                        filenameParts[filenameParts.length - 1] = "png";
                        String pngFileName = String.join(".", filenameParts);
                        maskFile = new File(maskDir, pngFileName);
                    }

                    textureBuilder = context.getTextureFactory().build2DColorTextureFromFileWithMask(imageFile, maskFile, true);
                }

                try(Texture2D<ContextType> viewTexture = textureBuilder
                    .setLinearFilteringEnabled(true)
                    .setMipmapsEnabled(true)
                    .createTexture())
                {
                    drawable.program().setTexture("viewImage", viewTexture);

                    width = viewTexture.getWidth();
                    height = viewTexture.getHeight();

                    if (enableShadowTest)
                    {
                        Matrix4 shadowModelView = Matrix4.lookAt(viewSet.getCameraPoseInverse(viewIndex).times(lightPosition.asPosition()).getXYZ(), center, new Vector3(0, 1, 0));

                        Matrix4 shadowProjection = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
                            .getProjectionMatrix(
                                viewSet.getRecommendedNearPlane(),
                                viewSet.getRecommendedFarPlane() * SHADOW_MAP_FAR_PLANE_CUSHION // double it for good measure
                            );

                        try(FramebufferObject<ContextType> shadowFBO = context.buildFramebufferObject(width, height)
                            .addDepthAttachment()
                            .createFramebufferObject())
                        {
                            Drawable<ContextType> shadowDrawable = context.createDrawable(depthRenderingProgram);
                            shadowDrawable.addVertexBuffer("position", positionBuffer);

                            depthRenderingProgram.setUniform("model_view", shadowModelView);
                            depthRenderingProgram.setUniform("projection", shadowProjection);

                            shadowFBO.clearDepthBuffer();
                            shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFBO);

                            drawable.program().setUniform("shadowMatrix", shadowProjection.times(shadowModelView));
                            drawable.program().setTexture("shadowImage", shadowFBO.getDepthAttachmentTexture());
                        }
                    }
                }
            }

            FramebufferObject<ContextType> depthFBO =
                context.buildFramebufferObject(width, height)
                    .addDepthAttachment()
                    .createFramebufferObject();

            try
            {
                Drawable<ContextType> depthDrawable = context.createDrawable(depthRenderingProgram);
                depthDrawable.addVertexBuffer("position", positionBuffer);

                depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(viewIndex));
                depthRenderingProgram.setUniform("projection",
                    viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
                        .getProjectionMatrix(
                            viewSet.getRecommendedNearPlane(),
                            viewSet.getRecommendedFarPlane()
                        )
                );

                depthFBO.clearDepthBuffer();
                depthDrawable.draw(PrimitiveMode.TRIANGLES, depthFBO);

                if (!useExistingTextureArray || viewTextures == null)
                {
                    drawable.program().setTexture("depthImage", depthFBO.getDepthAttachmentTexture());
                }

                for (int row = 0; row < textureSubdivision; row++)
                {
                    for (int col = 0; col < textureSubdivision; col++)
                    {
                        drawable.program().setUniform("minTexCoord",
                            new Vector2((float)col / (float)textureSubdivision, (float)row / (float)textureSubdivision));

                        drawable.program().setUniform("maxTexCoord",
                            new Vector2((float)(col+1) / (float)textureSubdivision, (float)(row+1) / (float)textureSubdivision));

                        mainFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                        mainFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                        mainFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        mainFBO.clearDepthBuffer();
                        drawable.draw(PrimitiveMode.TRIANGLES, mainFBO);

                        callback.execute(mainFBO, row, col);
                    }
                }

                return depthFBO;
            }
            catch(RuntimeException e)
            {
                depthFBO.close();
                throw e;
            }
        }
    }

    private VertexGeometry loadMesh() throws IOException
    {
        VertexGeometry mesh = VertexGeometry.createFromOBJFile(objFile);
        positionBuffer = context.createVertexBuffer().setData(mesh.getVertices());
        texCoordBuffer = context.createVertexBuffer().setData(mesh.getTexCoords());
        normalBuffer = context.createVertexBuffer().setData(mesh.getNormals());
        tangentBuffer = context.createVertexBuffer().setData(mesh.getTangents());
        center = mesh.getCentroid();
        materialFileName = mesh.getMaterialFileName();

        if (materialFileName == null)
        {
            materialFileName = objFile.getName().split("\\.")[0] + ".mtl";
        }

        if (mesh.getMaterial() == null)
        {
            materialName = materialFileName.split("\\.")[0];
        }
        else
        {
            materialName = mesh.getMaterial().getName();
            if (materialName == null)
            {
                materialName = materialFileName.split("\\.")[0];
            }
        }

        return mesh;
    }

    private Drawable<ContextType> getLightFitDrawable()
    {
        Drawable<ContextType> drawable = context.createDrawable(lightFitProgram);

        drawable.addVertexBuffer("position", positionBuffer);
        drawable.addVertexBuffer("texCoord", texCoordBuffer);
        drawable.addVertexBuffer("normal", normalBuffer);

        drawable.program().setUniform("gamma", param.getGamma());
        if (resources.getLuminanceMap() == null)
        {
            drawable.program().setTexture("luminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
        }
        else
        {
            drawable.program().setTexture("luminanceMap", resources.getLuminanceMap());
        }
        drawable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());

        drawable.program().setUniformBuffer("CameraPoses", resources.cameraPoseBuffer);
        drawable.program().setUniformBuffer("CameraWeights", resources.cameraWeightBuffer);

        if (!param.isImagePreprojectionUseEnabled())
        {
            drawable.program().setUniformBuffer("CameraProjections", resources.cameraProjectionBuffer);
            drawable.program().setUniformBuffer("CameraProjectionIndices", resources.cameraProjectionIndexBuffer);
        }

        drawable.program().setUniformBuffer("LightIndices", resources.lightIndexBuffer);

        drawable.program().setUniform("delta", param.getDiffuseDelta());
        drawable.program().setUniform("iterations", 1/*param.getDiffuseIterations()*/); // TODO rework light fitting

        return drawable;
    }

    private abstract static class LightFit<ContextType extends Context<ContextType>>
    {
        private final Drawable<ContextType> drawable;
        protected final int framebufferSize;
        protected final int framebufferSubdiv;

        private Vector3 position;
        private Vector3 intensity;

        Vector3 getPosition()
        {
            return position;
        }

        Vector3 getIntensity()
        {
            return intensity;
        }

        protected abstract void fitTexture(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer) throws IOException;

        LightFit(Drawable<ContextType> drawable, int framebufferSize, int framebufferSubdivision)
        {
            this.drawable = drawable;
            this.framebufferSize = framebufferSize;
            this.framebufferSubdiv = framebufferSubdivision;
        }

        void fit(int lightIndex) throws IOException
        {
            FramebufferSize lightFBOSize;

            float[] rawLightPositions;
            float[] rawLightIntensities;

            try(FramebufferObject<ContextType> framebuffer =
                drawable.getContext().buildFramebufferObject(framebufferSize, framebufferSize)
                    .addColorAttachments(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA32F), 2)
                    .createFramebufferObject())
            {
                lightFBOSize = framebuffer.getSize();

                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

                drawable.program().setUniform("lightIndex", lightIndex);
                fitTexture(drawable, framebuffer);

                //framebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "lightDebug.png"));

                System.out.println("Aggregating light estimates...");

                rawLightPositions = framebuffer.readFloatingPointColorBufferRGBA(0);
                rawLightIntensities = framebuffer.readFloatingPointColorBufferRGBA(1);
            }

            Vector4 lightPositionSum = new Vector4(0, 0, 0, 0);
            Vector4 lightIntensitySum = new Vector4(0, 0, 0, 0);

            for (int i = 0; i < lightFBOSize.height; i++)
            {
                for (int j = 0; j < lightFBOSize.width; j++)
                {
                    int indexStart = (i * lightFBOSize.width + j) * 4;
                    lightPositionSum = lightPositionSum.plus(
                        new Vector4(rawLightPositions[indexStart], rawLightPositions[indexStart+1], rawLightPositions[indexStart+2], 1.0f)
                            .times(rawLightPositions[indexStart+3]));
                    lightIntensitySum = lightIntensitySum.plus(
                        new Vector4(rawLightIntensities[indexStart], rawLightIntensities[indexStart+1], rawLightIntensities[indexStart+2], 1.0f)
                            .times(rawLightIntensities[indexStart+3]));
                }
            }

            position = lightPositionSum.dividedBy(lightPositionSum.w).getXYZ();
            intensity = lightIntensitySum.dividedBy(lightIntensitySum.w).getXYZ();
        }
    }

    private static class TexSpaceLightFit<ContextType extends Context<ContextType>> extends LightFit<ContextType>
    {
        private final File preprojDir;
        private final int preprojCount;

        TexSpaceLightFit(Drawable<ContextType> drawable, File preprojDir, int preprojCount, int framebufferSize, int framebufferSubdiv)
        {
            super(drawable, framebufferSize, framebufferSubdiv);
            this.preprojDir = preprojDir;
            this.preprojCount = preprojCount;
        }

        @Override
        protected void fitTexture(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer) throws IOException
        {
            int subdivSize = framebufferSize / framebufferSubdiv;

            for (int row = 0; row < framebufferSubdiv; row++)
            {
                for (int col = 0; col < framebufferSubdiv; col++)
                {
                    Texture3D<ContextType> preprojectedViews = drawable.getContext().getTextureFactory()
                        .build2DColorTextureArray(subdivSize, subdivSize, preprojCount).createTexture();

                    for (int i = 0; i < preprojCount; i++)
                    {
                        preprojectedViews.loadLayer(i, new File(new File(preprojDir, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
                    }

                    drawable.program().setTexture("viewImages", preprojectedViews);

                    drawable.program().setUniform("minTexCoord",
                        new Vector2((float)col / (float)framebufferSubdiv, (float)row / (float)framebufferSubdiv));

                    drawable.program().setUniform("maxTexCoord",
                        new Vector2((float)(col+1) / (float)framebufferSubdiv, (float)(row+1) / (float)framebufferSubdiv));

                    drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
                    drawable.getContext().finish();

                    if (framebufferSubdiv > 1)
                    {
                        System.out.println("Block " + (row*framebufferSubdiv + col + 1) + '/' + (framebufferSubdiv * framebufferSubdiv) +
                            "completed.");
                    }
                }
            }
        }
    }

    private static class ImgSpaceLightFit<ContextType extends Context<ContextType>> extends LightFit<ContextType>
    {
        private final Texture<ContextType> viewTextures;
        private final Texture<ContextType> depthTextures;

        ImgSpaceLightFit(Drawable<ContextType> drawable, Texture<ContextType> viewTextures, Texture<ContextType> depthTextures, int framebufferSize, int framebufferSubdiv)
        {
            super(drawable, framebufferSize, framebufferSubdiv);

            this.viewTextures = viewTextures;
            this.depthTextures = depthTextures;
        }

        @Override
        protected void fitTexture(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
        {
            int subdivSize = framebufferSize / framebufferSubdiv;

            for (int row = 0; row < framebufferSubdiv; row++)
            {
                for (int col = 0; col < framebufferSubdiv; col++)
                {
                    drawable.program().setTexture("viewImages", viewTextures);
                    drawable.program().setTexture("depthImages", depthTextures);

                    drawable.program().setUniform("minTexCoord",
                        new Vector2((float)col / (float)framebufferSubdiv, (float)row / (float)framebufferSubdiv));

                    drawable.program().setUniform("maxTexCoord",
                        new Vector2((float)(col+1) / (float)framebufferSubdiv, (float)(row+1) / (float)framebufferSubdiv));

                    drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
                    drawable.getContext().finish();

                    if (framebufferSubdiv > 1)
                    {
                        System.out.println("Block " + (row*framebufferSubdiv + col + 1) + '/' + (framebufferSubdiv * framebufferSubdiv) + " completed.");
                    }
                }
            }
        }
    }

    private LightFit<ContextType> createTexSpaceLightFit(int framebufferSize, int framebufferSubdiv)
    {
        return new TexSpaceLightFit<>(getLightFitDrawable(), tmpDir, viewSet.getCameraPoseCount(), framebufferSize, framebufferSubdiv);
    }

    private LightFit<ContextType> createImgSpaceLightFit(int framebufferSize, int framebufferSubdiv)
    {
        return new ImgSpaceLightFit<>(getLightFitDrawable(), viewTextures, depthTextures, framebufferSize, framebufferSubdiv);
    }

    void setupCommonShaderInputs(Drawable<ContextType> drawable)
    {
        drawable.addVertexBuffer("position", positionBuffer);
        drawable.addVertexBuffer("texCoord", texCoordBuffer);
        drawable.addVertexBuffer("normal", normalBuffer);
        drawable.addVertexBuffer("tangent", tangentBuffer);

        drawable.program().setUniformBuffer("CameraPoses", resources.cameraPoseBuffer);
        drawable.program().setUniformBuffer("CameraWeights", resources.cameraWeightBuffer);

        if (!param.isImagePreprojectionUseEnabled())
        {
            drawable.program().setUniformBuffer("CameraProjections", resources.cameraProjectionBuffer);
            drawable.program().setUniformBuffer("CameraProjectionIndices", resources.cameraProjectionIndexBuffer);
        }

        drawable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
        drawable.program().setUniform("gamma", param.getGamma());
        drawable.program().setUniform("fittingGamma", (float)FITTING_GAMMA);

        if (resources.getLuminanceMap() == null)
        {
            drawable.program().setTexture("luminanceMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_1D));
        }
        else
        {
            drawable.program().setTexture("luminanceMap", resources.getLuminanceMap());
        }

        drawable.program().setUniformBuffer("LightIndices", resources.lightIndexBuffer);

        if (lightPositionBuffer != null)
        {
            drawable.program().setUniformBuffer("LightPositions", lightPositionBuffer);
        }
        else
        {
            drawable.program().setUniformBuffer("LightPositions", resources.lightPositionBuffer);
        }

        if (lightIntensityBuffer != null)
        {
            drawable.program().setUniformBuffer("LightIntensities", lightIntensityBuffer);
        }
        else
        {
            drawable.program().setUniformBuffer("LightIntensities", resources.lightIntensityBuffer);
        }

        if (shadowMatrixBuffer != null)
        {
            drawable.program().setUniformBuffer("ShadowMatrices", shadowMatrixBuffer);
        }
    }

    DiffuseFit<ContextType> createDiffuseFit(Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(diffuseFitProgram);
        setupCommonShaderInputs(drawable);
        drawable.program().setUniform("delta", param.getDiffuseDelta());
        drawable.program().setUniform("iterations", param.getDiffuseIterations());
        drawable.program().setUniform("fit1Weight", param.getDiffuseInputNormalWeight());
        drawable.program().setUniform("fit3Weight", param.getDiffuseComputedNormalWeight());
        return new DiffuseFit<>(drawable, framebuffer, viewCount, subdiv);
    }

    public SpecularFit<ContextType> createSpecularFit(Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(specularFitProgram);
        setupCommonShaderInputs(drawable);
        return new SpecularFit<>(drawable, framebuffer, viewCount, subdiv);
    }

    AdjustFit<ContextType> createAdjustFit(int viewCount, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(adjustFitProgram);
        setupCommonShaderInputs(drawable);
        return new AdjustFit<>(drawable, viewCount, subdiv);
    }

    ErrorCalc<ContextType> createErrorCalc(int viewCount, int subdiv)
    {
        Drawable<ContextType> drawable = context.createDrawable(errorCalcProgram);
        setupCommonShaderInputs(drawable);
        return new ErrorCalc<>(drawable, viewCount, subdiv);
    }

    private static double getLinearDepth(double nonLinearDepth, double nearPlane, double farPlane)
    {
        return 2 * nearPlane * farPlane / (farPlane + nearPlane - (2 * nonLinearDepth - 1) * (farPlane - nearPlane));
    }

    double loadTextures() throws IOException
    {
        if (param.isImagePreprojectionUseEnabled() && param.isImagePreprojectionGenerationEnabled())
        {
            System.out.println("Pre-projecting images into texture space...");
            Date timestamp = new Date();

            tmpDir.mkdir();
            double minDepth = viewSet.getRecommendedFarPlane();

            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                File viewDir = new File(tmpDir, String.format("%04d", i));
                viewDir.mkdir();

                try(FramebufferObject<ContextType> depthFBO = projectIntoTextureSpace(projTexProgram, i, param.getTextureSize(), param.getTextureSubdivision(), false,
                    (framebuffer, row, col) ->
                    {
                        try
                        {
                            framebuffer.saveColorBufferToFile(0, "PNG", new File(viewDir, String.format("r%04dc%04d.png", row, col)));
                            if (param.isDebugModeEnabled())
                            {
                                framebuffer.saveColorBufferToFile(1, "PNG", new File(viewDir, String.format("geomInfo_r%04dc%04d.png", row, col)));
                            }
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }))
                {
                    if (i == viewSet.getPrimaryViewIndex())
                    {
                        short[] depthBufferData = depthFBO.readDepthBuffer();
                        for (short encodedDepth : depthBufferData)
                        {
                            int nonlinearDepth = 0xFFFF & (int) encodedDepth;
                            minDepth = Math.min(minDepth, getLinearDepth((2.0 * nonlinearDepth) / 0xFFFF - 1.0,
                                viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                        }
                    }
                }

                System.out.println("Completed " + (i+1) + '/' + viewSet.getCameraPoseCount());
            }

            System.out.println("Pre-projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

            return minDepth;
        }
        else if (!param.isImagePreprojectionUseEnabled())
        {
            if (param.isImageRescalingEnabled())
            {
                System.out.println("Rescaling images...");
                Date timestamp = new Date();

                Drawable<ContextType> downsampleRenderable = context.createDrawable(textureRectProgram);

                try
                    (
                        // Create an FBO for downsampling
                        FramebufferObject<ContextType> downsamplingFBO =
                            context.buildFramebufferObject(param.getImageWidth(), param.getImageHeight())
                                .addColorAttachment()
                                .createFramebufferObject();

                        VertexBuffer<ContextType> rectBuffer = context.createRectangle()
                    )
                {
                    downsampleRenderable.addVertexBuffer("position", rectBuffer);

                    // Downsample and store each image
                    for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
                    {
                        File imageFile = IBRResources.findImageFile(new File(imageDir, viewSet.getImageFileName(i)));

                        TextureBuilder<ContextType, ? extends Texture2D<ContextType>> fullSizeImageBuilder;

                        if (maskDir == null)
                        {
                            fullSizeImageBuilder = context.getTextureFactory().build2DColorTextureFromFile(imageFile, true);
                        }
                        else
                        {
                            File maskFile = IBRResources.findImageFile(new File(maskDir, viewSet.getImageFileName(0)));

                            fullSizeImageBuilder = context.getTextureFactory().build2DColorTextureFromFileWithMask(imageFile, maskFile, true);
                        }

                        try(Texture2D<ContextType> fullSizeImage = fullSizeImageBuilder
                            .setLinearFilteringEnabled(true)
                            .setMipmapsEnabled(true)
                            .createTexture())
                        {
                            downsamplingFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

                            textureRectProgram.setTexture("tex", fullSizeImage);

                            downsampleRenderable.draw(PrimitiveMode.TRIANGLE_FAN, downsamplingFBO);
                            context.finish();

                            if (rescaleDir != null)
                            {
                                String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
                                filenameParts[filenameParts.length - 1] = "png";
                                String pngFileName = String.join(".", filenameParts);
                                downsamplingFBO.saveColorBufferToFile(0, "PNG", new File(rescaleDir, pngFileName));
                            }
                        }

                        System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images rescaled.");
                    }
                }

                System.out.println("Rescaling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

                // Use rescale directory in the future
                imageDir = rescaleDir;
                rescaleDir = null;
                maskDir = null;
            }

            System.out.println("Loading images...");
            Date timestamp = new Date();

            // Read a single image to get the dimensions for the texture array
            File imageFile = IBRResources.findImageFile(new File(imageDir, viewSet.getImageFileName(0)));
            BufferedImage img = ImageIO.read(new FileInputStream(imageFile));
            viewTextures = context.getTextureFactory().build2DColorTextureArray(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount())
                .setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(true)
                .createTexture();

            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                imageFile = IBRResources.findImageFile(new File(imageDir, viewSet.getImageFileName(i)));

                if (maskDir == null)
                {
                    viewTextures.loadLayer(i, imageFile, true);
                }
                else
                {
                    File maskFile = IBRResources.findImageFile(new File(maskDir, viewSet.getImageFileName(i)));
                    viewTextures.loadLayer(i, imageFile, maskFile, true);
                }

                System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images loaded.");
            }

            System.out.println("Image loading completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

            System.out.println("Creating depth maps...");
            timestamp = new Date();

            // Build depth textures for each view
            int width = viewTextures.getWidth() / 2;
            int height = viewTextures.getHeight() / 2;
            depthTextures = context.getTextureFactory().build2DDepthTextureArray(width, height, viewSet.getCameraPoseCount()).createTexture();

            // Don't automatically generate any texture attachments for this framebuffer object
            try(FramebufferObject<ContextType> depthRenderingFBO = context.buildFramebufferObject(width, height).createFramebufferObject())
            {
                Drawable<ContextType> depthRenderable = context.createDrawable(depthRenderingProgram);
                depthRenderable.addVertexBuffer("position", positionBuffer);

                double minDepth = viewSet.getRecommendedFarPlane();

                // Render each depth texture
                for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
                {
                    depthRenderingFBO.setDepthAttachment(depthTextures.getLayerAsFramebufferAttachment(i));
                    depthRenderingFBO.clearDepthBuffer();

                    depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(i));
                    depthRenderingProgram.setUniform("projection",
                        viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
                            .getProjectionMatrix(
                                viewSet.getRecommendedNearPlane(),
                                viewSet.getRecommendedFarPlane()
                            )
                    );

                    depthRenderable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);

                    if (i == viewSet.getPrimaryViewIndex())
                    {
                        short[] depthBufferData = depthRenderingFBO.readDepthBuffer();
                        for (short encodedDepth : depthBufferData)
                        {
                            int nonlinearDepth = 0xFFFF & (int) encodedDepth;
                            minDepth = Math.min(minDepth, getLinearDepth((2.0 * nonlinearDepth) / 0xFFFF - 1.0,
                                viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                        }
                    }

                    //System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " depth maps created.");
                }

                System.out.println("Depth maps created in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

                return minDepth;
            }
        }
        else
        {
            return -1.0;
        }
    }

    public void fitLightSource(double avgDistance) throws IOException
    {
        if (!param.areLightSourcesInfinite())
        {
            if (param.isLightOffsetEstimationEnabled())
            {
                System.out.println("Beginning light fit...");

                Vector3 lightIntensity = new Vector3((float)(avgDistance * avgDistance));
                System.out.println("Using light intensity: " + lightIntensity.x + ' ' + lightIntensity.y + ' ' + lightIntensity.z);

                Date timestamp = new Date();

                NativeVectorBuffer lightPositionList = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, viewSet.getLightCount());
                NativeVectorBuffer lightIntensityList = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, viewSet.getLightCount());

                LightFit<ContextType> lightFit;

                if (param.isImagePreprojectionUseEnabled())
                {
                    lightFit = createTexSpaceLightFit(param.getTextureSize(), param.getTextureSubdivision());
                }
                else
                {
                    lightFit = createImgSpaceLightFit(param.getTextureSize(), param.getTextureSubdivision());
                }

                for (int i = 0; i < viewSet.getLightCount(); i++)
                {
                    System.out.println("Fitting light " + i + "...");

                    lightFit.fit(i);

                    Vector3 lightPosition = lightFit.getPosition();
                    //lightIntensity = lightFit.intensity();

                    System.out.println("Light position: " + lightPosition.x + ' ' + lightPosition.y + ' ' + lightPosition.z);
                    System.out.println("(Light intensity from fit: " + lightFit.getIntensity().x + ' ' + lightFit.getIntensity().y + ' ' + lightFit.getIntensity().z + ')');

                    lightPositionList.set(i, 0, lightPosition.x);
                    lightPositionList.set(i, 1, lightPosition.y);
                    lightPositionList.set(i, 2, lightPosition.z);
                    lightPositionList.set(i, 3, 1.0f);

                    lightIntensityList.set(i, 0, lightIntensity.x);
                    lightIntensityList.set(i, 1, lightIntensity.y);
                    lightIntensityList.set(i, 2, lightIntensity.z);

                    viewSet.setLightPosition(i, lightPosition);
                    viewSet.setLightIntensity(i, lightIntensity);
                }

                lightPositionBuffer = context.createUniformBuffer().setData(lightPositionList);
                lightIntensityBuffer = context.createUniformBuffer().setData(lightIntensityList);

                System.out.println("Light fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
            }
            else if (param.isLightIntensityEstimationEnabled())
            {
                NativeVectorBuffer lightIntensityList = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, viewSet.getLightCount());

                Vector3 lightIntensity = new Vector3((float)(avgDistance * avgDistance));
                System.out.println("Using light intensity: " + lightIntensity.x + ' ' + lightIntensity.y + ' ' + lightIntensity.z);

                for (int i = 0; i < viewSet.getLightCount(); i++)
                {
                    lightIntensityList.set(i, 0, lightIntensity.x);
                    lightIntensityList.set(i, 1, lightIntensity.y);
                    lightIntensityList.set(i, 2, lightIntensity.z);

                    viewSet.setLightIntensity(i, lightIntensity);
                }

                lightPositionBuffer = resources.lightPositionBuffer;
                lightIntensityBuffer = context.createUniformBuffer().setData(lightIntensityList);
            }
            else
            {
                System.out.println("Skipping light fit.");

                lightPositionBuffer = resources.lightPositionBuffer;
                lightIntensityBuffer =  resources.lightIntensityBuffer;
            }

//            if (!param.isImagePreprojectionUseEnabled())
//            {
//                System.out.println("Creating shadow maps...");
//                Date timestamp = new Date();
//
//                // Build shadow maps for each view
//                int width = param.getImageWidth(); //viewTextures.getWidth();
//                int height = param.getImageHeight(); //viewTextures.getHeight();
//                shadowTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
//
//                // Don't automatically generate any texture attachments for this framebuffer object
//                FramebufferObject<ContextType> shadowRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
//
//                Renderable<ContextType> shadowRenderable = context.createRenderable(depthRenderingProgram);
//                shadowRenderable.addVertexBuffer("position", positionBuffer);
//
//                // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
//                FloatVertexList flattenedShadowMatrices = new FloatVertexList(16, viewSet.getCameraPoseCount());
//
//                // Render each shadow map
//                for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
//                {
//                    shadowRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
//                    shadowRenderingFBO.clearDepthBuffer();
//
//                    Matrix4 modelView = Matrix4.lookAt(new Vector3(viewSet.getCameraPoseInverse(i).times(new Vector4(lightPosition, 1.0f))), center, new Vector3(0, 1, 0));
//                    depthRenderingProgram.setUniform("model_view", modelView);
//
//                    Matrix4 projection = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
//                            .getProjectionMatrix(
//                                viewSet.getRecommendedNearPlane(),
//                                viewSet.getRecommendedFarPlane() * SHADOW_MAP_FAR_PLANE_CUSHION // double it for good measure
//                            );
//                    depthRenderingProgram.setUniform("projection", projection);
//
//                    shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowRenderingFBO);
//
//                    Matrix4 fullTransform = projection.times(modelView);
//
//                    int d = 0;
//                    for (int col = 0; col < 4; col++) // column
//                    {
//                        for (int row = 0; row < 4; row++) // row
//                        {
//                            flattenedShadowMatrices.set(i, d, fullTransform.get(row, col));
//                            d++;
//                        }
//                    }
//                }
//
//                // Create the uniform buffer
//                shadowMatrixBuffer = context.createUniformBuffer().setData(flattenedShadowMatrices);
//
//                shadowRenderingFBO.delete();
//
//                System.out.println("Shadow maps created in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
//            }
        }
    }

    boolean loadMeshAndViewSet() throws IOException, XMLStreamException
    {
        System.out.println("Loading mesh...");
        Date timestamp = new Date();
        VertexGeometry mesh = loadMesh();

        System.out.println("Loading mesh completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        System.out.println("Loading view set...");
        timestamp = new Date();

        String[] vsetFileNameParts = vsetFile.getName().split("\\.");
        String fileExt = vsetFileNameParts[vsetFileNameParts.length-1];
        if ("vset".equalsIgnoreCase(fileExt))
        {
            System.out.println("Loading from VSET file.");

            viewSet = ViewSet.loadFromVSETFile(vsetFile);
        }
        else if ("xml".equalsIgnoreCase(fileExt))
        {
            System.out.println("Loading from Agisoft Photoscan XML file.");
            viewSet = ViewSet.loadFromAgisoftXMLFile(vsetFile);
            viewSet.setInfiniteLightSources(param.areLightSourcesInfinite());
        }
        else
        {
            System.out.println("Unrecognized file type, aborting.");
            return false;
        }

        viewSet.setTonemapping(param.getGamma(), param.getLinearLuminanceValues(), param.getEncodedLuminanceValues());

        // Only generate view set uniform buffers
        resources = IBRResources.getBuilderForContext(context)
            .useExistingViewSet(viewSet)
            .useExistingGeometry(mesh)
            .setLoadOptions(new SimpleLoadOptionsModel()
                .setColorImagesRequested(false)
                .setDepthImagesRequested(false))
            .create();

        System.out.println("Loading view set completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        return true;
    }
}
