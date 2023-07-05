package tetzlaff.ibrelight.export.general;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageLodResizer
{

    private static final int MIN_SIZE = 128;
    private final File outputDirectory;
    private final BufferedImage inputImage;

    public ImageLodResizer(File inputFile) throws IOException
    {
        outputDirectory = inputFile.getParentFile();
        inputImage = ImageIO.read(inputFile);
    }

    public ImageLodResizer(BufferedImage inputImage, File outputDirectory)
    {
        this.inputImage = inputImage;
        this.outputDirectory = outputDirectory;
    }

    //TODO: Delete test harness
    public static void main(String[] args) throws IOException
    {
        generateLods(new File("X:\\CHViewer\\exports\\temp\\albedo.png"));
    }

    public static void generateLods(File inputFile) throws IOException
    {
        File dir = inputFile.getParentFile();
        ImageLodResizer resize = new ImageLodResizer(inputFile);

        String filename = inputFile.getName();
        String extension = "";
        int i = filename.lastIndexOf('.'); //Strip file extension
        if (i > 0)
        {
            extension = filename.substring(i);
            filename = filename.substring(0, i);
        }

        for (int size = resize.inputImage.getHeight() / 2; size >= MIN_SIZE; size /= 2)
        {
            StringBuilder sb = new StringBuilder(filename);
            sb.append("-");
            sb.append(size);
            sb.append(extension);

            resize.saveAtResolution(new File(dir, sb.toString()), size);
        }
    }

    public void saveAtResolution(File file, int height) throws IOException
    {
        saveAtScale(file, (double) height / inputImage.getHeight());
    }

    public void saveAtScale(File file, double factor) throws IOException
    {
        saveAtResolution(file, (int)(inputImage.getWidth() * factor), (int)(inputImage.getHeight() * factor));
    }

    public void saveAtResolution(File file, int width, int height) throws IOException
    {
        BufferedImage scaled = scaleToResolution(width, height);
        ImageIO.write(scaled, getFormatFor(file), file);
    }

    private static String getFormatFor(File file)
    {
        return "PNG"; //TODO
    }

    public BufferedImage scaleToResolution(int width, int height)
    {
        return scaleBy((double) width / inputImage.getWidth(), (double) height / inputImage.getHeight());
    }

    private BufferedImage scaleBy(double factor)
    {
        return scaleBy(factor, factor);
    }

    private BufferedImage scaleBy(double factorX, double factorY)
    {
        int w = (int) (factorX * inputImage.getWidth());
        int h = (int) (factorY * inputImage.getHeight());
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform transform = new AffineTransform();
        transform.scale(factorX, factorY);
        AffineTransformOp operation = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        scaled = operation.filter(inputImage, scaled);
        return scaled;
    }

}
