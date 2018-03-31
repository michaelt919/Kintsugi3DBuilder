package tetzlaff.ibrelight.export.btf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.RenderingMode;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class BTFRequest implements IBRRequest
{
    private final int width;
    private final int height;
    private final File exportPath;
    private final ReadonlySettingsModel settings;
    private final Vector3 lightColor;
    private List<Integer> viewIndices;

    public BTFRequest(int width, int height, File exportPath, ReadonlySettingsModel settings, Vector3 lightColor)
    {
        this.width = width;
        this.height = height;
        this.exportPath = exportPath;
        this.settings = settings;
        this.lightColor = lightColor;
    }

    public void setViewIndices(List<Integer> viewIndices)
    {
        this.viewIndices = new ArrayList<>(viewIndices);
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
        throws IOException
    {
        IBRResources<ContextType> resources = renderable.getResources();
        ContextType context = resources.context;

        ProgramBuilder<ContextType> programBuilder =
            resources.getIBRShaderProgramBuilder(this.settings.get("renderingMode", RenderingMode.class))
                .define("BRDF_MODE", true)
                .define("VIRTUAL_LIGHT_COUNT", 1)
                .define("PHYSICALLY_BASED_MASKING_SHADOWING", this.settings.getBoolean("pbrGeometricAttenuationEnabled"))
                .define("FRESNEL_EFFECT_ENABLED", this.settings.getBoolean("fresnelEnabled"))
                .define("RELIGHTING_ENABLED", this.settings.getBoolean("relightingEnabled"))
                .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && this.settings.getBoolean("occlusionEnabled"))
                .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && this.settings.getBoolean("occlusionEnabled"))
                .define("SHADOWS_ENABLED", false)
                .define("MIPMAPS_ENABLED", false)
                .define("TANGENT_SPACE_OVERRIDE_ENABLED", true)
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"));

        if (viewIndices != null)
        {
            programBuilder.define("USE_VIEW_INDICES", true);
            programBuilder.define("VIEW_COUNT", viewIndices.size());
        }
        else
        {
            programBuilder.define("USE_VIEW_INDICES", false);
        }

        try(Program<ContextType> btfProgram = programBuilder.createProgram();
            FramebufferObject<ContextType> framebuffer = context.buildFramebufferObject(width, height)
                .addColorAttachment()
                .createFramebufferObject();
            UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer())
        {
            Drawable<ContextType> drawable = context.createDrawable(btfProgram);
            drawable.addVertexBuffer("position", resources.positionBuffer);
            drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            drawable.addVertexBuffer("normal", resources.normalBuffer);
            drawable.addVertexBuffer("tangent", resources.tangentBuffer);

            resources.setupShaderProgram(btfProgram);

            if (viewIndices != null)
            {
                NativeVectorBuffer viewIndexData = NativeVectorBufferFactory.getInstance()
                    .createEmpty(NativeDataType.INT, 1, viewIndices.size());

                for (int i = 0; i < viewIndices.size(); i++)
                {
                    viewIndexData.set(i, 0, viewIndices.get(i));
                }

                viewIndexBuffer.setData(viewIndexData);
                btfProgram.setUniformBuffer("ViewIndices", viewIndexBuffer);
            }

            btfProgram.setUniform("renderGamma", this.settings.getFloat("gamma"));
            btfProgram.setUniform("weightExponent", this.settings.getFloat("weightExponent"));
            btfProgram.setUniform("isotropyFactor", this.settings.getFloat("isotropyFactor"));
            btfProgram.setUniform("occlusionBias", this.settings.getFloat("occlusionBias"));

            btfProgram.setUniform("useEnvironmentTexture", false);
            btfProgram.setTexture("environmentMap", context.getTextureFactory().getNullTexture(SamplerType.FLOAT_CUBE_MAP));
            btfProgram.setUniform("ambientColor", Vector3.ZERO);

            ////////////////////////////////

//            // Backscattering
//            for (int i = 1; i <= 179; i++)
//            {
//                double theta = i / 180.0f * Math.PI;
//                btfProgram.setUniform("lightIntensityVirtual[0]", lightColor);
//                btfProgram.setUniform("lightDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));
//                btfProgram.setUniform("viewDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));

            // Joey's lab
            for (int i = 1; i <= 90; i++)
            {
                double theta = i / 180.0f * Math.PI;
                btfProgram.setUniform("lightIntensityVirtual[0]", lightColor);

                btfProgram.setUniform("lightDirTSOverride", new Vector3((float)(Math.sin(theta)*Math.sqrt(0.5)), -(float)(Math.sin(theta)*Math.sqrt(0.5)), (float)Math.cos(theta)));
                btfProgram.setUniform("viewDirTSOverride", new Vector3(-(float)(Math.cos(theta)*Math.sqrt(0.5)), (float)(Math.cos(theta)*Math.sqrt(0.5)), (float)Math.sin(theta)));

//                btfProgram.setUniform("lightDirTSOverride", new Vector3(-(float)Math.sin(theta), 0.0f, (float)Math.cos(theta)));
//                btfProgram.setUniform("viewDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));

            ////////////////////////////////

                context.getState().disableBackFaceCulling();

                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

                File exportFile = new File(exportPath, String.format("%02d.png", i));
                exportFile.getParentFile().mkdirs();
                framebuffer.saveColorBufferToFile(0, "PNG", exportFile);

                if (callback != null)
                {
                    callback.setProgress((double) i / (double) /*90*/180);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}
