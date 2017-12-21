package tetzlaff.ibrelight.core;

import java.util.function.DoubleUnaryOperator;

import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture1D;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.util.CubicHermiteSpline;

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

    public SampledLuminanceEncoding(DoubleUnaryOperator decodeFunction, DoubleUnaryOperator encodeFunction)
    {
        this.decodeFunction = decodeFunction;
        this.encodeFunction = encodeFunction;
    }

    public NativeVectorBuffer sampleDecodeFunction()
    {
        NativeVectorBuffer sampledDecodeFunction = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 256);
        for (int i = 0; i < 256; i++)
        {
            sampledDecodeFunction.set(i, 0, (float)decodeFunction.applyAsDouble(i));
        }

        return sampledDecodeFunction;
    }

    public NativeVectorBuffer sampleEncodeFunction()
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
        return context.build1DColorTexture(sampleDecodeFunction())
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .createTexture();
    }

    public <ContextType extends Context<ContextType>> Texture1D<ContextType> createInverseLuminanceMap(ContextType context)
    {
        return context.build1DColorTexture(sampleEncodeFunction())
                .setInternalFormat(ColorFormat.R32F)
                .setLinearFilteringEnabled(true)
                .createTexture();
    }
}
