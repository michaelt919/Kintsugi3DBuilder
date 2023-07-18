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

package tetzlaff.util;

import tetzlaff.gl.core.*;
import tetzlaff.gl.glfw.WindowFactory;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.gl.window.PollableWindow;
import tetzlaff.ibrelight.core.DistortionProjection;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;

public class ImageUndistorter<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final Context<ContextType> context;
    private final Program<ContextType> program;
    private final VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;

    public ImageUndistorter(Context<ContextType> context) throws FileNotFoundException
    {
        this.context = context;

        program = context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/imageUndistort.frag"))
                .createProgram();

        rect = context.createRectangle();

        drawable = context.createDrawable(program);
        drawable.addVertexBuffer("position", rect);
        drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
    }

    public Texture2D<ContextType> undistort(Texture2D<ContextType> inputImage, DistortionProjection distortion)
    {
        drawable.program().setTexture("inputImage", inputImage);
        drawable.program().setUniform("viewportSize", new Vector2(inputImage.getWidth(), inputImage.getHeight()));

        drawable.program().setUniform("focalLength", new Vector2(distortion.fx, distortion.fy));
        drawable.program().setUniform("opticalCenter", new Vector2(distortion.cx, distortion.cy));
        drawable.program().setUniform("coefficientsK", new Vector4(distortion.k1, distortion.k2, distortion.k3, distortion.k4));
        drawable.program().setUniform("coefficientsP", new Vector2(distortion.p1, distortion.p2));

        FramebufferObject<ContextType> framebuffer = context.buildFramebufferObject(inputImage.getWidth(), inputImage.getHeight())
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject();

        drawable.draw(framebuffer);
        int[] pixels = framebuffer.getTextureReaderForColorAttachment(0).readARGB();
        BufferedImage image = BufferedImageBuilder.build()
            .setDataFromArray(pixels, inputImage.getWidth(), inputImage.getHeight())
            .flipVertical()
            .create();
        return context.getTextureFactory().build2DColorTextureFromImage(image, false).createTexture();
    }

    @Override
    public void close() throws Exception
    {

    }

    //Testing bootstrap
    public static void main(String[] args) throws Exception
    {
        PollableWindow<OpenGLContext> window = WindowFactory.buildOpenGLWindow("Test", 100, 100)
                .setResizable(true)
                .setMultisamples(4)
                .create();

        Context<OpenGLContext> context = window.getContext();
        window.show();

        ImageUndistorter<OpenGLContext> iu = new ImageUndistorter<OpenGLContext>(context);

        //DistortionProjection proj = new DistortionProjection(480f, 360f, 480f, 480f, 240f, 180f, 0.18f, 0.29f, 0.67f, -0.41f, -0.06f, 0.11f, 0);
        DistortionProjection proj = new DistortionProjection(480f, 360f, 480f, 480f, 240f, 180f, 0.2f, 0f, 0, 0, 0,0,0);

        BufferedImage in = ImageIO.read(new File("X:\\CHViewer\\tmp\\canvas-simple.png"));
        Texture2D<OpenGLContext> inTex = context.getTextureFactory().build2DColorTextureFromImage(in, false).createTexture();
        Texture2D<OpenGLContext> out = iu.undistort(inTex, proj);
        out.getColorTextureReader().saveToFile("PNG", new File("X:\\CHViewer\\tmp\\out.png"));
    }
}
