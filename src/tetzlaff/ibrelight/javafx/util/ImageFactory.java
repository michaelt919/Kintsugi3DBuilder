package tetzlaff.ibrelight.javafx.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.util.AbstractImage;

public final class ImageFactory
{
    private ImageFactory()
    {
    }

    private static double clamp(double unclamped)
    {
        return Math.max(0, Math.min(1, unclamped));
    }


    private static class PixelReaderImpl extends PixelReaderBase
    {
        private final AbstractImage image;

        PixelReaderImpl(AbstractImage image)
        {
            this.image = image;
        }

        @Override
        public Color getColor(int x, int y)
        {
            DoubleVector4 colorVector = image.getRGBA(x, y);
            return new Color(clamp(colorVector.x), clamp(colorVector.y), clamp(colorVector.z), clamp(colorVector.w));
        }
    }

    public static Image createFromAbstractImage(AbstractImage image)
    {
        return new WritableImage(new PixelReaderImpl(image), image.getWidth(), image.getHeight());
    }
}
