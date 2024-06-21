package org.jengineering.sjmply;
/* Copyright 2016 Dirk Toewe
 * 
 * This file is part of org.jengineering.sjmply.
 *
 * org.jengineering.sjmply is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * org.jengineering.sjmply is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.jengineering.sjmply. If not, see <http://www.gnu.org/licenses/>.
 */

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