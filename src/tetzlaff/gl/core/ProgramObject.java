/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.core;

import java.util.Optional;

import tetzlaff.gl.vecmath.*;

/**
 * Represents a program that is a graphics resource, which must be closed when no longer needed.
 *
 * @param <ContextType> The type of the GL context that the program is associated with.
 */
public interface ProgramObject<ContextType extends Context<ContextType>> extends Resource, Program<ContextType>
{

}