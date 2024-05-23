/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package org.jengineering.sjmply;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Byte.toUnsignedLong;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/** Reader for {@link PLYFormat#BINARY_LITTLE_ENDIAN} encoded PLY data.
 *  
 *  @author Dirk Toewe
 */
class DataStreamInLE implements DataStreamIn
{
// FIELDS
  private final byte[] cache = new byte[8];
  private final InputStream in;

// CONSTRUCTORS
  DataStreamInLE( InputStream _in ) { in =_in; }

// METHODS
  @Override public byte readInt8 () throws IOException { return readUInt8(); }
  @Override public byte readUInt8() throws IOException
  {
    int result = in.read();
    assert result < 256;
    if( result < 0 )
      throw new EOFException();
    return (byte) result;
  }

  @Override public short readInt16 () throws IOException { return readUInt16(); }
  @Override public short readUInt16() throws IOException
  {
    if( 2 != in.read(cache,0,2) )
      throw new EOFException();
    return (short) (
        toUnsignedInt(cache[0]) << 0
      | toUnsignedInt(cache[1]) << 8
    );
  }

  @Override public int readInt32 () throws IOException { return readUInt32(); }
  @Override public int readUInt32() throws IOException
  {
    if( 4 != in.read(cache,0,4) )
      throw new EOFException();
    return toUnsignedInt(cache[0]) << 0
         | toUnsignedInt(cache[1]) << 8
         | toUnsignedInt(cache[2]) <<16
         | toUnsignedInt(cache[3]) <<24;
  }

  @Override public float  readFloat32() throws IOException { return intBitsToFloat( readUInt32() ); }
  @Override public double readFloat64() throws IOException
  {
    if( 8 != in.read(cache,0,8) )
      throw new EOFException();
    return longBitsToDouble(
        toUnsignedLong(cache[0]) << 0
      | toUnsignedLong(cache[1]) << 8
      | toUnsignedLong(cache[2]) <<16
      | toUnsignedLong(cache[3]) <<24
      | toUnsignedLong(cache[4]) <<32
      | toUnsignedLong(cache[5]) <<40
      | toUnsignedLong(cache[6]) <<48
      | toUnsignedLong(cache[7]) <<56
    );
  }
}