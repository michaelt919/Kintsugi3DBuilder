package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

import tetzlaff.gl.*;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.RenderingMode;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

abstract class RenderRequestBase implements IBRRequest
{
    private static final File TEX_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "texspace_noscale.vert").toFile();
    private static final File IMG_SPACE_VERTEX_SHADER = Paths.get("shaders", "common", "imgspace.vert").toFile();

    private final int width;
    private final int height;
    private final File vertexShader;
    private final File fragmentShader;
    private final ReadonlySettingsModel settingsModel;
    private final File outputDirectory;

    RenderRequestBase(int width, int height, ReadonlySettingsModel settingsModel, File vertexShader, File fragmentShader, File outputDirectory)
    {
        this.width = width;
        this.height = height;
        this.settingsModel = settingsModel;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.outputDirectory = outputDirectory;
    }

    abstract static class BuilderBase implements RenderRequestBuilder
    {
        private final ReadonlySettingsModel settingsModel;
        private final File fragmentShader;
        private final File outputDirectory;

        private int width = 1024;
        private int height = 1024;
        private File vertexShader = TEX_SPACE_VERTEX_SHADER;

        BuilderBase(ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            this.settingsModel = settingsModel;
            this.fragmentShader = fragmentShader;
            this.outputDirectory = outputDirectory;
        }

        protected int getWidth()
        {
            return width;
        }

        protected int getHeight()
        {
            return height;
        }

        protected ReadonlySettingsModel getSettingsModel()
        {
            return settingsModel;
        }

        protected File getVertexShader()
        {
            return vertexShader;
        }

        protected File getFragmentShader()
        {
            return fragmentShader;
        }

        protected File getOutputDirectory()
        {
            return outputDirectory;
        }

        @Override
        public RenderRequestBuilder useTextureSpaceVertexShader()
        {
            this.vertexShader = TEX_SPACE_VERTEX_SHADER;
            return this;
        }

        @Override
        public RenderRequestBuilder useCameraSpaceVertexShader()
        {
            this.vertexShader = IMG_SPACE_VERTEX_SHADER;
            return this;
        }

        @Override
        public RenderRequestBuilder useCustomVertexShader(File vertexShader)
        {
            this.vertexShader = vertexShader;
            return this;
        }

        @Override
        public RenderRequestBuilder setWidth(int width)
        {
            this.width = width;
            return this;
        }

        @Override
        public RenderRequestBuilder setHeight(int height)
        {
            this.height = height;
            return this;
        }
    }

    protected <ContextType extends Context<ContextType>>
        Program<ContextType> createProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, vertexShader)
            .addShader(ShaderType.FRAGMENT, fragmentShader)
            .createProgram();

        resources.setupShaderProgram(program, this.settingsModel.get("renderingMode", RenderingMode.class));

        program.setUniform("renderGamma", this.settingsModel.getFloat("gamma"));
        program.setUniform("weightExponent", this.settingsModel.getFloat("weightExponent"));
        program.setUniform("isotropyFactor", this.settingsModel.getFloat("isotropyFactor"));
        program.setUniform("occlusionEnabled", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"));
        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));
        program.setUniform("imageBasedRenderingEnabled", this.settingsModel.get("renderingMode", RenderingMode.class).isImageBased());
        program.setUniform("relightingEnabled", this.settingsModel.getBoolean("relightingEnabled"));
        program.setUniform("pbrGeometricAttenuationEnabled", this.settingsModel.getBoolean("pbrGeometricAttenuationEnabled"));
        program.setUniform("fresnelEnabled", this.settingsModel.getBoolean("fresnelEnabled"));

        return program;
    }

    protected <ContextType extends Context<ContextType>> Framebuffer<ContextType> createFramebuffer(ContextType context)
    {
        return context.buildFramebufferObject(width, height)
            .addColorAttachment()
            .createFramebufferObject();
    }

    protected static <ContextType extends Context<ContextType>> Drawable<ContextType>
        createDrawable(Program<ContextType> program, IBRResources<ContextType> resources)
    {
        Drawable<ContextType> drawable = program.getContext().createDrawable(program);
        drawable.addVertexBuffer("position", resources.positionBuffer);
        drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        drawable.addVertexBuffer("normal", resources.normalBuffer);
        drawable.addVertexBuffer("tangent", resources.tangentBuffer);
        return drawable;
    }

    protected static <ContextType extends Context<ContextType>> void render(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        drawable.getContext().getState().disableBackFaceCulling();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }
}
