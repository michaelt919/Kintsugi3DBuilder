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

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Float.floatToRawIntBits;

import java.io.IOException;
import java.io.OutputStream;

/** Writer for {@link PLYFormat#BINARY_LITTLE_ENDIAN} encoded PLY data.
 *  
 *  @author Dirk Toewe
 */
class DataStreamOutLE extends DataStreamOut
{
// FIELDS
  private final byte[] cache = new byte[8];
  private final OutputStream out;

// CONSTRUCTORS
  DataStreamOutLE( OutputStream _out ) { out =_out; }

// METHODS
  @Override void writeInt8 ( byte  int8 ) throws IOException { out.write( Byte.toUnsignedInt( int8) ); }
  @Override void writeUInt8( byte uint8 ) throws IOException { out.write( Byte.toUnsignedInt(uint8) );}

  @Override void writeInt16 ( short  int16 ) throws IOException { writeUInt16(int16); }
  @Override void writeUInt16( short uint16 ) throws IOException
  {
    cache[0] = (byte) (uint16 >>> 0);
    cache[1] = (byte) (uint16 >>> 8);
    out.write(cache,0,2);
  }

  @Override void writeInt32 ( int  int32 ) throws IOException { writeUInt32(int32); }
  @Override void writeUInt32( int uint32 ) throws IOException
  {
    cache[0] = (byte) (uint32 >>> 0);
    cache[1] = (byte) (uint32 >>> 8);
    cache[2] = (byte) (uint32 >>>16);
    cache[3] = (byte) (uint32 >>>24);
    out.write(cache,0,4);
  }

  @Override void writeFloat32( float float32 ) throws IOException { writeUInt32( floatToRawIntBits(float32) ); }
  @Override void writeFloat64( double float64 ) throws IOException
  {
    long bits = doubleToRawLongBits(float64);
    cache[0] = (byte) (bits >>> 0);
    cache[1] = (byte) (bits >>> 8);
    cache[2] = (byte) (bits >>>16);
    cache[3] = (byte) (bits >>>24);
    cache[4] = (byte) (bits >>>32);
    cache[5] = (byte) (bits >>>40);
    cache[6] = (byte) (bits >>>48);
    cache[7] = (byte) (bits >>>56);
    out.write(cache,0,8);
  }
}