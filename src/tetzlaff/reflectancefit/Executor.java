package tetzlaff.reflectancefit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.core.*;

/**
 * The main entry point for the reflectance parameter fitting computation.
 * In the context of the user interface, this class contains the code that is run when the "Execute" button is pressed.
 * @param <ContextType>
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class Executor<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final File cameraFile;
    private final File modelFile;
    private final File imageDir;
    private final File maskDir;
    private final File rescaleDir;
    private final File outputDir;
    private final Options options;

    /**
     * The constructor for the Executor object.
     * @param context       A graphics context to use for GPU operations.
     * @param cameraFile    Either a VSET file or an XML camera definition file exported by PhotoScan using the “Export Cameras…” feature.
     *                      A VSET file is generated after running the reflectance parameter fitting.
     *                      Loading a VSET file instead of the original XML file can automatically initialize some settings (ColorChecker values and the primary view).
     * @param modelFile     A Wavefront OBJ file containing the geometry model (exported from PhotoScan using “Export Model…”).
     * @param imageDir      A directory containing undistorted images (exported from PhotoScan using “Undistort Photos…”).
     * @param maskDir       Optional.  A directory containing undistorted masks.
     *                      Masks can be used to restrict which pixels in an image will be used.
     *                      If this directory is not provided, the alpha channel of the images will be used as a mask if it exists.
     * @param rescaleDir    Optional.  A directory where rescaled images will be saved for future re-use if “Rescale Images” is enabled.
     *                      If you set this, you can use this directory for “Images” in the future to make processing on the same dataset finish faster.
     * @param outputDir     The directory where the final textures will be stored.
     * @param options       An object containing options for controlling aspects of the parameter fitting computation.
     */
    public Executor(ContextType context, File cameraFile, File modelFile, File imageDir, File maskDir, File rescaleDir, File outputDir, Options options)
    {
        this.context = context;
        this.cameraFile = cameraFile;
        this.modelFile = modelFile;
        this.imageDir = imageDir;
        this.maskDir = maskDir;
        this.rescaleDir = rescaleDir;
        this.outputDir = outputDir;
        this.options = options;
    }

    /**
     * Runs the main reflectance parameter fitting computation.
     * @throws IOException
     * @throws XMLStreamException
     */
    public void execute() throws IOException, XMLStreamException
    {
        try(ParameterFittingResources<ContextType> resources = new ParameterFittingResources<>(context, cameraFile, modelFile, imageDir, maskDir, rescaleDir, options))
        {
            System.out.println("Max vertex uniform components across all blocks:" + context.getState().getMaxCombinedVertexUniformComponents());
            System.out.println("Max fragment uniform components across all blocks:" + context.getState().getMaxCombinedFragmentUniformComponents());
            System.out.println("Max size of a uniform block in bytes:" + context.getState().getMaxUniformBlockSize());
            System.out.println("Max texture array layers:" + context.getState().getMaxArrayTextureLayers());

            resources.loadMeshAndViewSet();

            outputDir.mkdirs();

            if(!resources.getViewSet().hasCustomLuminanceEncoding())
            {
                System.out.println("WARNING: no luminance mapping found.  Reflectance values are not physically grounded.");
            }

            context.getState().enableDepthTest();
            context.getState().disableBackFaceCulling();

            resources.compileShaders();

            System.out.println("Primary view: " + options.getPrimaryViewName());
            resources.getViewSet().setPrimaryView(options.getPrimaryViewName());
            System.out.println("Primary view index: " + resources.getViewSet().getPrimaryViewIndex());

            // Load textures, generate visibility depth textures, estimate light source intensity
            double avgDistance = resources.loadTextures();
            resources.estimateLightIntensity(avgDistance);

            resources.getViewSet().setGeometryFileName(modelFile.getName());
            Path relativePathToRescaledImages = outputDir.toPath().relativize(imageDir.toPath());
            resources.getViewSet().setRelativeImagePathName(relativePathToRescaledImages.toString());

            try(FileOutputStream outputStream = new FileOutputStream(new File(outputDir, cameraFile.getName().split("\\.")[0] + ".vset")))
            {
                resources.getViewSet().writeVSETFileToStream(outputStream);
                outputStream.flush();
            }

            if (options.isDiffuseTextureEnabled() || options.isSpecularTextureEnabled())
            {
                File objFileCopy = new File(outputDir, modelFile.getName());
                Files.copy(modelFile.toPath(), objFileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);

                ParameterFittingResult result = new ParameterFitting<>(context, resources, options).fit();

                System.out.println("Saving textures...");
                Date timestamp = new Date();

                result.writeToFiles(new File(outputDir, resources.getMaterialFileName()), resources.getMaterialName());

                System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
            }
        }
    }
}
