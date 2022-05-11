package tetzlaff.ibrelight.export.PTMfit;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.util.ShaderHoleFill;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TangentSpaceWeightsToObjectSpace <ContextType extends Context<ContextType>>
{
    private final IBRResources<ContextType> resources;
    private final TextureFitSettings settings;

    public TangentSpaceWeightsToObjectSpace(IBRResources<ContextType> resources, TextureFitSettings settings)
    {
        this.resources = resources;
        this.settings = settings;
    }

    public void run(PTMsolution solutions, ProgramBuilder<ContextType> programBuilder, int weightStart, int weightCount)
    {
        try (Program<ContextType> program = programBuilder.createProgram();
             FramebufferObject<ContextType> fbo = resources.context.buildFramebufferObject(settings.width, settings.height)
                 .addColorAttachments(ColorFormat.RGBA8, weightCount)
                 .createFramebufferObject();
             FramebufferObject<ContextType> fbo2 = resources.context.buildFramebufferObject(settings.width, settings.height)
                     .addColorAttachments(ColorFormat.RGBA8, weightCount)
                     .createFramebufferObject();
             Texture3D<ContextType> weightMaps =
                     resources.context.getTextureFactory().build2DColorTextureArray(settings.width, settings.height, 10)
                     .setInternalFormat(ColorFormat.RGBA32F)
                     .setLinearFilteringEnabled(true)
                     .setMipmapsEnabled(false)
                     .createTexture())
        {
            // Run the reconstruction and save the results to file
            NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
            NativeVectorBuffer weightMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, settings.width * settings.height);
            for (int p = 0; p < settings.width * settings.height; p++)
            {
                weightMapBuffer.set(p, 3, /*solutions.areWeightsValid(p) ? 1.0 : 0.0*/ 1.0);
            }

            for (int b = 0; b < solutions.getPTMmodel().getBasisFunctionCount(); b++)
            {
                // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
                for (int p = 0; p < settings.width * settings.height; p++)
                {
                    weightMapBuffer.set(p, 0, solutions.getWeights(p).get(b));
                }
                for (int p =settings.width * settings.height ; p < settings.width * settings.height*2; p++)
                {
                    weightMapBuffer.set(p%(settings.width * settings.height), 1, solutions.getWeights(p).get(b));
                }
                for (int p = settings.width * settings.height*2; p < settings.width * settings.height*3; p++)
                {
                    weightMapBuffer.set(p%(settings.width * settings.height), 2, solutions.getWeights(p).get(b));
                }

                // Immediately load the weight map so that we can reuse the local memory buffer.
                weightMaps.loadLayer(b, weightMapBuffer);

            }

            program.setTexture("weightMaps", weightMaps);
            Drawable<ContextType> drawable = resources.createDrawable(program);
            for (int i = 0; i < weightCount; i++)
            {
                fbo.clearColorBuffer(i, 0, 0, 0, 0);
            }
            drawable.draw(PrimitiveMode.TRIANGLES, fbo);
            FramebufferObject<ContextType> finalFBO = new ShaderHoleFill<>(resources.context).execute(fbo, fbo2);
            saveToFile(finalFBO, weightStart, weightCount);
        }

        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }
    private void saveToFile(Framebuffer<ContextType> framebuffer, int weightStart, int weightCount)
    {
        try
        {
            for (int i = 0; i < weightCount; i++)
            {
                String filename = String.format("objWeights%02d.png", weightStart + i);
                framebuffer.saveColorBufferToFile(i, "PNG",
                        new File(settings.outputDirectory, filename));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
