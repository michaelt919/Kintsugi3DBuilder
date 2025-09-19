/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.core;

import kintsugi3d.gl.vecmath.IntVector4;
import kintsugi3d.util.ColorList;
import kintsugi3d.util.ColorNativeBufferList;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ColorTextureReader
{
    /**
     * Gets the width of the texture.
     * @return The width of the texture.
     */
    int getWidth();

    /**
     * Gets the height of the texture.
     * @return The height of the texture.
     */
    int getHeight();

    /**
     * Reads the pixels from the texture into an existing buffer in memory.
     * Only a rectangular subset of the pixels will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void readARGB(ByteBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture as floating point numbers.
     * Only a rectangular subset of the pixels will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void readFloatingPointRGBA(FloatBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture as 32-bit integers.
     * Only a rectangular subset of the pixels will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     */
    void readIntegerRGBA(IntBuffer destination, int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture.
     * The entire framebuffer will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    default void readARGB(ByteBuffer destination)
    {
        this.readARGB(destination, 0, 0, getWidth(), getHeight());
    }

    /**
     * Reads the pixels from the texture as floating point numbers.
     * The entire framebuffer will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    default void readFloatingPointRGBA(FloatBuffer destination)
    {
        this.readFloatingPointRGBA(destination, 0, 0, getWidth(), getHeight());
    }

    /**
     * Reads the pixels from the texture as 32-bit integers.
     * The entire framebuffer will be read.
     * @param destination The buffer into which to copy the framebuffer data.
     */
    default void readIntegerRGBA(IntBuffer destination)
    {
        this.readIntegerRGBA(destination, 0, 0, getWidth(), getHeight());
    }

    /**
     * Reads the pixels from the texture.
     * The entire framebuffer will be read.
     * @return An array containing the pixels.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    default int[] readARGB()
    {
        return this.readARGB(0, 0, getWidth(), getHeight());
    }

    /**
     * Reads the pixels from the texture.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the pixels.
     * Each entry is encoded as a 32-bit integer using 8-bytes each for the alpha, red, green, and blue channels, respectively, from highest order byte to lowest.
     */
    int[] readARGB(int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture as floating point numbers.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the pixels as floating point numbers.
     */
    float[] readFloatingPointRGBA(int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture as floating point numbers.
     * The entire framebuffer will be read.
     * @return An array containing the pixels as floating point numbers.
     */
    default float[] readFloatingPointRGBA()
    {
        return this.readFloatingPointRGBA(0, 0, getWidth(), getHeight());
    }

    /**
     * Reads the pixels from the texture as floating point numbers.
     * The entire framebuffer will be read.
     * @return An array containing the pixels as floating point numbers.
     */
    default ColorList readColorListRGBA(int x, int y, int width, int height)
    {
        ColorNativeBufferList list = new ColorNativeBufferList(width * height);
        readFloatingPointRGBA(list.buffer, x, y, width, height);
        return list;
    }

    /**
     * Reads the pixels from the texture as floating point numbers.
     * The entire framebuffer will be read.
     * @return An array containing the pixels as floating point numbers.
     */
    default ColorList readColorListRGBA()
    {
        ColorNativeBufferList list = new ColorNativeBufferList(getWidth() * getHeight());
        readFloatingPointRGBA(list.buffer);
        return list;
    }

    /**
     * Reads the pixels from the texture as 32-bit integers.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @return An array containing the pixels as floating point numbers.
     */
    int[] readIntegerRGBA(int x, int y, int width, int height);

    /**
     * Reads the pixels from the texture as 32-bit integers.
     * The entire framebuffer will be read.
     * @return An array containing the pixels as floating point numbers.
     */
    default int[] readIntegerRGBA()
    {
        return this.readIntegerRGBA(0, 0, getWidth(), getHeight());
    }

    /**
     * Saves the pixels from the texture to a file.
     * The entire framebuffer will be read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveToFile(String fileFormat, File file) throws IOException;

    /**
     * Saves the pixels from the texture to a file and applies tonemapping.
     * The entire framebuffer will be read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @param tonemapper The tonemapping function to be applied (operates in unnormalized [0, 255] space)
     *                   Values will be automatically clamped to [0, 255] after applying tonemapping.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveToFile(String fileFormat, File file, Function<IntVector4, IntVector4> tonemapper) throws IOException;

    /**
     * Saves the pixels from the texture to a file and applies tonemapping.
     * The entire framebuffer will be read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @param tonemapper The tonemapping function to be applied (operates in unnormalized [0, 255] space)
     *                   Values will be automatically clamped to [0, 255] after applying tonemapping.
     *                   The second parameter of the tonemapping function is the index of the pixel.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveToFile(String fileFormat, File file, BiFunction<IntVector4, Integer, IntVector4> tonemapper) throws IOException;

    /**
     * Saves the pixels from the texture to a file.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveToFile(int x, int y, int width, int height, String fileFormat, File file) throws IOException;

    /**
     * Saves the the pixels from the texture to a file and applies tonemapping.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @param tonemapper The tonemapping function to be applied (operates in unnormalized [0, 255] space).
     *                   Values will be automatically clamped to [0, 255] after applying tonemapping.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveToFile(int x, int y, int width, int height, String fileFormat, File file, Function<IntVector4, IntVector4> tonemapper) throws IOException;

    /**
     * Saves the pixels from the texture to a file and applies tonemapping.
     * Only a rectangular subset of the pixels will be read.
     * @param x The column at which to begin reading.
     * @param y The row at which to begin reading.
     * @param width The number of columns to read.
     * @param height The number of rows to read.
     * @param fileFormat The format of the file to be written.
     * @param file The file to be written.
     * @param tonemapper The tonemapping function to be applied (operates in unnormalized [0, 255] space).
     *                   Values will be automatically clamped to [0, 255] after applying tonemapping.
     *                   The second parameter of the tonemapping function is the index of the pixel.
     * @throws IOException Thrown if any file I/O problems occur when writing the file.
     */
    void saveToFile(int x, int y, int width, int height, String fileFormat, File file, BiFunction<IntVector4, Integer, IntVector4> tonemapper) throws IOException;
}
