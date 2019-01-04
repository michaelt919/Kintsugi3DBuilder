package tetzlaff.texturefit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import tetzlaff.gl.core.*;

class TextureFitter<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final File outputDir;
    private final File tmpDir;
    private final TextureFitParameters param;
    private final TextureFitResources<ContextType> resources;

    TextureFitter(ContextType context, File outputDir, File tmpDir, TextureFitParameters param, TextureFitResources<ContextType> resources)
    {
        this.context = context;
        this.outputDir = outputDir;
        this.tmpDir = tmpDir;
        this.param = param;
        this.resources = resources;
    }

    private void writeMTLFile(String comments, String materialFileName, String materialName) throws FileNotFoundException
    {
        try(PrintStream materialStream = new PrintStream(new File(outputDir, materialFileName)))
        {
            materialStream.println(comments);
            materialStream.println("newmtl " + materialName);

            if (param.isDiffuseTextureEnabled())
            {
                materialStream.println("Kd 1.0 1.0 1.0");
                materialStream.println("map_Kd " + materialName + "_Kd.png");
                materialStream.println("Ka 1.0 1.0 1.0");
                materialStream.println("map_Ka " + materialName + "_Kd.png");
            }
            else
            {
                materialStream.println("Kd 0.0 0.0 0.0");

                if (param.isSpecularTextureEnabled())
                {
                    materialStream.println("Ka 1.0 1.0 1.0");
                    materialStream.println("map_Ka " + materialName + "_Ks.png");
                }
                else
                {
                    materialStream.println("Ka 0 0 0");
                }
            }

            if (param.isSpecularTextureEnabled())
            {
                materialStream.println("Ks 1.0 1.0 1.0");
                materialStream.println("map_Ks " + materialName + "_Ks.png");

                //if (param.isRoughnessTextureEnabled())
                //{
                materialStream.println("Pr 1.0");
                materialStream.println("map_Pr " + materialName + "_Pr.png");
                //}
                //            else
                //            {
                //                materialStream.println("Pr " + specularParams.roughness);
                //            }
            }
            else
            {
                materialStream.println("Ks 0 0 0");
            }

            if ((param.isDiffuseTextureEnabled() && param.isNormalTextureEnabled()) || param.isSpecularTextureEnabled())
            {
                materialStream.println("norm " + materialName + "_norm.png");
            }

            materialStream.println("d 1.0");
            materialStream.println("Tr 0.0");
            materialStream.println("illum 5");

            materialStream.flush();
        }
    }

    void fitAndSaveTextures() throws IOException
    {
        outputDir.mkdirs();
        File auxDir = new File(outputDir, "_aux");
        auxDir.mkdirs();

        if (param.isDebugModeEnabled())
        {
            new File(auxDir, "specularResidDebug").mkdir();
        }

        try
        (
            FramebufferObject<ContextType> framebuffer1 =
                context.buildFramebufferObject(param.getTextureSize(), param.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject();

            FramebufferObject<ContextType> framebuffer2 =
                context.buildFramebufferObject(param.getTextureSize(), param.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject();

            FramebufferObject<ContextType> framebuffer3 =
                context.buildFramebufferObject(param.getTextureSize(), param.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject();

            FramebufferObject<ContextType> errorFramebuffer1 =
                context.buildFramebufferObject(param.getTextureSize(), param.getTextureSize())
                    .addColorAttachments(ColorFormat.RG32F, 1)
                    .addColorAttachments(ColorFormat.R8, 1)
                    .createFramebufferObject();

            FramebufferObject<ContextType> errorFramebuffer2 =
                context.buildFramebufferObject(param.getTextureSize(), param.getTextureSize())
                    .addColorAttachments(ColorFormat.RG32F, 1)
                    .addColorAttachments(ColorFormat.R8, 1)
                    .createFramebufferObject();

            FramebufferObject<ContextType> peakIntensityFramebuffer =
                context.buildFramebufferObject(param.getTextureSize(), param.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 1)
                    .createFramebufferObject()
        )
        {
            FramebufferObject<ContextType> diffuseFitFramebuffer = framebuffer1;

            int subdivSize = param.getTextureSize() / param.getTextureSubdivision();

            Date timestamp = null;

            if (param.isDiffuseTextureEnabled())
            {
                System.out.println("Beginning diffuse fit (" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " blocks)...");
                timestamp = new Date();

                diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

                DiffuseFit<ContextType> diffuseFit =
                    resources.createDiffuseFit(diffuseFitFramebuffer, resources.getViewSet().getCameraPoseCount(), param.getTextureSubdivision());

                if (param.isImagePreprojectionUseEnabled())
                {
                    File diffuseTempDirectory = new File(tmpDir, "diffuse");
                    File normalTempDirectory = new File(tmpDir, "normal");
                    File normalTSTempDirectory = new File(tmpDir, "normalTS");

                    diffuseTempDirectory.mkdir();
                    normalTempDirectory.mkdir();
                    normalTSTempDirectory.mkdir();

                    FramebufferObject<ContextType> currentFramebuffer = diffuseFitFramebuffer;
                    diffuseFit.fitTextureSpace(tmpDir,
                        (row, col) ->
                        {
                            currentFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize,
                                "PNG", new File(diffuseTempDirectory, String.format("r%04dc%04d.png", row, col)));

                            currentFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize,
                                "PNG", new File(normalTempDirectory, String.format("r%04dc%04d.png", row, col)));

                            currentFramebuffer.saveColorBufferToFile(3, col * subdivSize, row * subdivSize, subdivSize, subdivSize,
                                "PNG", new File(normalTSTempDirectory, String.format("r%04dc%04d.png", row, col)));

                            System.out.println("Block " + (row * param.getTextureSubdivision() + col + 1) + '/' +
                                (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                        });
                }
                else
                {
                    diffuseFit.fitImageSpace(resources.getViewTextures(), resources.getDepthTextures(), resources.getShadowTextures(),
                        (row, col) ->
                            System.out.println("Block " + (row * param.getTextureSubdivision() + col + 1) + '/' +
                                (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed."));
                }

                System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

                System.out.println("Filling empty regions...");
                timestamp = new Date();
            }
            else
            {
                System.out.println("Skipping diffuse fit.");
            }

            try(VertexBuffer<ContextType> rectBuffer = context.createRectangle())
            {
                Drawable<ContextType> holeFillRenderable = context.createDrawable(resources.getHoleFillProgram());
                holeFillRenderable.addVertexBuffer("position", rectBuffer);
                resources.getHoleFillProgram().setUniform("minFillAlpha", 0.5f);

                FramebufferObject<ContextType> backFramebuffer = framebuffer2;

                if (param.isDiffuseTextureEnabled())
                {
                    System.out.println("Diffuse fill...");

                    // Diffuse
                    FramebufferObject<ContextType> frontFramebuffer = diffuseFitFramebuffer;
                    for (int i = 0; i < param.getTextureSize() / 2; i++)
                    {
                        backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
                        backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

                        resources.getHoleFillProgram().setTexture("input0", frontFramebuffer.getColorAttachmentTexture(0));
                        resources.getHoleFillProgram().setTexture("input1", frontFramebuffer.getColorAttachmentTexture(1));
                        resources.getHoleFillProgram().setTexture("input2", frontFramebuffer.getColorAttachmentTexture(2));
                        resources.getHoleFillProgram().setTexture("input3", frontFramebuffer.getColorAttachmentTexture(3));

                        holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);
                        context.finish();

                        FramebufferObject<ContextType> tmp = frontFramebuffer;
                        frontFramebuffer = backFramebuffer;
                        backFramebuffer = tmp;
                    }
                    diffuseFitFramebuffer = frontFramebuffer;

                    System.out.println("Empty regions filled in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
                    System.out.println("Saving textures...");
                    timestamp = new Date();

                    diffuseFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-old.png"));
                    diffuseFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "diffuse-normal.png"));
                    //diffuseFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(textureDirectory, "ambient.png"));
                    diffuseFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "diffuse-normalts.png"));

                    if (!param.isSpecularTextureEnabled())
                    {
                        frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(outputDir, resources.getMaterialName() + "_Kd.png"));

                        if (param.isNormalTextureEnabled())
                        {
                            frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(outputDir, resources.getMaterialName() + "_norm.png"));
                        }
                    }

                    System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
                }

                System.out.println("Fitting specular residual...");

                if (param.isSpecularTextureEnabled())
                {
                    System.out.println("Creating specular reflectivity texture...");

                    ErrorCalc<ContextType> errorCalc = resources.createErrorCalc(resources.getViewSet().getCameraPoseCount(), param.getTextureSubdivision());
                    Drawable<ContextType> finalizeDrawable = context.createDrawable(resources.getFinalizeProgram());
                    finalizeDrawable.addVertexBuffer("position", rectBuffer);

                    FramebufferObject<ContextType> frontFramebuffer = framebuffer3;

//                    resources.getPeakIntensityEstimator().init(resources::setupCommonShaderInputs, resources.getViewTextures(), resources.getDepthTextures(), resources.getShadowTextures(),
//                        (param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer : frontFramebuffer).getColorAttachmentTexture(0),
//                        param.isDiffuseTextureEnabled() ?
//                            diffuseFitFramebuffer.getColorAttachmentTexture(3) :
//                            frontFramebuffer.getColorAttachmentTexture(1));
//                    Vector4[] peakIntensityEstimates = resources.getPeakIntensityEstimator().estimate(
//                        param.getTextureSize() / 2, param.getTextureSize() / 2,
//                        resources.getGeometry().getBoundingRadius() * 0.25f, 0.25f);

                    FramebufferObject<ContextType> frontErrorFramebuffer = errorFramebuffer1;
                    FramebufferObject<ContextType> backErrorFramebuffer = errorFramebuffer2;

//                    float[] peakIntensityData = new float[peakIntensityEstimates.length * 4];
//                    for (int i = 0; i < peakIntensityEstimates.length; i++)
//                    {
//                        peakIntensityData[i * 4] = peakIntensityEstimates[i].x;
//                        peakIntensityData[i * 4 + 1] = peakIntensityEstimates[i].y;
//                        peakIntensityData[i * 4 + 2] = peakIntensityEstimates[i].z;
//                        peakIntensityData[i * 4 + 3] = peakIntensityEstimates[i].w;
//                    }

                    FramebufferObject<ContextType> tmp;

//                    try(Texture2D<ContextType> peakSpecularTexture =
//                        context.getTextureFactory().build2DColorTextureFromBuffer(param.getTextureSize() / 2, param.getTextureSize() / 2,
//                            NativeVectorBufferFactory.getInstance().createFromFloatArray(
//                                4, (param.getTextureSize() / 2) * (param.getTextureSize() / 2), peakIntensityData))
//                            .setInternalFormat(ColorFormat.RGBA32F)
//                            .setLinearFilteringEnabled(true)
//                            .createTexture())
                    {
                        frontFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                        frontFramebuffer.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f); // normal map
                        frontFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        frontFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                        frontFramebuffer.clearColorBuffer(4, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 0.0f);

                        frontErrorFramebuffer.clearColorBuffer(0, 128.0f, Float.MAX_VALUE, 0.0f, 0.0f);
                        backErrorFramebuffer.clearColorBuffer(0, 0.0f, -1.0f, 0.0f, 0.0f);

                        File diffuseTempDirectory = new File(tmpDir, "diffuse");
                        File specularTempDirectory = new File(tmpDir, "specular");

                        if (param.isImagePreprojectionUseEnabled())
                        {
                            diffuseTempDirectory.mkdirs();
                            specularTempDirectory.mkdirs();
                        }

                        backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
//
//                        resources.getSpecularPeakFit().setFramebuffer(backFramebuffer);
//
//                        resources.getSpecularPeakFit().fitImageSpace(resources.getViewTextures(), resources.getDepthTextures(), resources.getShadowTextures(),
//                            (param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer : frontFramebuffer).getColorAttachmentTexture(0),
//                            param.isDiffuseTextureEnabled() ?
//                                diffuseFitFramebuffer.getColorAttachmentTexture(3) :
//                                frontFramebuffer.getColorAttachmentTexture(1),
//                            peakSpecularTexture,
//                            (row, col) ->
//                                System.out.println("Block " + (row * param.getTextureSubdivision() + col + 1) + '/' +
//                                    (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed."));

                        SpecularFit<ContextType> specularFit = resources.createSpecularFit(backFramebuffer, resources.getViewSet().getCameraPoseCount(), param.getTextureSubdivision());

                        if (param.isImagePreprojectionUseEnabled())
                        {
                            // TODO work out how to support image preprojection
                            FramebufferObject<ContextType> currentFramebuffer = backFramebuffer;
                            specularFit.fitTextureSpace(tmpDir,
                                param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer.getColorAttachmentTexture(0) : frontFramebuffer.getColorAttachmentTexture(0),
                                param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer.getColorAttachmentTexture(3) : frontFramebuffer.getColorAttachmentTexture(1),
                                context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D),//peakSpecularTexture,
                                (row, col) ->
                                {
//                                    currentFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize,
//                                            "PNG", new File(specularTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));
//
//                                    currentFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize,
//                                            "PNG", new File(diffuseTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));

                                    System.out.println("Block " + (row * param.getTextureSubdivision() + col + 1) + '/' +
                                        (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                                });
                        }
                        else
                        {
                            specularFit.fitImageSpace(resources.getViewTextures(), resources.getDepthTextures(), resources.getShadowTextures(),
                                param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer.getColorAttachmentTexture(0) : frontFramebuffer.getColorAttachmentTexture(0),
                                param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer.getColorAttachmentTexture(3) : frontFramebuffer.getColorAttachmentTexture(1),
                                context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D),//peakSpecularTexture,
                                (row, col) ->
                                {
                                    System.out.println("Block " + (row * param.getTextureSubdivision() + col + 1) + '/' +
                                        (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                                });
                        }

                        tmp = frontFramebuffer;
                        frontFramebuffer = backFramebuffer;
                        backFramebuffer = tmp;

                        int pixelCount = 0;
                        float[] specularFitData = frontFramebuffer.readFloatingPointColorBufferRGBA(2);
                        for (int i = 0; i * 4 + 3 < specularFitData.length; i++)
                        {
                            if (specularFitData[i * 4 + 3] > 0)
                            {
                                pixelCount++;
                            }
                        }

//                        System.out.println("Pixel count: " + pixelCount);

                        frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-raw.png"));
                        frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-raw.png"));
                        frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-raw.png"));
                        frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-raw.png"));
                        frontFramebuffer.saveColorBufferToFile(4, "PNG", new File(auxDir, "roughness-error.png"));
                    }

                    double lastRMSError;

                    resources.getPeakIntensityProgram().setUniform("gamma", param.getGamma());
                    resources.getPeakIntensityProgram().setTexture("specularTexture", frontFramebuffer.getColorAttachmentTexture(2));
                    resources.getPeakIntensityProgram().setTexture("roughnessTexture", frontFramebuffer.getColorAttachmentTexture(3));

                    Drawable<ContextType> peakIntensityDrawable = context.createDrawable(resources.getPeakIntensityProgram());
                    peakIntensityDrawable.addVertexBuffer("position", rectBuffer);
                    peakIntensityDrawable.draw(PrimitiveMode.TRIANGLE_FAN, peakIntensityFramebuffer);

                    peakIntensityFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "peak.png"));

                    frontErrorFramebuffer.clearColorBuffer(0, 128.0f, Float.MAX_VALUE, 0.0f, 0.0f);
                    backErrorFramebuffer.clearColorBuffer(0, 0.0f, -1.0f, 0.0f, 0.0f);

                    resources.getErrorCalcProgram().setUniform("ignoreDampingFactor", true);

                    if (param.isImagePreprojectionUseEnabled())
                    {
                        errorCalc.fitTextureSpace(
                            backErrorFramebuffer,
                            tmpDir,
                            frontFramebuffer.getColorAttachmentTexture(0),
                            frontFramebuffer.getColorAttachmentTexture(1),
                            frontFramebuffer.getColorAttachmentTexture(2),
                            frontFramebuffer.getColorAttachmentTexture(3),
                            frontErrorFramebuffer.getColorAttachmentTexture(0),
                            (row, col) ->
                            {
//                                    System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" +
//                                            (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                            });
                    }
                    else
                    {
                        errorCalc.fitImageSpace(
                            backErrorFramebuffer,
                            resources.getViewTextures(), resources.getDepthTextures(), resources.getShadowTextures(),
                            frontFramebuffer.getColorAttachmentTexture(0),
                            frontFramebuffer.getColorAttachmentTexture(1),
                            frontFramebuffer.getColorAttachmentTexture(2),
                            frontFramebuffer.getColorAttachmentTexture(3),
                            frontErrorFramebuffer.getColorAttachmentTexture(0),
                            (row, col) ->
                            {
//                                    System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" +
//                                            (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                            });
                    }

                    context.finish();

                    backErrorFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "error-mask-init.png"));

                    tmp = frontErrorFramebuffer;
                    frontErrorFramebuffer = backErrorFramebuffer;
                    backErrorFramebuffer = tmp;

                    double initSumSqError = 0.0;
                    int initSumMask = 0;
                    float[] errorData = frontErrorFramebuffer.readFloatingPointColorBufferRGBA(0);
                    for (int j = 0; j * 4 + 3 < errorData.length; j++)
                    {
                        float error = errorData[j * 4 + 1]; // Green channel holds squared error
                        if (error >= 0)
                        {
                            initSumSqError += error;
                            initSumMask++;
                        }
                    }

                    lastRMSError = Math.sqrt(initSumSqError / initSumMask);

//                    System.out.println("Sum squared error: " + initSumSqError);
//                    System.out.println("RMS error: " + lastRMSError);

                    if (param.isLevenbergMarquardtOptimizationEnabled())
                    {
                        // Non-linear adjustment
                        AdjustFit<ContextType> adjustFit = resources.createAdjustFit(resources.getViewSet().getCameraPoseCount(), param.getTextureSubdivision());

                        System.out.println("Adjusting fit...");

                        boolean saveDebugTextures = false;
                        boolean useGlobalDampingFactor = true;
                        float globalDampingFactor = 128.0f;
                        int iteration = 0;

                        while (globalDampingFactor <= 0x100000 /* ~ 1 million */)
                        {
                            backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                            backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                            backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                            backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                            backFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);

                            if(useGlobalDampingFactor)
                            {
                                // hack to override damping factor and never discard the result - TODO make this more elegant
                                frontErrorFramebuffer.clearColorBuffer(0, globalDampingFactor, Float.MAX_VALUE, 0.0f, 0.0f);
                            }

                            if (param.isImagePreprojectionUseEnabled())
                            {
                                adjustFit.fitTextureSpace(
                                    backFramebuffer,
                                    tmpDir,
                                    frontFramebuffer.getColorAttachmentTexture(0),
                                    frontFramebuffer.getColorAttachmentTexture(1),
                                    frontFramebuffer.getColorAttachmentTexture(2),
                                    frontFramebuffer.getColorAttachmentTexture(3),
                                    peakIntensityFramebuffer.getColorAttachmentTexture(0),
                                    frontErrorFramebuffer.getColorAttachmentTexture(0),
                                    (row, col) ->
                                    {
                                        //                                    currentFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize,
                                        //                                            "PNG", new File(diffuseTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));
                                        //
                                        //                                    currentFramebuffer.saveColorBufferToFile(2, col * subdivSize, row * subdivSize, subdivSize, subdivSize,
                                        //                                            "PNG", new File(specularTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));

                                        //                                        System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" +
                                        //                                                (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                                    });
                            }
                            else
                            {
                                adjustFit.fitImageSpace(
                                    backFramebuffer,
                                    resources.getViewTextures(), resources.getDepthTextures(), resources.getShadowTextures(),
                                    frontFramebuffer.getColorAttachmentTexture(0),
                                    frontFramebuffer.getColorAttachmentTexture(1),
                                    frontFramebuffer.getColorAttachmentTexture(2),
                                    frontFramebuffer.getColorAttachmentTexture(3),
                                    peakIntensityFramebuffer.getColorAttachmentTexture(0),
                                    frontErrorFramebuffer.getColorAttachmentTexture(0),
                                    (row, col) ->
                                    {
                                        //                                        System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" +
                                        //                                                (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                                    });
                            }

                            context.finish();

                            if (saveDebugTextures)
                            {
                                backFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-test1.png"));
                                backFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-test1.png"));
                                backFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-test1.png"));
                                backFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-test1.png"));
                                backFramebuffer.saveColorBufferToFile(4, "PNG", new File(auxDir, "stddev-test1.png"));
                            }

                            backErrorFramebuffer.clearColorBuffer(0, 0.0f, -1.0f, 0.0f, 0.0f);

                            if (param.isImagePreprojectionUseEnabled())
                            {
                                errorCalc.fitTextureSpace(
                                    backErrorFramebuffer,
                                    tmpDir,
                                    backFramebuffer.getColorAttachmentTexture(0),
                                    backFramebuffer.getColorAttachmentTexture(1),
                                    backFramebuffer.getColorAttachmentTexture(2),
                                    backFramebuffer.getColorAttachmentTexture(3),
                                    frontErrorFramebuffer.getColorAttachmentTexture(0),
                                    (row, col) ->
                                    {
                                        //                                        System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" +
                                        //                                                (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                                    });
                            }
                            else
                            {
                                errorCalc.fitImageSpace(
                                    backErrorFramebuffer,
                                    resources.getViewTextures(), resources.getDepthTextures(), resources.getShadowTextures(),
                                    backFramebuffer.getColorAttachmentTexture(0),
                                    backFramebuffer.getColorAttachmentTexture(1),
                                    backFramebuffer.getColorAttachmentTexture(2),
                                    backFramebuffer.getColorAttachmentTexture(3),
                                    frontErrorFramebuffer.getColorAttachmentTexture(0),
                                    (row, col) ->
                                    {
                                        //                                        System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" +
                                        //                                                (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
                                    });
                            }

                            context.finish();

                            if (saveDebugTextures)
                            {
                                backErrorFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "error-mask-test.png"));
                            }

                            tmp = frontErrorFramebuffer;
                            frontErrorFramebuffer = backErrorFramebuffer;
                            backErrorFramebuffer = tmp;

                            double sumSqError = 0.0;
                            int sumMask = 0;
                            errorData = frontErrorFramebuffer.readFloatingPointColorBufferRGBA(0);
                            for (int j = 0; j * 4 + 3 < errorData.length; j++)
                            {
                                float error = errorData[j * 4 + 1]; // Green channel holds squared error
                                if (error >= 0)
                                {
                                    sumSqError += error;
                                    sumMask++;
                                }
                            }

                            double rmsError = Math.sqrt(sumSqError / sumMask);

                            System.out.println("Sum squared error: " + sumSqError);
                            System.out.println("RMS error: " + rmsError);

                            if (rmsError < lastRMSError)
                            {
                                lastRMSError = rmsError;

                                System.out.println("Saving iteration.");

                                if (useGlobalDampingFactor)
                                {
                                    globalDampingFactor /= 2;

                                    System.out.println("Next damping factor: " + globalDampingFactor);

                                    // Set the mask framebuffer to all 1 (hack - TODO make this more elegant)
                                    frontErrorFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);
                                }
                                else
                                {
                                    // If the damping factor isn't being used, set to the minimum, which will function as a countdown if an iteration is unproductive.
                                    globalDampingFactor = 0.0078125f;
                                }

                                resources.getFinalizeProgram().setTexture("input0", backFramebuffer.getColorAttachmentTexture(0));
                                resources.getFinalizeProgram().setTexture("input1", backFramebuffer.getColorAttachmentTexture(1));
                                resources.getFinalizeProgram().setTexture("input2", backFramebuffer.getColorAttachmentTexture(2));
                                resources.getFinalizeProgram().setTexture("input3", backFramebuffer.getColorAttachmentTexture(3));
                                resources.getFinalizeProgram().setTexture("input4", backFramebuffer.getColorAttachmentTexture(4));
                                resources.getFinalizeProgram().setTexture("alphaMask", frontErrorFramebuffer.getColorAttachmentTexture(1));

                                finalizeDrawable.draw(PrimitiveMode.TRIANGLE_FAN, frontFramebuffer);
                                context.finish();
                            }
                            else
                            {
                                // If useGlobalDampingFactor == false, then this effectively serves as a countdown in the case of an unproductive iteration.
                                // If enough unproductive iterations occur, then this variable will keep doubling until it exceeds the maximum value.
                                globalDampingFactor *= 2;

                                System.out.println("Discarding iteration.");
                                System.out.println("Next damping factor: " + globalDampingFactor);
                            }

                            if (saveDebugTextures)
                            {
                                frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-test2.png"));
                                frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-test2.png"));
                                frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-test2.png"));
                                frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-test2.png"));
                                frontFramebuffer.saveColorBufferToFile(4, "PNG", new File(auxDir, "stddev-test2.png"));
                            }

                            System.out.println("Iteration " + (iteration+1) + " complete.");
                            System.out.println();

                            iteration++;
                        }

                        frontErrorFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "error-mask-final.png"));
                    }

                    System.out.println("Filling holes...");

                    // Fill holes
                    for (int i = 0; i < param.getTextureSize() / 2; i++)
                    {
                        backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

                        resources.getHoleFillProgram().setTexture("input0", frontFramebuffer.getColorAttachmentTexture(0));
                        resources.getHoleFillProgram().setTexture("input1", frontFramebuffer.getColorAttachmentTexture(1));
                        resources.getHoleFillProgram().setTexture("input2", frontFramebuffer.getColorAttachmentTexture(2));
                        resources.getHoleFillProgram().setTexture("input3", frontFramebuffer.getColorAttachmentTexture(3));

                        holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);
                        context.finish();

                        tmp = frontFramebuffer;
                        frontFramebuffer = backFramebuffer;
                        backFramebuffer = tmp;
                    }

                    System.out.println("Saving textures...");

                    // Save a copy of all of the channels, even the ones that shouldn't be needed, in the _aux folder.
                    frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(auxDir, "diffuse-final.png"));
                    frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(auxDir, "normal-final.png"));
                    frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(auxDir, "specular-final.png"));
                    frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(auxDir, "roughness-final.png"));

                    if (param.isDiffuseTextureEnabled())
                    {
                        frontFramebuffer.saveColorBufferToFile(0, "PNG", new File(outputDir, resources.getMaterialName() + "_Kd.png"));
                    }

                    if ((param.isDiffuseTextureEnabled() && param.isNormalTextureEnabled()) || param.isSpecularTextureEnabled())
                    {
                        frontFramebuffer.saveColorBufferToFile(1, "PNG", new File(outputDir, resources.getMaterialName() + "_norm.png"));
                    }

                    frontFramebuffer.saveColorBufferToFile(2, "PNG", new File(outputDir, resources.getMaterialName() + "_Ks.png"));
                    frontFramebuffer.saveColorBufferToFile(3, "PNG", new File(outputDir, resources.getMaterialName() + "_Pr.png"));

                    writeMTLFile("# RMS fitting error: " + lastRMSError, resources.getMaterialFileName(), resources.getMaterialName());
                }
            }
        }
    }
}
