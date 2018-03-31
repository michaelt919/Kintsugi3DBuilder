package tetzlaff.ibrelight.export.fidelity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class TextureFitFidelityTechnique<ContextType extends Context<ContextType>> implements FidelityEvaluationTechnique<ContextType>
{
    private IBRResources<ContextType> resources;
    private final boolean usePerceptuallyLinearError;

    private FramebufferObject<ContextType> textureFitFramebuffer;

    private FramebufferObject<ContextType> textureFitBaselineFramebuffer;

    private Texture2D<ContextType> maskTexture;

    private Program<ContextType> fidelityProgram;
    private Drawable<ContextType> fidelityDrawable;
    private FramebufferObject<ContextType> fidelityFramebuffer;

    private NativeVectorBuffer viewIndexData;

    public TextureFitFidelityTechnique(boolean usePerceptuallyLinearError)
    {
        this.usePerceptuallyLinearError = usePerceptuallyLinearError;
    }

    @Override
    public boolean isGuaranteedInterpolating()
    {
        return false;
    }

    @Override
    public boolean isGuaranteedMonotonic()
    {
        return false;
    }

    @Override
    public void initialize(IBRResources<ContextType> resources, ReadonlySettingsModel settings, int size) throws IOException
    {
        this.resources = resources;

        resources.context.getState().disableBackFaceCulling();

        fidelityProgram = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/texturefit/fidelity.frag"))
            .createProgram();

        fidelityFramebuffer = resources.context.buildFramebufferObject(size, size)
            .addColorAttachment(ColorFormat.RG32F)
            .createFramebufferObject();

        fidelityDrawable = resources.context.createDrawable(fidelityProgram);
        fidelityDrawable.addVertexBuffer("position", resources.positionBuffer);
        fidelityDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        fidelityDrawable.addVertexBuffer("normal", resources.normalBuffer);
        fidelityDrawable.addVertexBuffer("tangent", resources.tangentBuffer);

        textureFitFramebuffer = resources.context.buildFramebufferObject(size, size)
            .addColorAttachments(ColorFormat.RGBA8, 4)
            .createFramebufferObject();

        textureFitBaselineFramebuffer = resources.context.buildFramebufferObject(size, size)
            .addColorAttachments(ColorFormat.RGBA8, 4)
            .createFramebufferObject();

        try(Program<ContextType> textureFitBaselineProgram = resources.getIBRShaderProgramBuilder()
            .define("VIEW_COUNT", resources.viewSet.getCameraPoseCount())
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/texturefit/specularfit_imgspace.frag"))
            .createProgram())
        {
            Drawable<ContextType> textureFitBaselineDrawable = resources.context.createDrawable(textureFitBaselineProgram);
            textureFitBaselineDrawable.addVertexBuffer("position", resources.positionBuffer);
            textureFitBaselineDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            textureFitBaselineDrawable.addVertexBuffer("normal", resources.normalBuffer);
            textureFitBaselineDrawable.addVertexBuffer("tangent", resources.tangentBuffer);

            // Baseline
            resources.setupShaderProgram(textureFitBaselineDrawable.program());

            if (this.usePerceptuallyLinearError)
            {
                textureFitBaselineDrawable.program().setUniform("fittingGamma", 2.2f);
            }
            else
            {
                textureFitBaselineDrawable.program().setUniform("fittingGamma", 1.0f);
            }

            textureFitBaselineDrawable.program().setUniform("standaloneMode", true);

            textureFitBaselineFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
            textureFitBaselineFramebuffer.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f);
            textureFitBaselineFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 1.0f);
            textureFitBaselineFramebuffer.clearColorBuffer(3, 1.0f, 1.0f, 1.0f, 1.0f);
            textureFitBaselineFramebuffer.clearDepthBuffer();

            textureFitBaselineDrawable.draw(PrimitiveMode.TRIANGLES, textureFitBaselineFramebuffer);
        }

        viewIndexData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.INT, 1, resources.viewSet.getCameraPoseCount());
    }

    @Override
    public void setMask(File maskFile) throws IOException
    {
        if (this.maskTexture != null)
        {
            this.maskTexture.close();
            this.maskTexture = null;
        }

        if (maskFile != null)
        {
            this.maskTexture = resources.context.getTextureFactory().build2DColorTextureFromFile(maskFile, true)
                    .setInternalFormat(ColorFormat.R8)
                    .setLinearFilteringEnabled(false)
                    .setMipmapsEnabled(false)
                    .createTexture();
        }
    }

    @Override
    public void updateActiveViewIndexList(List<Integer> activeViewIndexList)
    {
        for (int i = 0; i < activeViewIndexList.size(); i++)
        {
            viewIndexData.set(i, 0, activeViewIndexList.get(i));
        }

        try (Program<ContextType> textureFitProgram = resources.getIBRShaderProgramBuilder()
                .define("USE_VIEW_INDICES", true)
                .define("VIEW_COUNT", activeViewIndexList.size())
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/texturefit/specularfit_imgspace.frag"))
                .createProgram();
            UniformBuffer<ContextType> viewIndexBuffer = resources.context.createUniformBuffer().setData(viewIndexData))
        {
            Drawable<ContextType> textureFitDrawable = resources.context.createDrawable(textureFitProgram);
            textureFitDrawable.addVertexBuffer("position", resources.positionBuffer);
            textureFitDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            textureFitDrawable.addVertexBuffer("normal", resources.normalBuffer);
            textureFitDrawable.addVertexBuffer("tangent", resources.tangentBuffer);

            resources.setupShaderProgram(textureFitDrawable.program());
            textureFitDrawable.program().setUniformBuffer("ViewIndices", viewIndexBuffer);

            if (this.usePerceptuallyLinearError)
            {
                textureFitDrawable.program().setUniform("fittingGamma", 2.2f);
            }
            else
            {
                textureFitDrawable.program().setUniform("fittingGamma", 1.0f);
            }

            textureFitDrawable.program().setUniform("standaloneMode", true);

            textureFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
            textureFitFramebuffer.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f);
            textureFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 1.0f);
            textureFitFramebuffer.clearColorBuffer(3, 1.0f, 1.0f, 1.0f, 1.0f);
            textureFitFramebuffer.clearDepthBuffer();

            textureFitDrawable.draw(PrimitiveMode.TRIANGLES, textureFitFramebuffer);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public double evaluateBaselineError(int targetViewIndex, File debugFile)
    {
        resources.setupShaderProgram(fidelityDrawable.program());

        fidelityDrawable.program().setUniform("model_view", resources.viewSet.getCameraPose(targetViewIndex));
        fidelityDrawable.program().setUniform("targetViewIndex", targetViewIndex);

        if (this.usePerceptuallyLinearError)
        {
            fidelityDrawable.program().setUniform("fittingGamma", 2.2f);
        }
        else
        {
            fidelityDrawable.program().setUniform("fittingGamma", 1.0f);
        }

        fidelityDrawable.program().setUniform("evaluateInXYZ", false);

        fidelityDrawable.program().setUniform("useMaskTexture", this.maskTexture != null);
        fidelityDrawable.program().setTexture("maskTexture", this.maskTexture);

        fidelityDrawable.program().setTexture("normalEstimate", textureFitBaselineFramebuffer.getColorAttachmentTexture(1));
        fidelityDrawable.program().setTexture("specularEstimate", textureFitBaselineFramebuffer.getColorAttachmentTexture(2));
        fidelityDrawable.program().setTexture("roughnessEstimate", textureFitBaselineFramebuffer.getColorAttachmentTexture(3));

        fidelityFramebuffer.clearColorBuffer(0, -1.0f, -1.0f, -1.0f, -1.0f);
        fidelityFramebuffer.clearDepthBuffer();

        fidelityDrawable.draw(PrimitiveMode.TRIANGLES, fidelityFramebuffer);

        double baselineSumSqError = 0.0;
        //double sumWeights = 0.0;
        double baselineSumMask = 0.0;

        float[] baselineFidelityArray = fidelityFramebuffer.readFloatingPointColorBufferRGBA(0);
        for (int k = 0; 4 * k + 3 < baselineFidelityArray.length; k++)
        {
            if (baselineFidelityArray[4 * k + 1] >= 0.0f)
            {
                baselineSumSqError += baselineFidelityArray[4 * k];
                //sumWeights += baselineFidelityArray[4 * k + 1];
                baselineSumMask += 1.0;
            }
        }

        double baselineError = Math.sqrt(baselineSumSqError / baselineSumMask);

        if (debugFile != null)
        {
            try
            {
                fidelityFramebuffer.saveColorBufferToFile(0, "PNG", debugFile);

                textureFitBaselineFramebuffer.saveColorBufferToFile(0, "PNG", new File(debugFile.getParentFile(), "baseline_diffuse.png"));
                textureFitBaselineFramebuffer.saveColorBufferToFile(1, "PNG", new File(debugFile.getParentFile(), "baseline_normal.png"));
                textureFitBaselineFramebuffer.saveColorBufferToFile(2, "PNG", new File(debugFile.getParentFile(), "baseline_specular.png"));
                textureFitBaselineFramebuffer.saveColorBufferToFile(3, "PNG", new File(debugFile.getParentFile(), "baseline_roughness.png"));
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        return baselineError;
    }

    @Override
    public double evaluateError(int targetViewIndex, File debugFile)
    {
        resources.setupShaderProgram(fidelityDrawable.program());

        fidelityDrawable.program().setUniform("model_view", resources.viewSet.getCameraPose(targetViewIndex));
        fidelityDrawable.program().setUniform("targetViewIndex", targetViewIndex);

        if (this.usePerceptuallyLinearError)
        {
            fidelityDrawable.program().setUniform("fittingGamma", 2.2f);
        }
        else
        {
            fidelityDrawable.program().setUniform("fittingGamma", 1.0f);
        }

        fidelityDrawable.program().setUniform("evaluateInXYZ", false);

        fidelityDrawable.program().setUniform("useMaskTexture", this.maskTexture != null);
        fidelityDrawable.program().setTexture("maskTexture", this.maskTexture);

        fidelityDrawable.program().setTexture("normalEstimate", textureFitFramebuffer.getColorAttachmentTexture(1));
        fidelityDrawable.program().setTexture("specularEstimate", textureFitFramebuffer.getColorAttachmentTexture(2));
        fidelityDrawable.program().setTexture("roughnessEstimate", textureFitFramebuffer.getColorAttachmentTexture(3));

        fidelityFramebuffer.clearColorBuffer(0, -1.0f, -1.0f, -1.0f, -1.0f);
        fidelityFramebuffer.clearDepthBuffer();

        fidelityDrawable.draw(PrimitiveMode.TRIANGLES, fidelityFramebuffer);

        if (debugFile != null)
        {
            File pngDebugFile;

            if (debugFile.getName().endsWith(".png"))
            {
                pngDebugFile = debugFile;
            }
            else
            {
                pngDebugFile = new File(debugFile + ".png");
            }

            try
            {
                fidelityFramebuffer.saveColorBufferToFile(0, "PNG", pngDebugFile);

                textureFitFramebuffer.saveColorBufferToFile(0, "PNG",
                        new File(pngDebugFile.getParentFile(), "diffuse_" + pngDebugFile.getName()));
                textureFitFramebuffer.saveColorBufferToFile(1, "PNG",
                        new File(pngDebugFile.getParentFile(), "normal_" + pngDebugFile.getName()));
                textureFitFramebuffer.saveColorBufferToFile(2, "PNG",
                        new File(pngDebugFile.getParentFile(), "specular_" + pngDebugFile.getName()));
                textureFitFramebuffer.saveColorBufferToFile(3, "PNG",
                        new File(pngDebugFile.getParentFile(), "roughness_" + pngDebugFile.getName()));
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        double sumSqError = 0.0;
        //double sumWeights = 0.0;
        double sumMask = 0.0;

        float[] fidelityArray = fidelityFramebuffer.readFloatingPointColorBufferRGBA(0);
        for (int k = 0; 4 * k + 3 < fidelityArray.length; k++)
        {
            if (fidelityArray[4 * k + 1] >= 0.0f)
            {
                sumSqError += fidelityArray[4 * k];
                //sumWeights += fidelityArray[4 * k + 1];
                sumMask += 1.0;
            }
        }

        double renderError = Math.sqrt(sumSqError / sumMask);
        return renderError;
    }

    @Override
    public void close()
    {
        if (textureFitFramebuffer != null)
        {
            textureFitFramebuffer.close();
        }

        if (fidelityProgram != null)
        {
            fidelityProgram.close();
        }

        if (fidelityFramebuffer != null)
        {
            fidelityFramebuffer.close();
        }
    }
}
