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

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.OutputStream;;

class DataStreamOutASCII extends DataStreamOut
{
// FIELDS
  private final OutputStream out;
  private boolean first = true;

// CONSTRUCTORS
  DataStreamOutASCII( OutputStream _out ) { out =_out; }

// METHODS
  @Override
  void endElement() throws IOException
  {
    out.write('\n');
    first = true;
  }

  @Override void writeInt8 ( byte  int8 ) throws IOException { writeInt64(int8);}
  @Override void writeUInt8( byte uint8 ) throws IOException { writeInt64( Byte.toUnsignedLong(uint8) ); }

  @Override void writeInt16 ( short  int16 ) throws IOException { writeInt64(int16); }
  @Override void writeUInt16( short uint16 ) throws IOException { writeInt64( Short.toUnsignedLong(uint16) ); }

  @Override void writeInt32 ( int  int32 ) throws IOException { writeInt64(int32); }
  @Override void writeUInt32( int uint32 ) throws IOException { writeInt64( Integer.toUnsignedLong(uint32) ); }

  private void writeInt64( long int64 ) throws IOException
  {
    if( ! first )
      out.write(' ');
    first = false;
    out.write( Long.toString(int64).getBytes(US_ASCII) );
  }

  @Override void writeFloat32( float float32 ) throws IOException { writeFloat64(float32); }
  @Override void writeFloat64( double float64 ) throws IOException
  {
    if( ! first )
      out.write(' ');
    first = false;
    out.write( Double.toString(float64).getBytes(US_ASCII) );
  }
}