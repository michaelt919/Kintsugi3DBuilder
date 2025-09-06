package kintsugi3d.builder.javafx.util;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Used by UI elements to display resolution in the form DxD where D is both the width and height.
 */
public class SquareResolution
{
    private final IntegerProperty size = new SimpleIntegerProperty(2048);

    public SquareResolution()
    {
    }

    public SquareResolution(int size)
    {
        this.size.set(size);
    }

    public SquareResolution(Number size)
    {
        this.size.set(size.intValue());
    }

    public int getSize()
    {
        return size.get();
    }

    public void setSize(int size)
    {
        this.size.set(size);
    }

    public IntegerProperty sizeProperty()
    {
        return size;
    }

    @Override
    public int hashCode()
    {
        return Integer.hashCode(getSize());
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof SquareResolution && this.getSize() == ((SquareResolution)other).getSize();
    }

    @Override
    public String toString()
    {
        return String.format("%dx%d", getSize(), getSize());
    }
}
