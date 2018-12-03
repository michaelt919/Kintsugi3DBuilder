package tetzlaff.reflectancefit;

import java.io.IOException;
import java.util.Date;

import tetzlaff.gl.core.*;

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ParameterFitting<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final ParameterFittingResources<ContextType> resources;
    private final Options param;

    ParameterFitting(ContextType context, ParameterFittingResources<ContextType> resources, Options param)
    {
        this.context = context;
        this.resources = resources;
        this.param = param;
    }

    public ParameterFittingResult fit() throws IOException
    {
        try
        (
            FramebufferObject<ContextType> framebuffer1 =
                this.context.buildFramebufferObject(this.param.getTextureSize(), this.param.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject();

            FramebufferObject<ContextType> framebuffer2 =
                this.context.buildFramebufferObject(this.param.getTextureSize(), this.param.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject();

            FramebufferObject<ContextType> frontFramebufferSpecular =
                this.context.buildFramebufferObject(this.param.getTextureSize(), this.param.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject()
        )
        {
            FramebufferObject<ContextType> diffuseFitFramebuffer = framebuffer1;

            if (this.param.isDiffuseTextureEnabled())
            {
                System.out.println("Beginning diffuse fit (" + (this.param.getTextureSubdivision() * this.param.getTextureSubdivision()) + " blocks)...");
                Date timestamp = new Date();

                diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

                DiffuseFit<ContextType> diffuseFit = resources.createDiffuseFit(diffuseFitFramebuffer, this.param.getTextureSubdivision());

                diffuseFit.fitImageSpace(resources.getViewTextures(), resources.getDepthTextures(),
                    (row, col) ->
                        System.out.println("Block " + (row * this.param.getTextureSubdivision() + col + 1) + '/' +
                            (this.param.getTextureSubdivision() * this.param.getTextureSubdivision()) + " completed."));

                System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

                System.out.println("Filling empty regions...");
            }
            else
            {
                System.out.println("Skipping diffuse fit.");
            }

            Date timestamp = new Date();

            try(VertexBuffer<ContextType> rectBuffer = this.context.createRectangle())
            {
                Drawable<ContextType> holeFillRenderable = this.context.createDrawable(resources.getHoleFillProgram());
                holeFillRenderable.addVertexBuffer("position", rectBuffer);
                resources.getHoleFillProgram().setUniform("minFillAlpha", 0.5f);

                FramebufferObject<ContextType> backFramebuffer = framebuffer2;

                if (this.param.isDiffuseTextureEnabled())
                {
                    System.out.println("Diffuse fill...");

                    // Diffuse
                    FramebufferObject<ContextType> frontFramebuffer = diffuseFitFramebuffer;
                    for (int i = 0; i < this.param.getTextureSize() / 2; i++)
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
                        this.context.finish();

                        FramebufferObject<ContextType> tmp = frontFramebuffer;
                        frontFramebuffer = backFramebuffer;
                        backFramebuffer = tmp;
                    }

                    diffuseFitFramebuffer = frontFramebuffer;

                    System.out.println("Empty regions filled in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
                }

                if (this.param.isSpecularTextureEnabled())
                {
                    System.out.println("Fitting specular residual...");
                    System.out.println("Creating specular reflectivity texture...");

                    frontFramebufferSpecular.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                    frontFramebufferSpecular.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f); // normal map
                    frontFramebufferSpecular.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                    frontFramebufferSpecular.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                    frontFramebufferSpecular.clearColorBuffer(4, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 1.0f);

                    backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(4, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 1.0f);

                    SpecularFit<ContextType> specularFit = resources.createSpecularFit(backFramebuffer, this.param.getTextureSubdivision());

                    specularFit.fitImageSpace(resources.getViewTextures(), resources.getDepthTextures(),
                        (this.param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer : frontFramebufferSpecular).getColorAttachmentTexture(0),
                        this.param.isDiffuseTextureEnabled() ? diffuseFitFramebuffer.getColorAttachmentTexture(3) : frontFramebufferSpecular.getColorAttachmentTexture(1),
                        (row, col) ->
                            System.out.println("Block " + (row * this.param.getTextureSubdivision() + col + 1) + '/' +
                                (this.param.getTextureSubdivision() * this.param.getTextureSubdivision()) + " completed."));

                    FramebufferObject<ContextType> frontFramebufferHoleFill = backFramebuffer;
                    FramebufferObject<ContextType> backFramebufferHoleFill = frontFramebufferSpecular;

                    // Fill holes
                    for (int i = 0; i < this.param.getTextureSize() / 2; i++)
                    {
                        backFramebufferHoleFill.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);

                        resources.getHoleFillProgram().setTexture("input0", frontFramebufferHoleFill.getColorAttachmentTexture(0));
                        resources.getHoleFillProgram().setTexture("input1", frontFramebufferHoleFill.getColorAttachmentTexture(1));
                        resources.getHoleFillProgram().setTexture("input2", frontFramebufferHoleFill.getColorAttachmentTexture(2));
                        resources.getHoleFillProgram().setTexture("input3", frontFramebufferHoleFill.getColorAttachmentTexture(3));
                        resources.getHoleFillProgram().setTexture("input4", frontFramebufferHoleFill.getColorAttachmentTexture(4));

                        holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebufferHoleFill);
                        this.context.finish();

                        FramebufferObject<ContextType> tmp = frontFramebufferHoleFill;
                        frontFramebufferHoleFill = backFramebufferHoleFill;
                        backFramebufferHoleFill = tmp;
                    }

                    return ParameterFittingResult.fromFramebuffer(frontFramebufferHoleFill, this.param);
                }
                else
                {
                    return ParameterFittingResult.fromFramebuffer(diffuseFitFramebuffer, this.param);
                }
            }
        }
    }
}
