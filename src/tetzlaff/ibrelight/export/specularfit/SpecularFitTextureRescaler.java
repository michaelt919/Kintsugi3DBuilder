package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.util.ImageLodResizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpecularFitTextureRescaler
{
    private final ExportSettings settings;

    public SpecularFitTextureRescaler(ExportSettings settings)
    {
        this.settings = settings;
    }

    public void rescaleAll(File outputDirectory, int basisCount)
    {
        List<String> files = new ArrayList<>(List.of(new String[]{"albedo.png", "diffuse.png", "specular.png", "orm.png", "normal.png"}));

        if (settings.isCombineWeights())
        {
            for (int i = 0; i < (basisCount + 3) / 4; i++)
            {
                files.add(SpecularFitSerializer.getCombinedWeightFilename(i));
            }
        }
        else
        {
            for (int i = 0; i < basisCount; i++)
            {
                files.add(SpecularFitSerializer.getWeightFileName(i));
            }
        }

        for (String file : files)
        {
            try
            {
                generateLodsFor(new File(outputDirectory, file));
            }
            catch (IOException e)
            {
                System.out.println("Failed to generate LODs for file: " + file);
                e.printStackTrace();
            }
        }
    }

    public void generateLodsFor(File file) throws IOException
    {
        ImageLodResizer.generateLods(file, settings.getMinimumTextureResolution());
    }

}
