package tetzlaff.util;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Singleton class
 */
public class ImageFinder
{
    private static ImageFinder INSTANCE = new ImageFinder();

    public static ImageFinder getInstance()
    {
        return INSTANCE;
    }

    private ImageFinder()
    {
    }

    // TODO move outside this class
    public File findImageFile(File requestedFile) throws FileNotFoundException
    {
        if (requestedFile.exists())
        {
            return requestedFile;
        }
        else
        {
            // Try some alternate file formats/extensions
            String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
            for(String extension : altFormats)
            {
                String[] filenameParts = requestedFile.getName().split("\\.");

                String altFileName;
                if (filenameParts.length > 1)
                {
                    filenameParts[filenameParts.length - 1] = extension;
                    altFileName = String.join(".", filenameParts);
                }
                else
                {
                    altFileName = String.join(".", filenameParts[0], extension);
                }

                File imageFileGuess = new File(requestedFile.getParentFile(), altFileName);

                System.out.printf("Trying '%s'\n", imageFileGuess.getAbsolutePath());
                if (imageFileGuess.exists())
                {
                    System.out.printf("Found!!\n");
                    return imageFileGuess;
                }
            }

            // Is it still not there?
            throw new FileNotFoundException(
                    String.format("'%s' not found.", requestedFile.getName()));
        }
    }
}
