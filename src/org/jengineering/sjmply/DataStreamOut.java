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

abstract class DataStreamOut
{
// FIELDS

// CONSTRUCTORS

// METHODS
  void endElement() throws IOException {}

  abstract void writeInt8 ( byte  int8 ) throws IOException;
  abstract void writeUInt8( byte uint8 ) throws IOException;

  abstract void writeInt16 ( short  int16 ) throws IOException;
  abstract void writeUInt16( short uint16 ) throws IOException;

  abstract void writeInt32 ( int  int32 ) throws IOException;
  abstract void writeUInt32( int uint32 ) throws IOException;

  abstract void writeFloat32( float  float32 ) throws IOException;
  abstract void writeFloat64( double float64 ) throws IOException;
}