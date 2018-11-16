package tetzlaff.texturefit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLStreamException;

import tetzlaff.gl.core.*;

public class TextureFitExecutor<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final File vsetFile;
    private final File objFile;
    private final File outputDir;
    private final TextureFitParameters param;

    private final File imageDir;
    private final File maskDir;
    private final File rescaleDir;

    public TextureFitExecutor(ContextType context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, File outputDir, TextureFitParameters param)
    {
        this.context = context;
        this.vsetFile = vsetFile;
        this.objFile = objFile;
        this.imageDir = imageDir;
        this.maskDir = maskDir;
        this.rescaleDir = rescaleDir;
        this.outputDir = outputDir;
        this.param = param;
    }

    public void execute() throws IOException, XMLStreamException
    {
        System.out.println("Max vertex uniform components across all blocks:" + context.getState().getMaxCombinedVertexUniformComponents());
        System.out.println("Max fragment uniform components across all blocks:" + context.getState().getMaxCombinedFragmentUniformComponents());
        System.out.println("Max size of a uniform block in bytes:" + context.getState().getMaxUniformBlockSize());
        System.out.println("Max texture array layers:" + context.getState().getMaxArrayTextureLayers());

        try(TextureFitResources<ContextType> resources = new TextureFitResources<>(context, vsetFile, objFile, imageDir, maskDir, rescaleDir, param))
        {
            if (!resources.loadMeshAndViewSet())
            {
                return;
            }

            outputDir.mkdir();

            File tmpDir = new File(outputDir, "tmp");

            if(!resources.getViewSet().hasCustomLuminanceEncoding())
            {
                System.out.println("WARNING: no luminance mapping found.  Reflectance values are not physically grounded.");
            }

            context.getState().enableDepthTest();
            context.getState().disableBackFaceCulling();

            resources.compileShaders();

            System.out.println("Primary view: " + param.getPrimaryViewName());
            resources.getViewSet().setPrimaryView(param.getPrimaryViewName());
            System.out.println("Primary view index: " + resources.getViewSet().getPrimaryViewIndex());

            // Load textures, generate visibility depth textures, fit light source and generate shadow depth textures
            double avgDistance = resources.loadTextures();
            resources.fitLightSource(avgDistance);

            resources.getViewSet().setGeometryFileName(objFile.getName());
            Path relativePathToRescaledImages = outputDir.toPath().relativize(imageDir.toPath());
            resources.getViewSet().setRelativeImagePathName(relativePathToRescaledImages.toString());

            try(FileOutputStream outputStream = new FileOutputStream(new File(outputDir, vsetFile.getName().split("\\.")[0] + ".vset")))
            {
                resources.getViewSet().writeVSETFileToStream(outputStream);
                outputStream.flush();
            }

            if (param.isDiffuseTextureEnabled() || param.isNormalTextureEnabled() || param.isSpecularTextureEnabled())
            {
                File objFileCopy = new File(outputDir, objFile.getName());
                Files.copy(objFile.toPath(), objFileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);

                new TextureFitter<>(context, outputDir, tmpDir, param, resources).fitAndSaveTextures();
            }

            //System.out.println("Resampling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        }
    }
}
