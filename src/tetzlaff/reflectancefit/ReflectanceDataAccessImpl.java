package tetzlaff.reflectancefit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.util.VertexGeometryImpl;
import tetzlaff.imagedata.GraphicsResources;
import tetzlaff.imagedata.ViewSet;
import tetzlaff.imagedata.ViewSetImpl;

/**
 * An implementation of ReflectanceDataAccess that reads all of the required data from files on the hard drive.
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ReflectanceDataAccessImpl implements ReflectanceDataAccess
{
    private final File vsetFile;
    private final File objFile;
    private File imageDir;
    private File maskDir;

    private ViewSet viewSet;

    /**
     * Creates a new instance of this implementation using files and directories provided by the user.
     * @param vsetFile The view set file (in either the VSET file format or the XML format used by Agisoft Photoscan.
     * @param objFile A Wavefront OBJ file containing the geometry of the reflecting surface.
     * @param imageDir A directory containing all of the photographs to be used.
     * @param maskDir An optional directory containing masks for all of the photographs.
     *                If this parameter is null, it is assumed that masks are unavailable.
     */
    public ReflectanceDataAccessImpl(File vsetFile, File objFile, File imageDir, File maskDir)
    {
        this.vsetFile = vsetFile;
        this.objFile = objFile;
        this.imageDir = imageDir;
        this.maskDir = maskDir;
    }

    @Override
    public String getDefaultMaterialName()
    {
        return objFile.getName().split("\\.")[0];
    }

    @Override
    public VertexGeometry retrieveMesh() throws FileNotFoundException
    {
        return VertexGeometryImpl.createFromOBJFile(objFile);
    }

    @Override
    public void initializeViewSet() throws IOException
    {
        String[] vsetFileNameParts = vsetFile.getName().split("\\.");
        String fileExt = vsetFileNameParts[vsetFileNameParts.length-1];
        if ("vset".equalsIgnoreCase(fileExt))
        {
            System.out.println("Loading from VSET file.");

            viewSet = ViewSetImpl.loadFromVSETFile(vsetFile);
        }
        else if ("xml".equalsIgnoreCase(fileExt))
        {
            System.out.println("Loading from Agisoft Photoscan XML file.");

            try
            {
                viewSet = ViewSetImpl.loadFromAgisoftXMLFile(vsetFile);
            }
            catch (XMLStreamException e)
            {
                throw new IOException(e);
            }

            viewSet.setInfiniteLightSources(false);
        }
        else
        {
            throw new IllegalStateException("Unrecognized file type.");
        }
    }

    @Override
    public ViewSet getViewSet()
    {
        return viewSet;
    }

    @Override
    public BufferedImage retrieveImage(int index) throws IOException
    {
        return ImageIO.read(new FileInputStream(GraphicsResources.findImageFile(new File(imageDir, viewSet.getImageFileName(index)))));
    }

    @Override
    public Optional<BufferedImage> retrieveMask(int index) throws IOException
    {
        return maskDir == null ? Optional.empty() :
            Optional.of(ImageIO.read(new FileInputStream(GraphicsResources.findImageFile(new File(maskDir, viewSet.getImageFileName(index))))));
    }

    /**
     * Rescales the available images, and saves them to the hard drive in a new directory.
     * This new directory subsequently becomes the directory from which images will be loaded from when this implementation is used.
     * @param context A graphics context to use for the rescaling computation.
     * @param width The desired image width.
     * @param height The desired image height.
     * @param rescaleDir A directory in which to store the rescaled images.
     * @param <ContextType> The type of graphics context passed in.
     * @throws IOException An IO exception is thrown if an error occurs either while reading in images, or while writing out the rescaled images.
     */
    public <ContextType extends Context<ContextType>> void rescaleImages(ContextType context, int width, int height, File rescaleDir) throws IOException
    {
        System.out.println("Rescaling images...");
        Date timestamp = new Date();

        try
        (
            // Create an FBO for downsampling
            FramebufferObject<ContextType> downsamplingFBO =
                context.buildFramebufferObject(width, height)
                    .addColorAttachment()
                    .createFramebufferObject();

            VertexBuffer<ContextType> rectBuffer = context.createRectangle();

            Program<ContextType> textureRectProgram = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texture.vert").toFile())
                .addShader(ShaderType.FRAGMENT, Paths.get("shaders", "common", "texture.frag").toFile())
                .createProgram()
        )
        {
            Drawable<ContextType> downsampleRenderable = context.createDrawable(textureRectProgram);

            downsampleRenderable.addVertexBuffer("position", rectBuffer);

            // Downsample and store each image
            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                File imageFile = GraphicsResources.findImageFile(new File(imageDir, viewSet.getImageFileName(i)));

                TextureBuilder<ContextType, ? extends Texture2D<ContextType>> fullSizeImageBuilder;

                if (maskDir == null)
                {
                    fullSizeImageBuilder = context.getTextureFactory().build2DColorTextureFromFile(imageFile, true);
                }
                else
                {
                    File maskFile = GraphicsResources.findImageFile(new File(maskDir, viewSet.getImageFileName(0)));

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
        maskDir = null;
    }
}
