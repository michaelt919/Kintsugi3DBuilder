package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.optimization.ErrorReport;
import tetzlaff.optimization.ShaderBasedOptimization;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * TODO: sketched out but not fully functional; may not be needed
 * @param <ContextType>
 */
public class RoughnessOptimizationIterative<ContextType extends Context<ContextType>>
        extends RoughnessOptimizationBase<ContextType>
{
    private final TextureFitSettings settings;

    private final ShaderBasedOptimization<ContextType> roughnessOptimization;
//    private Program<ContextType> errorCalcProgram;

    private final double convergenceTolerance;
    private final int unsuccessfulLMIterationsAllowed;

    /**
     *
     * @param basisResources Used for the initial estimate
     * @throws FileNotFoundException
     */
    public RoughnessOptimizationIterative(
        BasisResources<ContextType> basisResources, Supplier<Texture2D<ContextType>> getDiffuseTexture,
        double convergenceTolerance, int unsuccessfulLMIterationsAllowed)
            throws FileNotFoundException
    {
        // Inherit from base class to facilitate initial fit.
        super(basisResources);
        this.settings = basisResources.getTextureFitSettings();
        this.convergenceTolerance = convergenceTolerance;
        this.unsuccessfulLMIterationsAllowed = unsuccessfulLMIterationsAllowed;

        roughnessOptimization = new ShaderBasedOptimization<>(
            getRoughnessEstimationProgramBuilder(basisResources.context),
                basisResources.context.buildFramebufferObject(
                    basisResources.getTextureFitSettings().width, basisResources.getTextureFitSettings().height)
                // Reflectivity map
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                    .setLinearFilteringEnabled(true))
                // Roughness map
                .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB32F)
                        .setLinearFilteringEnabled(true))
                // Damping factor (R) and error (G) while fitting
                .addColorAttachment(ColorFormat.RG32F),
            program -> // Just use the rectangle as geometry
            {
                Drawable<ContextType> drawable = basisResources.context.createDrawable(program);
                drawable.addVertexBuffer("position", rect);
                drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
                return drawable;
            });

//        errorCalcProgram = createErrorCalcProgram(context);
//        Drawable<ContextType> errorCalcDrawable = context.createDrawable(errorCalcProgram);
//        errorCalcDrawable.addVertexBuffer("position", rect);

        roughnessOptimization.addSetupCallback((estimationProgram, backFramebuffer) ->
        {
            // Bind previous estimate textures to the shader
            estimationProgram.setTexture("diffuseEstimate", getDiffuseTexture.get());
            estimationProgram.setTexture("specularEstimate", getReflectivityTexture()); // front FBO, attachment 0
            estimationProgram.setTexture("roughnessEstimate", getRoughnessTexture()); // front FBO, attachment 1
            estimationProgram.setTexture("dampingTex", roughnessOptimization.getFrontFramebuffer().getColorAttachmentTexture(2));

            // Gamma correction constants
            float gamma = basisResources.getTextureFitSettings().gamma;
            estimationProgram.setUniform("gamma", gamma);
            estimationProgram.setUniform("gammaInv", 1.0f / gamma);

            // Clear framebuffer
            backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
            backFramebuffer.clearColorBuffer(2, 1.0f /* damping */, Float.MAX_VALUE /* error */, 0.0f, 0.0f);

            if (SpecularOptimization.DEBUG)
            {
                System.out.println("Optimizing roughness...");
            }
        });
    }

    @Override
    public void close()
    {
        roughnessOptimization.close();
//        errorCalcProgram.close();
    }

    @Override
    protected FramebufferObject<ContextType> getFramebuffer()
    {
        return roughnessOptimization.getFrontFramebuffer();
    }

    @Override
    public void execute()
    {
        // Generate initial estimate
        // Renders directly into "front" framebuffer which is fine for the first pass since then we don't have to swap
        super.execute();

        // Set damping factor to 1.0 initially at each position.
        roughnessOptimization.getFrontFramebuffer().clearColorBuffer(2, 1.0f, 1.0f, 1.0f, 1.0f);

        ErrorReport errorReport = new ErrorReport(settings.width * settings.height);

        // Estimate using the Levenberg-Marquardt algorithm.
        roughnessOptimization.runUntilConvergence(
            framebuffer ->
            {
                float[] dampingError = framebuffer.readFloatingPointColorBufferRGBA(2);
                errorReport.setError(IntStream.range(0, settings.width * settings.height)
                    .parallel()
                    .mapToDouble(p -> dampingError[4 * p + 1])
                    .sum());
                return errorReport;
            },
            convergenceTolerance, unsuccessfulLMIterationsAllowed);
    }

    private static <ContextType extends Context<ContextType>>
    ProgramBuilder<ContextType> getRoughnessEstimationProgramBuilder(
            ContextType context)
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/optimizeRoughness.frag"));
    }

    private static <ContextType extends Context<ContextType>>
    Program<ContextType> createErrorCalcProgram(ContextType context) throws FileNotFoundException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/basisToGGXErrorCalc.frag"))
            .createProgram();
    }
}
