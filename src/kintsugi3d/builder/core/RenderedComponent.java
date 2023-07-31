/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.core;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.vecmath.Matrix4;

public interface RenderedComponent<ContextType extends Context<ContextType>> extends AutoCloseable
{
    void initialize() throws Exception;

    /**
     * May reload shaders if compiled settings have changed
     * @throws Exception
     */
    default void update() throws Exception
    {
    }

    /**
     * Force reload shaders
     * @throws Exception
     */
    void reloadShaders() throws Exception;

    void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport);

    default void draw(FramebufferObject<ContextType> framebuffer, Matrix4 view, Matrix4 projection)
    {
        FramebufferSize size = framebuffer.getSize();
        draw(framebuffer, new CameraViewport(view, projection, projection, 0, 0, size.width, size.height));
    }

    /**
     * Optionally render in subdivisions to prevent GPU timeout
     * @param framebuffer
     * @param subdivWidth
     * @param subdivHeight
     * @param cameraViewport
     */
    default void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight,
                                    CameraViewport cameraViewport)
    {
        for (int x = cameraViewport.getX(); x < cameraViewport.getWidth(); x += subdivWidth)
        {
            for (int y = cameraViewport.getY(); y < cameraViewport.getHeight(); y += subdivHeight)
            {
                int effectiveWidth = Math.min(subdivWidth, cameraViewport.getWidth() - x);
                int effectiveHeight = Math.min(subdivHeight, cameraViewport.getHeight() - y);

                float scaleX = (float)cameraViewport.getWidth() / (float)effectiveWidth;
                float scaleY = (float)cameraViewport.getHeight() / (float)effectiveHeight;
                float centerX = (2 * x + effectiveWidth - cameraViewport.getWidth()) / (float)cameraViewport.getWidth();
                float centerY = (2 * y + effectiveHeight - cameraViewport.getHeight()) / (float)cameraViewport.getHeight();

                Matrix4 viewportProjection = Matrix4.scale(scaleX, scaleY, 1.0f)
                        .times(Matrix4.translate(-centerX, -centerY, 0))
                        .times(cameraViewport.getViewportProjection());

                // Render to off-screen buffer
                this.draw(framebuffer,
                    new CameraViewport(cameraViewport.getView(), cameraViewport.getFullProjection(),
                        viewportProjection, x, y, effectiveWidth, effectiveHeight));

                // Flush to prevent timeout
                framebuffer.getContext().flush();
            }
        }
    }

    /**
     * Optionally render in subdivisions to prevent GPU timeout
     * @param framebuffer
     * @param subdivWidth
     * @param subdivHeight
     * @param view
     * @param projection
     */
    default void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight,
                                    Matrix4 view, Matrix4 projection)
    {
        FramebufferSize size = framebuffer.getSize();
        drawInSubdivisions(framebuffer, subdivWidth, subdivHeight,
            new CameraViewport(view, projection, projection, 0, 0, size.width, size.height));
    }
}
