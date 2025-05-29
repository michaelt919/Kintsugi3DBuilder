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

package org.jengineering.sjmply;

import java.io.IOException;
/** Super interface of PLY file data input streams. Offers methods to read the different primitive values.
 *  
 *  @author Dirk Toewe
 */
interface DataStreamIn
{
// FIELDS

// CONSTRUCTORS

// METHODS
  /** Proceeds streaming in the data of the next element. This method is used by {@link DataStreamInASCII}
   *  to ensure that there is a line separator between the data of different elements.
   *  
   *  @throws IOException
   */
  default void endElement() throws IOException {}

  abstract byte readInt8 () throws IOException;
  abstract byte readUInt8() throws IOException;

  abstract short readInt16 () throws IOException;
  abstract short readUInt16() throws IOException;

  abstract int readInt32 () throws IOException;
  abstract int readUInt32() throws IOException;

  abstract float  readFloat32() throws IOException;
  abstract double readFloat64() throws IOException;
}