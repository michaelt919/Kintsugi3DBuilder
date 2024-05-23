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