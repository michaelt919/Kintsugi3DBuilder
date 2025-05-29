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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

/** A wrapper around an ASCII character input stream, which allows line-by-line reading
 *  of the data. The advantage of this class over a {@link BufferedReader} is that this
 *  class does not read any characters beyond the line being read. 
 *  
 *  @author Dirk Toewe
 */
class HeaderInputStream
{
// FIELDS
  final InputStream in;
  private final StringBuilder sb = new StringBuilder();
  private int
    lineCount = 0,
    lineCountTotal = 0;
  boolean carriageReturnEncountered = false;

// CONSTRUCTORS
  HeaderInputStream( InputStream _in ) { in =_in; }

// METHODS
  public CharSequence readLine() throws IOException
  {
    for(;;) {
      sb.setLength(0);
      int chr = in.read();
      if( carriageReturnEncountered && chr == '\n' )
        chr = in.read();
      carriageReturnEncountered = false;

      for( ; '\n' != chr && '\r' != chr; chr = in.read() )
        if( -1 == chr )
          throw new IOException("PLY file header ended unexpectedly.");
        else
          sb.append( (char) chr );

      carriageReturnEncountered = '\r' == chr;
      lineCountTotal += 1;
      if( sb.codePoints().allMatch(Character::isWhitespace) )
        continue;
      lineCount += 1;
      return sb;
    }
  }

  public int lineCount() { return lineCount; }
  public int lineCountTotal() { return lineCountTotal; }
}