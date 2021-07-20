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
//    public final Texture2D<ContextType> weightMask;
//    public final Texture2D<ContextType> basisMaps;
//    public final UniformBuffer<ContextType> diffuseUniformBuffer;



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

//        weightMask = context.getTextureFactory().build2DColorTexture(settings.width, settings.height)
//                .setInternalFormat(ColorFormat.R8)
//                .setLinearFilteringEnabled(true)
//                .setMipmapsEnabled(false)
//                .createTexture();

//        basisMaps = context.getTextureFactory().build1DColorTextureArray(
//                91,8)
//                .setInternalFormat(ColorFormat.RGB32F)
//                .setLinearFilteringEnabled(true)
//                .setMipmapsEnabled(false)
//                .createTexture();
//
//        diffuseUniformBuffer = context.createUniformBuffer();
//
//        weightMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None, TextureWrapMode.None);
//        basisMaps.setTextureWrap(TextureWrapMode.None, TextureWrapMode.None);
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
//                    program.setTexture("basisFunctions", this.basisMaps);
                    program.setTexture("weightMaps", this.weightMaps);
//                    program.setTexture("weightMask", this.weightMask);
//                    program.setUniformBuffer("DiffuseColors", this.diffuseUniformBuffer);
                }
                )


        )
        {
            // Run the reconstruction and save the results to file
            reconstruction.execute((k, framebuffer) -> saveReconstructionToFile(directoryName, k, framebuffer));
            NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
            NativeVectorBuffer weightMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 1, settings.width * settings.height);
            NativeVectorBuffer basisMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3, 8 * (90 + 1));
            NativeVectorBuffer diffuseNativeBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, 8);
            for (int p = 0; p < settings.width * settings.height; p++)
            {
                weightMapBuffer.set(p, 0, solutions.areWeightsValid(p) ? 1.0 : 0.0);
            }
//            weightMask.load(weightMapBuffer);

            for (int b = 0; b < 8; b++)
            {
                // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
                for (int p = 0; p < settings.width * settings.height; p++)
                {
                    weightMapBuffer.set(p, 0, solutions.getWeights(p).get(b));
                }

                // Immediately load the weight map so that we can reuse the local memory buffer.
                weightMaps.loadLayer(b, weightMapBuffer);

                // Copy basis functions by color channel into the basis map buffer that will eventually be sent to the GPU..
                for (int m = 0; m <= 90; m++)
                {
                    // Format necessary for OpenGL is essentially transposed from the storage in the solution vectors.
                    basisMapBuffer.set(m + (90 + 1) * b, 0, new SimpleMatrix(91, 8, DMatrixRMaj.class).get(m, b));
                    basisMapBuffer.set(m + (90 + 1) * b, 1, new SimpleMatrix(91, 8, DMatrixRMaj.class).get(m, b));
                    basisMapBuffer.set(m + (90 + 1) * b, 2, new SimpleMatrix(91, 8, DMatrixRMaj.class).get(m, b));
                }

                // Store each channel of the diffuse albedo in the local buffer.
                diffuseNativeBuffer.set(b, 0, solutions.getDiffuseAlbedo(b).x);
                diffuseNativeBuffer.set(b, 1, solutions.getDiffuseAlbedo(b).y);
                diffuseNativeBuffer.set(b, 2, solutions.getDiffuseAlbedo(b).z);
                diffuseNativeBuffer.set(b, 3, 1.0f);
            }

            // Send the basis functions to the GPU.
//            basisMaps.load(basisMapBuffer);

            // Send the diffuse albedos to the GPU.
//            diffuseUniformBuffer.setData(diffuseNativeBuffer);

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
