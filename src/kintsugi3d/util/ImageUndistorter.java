/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector4;

public class ImageUndistorter<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final Context<ContextType> context;
    private final ProgramObject<ContextType> program;
    private final VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;

    public ImageUndistorter(Context<ContextType> context) throws IOException
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
        drawable.program().setUniform("viewportSize", new Vector2(distortion.width, distortion.height));

        drawable.program().setUniform("focalLength", new Vector2(distortion.fx, distortion.fy));
        drawable.program().setUniform("opticalCenter", new Vector2(distortion.cx, distortion.cy));
        drawable.program().setUniform("coefficientsK", new Vector4(distortion.k1, distortion.k2, distortion.k3, distortion.k4));
        drawable.program().setUniform("coefficientsP", new Vector2(distortion.p1, distortion.p2));
        drawable.program().setUniform("skew", distortion.skew);

        try (FramebufferObject<ContextType> framebuffer = context
                .buildFramebufferObject(Math.round(distortion.width), Math.round(distortion.height))
                .addEmptyColorAttachment()
                .createFramebufferObject())
        {
            Texture2D<ContextType> outputTexture = context.getTextureFactory()
                .build2DColorTexture(framebuffer.getSize().width, framebuffer.getSize().height)
                .createTexture();
            framebuffer.setColorAttachment(0, outputTexture);

            drawable.draw(framebuffer);

            return outputTexture;
        }
    }

    public BufferedImage undistort(BufferedImage inputImage, boolean mipmapsEnabled, DistortionProjection distortion)
    {
        try(Texture2D<ContextType> inTex = context.getTextureFactory()
            .build2DColorTextureFromImage(inputImage, true)
            .setLinearFilteringEnabled(true)
            .setMipmapsEnabled(mipmapsEnabled)
            .createTexture();
            Texture2D<ContextType> outTex = undistort(inTex, distortion))
        {
            return BufferedImageBuilder.build()
                .setDataFromArray(outTex.getColorTextureReader().readARGB(), outTex.getWidth(), outTex.getHeight())
                .flipVertical()
                .create();
        }
    }

    public void undistortFile(File inputImage, boolean mipmapsEnabled, DistortionProjection distortion, File outputImage) throws IOException
    {
        BufferedImage imageIn = ImageIO.read(inputImage);
        BufferedImage imageOut = undistort(imageIn, mipmapsEnabled, distortion);
        ImageIO.write(imageOut, "PNG", outputImage);
    }

    @Override
    public void close()
    {
        program.close();
        drawable.close();
        rect.close();
    }

//    //Testing bootstrap
//    public static void main(String[] args) throws Exception
//    {
//        PollableWindow<OpenGLContext> window = WindowFactory.buildOpenGLWindow("Test", 100, 100)
//                .setResizable(true)
//                .setMultisamples(4)
//                .create();
//
//        Context<OpenGLContext> context = window.getContext();
//        window.show();
//
//        ImageUndistorter<OpenGLContext> iu = new ImageUndistorter<OpenGLContext>(context);
//
//        //DistortionProjection proj = new DistortionProjection(480f, 360f, 480f, 480f, 240f, 180f, 0.18f, 0.29f, 0.67f, -0.41f, -0.06f, 0.11f, 0);
//        DistortionProjection proj = new DistortionProjection(480f, 360f, 480f, 480f, 240f, 180f,  0.7f, 0.78f, -0.83f, 0, 0,0,0);
//
//        BufferedImage in = ImageIO.read(new File("X:\\CHViewer\\tmp\\canvas2.png"));
//        Texture2D<OpenGLContext> inTex = context.getTextureFactory().build2DColorTextureFromImage(in, true).createTexture();
//        Texture2D<OpenGLContext> out = iu.undistort(inTex, proj);
//        out.getColorTextureReader().saveToFile("PNG", new File("X:\\CHViewer\\tmp\\out.png"));
//    }
}
