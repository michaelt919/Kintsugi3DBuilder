/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core;

import java.util.function.DoubleUnaryOperator;

import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Texture1D;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.builder.resources.LuminanceMapResources;
import kintsugi3d.util.CubicHermiteSpline;

public class SampledLuminanceEncoding 
{
    public final DoubleUnaryOperator decodeFunction;
    public final DoubleUnaryOperator encodeFunction;

    public SampledLuminanceEncoding(float gamma)
    {
        this.decodeFunction = encoded -> Math.pow(encoded / 255.0, gamma);
        this.encodeFunction = decoded -> Math.pow(decoded, 1.0 / gamma) * 255.0;
    }

    public SampledLuminanceEncoding(double[] linear, byte[] encoded, float gamma)
    {
        if (linear.length != encoded.length)
        {
            throw new IllegalArgumentException("Input arrays must be of equal length.");
        }

        int length = encoded.length;

        // Add endpoint if first encoded value is not 0
        if (encoded[0] != (byte)0x00)
        {
            length++;
        }

        // Add endpoint if last encoded value is not 255
        if (encoded[encoded.length-1] != (byte)0xFF)
        {
            length++;
        }

        double[] x = new double[length];
        double[] y = new double[length];

        int k = 0;

        // Add endpoint if first encoded value is not 0
        if (encoded[0] != (byte)0x00)
        {
            x[0] = 0.0;
            y[0] = 0.0;
            k++;
        }

        // Fill intermediate values.
        for (int i = 0; i < encoded.length; i++)
        {
            x[k] = (double)(0xFF & (int)encoded[i]);
            y[k] = Math.pow(linear[i], 1.0 / gamma);
            k++;
        }

        // Add endpoint if last encoded value is not 255
        if (encoded[encoded.length-1] != (byte)0xFF)
        {
            x[x.length - 1] = 255.0;
            y[y.length - 1] = y[y.length - 2] * 255.0 / x[x.length - 2]; // Extrapolate linearly in gamma-corrected color space
        }

        this.decodeFunction = new CubicHermiteSpline(x, y, true)
            .andThen(gammaCorrected -> Math.pow(gammaCorrected, gamma));

        this.encodeFunction = new CubicHermiteSpline(y, x, true)
            .compose(physicallyLinear -> Math.pow(physicallyLinear, 1.0 / gamma));
    }

    public SampledLuminanceEncoding(DoubleUnaryOperator decodeFunction, DoubleUnaryOperator encodeFunction)
    {
        this.decodeFunction = decodeFunction;
        this.encodeFunction = encodeFunction;
    }

    public ReadonlyNativeVectorBuffer sampleDecodeFunction()
    {
        NativeVectorBuffer sampledDecodeFunction = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 256);
        for (int i = 0; i < 256; i++)
        {
            sampledDecodeFunction.set(i, 0, (float)decodeFunction.applyAsDouble(i));
        }

        return sampledDecodeFunction;
    }

    public ReadonlyNativeVectorBuffer sampleEncodeFunction()
    {
        NativeVectorBuffer sampledEncodeFunction = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 256);
        for (int i = 0; i < 256; i++)
        {
            sampledEncodeFunction.set(i, 0, (float)encodeFunction.applyAsDouble((double)i / 255.0 * decodeFunction.applyAsDouble(255.0)) / 255.0f);
        }

        return sampledEncodeFunction;
    }

    public <ContextType extends Context<ContextType>> Texture1D<ContextType> createLuminanceMap(ContextType context)
    {
        return context.getTextureFactory().build1DColorTexture(sampleDecodeFunction())
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .createTexture();
    }

    public <ContextType extends Context<ContextType>> Texture1D<ContextType> createInverseLuminanceMap(ContextType context)
    {
        return context.getTextureFactory().build1DColorTexture(sampleEncodeFunction())
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .createTexture();
    }

    public <ContextType extends Context<ContextType>>LuminanceMapResources<ContextType> createResources(ContextType context)
    {
        return LuminanceMapResources.createFromEncoding(context, this);
    }
}
