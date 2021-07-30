package tetzlaff.ibrelight.export.PTMfit;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.IBRResources;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.rendering.ImageReconstruction;
public class PTMReconstruction <ContextType extends Context<ContextType>>{
    private final IBRResources<ContextType> resources;
    private final TextureFitSettings settings;
    private final int imageWidth;
    private final int imageHeight;
    public final Texture3D<ContextType> weightMaps;


    public PTMReconstruction(IBRResources<ContextType> resources,TextureFitSettings settings) {
        this.settings = settings;
        this.resources=resources;
        Projection defaultProj = resources.viewSet.getCameraProjection(resources.viewSet.getCameraProjectionIndex(
                resources.viewSet.getPrimaryViewIndex()));

        if (defaultProj.getAspectRatio() < 1.0)
        {
            imageWidth = settings.width;
            imageHeight = Math.round(imageWidth / defaultProj.getAspectRatio());
        }
        else
        {
            imageHeight = settings.height;
            imageWidth = Math.round(imageHeight * defaultProj.getAspectRatio());
        }
        ContextType context=resources.context;
        weightMaps = context.getTextureFactory().build2DColorTextureArray(settings.width, settings.height, 6)
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture();
    }

    public void reconstruct(PTMsolution solutions,ProgramBuilder<ContextType> programBuilder, String directoryName){
        new File(settings.outputDirectory, directoryName).mkdir();
        try (
                ImageReconstruction<ContextType> reconstruction = new ImageReconstruction<>(
                resources,
                programBuilder,
                resources.context.buildFramebufferObject(imageWidth, imageHeight)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addDepthAttachment(),
                program ->
                {
                    resources.setupShaderProgram(program);
                    program.setTexture("weightMaps", this.weightMaps);
                }
                )



        )
        {
            // Run the reconstruction and save the results to file

            reconstruction.execute((k, framebuffer) -> saveReconstructionToFile(directoryName, k, framebuffer));
            NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
            NativeVectorBuffer weightMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 1, settings.width * settings.height);
            for (int p = 0; p < settings.width * settings.height; p++)
            {
                weightMapBuffer.set(p, 0, solutions.areWeightsValid(p) ? 1.0 : 0.0);
            }


            for (int b = 0; b < 6; b++)
            {
                // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
                for (int p = 0; p < settings.width * settings.height; p++)
                {
                    weightMapBuffer.set(p, 0, solutions.getWeights(p).get(b));
                }

                // Immediately load the weight map so that we can reuse the local memory buffer.
                weightMaps.loadLayer(b, weightMapBuffer);

            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }
    private void saveReconstructionToFile(String directoryName, int k, Framebuffer<ContextType> framebuffer)
    {
        try
        {
            String filename = String.format("%04d.png", k);
            framebuffer.saveColorBufferToFile(0, "PNG",
                    new File(new File(settings.outputDirectory, directoryName), filename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
