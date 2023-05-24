package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.gl.core.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class RoughnessOptimizationBase<ContextType extends Context<ContextType>>
        implements RoughnessOptimization<ContextType>
{
    protected final SpecularFitSettings settings;
    protected final Program<ContextType> specularRoughnessFitProgram;
    protected final VertexBuffer<ContextType> rect;
    protected final Drawable<ContextType> specularRoughnessFitDrawable;

    protected RoughnessOptimizationBase(ContextType context, BasisResources<ContextType> resources, SpecularFitSettings settings)
        throws FileNotFoundException
    {
        this.settings = settings;

        // Fit specular parameters from weighted basis functions
        specularRoughnessFitProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/specularRoughnessFitNew.frag"))
                .define("BASIS_COUNT", settings.basisCount)
                .define("MICROFACET_DISTRIBUTION_RESOLUTION", settings.microfacetDistributionResolution)
                .createProgram();

        // Create basic rectangle vertex buffer
        rect = context.createRectangle();
        specularRoughnessFitDrawable = context.createDrawable(specularRoughnessFitProgram);
        specularRoughnessFitDrawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);

        // Set up shader program
        specularRoughnessFitDrawable.addVertexBuffer("position", rect);
        resources.useWithShaderProgram(specularRoughnessFitProgram);
        specularRoughnessFitProgram.setUniform("gamma", settings.additional.getFloat("gamma"));
        specularRoughnessFitProgram.setUniform("fittingGamma", 1.0f);

    }

    @Override
    public void clear()
    {
        // Set initial assumption for reflectivity / roughness
        getFramebuffer().clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
        getFramebuffer().clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    protected abstract FramebufferObject<ContextType> getFramebuffer();

    @Override
    public Texture2D<ContextType> getReflectivityTexture()
    {
        return getFramebuffer().getColorAttachmentTexture(0);
    }

    @Override
    public Texture2D<ContextType> getRoughnessTexture()
    {
        return getFramebuffer().getColorAttachmentTexture(1);
    }

    @Override
    public void execute()
    {
        // Fit specular so that we have a roughness estimate for masking/shadowing.
        getFramebuffer().clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        getFramebuffer().clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
        specularRoughnessFitDrawable.draw(getFramebuffer());
    }

    @Override
    public void saveTextures()
    {
        try
        {
            getFramebuffer().saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "specular.png"));
            getFramebuffer().saveColorBufferToFile(1, "PNG", new File(settings.outputDirectory, "roughness.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        specularRoughnessFitProgram.close();
        rect.close();
    }
}
