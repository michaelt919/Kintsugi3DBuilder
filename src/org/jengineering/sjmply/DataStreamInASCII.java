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

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;

/** A utility InputStream-wrapper for parsing ASCII PLY file contents.
 *  It skips empty lines automatically and handles line breaks. Data
 *  can only be continually read until a line break. Once a line break
 *  is encountered the stream gets paused. All further attempts to {@link #read()}
 *  from the stream will cause an {@link IllegalStateException}, until
 *  {@link #resume()} is called.
 *  
 *  @author Dirk Toewe
 */
class DataStreamInASCII extends InputStream implements DataStreamIn
{
// FIELDS
  private final InputStream in;
  private int
    lineCountTotal;
  private boolean
    carriageReturnEncountered = false,
    firstReadInLine = true,
    paused = false;

// CONSTRUCTORS
  DataStreamInASCII( HeaderInputStream lineReader )
  {
    in = lineReader.in;
    lineCountTotal = 1 + lineReader.lineCountTotal();
    carriageReturnEncountered = lineReader.carriageReturnEncountered;
  }

// METHODS
  @Override public int read() throws IOException
  {
    if( paused )
      throw new IOException("Line ended unexpectedly.");
    assert ! (carriageReturnEncountered && ! firstReadInLine);
    read: for(;;)
    {
      int chr = in.read();
      if( carriageReturnEncountered && chr == '\n' )
        chr = in.read();
      carriageReturnEncountered = false;
  
      if( firstReadInLine ) // skip leading whitespaces and completely empty lines
        while( Character.isWhitespace(chr) )
          switch(chr) {
            case '\r': carriageReturnEncountered = true;
            case '\n':
              lineCountTotal += 1;
              continue read;
            default: chr = in.read();
          }
      else if( '\n' == chr || '\r' == chr )
      {
        lineCountTotal += 1;
        carriageReturnEncountered = '\r' == chr;
        paused = true;
      }
      else paused = -1 == chr;
      firstReadInLine = false;
      return chr;
    }
  }

  public void endElement() throws IOException
  {
    while( ! paused ) {
      int chr = read();
      if( -1 != chr && ! Character.isWhitespace(chr) )
        throw new IOException( format("Invalid character at line end: %s.", Decimal.tokenStr(chr) ) );
    }
    firstReadInLine = true;
    paused = false;
  }

  public int lineCountTotal() { return lineCountTotal; }

  @Override public byte readInt8() throws IOException
  {
    long result = Integral.read(this);
    if( Byte.MIN_VALUE > result || result > Byte.MAX_VALUE )
      throw new IOException( format("%d is not a valid INT8.", result) );
    return (byte) result;
  }

  @Override public byte readUInt8() throws IOException
  {
    long result = Integral.read(this);
    if( 0 > result || result > (1<<8)-1 )
      throw new IOException( format("%d is not a valid UINT8.", result) );
    return (byte) result;
  }

  @Override public short readInt16() throws IOException
  {
    long result = Integral.read(this);
    if( Short.MIN_VALUE > result || result > Short.MAX_VALUE )
      throw new IOException( format("%d is not a valid INT16.", result) );
    return (short) result;
  }

  @Override public short readUInt16() throws IOException
  {
    long result = Integral.read(this);
    if( 0 > result || result > (1<<16)-1 )
      throw new IOException( format("%d is not a valid UINT16.", result) );
    return (short) result;
  }

  @Override public int readInt32() throws IOException
  {
    long result = Integral.read(this);
    if( Integer.MIN_VALUE > result || result > Integer.MAX_VALUE )
      throw new IOException( format("%d is not a valid INT32.", result) );
    return (int) result;
  }

  @Override public int readUInt32() throws IOException
  {
    long result = Integral.read(this);
    if( 0 > result || result > (1L<<32)-1 )
      throw new IOException( format("%d is not a valid UINT32.", result) );
    return (int) result;
  }

  @Override public float  readFloat32() throws IOException { return (float) Decimal.read(this); }
  @Override public double readFloat64() throws IOException { return         Decimal.read(this); }

  @Override public void close() throws IOException { in.close(); }
}