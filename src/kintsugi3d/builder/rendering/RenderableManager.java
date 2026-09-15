/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering;

import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.interactive.InteractiveRenderable;
import kintsugi3d.gl.window.FramebufferCanvas;

import java.util.function.Consumer;

public interface RenderableManager<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{
    boolean isRenderableLoaded();
    ProjectRenderableInstance<ContextType> getMainRenderable(); // TODO this would ideally just be a ViewportRenderable
    void addMainRenderableLoadCallback(Consumer<ProjectRenderableInstance<?>> callback); // TODO this would ideally just be a ViewportRenderable
    void addRenderView(UserShader shader, FramebufferSize initialSize,
                       int safeLeftPadding, int safeTopPadding, int safeRightPadding, int safeBottomPadding,
                       Consumer<FramebufferCanvas<?>> framebufferCallback);
}
