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

package kintsugi3d.builder.core;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.core.Resource;
import kintsugi3d.gl.vecmath.Matrix4;

public interface RenderedComponent<ContextType extends Context<ContextType>> extends Resource
{
    void initialize();

    /**
     * May reload shaders if compiled settings have changed
     */
    default void update()
    {
    }

    /**
     * Force reload shaders
     */
    void reloadShaders();

    void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport);

    default void draw(FramebufferObject<ContextType> framebuffer, Matrix4 view, Matrix4 projection)
    {
        FramebufferSize size = framebuffer.getSize();
        draw(framebuffer, new CameraViewport(view, projection, Matrix4.IDENTITY, 0, 0, size.width, size.height));
    }

    static Matrix4 getViewportCrop(
        float x, float y, float effectiveWidth, float effectiveHeight, float fullWidth, float fullHeight)
    {
        float scaleX = fullWidth / effectiveWidth;
        float scaleY = fullHeight / effectiveHeight;
        float centerX = (2 * x + effectiveWidth - fullWidth) / fullWidth;
        float centerY = (2 * y + effectiveHeight - fullHeight) / fullHeight;

        return Matrix4.scale(scaleX, scaleY, 1.0f)
            .times(Matrix4.translate(-centerX, -centerY, 0));
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

                Matrix4 viewportCrop =
                    getViewportCrop(x, y, effectiveWidth, effectiveHeight, cameraViewport.getWidth(), cameraViewport.getHeight())
                        .times(cameraViewport.getViewportCrop());

                // Render to off-screen buffer
                this.draw(framebuffer,
                    new CameraViewport(cameraViewport.getView(), cameraViewport.getFullProjection(),
                        viewportCrop, x, y, effectiveWidth, effectiveHeight));

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
            new CameraViewport(view, projection, Matrix4.IDENTITY, 0, 0, size.width, size.height));
    }
}
