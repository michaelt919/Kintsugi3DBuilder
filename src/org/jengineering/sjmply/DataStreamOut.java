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