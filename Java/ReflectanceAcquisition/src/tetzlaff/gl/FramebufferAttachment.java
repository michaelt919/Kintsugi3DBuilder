/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl;

/**
 * An interface for an object that can serve as an attachment for a framebuffer.
 * This is an empty interface that simply serves as a placeholder in the type hierarchy,
 * so that a class can explicitly state that it can serve this role in a manner that can be checked at compile-time with loose coupling.
 * Implementations should provide whatever methods are needed to ensure that they can fulfill this role for a specific GL architecture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the framebuffer attachment is associated with.
 * Sub-types should be able to appropriately handle usage in conjunction with to any implementation of Framebuffer<ContextType>.
 * This could mean ensuring compatibility with any such implementation, and/or throwing an exception for implementations that are not compatible.
 */
public interface FramebufferAttachment<ContextType extends Context<ContextType>> extends Contextual<ContextType>
{

}
