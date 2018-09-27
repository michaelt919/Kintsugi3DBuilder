package tetzlaff.texturefit;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Vector2;

class ParameterizedFitBase<ContextType extends Context<ContextType>>
{
    @FunctionalInterface
    interface SubdivisionRenderingCallback
    {
        void execute(int row, int col) throws IOException;
    }

    final Drawable<ContextType> drawable;
    private final int viewCount;
    private final int subdiv;

    ParameterizedFitBase(Drawable<ContextType> drawable, int viewCount, int subdiv)
    {
        this.subdiv = subdiv;
        this.viewCount = viewCount;
        this.drawable = drawable;
    }

    private void fitSubdiv(Framebuffer<ContextType> framebuffer, int row, int col,
            Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages)
    {
        int subdivWidth = framebuffer.getSize().width / subdiv;
        int subdivHeight = framebuffer.getSize().height / subdiv;

        drawable.program().setTexture("viewImages", viewImages);
        drawable.program().setTexture("depthImages", depthImages);
        drawable.program().setTexture("shadowImages",
            shadowImages == null ? drawable.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D_ARRAY) : shadowImages);

        drawable.program().setUniform("minTexCoord",
                new Vector2((float)col / (float)subdiv, (float)row / (float)subdiv));

        drawable.program().setUniform("maxTexCoord",
                new Vector2((float)(col+1) / (float)subdiv, (float)(row+1) / (float)subdiv));

        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivWidth, row * subdivHeight, subdivWidth, subdivHeight);
        drawable.getContext().finish();
    }

    void fitImageSpace(Framebuffer<ContextType> framebuffer, Texture<ContextType> viewImages, Texture<ContextType> depthImages,
        Texture<ContextType> shadowImages, SubdivisionRenderingCallback callback) throws IOException
    {
        if (this.subdiv == 1)
        {
            this.fitSubdiv(framebuffer, 0, 0, viewImages, depthImages, shadowImages);
        }
        else
        {
            for (int row = 0; row < this.subdiv; row++)
            {
                for (int col = 0; col < this.subdiv; col++)
                {
                    this.fitSubdiv(framebuffer, row, col, viewImages, depthImages, shadowImages);
                    callback.execute(row, col);
                }
            }
        }
    }

    void fitTextureSpace(Framebuffer<ContextType> framebuffer, File preprocessDirectory, SubdivisionRenderingCallback callback) throws IOException
    {
        int subdivWidth = framebuffer.getSize().width / subdiv;
        int subdivHeight = framebuffer.getSize().height / subdiv;

        if (this.subdiv == 1)
        {
            try(Texture3D<ContextType> preprojectedViews =
                drawable.getContext().getTextureFactory().build2DColorTextureArray(subdivWidth, subdivHeight, viewCount).createTexture())
            {
                for (int i = 0; i < this.viewCount; i++)
                {
                    preprojectedViews.loadLayer(i, new File(new File(preprocessDirectory, String.format("%04d", i)), String.format("r%04dc%04d.png", 0, 0)), true);
                }

                this.fitSubdiv(framebuffer, 0, 0, preprojectedViews, null, null);

            }
        }
        else
        {
            for (int row = 0; row < this.subdiv; row++)
            {
                for (int col = 0; col < this.subdiv; col++)
                {
                    try(Texture3D<ContextType> preprojectedViews =
                        drawable.getContext().getTextureFactory().build2DColorTextureArray(subdivWidth, subdivHeight, viewCount).createTexture())
                    {
                        for (int i = 0; i < this.viewCount; i++)
                        {
                            preprojectedViews.loadLayer(i, new File(new File(preprocessDirectory, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
                        }

                        this.fitSubdiv(framebuffer, row, col, preprojectedViews, null, null);
                        callback.execute(row, col);
                    }
                }
            }
        }
    }
}
