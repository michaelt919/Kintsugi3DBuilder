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

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ReflectanceDataAccessImpl<ContextType extends Context<ContextType>> implements ReflectanceDataAccess
{
    private final File vsetFile;
    private final File objFile;
    private File imageDir;
    private File maskDir;
    private File rescaleDir;

    private final ContextType context;
    private final Options options;

    private ViewSet viewSet;

    public ReflectanceDataAccessImpl(ContextType context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, Options options)
    {
        this.context = context;
        this.options = options;
        this.vsetFile = vsetFile;
        this.objFile = objFile;
        this.imageDir = imageDir;
        this.maskDir = maskDir;
        this.rescaleDir = rescaleDir;
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
    public void initializeViewSet() throws FileNotFoundException, XMLStreamException
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
            viewSet = ViewSetImpl.loadFromAgisoftXMLFile(vsetFile);
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

    public void rescaleImages() throws IOException
    {
        System.out.println("Rescaling images...");
        Date timestamp = new Date();

        try
        (
            // Create an FBO for downsampling
            FramebufferObject<ContextType> downsamplingFBO =
                context.buildFramebufferObject(options.getImageWidth(), options.getImageHeight())
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
        rescaleDir = null;
        maskDir = null;
    }
}
