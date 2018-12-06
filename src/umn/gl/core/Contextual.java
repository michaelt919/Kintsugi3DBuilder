/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.core;

/**
 * A simple interface for a type that is associated with a Context implementation.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the Context.
 */
public interface Contextual<ContextType extends Context<ContextType>>
{
    /**
     * Gets the associated GL context.
     * @return A GL context associated with this object.
     */
    ContextType getContext();
}
