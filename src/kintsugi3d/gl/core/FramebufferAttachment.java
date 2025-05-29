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

/**
 * An interface for an object that can serve as an attachment for a framebuffer.
 * This is an empty interface that simply serves as a placeholder in the type hierarchy,
 * so that a class can explicitly state that it can serve this role in a manner that can be checked at compile-time with loose coupling.
 * Implementations should provide whatever methods are needed to ensure that they can fulfill this role for a specific GL architecture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the framebuffer attachment is associated with.
 * Sub-types should be able to appropriately handle usage in conjunction with to any implementation of Framebuffer&lt;ContextType&gt;.
 * This could mean ensuring compatibility with any such implementation, and/or throwing an exception for implementations that are not compatible.
 */
public interface FramebufferAttachment<ContextType extends Context<ContextType>> extends ContextBound<ContextType>
{

}
