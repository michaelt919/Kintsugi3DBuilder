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

package kintsugi3d.builder.rendering.components.split;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.vecmath.Matrix4;

/**
 * A component where half of the screen is rendered using one RenderedComponent, and the other half is rendered
 * using another RenderedComponent.
 * The left and right RenderedComponents are not managed by this component, and will not be initialized, updated, or
 * closed by this component -- only the draw() method calls the left and right components draw() methods.
 * @param <ContextType>
 */
public class SplitScreenComponent<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final RenderedComponent<ContextType> leftHalf;
    private final RenderedComponent<ContextType> rightHalf;

    private float splitPercentage;
    private int splitPixel;

    /**
     * leftHalf and rightHalf are NOT considered owned by this component;
     * i.e. close() will not release resources.
     * @param leftHalf
     * @param rightHalf
     */
    public SplitScreenComponent(RenderedComponent<ContextType> leftHalf, RenderedComponent<ContextType> rightHalf)
    {
        this.leftHalf = leftHalf;
        this.rightHalf = rightHalf;
    }

    /**
     * Does nothing as each half of the split screen is expected to be managed by another object.
     */
    @Override
    public void initialize()
    {
    }

    /**
     * Does nothing as each half of the split screen is expected to be managed by another object.
     */
    @Override
    public void reloadShaders()
    {
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight,
                                   CameraViewport cameraViewport)
    {
        for (int x = cameraViewport.getX(); x < cameraViewport.getWidth(); x += subdivWidth)
        {
            for (int y = cameraViewport.getY(); y < cameraViewport.getHeight(); y += subdivHeight)
            {
                // Check if the left half is in the subdivision
                if (x < splitPixel)
                {
                    // Scale y-axis to adjust aspect ratio to split.
                    Matrix4 leftFullProj =
                        Matrix4.scale(1.0f, (float) splitPercentage, 1.0f).times(cameraViewport.getFullProjection());

                    // Viewport crop matrix will be different for each half.
                    int effectiveWidth = Math.min(subdivWidth, splitPixel - x);
                    int effectiveHeight = Math.min(subdivHeight, cameraViewport.getHeight() - y);
                    Matrix4 viewportCrop =
                        RenderedComponent.getViewportCrop(x, y, effectiveWidth, effectiveHeight, splitPixel, cameraViewport.getHeight())
                            .times(cameraViewport.getViewportCrop());

                    // draw for the left half of the viewport
                    CameraViewport leftViewport = new CameraViewport(cameraViewport.getView(), leftFullProj, viewportCrop,
                        x, y, Math.min(effectiveWidth, splitPixel - x), effectiveHeight);
                    leftHalf.draw(framebuffer, leftViewport);
                }

                int effectiveWidth = Math.min(subdivWidth, cameraViewport.getWidth() - x);
                int endX = x + effectiveWidth;

                // Check if the right half is in the subdivision
                if (splitPixel < endX)
                {
                    // Scale y-axis to adjust aspect ratio to split.
                    Matrix4 rightFullProj =
                        Matrix4.scale(1.0f, (float) (1 - splitPercentage), 1.0f).times(cameraViewport.getFullProjection());

                    // Viewport crop matrix will be different for each half.
                    effectiveWidth = Math.min(effectiveWidth, x + subdivWidth - splitPixel); // adjust for case when subdiv spans both halves of the split.
                    int effectiveHeight = Math.min(subdivHeight, cameraViewport.getHeight() - y);
                    Matrix4 viewportCrop =
                        RenderedComponent.getViewportCrop(Math.max(0, x - splitPixel), y, effectiveWidth, effectiveHeight,
                                cameraViewport.getWidth() - splitPixel, cameraViewport.getHeight())
                            .times(cameraViewport.getViewportCrop());

                    // draw for the right half of the viewport
                    CameraViewport rightViewport = new CameraViewport(cameraViewport.getView(), rightFullProj, viewportCrop,
                        Math.max(x, splitPixel), y, Math.min(effectiveWidth, endX - splitPixel), effectiveHeight);
                    rightHalf.draw(framebuffer, rightViewport);
                }

                // Flush to prevent timeout
                framebuffer.getContext().flush();
            }
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        this.drawInSubdivisions(framebuffer, cameraViewport.getWidth(), cameraViewport.getHeight(), cameraViewport);
    }

    /**
     * Does nothing as each half of the split screen is expected to be managed by another object.
     */
    @Override
    public void close()
    {
    }

    /**
     * Gets the percentage of the full viewport where the split between the left and right content occurs.
     * @return
     */
    public float getSplitPercentage()
    {
        return splitPercentage;
    }

    /**
     * Gets the pixel x-coordinate where the split between the left and right content occurs.
     * @return
     */
    public double getSplitPixel()
    {
        return splitPixel;
    }

    /**
     * Sets the viewport percentage where the split between the left and right content occurs.
     * @param splitPercentage
     * @param fullViewportWidth Used to determine the pixel x-coordinate where the split occurs.
     */
    public void setSplit(float splitPercentage, int fullViewportWidth)
    {
        this.splitPercentage = splitPercentage;
        this.splitPixel = (int)Math.round(fullViewportWidth * splitPercentage);
    }
}
