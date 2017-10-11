package tetzlaff.util;

import tetzlaff.gl.vecmath.DoubleVector4;

public interface AbstractImage
{
    int getWidth();
    int getHeight();
    DoubleVector4 getRGBA(int x, int y);
}
