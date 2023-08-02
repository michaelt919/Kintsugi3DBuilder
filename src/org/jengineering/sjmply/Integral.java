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

import static java.lang.Math.multiplyExact;
import static java.lang.Math.subtractExact;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Parser for decimal integer numbers.
 *  
 *  @author Dirk Toewe
 */
class Integral
{
  /** Returns the next character from this stream which is not a non-breaking whitespace.
   */
  static int skipNB( InputStream in ) throws IOException
  {
    for(;;) {
      int nextChar = in.read();
      switch(nextChar) {
        case ' ': case '\t': continue;
        default: return nextChar;
      }
    }
  }

  static long read( String str ) throws IOException
  {
    return read( new ByteArrayInputStream( str.getBytes() ) );
  }

  static long read( InputStream in ) throws IOException
  {
    int nextChar = skipNB(in);
    boolean isNegative = false; // the sign
     //
    // PARSE SIGN
   //
    switch( nextChar ) {
      case '-': isNegative = ! isNegative;
      case '+': nextChar = in.read();//whitespaces are fine in java ( e.g. in "double d = - 12e3" )
      default : break;
    }
     //
    // PARSE FIRST CHARACTER
   //
    switch( nextChar ) {
      default : throw new IOException("Invalid character: " + Decimal.tokenStr(nextChar) );
      case '0': nextChar = in.read();
        if( 'x' == nextChar || 'X' == nextChar )
          return parseHex(in,isNegative);
        while( '0' == nextChar )
          nextChar = in.read();
      case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
        return parseDecimal(nextChar,in,isNegative);
    }
  }

  private static long parseHex( InputStream in, boolean isNegative ) throws IOException
  {
    long result = 0;
    for(;;) {
      int nextChar = in.read();
      switch(nextChar) {
        default:
          Decimal.checkEnd(nextChar);
          if(isNegative) return result;
          else           return multiplyExact(-1,result);
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':           result = multiplyExact(result,16) - (nextChar - '0'   ); continue;
        case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': result = multiplyExact(result,16) - (nextChar - 'A'+10); continue;
        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': result = multiplyExact(result,16) - (nextChar - 'a'+10); continue;
      }
    }
  }

  private static long parseDecimal( int nextChar, InputStream reader, boolean isNegative ) throws IOException
  {
    long result = 0;
    for(;;)
      switch( nextChar ) {
        default:
          Decimal.checkEnd(nextChar);
          if(isNegative) return result;
          else           return multiplyExact(-1,result);
        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
          result = subtractExact( multiplyExact(result,10), nextChar - '0' );
          nextChar = reader.read();
      }
  }
}