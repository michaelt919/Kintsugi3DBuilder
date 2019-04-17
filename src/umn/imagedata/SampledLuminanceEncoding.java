/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.imagedata;

import java.util.function.DoubleUnaryOperator;

import umn.gl.core.ColorFormat;
import umn.gl.core.Context;
import umn.gl.core.Texture1D;
import umn.gl.nativebuffer.NativeDataType;
import umn.gl.nativebuffer.NativeVectorBuffer;
import umn.gl.nativebuffer.NativeVectorBufferFactory;
import umn.util.CubicHermiteSpline;

/**
 * A class for representing the luminance encoding for an image.
 * This class allows us to convert between tonemapped and physically linear images by interpolating a discrete number of luminance calibration samples
 * using cubic hermite splines.
 */
public class SampledLuminanceEncoding 
{
    /**
     * Gets the function that converts from encoded (tonemapped) luminance values to decoded (linear) luminance values.
     */
    public final DoubleUnaryOperator decodeFunction;

    /**
     * Gets the function that converts from decoded (linear) luminance values to encoded (tonemapped) luminance values.
     */
    public final DoubleUnaryOperator encodeFunction;

    /**
     * Creates a new encoding using only a gamma curve, with no luminance calibration.
     * @param gamma The exponent used for gamma correction.
     */
    public SampledLuminanceEncoding(float gamma)
    {
        this.decodeFunction = encoded -> Math.pow(encoded / 255.0, gamma);
        this.encodeFunction = decoded -> Math.pow(decoded, 1.0 / gamma) * 255.0;
    }

    /**
     * Creates a new encoding using both a gamma curve and discrete number of luminance calibration samples.
     * @param linear An array containing the known, linear reflectance values of one or more color calibration targets.
     * This array should have the same length as encodedLuminanceValues.  If this is not the case, an exception will be thrown.
     * @param encoded An array containing the representative pixel values (ranging from 0-255) for one or more color calibration targets.
     * The values in the array are assumed to be unsigned bytes; this means that it is necessary to mask each value with 0x000000FF in order for Java
     * to correctly interpret the value as an exclusively positive value between 0 and 255.
     * This array should have the same length as linearLuminanceValues.  If this is not the case, an exception will be thrown
     * @param gamma The exponent used for gamma correction.
     */
    public SampledLuminanceEncoding(double[] linear, byte[] encoded, float gamma)
    {
        if (linear.length != encoded.length)
        {
            throw new IllegalArgumentException("Input arrays must be of equal length.");
        }

        double[] x = new double[encoded.length + 2];
        x[0] = 0.0;
        for (int k = 1; k < x.length-1; k++)
        {
            x[k] = (double)(0xFF & (int)encoded[k-1]);
        }
        x[x.length-1] = 255.0;

        double[] y = new double[linear.length + 2];
        y[0] = 0.0;
        for (int k = 1; k < y.length-1; k++)
        {
            y[k] = Math.pow(linear[k-1], 1.0 / gamma);
        }
        y[y.length-1] = y[y.length-2] * 255.0 / x[x.length-2]; // Extrapolate linearly in gamma-corrected color space

        this.decodeFunction = new CubicHermiteSpline(x, y, true)
            .andThen(gammaCorrected -> Math.pow(gammaCorrected, gamma));

        this.encodeFunction = new CubicHermiteSpline(y, x, true)
            .compose(physicallyLinear -> Math.pow(physicallyLinear, 1.0 / gamma));
    }

    /**
     * Creates a new encoding using arbitrary decode and encode functions.
     * @param decodeFunction The function to use to decode, that is, to convert tonemapped luminance values to linear luminance values.
     * @param encodeFunction The function to use to encode, that is, to convert linear luminance values to tonemapped luminance values.
     */
    public SampledLuminanceEncoding(DoubleUnaryOperator decodeFunction, DoubleUnaryOperator encodeFunction)
    {
        this.decodeFunction = decodeFunction;
        this.encodeFunction = encodeFunction;
    }

    /**
     * Gets the decode function, sampled at 256 discrete intervals, in a native memory buffer that can be passed to a uniform buffer in a graphics context.
     * @return A buffer containing the sampled decode function.
     */
    public NativeVectorBuffer sampleDecodeFunction()
    {
        NativeVectorBuffer sampledDecodeFunction = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 256);
        for (int i = 0; i < 256; i++)
        {
            sampledDecodeFunction.set(i, 0, (float)decodeFunction.applyAsDouble(i));
        }

        return sampledDecodeFunction;
    }

    /**
     * Gets the encode function, sampled at 256 discrete intervals, in a native memory buffer that can be passed to a uniform buffer in a graphics context.
     * @return A buffer containing the sampled encode function.
     */
    public NativeVectorBuffer sampleEncodeFunction()
    {
        NativeVectorBuffer sampledEncodeFunction = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 256);
        for (int i = 0; i < 256; i++)
        {
            sampledEncodeFunction.set(i, 0, (float)encodeFunction.applyAsDouble((double)i / 255.0 * decodeFunction.applyAsDouble(255.0)) / 255.0f);
        }

        return sampledEncodeFunction;
    }

    /**
     * Creates a 1D texture in a graphics context that can be used to apply the decode function in a shader program.
     * @param context The graphics context to create the texture for.
     * @param <ContextType> The type of graphics context.
     * @return A 1D texture containing the decode function sampled at 256 discrete intervals.
     */
    public <ContextType extends Context<ContextType>> Texture1D<ContextType> createLuminanceMap(ContextType context)
    {
        return context.getTextureFactory().build1DColorTexture(sampleDecodeFunction())
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .createTexture();
    }

    /**
     * Creates a 1D texture in a graphics context that can be used to apply the encode function in a shader program.
     * @param context The graphics context to create the texture for.
     * @param <ContextType> The type of graphics context.
     * @return A 1D texture containing the encode function sampled at 256 discrete intervals.
     */
    public <ContextType extends Context<ContextType>> Texture1D<ContextType> createInverseLuminanceMap(ContextType context)
    {
        return context.getTextureFactory().build1DColorTexture(sampleEncodeFunction())
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .createTexture();
    }
}
