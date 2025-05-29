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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static java.lang.Double.*;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;

/** Parser for decimal floating point numbers.
 *  
 *  @author Dirk Toewe
 */
/* Double-Double-Arithmetic inspired by:
 * class : DoubleDouble.java
 * author: Matin Davis
 * url   : http://tsusiatsoftware.net/dd/main.html
 * see   : info/doubledouble-src.zip
 */
class Decimal
{
//STATIC FIELDS
  /** Constant used for double-double arithmetics.
   */
  private static final double SPLIT = 134217729.0d;
  /** Boolean constant which signalizes that a parsed source has had a leading sign character.
   */
  static final boolean
    NO_DIGIT_SEEN = false,
       DIGIT_SEEN = true;
  /** 
   */
  private final static int MAX_DIGIT_COUNT_PLUS_ONE = 2 + digitCount(10, 1L<<52);
  /** 
   */
  private final static short
    MAX_EXPONENT  = +308,
    MIN_EXPONENT  = -324;
  /** 
   */
  private final static double[]
    DD_10_POW_HIGH = new double[MAX_EXPONENT + 1],
    DD_10_POW_LOW  = new double[MAX_EXPONENT + 1];
  /** 
   */
  private static final int MIN_DOUBLE_DOUBLE_EXPONENT_ABS = -6 - MIN_EXPONENT - MAX_DIGIT_COUNT_PLUS_ONE;
  /** 
   */
  private static final BigDecimal[] FP_POW_10 = new BigDecimal[ -MIN_EXPONENT + MAX_DIGIT_COUNT_PLUS_ONE - MIN_DOUBLE_DOUBLE_EXPONENT_ABS ];

//STATIC CONSTRUCTOR
  static {
    DD_10_POW_HIGH[0] = 1;
    DD_10_POW_HIGH[1] =10;
    //RANGE OF APROXIMATED POWERS OF 10 (REPRESENTED IN A Double-Double-Arithmetic REPRESENTATION)
    for( int i = 2; i < DD_10_POW_HIGH.length; i++ )
    {
      final int step = i / 2;//quadration results in an logarithmically increasing error (wich is assumed to be acceptable) on the other hand multiplying two equal numbers has least danger of overflow
      double
        hi   = DD_10_POW_HIGH[step],
        lo   = DD_10_POW_LOW [step],
        facHi= DD_10_POW_HIGH[i - step],
        facLo= DD_10_POW_LOW [i - step],
        hx, tx, hy, ty, C, c;
      C = SPLIT * hi;
      hx  = C - hi;
      c = SPLIT * facHi;
      hx  = C - hx;
      tx  = hi - hx;
      hy  = c - facHi; 
      C = hi * facHi;
      hy  = c - hy;
      ty  = facHi - hy;
      c = ( ( ( ( hx * hy - C ) + hx * ty ) + tx * hy ) + tx * ty ) + ( hi * facLo + lo * facHi );
      DD_10_POW_HIGH[i] = C+c;
      DD_10_POW_LOW [i] = c + (C - DD_10_POW_HIGH[i]);
    }
    BigDecimal tenth = ONE.divide(TEN);
    FP_POW_10[ 0 ] = tenth.pow(MIN_DOUBLE_DOUBLE_EXPONENT_ABS);
    for( int i = 1; i < FP_POW_10.length; i++ )
      FP_POW_10[ i ] = FP_POW_10[ i - 1 ].multiply(tenth);
  }

//STATIC METHODS
  private static final BigDecimal fp10Pow( int exponent )
  {
    return FP_POW_10[ exponent - MIN_DOUBLE_DOUBLE_EXPONENT_ABS ];
  }

  private static int digitCount( int radix, long mantissa )
  {
    if( 0 > radix ) throw new IllegalArgumentException("radix must not be negative.");
    if( 1 > radix ) throw new IllegalArgumentException("radix must not be zero.");
    int i = 0;
    do {
      mantissa /= radix;
      i++;
    }
    while( 0 != mantissa );
    return i;
  }

//FIELDS

//CONSTRUCTORS

//METHODS
  static void checkEnd( int token ) throws IOException
  {
    if( ! ( -1 == token || Character.isWhitespace(token) ) )
      throw new IOException("Invalid character: " + tokenStr(token) );
  }

  static String tokenStr( int token )
  {
    if( 0 > token )
      return Integer.toString(token);
    return "'" + (char) token + "'";
  }

  static double read( String str ) throws IOException
  {
    return read( new ByteArrayInputStream( str.getBytes(StandardCharsets.UTF_8) ) );
  }

  static double read( InputStream in ) throws IOException
  {
    int nextChar = Integral.skipNB(in);
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
      case 'i': case 'I': return parseInfinity(in, isNegative);
      case 'n': case 'N': return parseNaN(in);
      default : throw new IOException("Invalid character: " + tokenStr(nextChar) );
      case '0': nextChar = in.read();
        if( 'x' == nextChar || 'X' == nextChar )
          return parseHex(in,isNegative);
        while( '0' == nextChar )
          nextChar = in.read();
      case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
        return parseIntPart( nextChar, in, isNegative );
      case '.': return parseFracPart(in, isNegative, NO_DIGIT_SEEN, 0L, (byte) 0, 0);
    }
  }

  private static double parseHex( InputStream in, boolean isNegative ) throws IOException
  {
    double result = 0;
    for(;;) {
      int nextChar = in.read();
      switch(nextChar) {
        default:
          checkEnd(nextChar);
          if(isNegative) return -result;
          else           return +result;
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':           result = result*16 + (nextChar - '0'   ); continue;
        case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': result = result*16 + (nextChar - 'A'+10); continue;
        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': result = result*16 + (nextChar - 'a'+10); continue;
      }
    }
  }

  private static double parseIntPart( int nextChar, InputStream reader, boolean isNegative ) throws IOException
  {
    long mantissa = 0; // mantissa of the parsed number
    byte digitCount = 0; // number of digits
    int exponent = 0; // the exponent exclusive the mantissa exponent
    for(;;)
      switch( nextChar )
      {
      case '.': return parseFracPart(reader, isNegative, DIGIT_SEEN, mantissa, digitCount, exponent);
      case 'e': case 'E': return parseExponent(reader, isNegative, mantissa, digitCount, exponent);
      case 'f': case 'F': checkEnd( reader.read() ); return toFloat( isNegative, digitCount, mantissa, exponent );
      case 'd': case 'D': nextChar = reader.read();
      default : checkEnd(nextChar); return toDouble(isNegative, digitCount, mantissa, exponent);
      case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
        if( digitCount < MAX_DIGIT_COUNT_PLUS_ONE )
        {
          digitCount++;
          mantissa = 10*mantissa + (nextChar - '0');
        }
        else if( exponent != Integer.MAX_VALUE ) // OVERFLOW SAFE
          exponent++;
        nextChar = reader.read();
      }
  }

  private static double parseFracPart( InputStream reader, boolean isNegative, boolean digitSeen, long mantissa, byte digitCount, int exponent ) throws IOException
  {
    // mantissaExponent ist die anzahl relevanter nachkommastellen (d.h. jene die im bereich der double präzision liegen)
    int nextChar = reader.read();
    //MAKE SURE LEADING ZEROS ARE NOT ADDED TO THE digitCount WHICH WOULD LEAD TO LOWER PRECISION
    if( 0 == mantissa )
      while( '0' == nextChar )
      {
        digitSeen = true;
        if( exponent > ( Integer.MIN_VALUE + Long.SIZE ) ) // prevend underflow
          exponent--;
        nextChar = reader.read();
      }
    for(;;)
      switch( nextChar ) {
        case 'e': case 'E':
          if( ! digitSeen )
            throw new IOException( "No digit seen before exponential separator 'E' or 'e'." );
          return parseExponent(reader, isNegative, mantissa, digitCount, exponent);
        case 'f': case 'F':
          if( ! digitSeen )
            throw new IOException( "No digit seen before float ending separator 'F' or 'f'." );
          checkEnd( reader.read() );
          return toFloat( isNegative, digitCount, mantissa, exponent );
        case 'd': case 'D':
          if( ! digitSeen )
            throw new IOException( "No digit seen before double ending separator 'D' or 'd'" );
          nextChar = reader.read();
        default: checkEnd(nextChar); return toDouble( isNegative, digitCount, mantissa, exponent );
        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
          if( digitCount < MAX_DIGIT_COUNT_PLUS_ONE ) {
            digitSeen = true;
            digitCount++;
            exponent--;
            mantissa = 10L * mantissa + (nextChar - '0');
          }
          nextChar = reader.read();
      }
  }

  private static double parseExponent( InputStream reader, boolean isNegative, long mantissa, byte digitCount, int mantissaExponent ) throws IOException
  {
    final boolean expNegative;
    int exponent = 0;
    int nextChar = reader.read();
     //
    // PARSE EXPONENT SIGN
   //
    switch( nextChar ) {
      case '-': nextChar = reader.read(); expNegative = true; break;
      case '+': nextChar = reader.read();
      default : expNegative = false;
    }
    // CHECK FOR VALID BEGINNING
    switch( nextChar )
    {
      default : throw new IOException( "Exponential separator seen but no digit only: '" + ( nextChar == -1 ? "-1" : (char) nextChar ) + "'" );
      case '0':  case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
    }
     //
    // PARSE EXPONENT DIGITS
   //
    for(;;)
      switch( nextChar ) {
        case 'f': case 'F': checkEnd( reader.read() ); return toFloat( isNegative, digitCount, mantissa, exponent + mantissaExponent );
        case 'd': case 'D': nextChar = reader.read();
        default : checkEnd(nextChar); return toDouble( isNegative, digitCount, mantissa, exponent + mantissaExponent );
        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
          if( expNegative ) {
            if( MIN_EXPONENT < ( exponent + mantissaExponent ) ) exponent = 10 * exponent - (nextChar - '0');
          }
          else
            if( MAX_EXPONENT > ( exponent + mantissaExponent ) ) exponent = 10 * exponent + (nextChar - '0');
          nextChar = reader.read();
      }
  }

  private static Double parseInfinity( InputStream reader, boolean isNegative ) throws IOException
  {
    String
      lowerCase = "nfinity",
      upperCase = "NFINITY";
    for( int i = 0; i < lowerCase.length(); i++ )
    {
      final int nextChar = reader.read();
      if(    nextChar != lowerCase.charAt(i)
          && nextChar != upperCase.charAt(i) )
        if( i == 2 )
          break;
        else
          throw new IOException("Invalid character in \"infinity\"-sequence: '" + (char) nextChar + "'");
    }
    //PER DEFINITION THE parseNext() METHOD READS THE FIRST INVALID CHAR SO CALL READ ONE MORE TIME
    checkEnd(reader.read());
    return isNegative
      ? NEGATIVE_INFINITY
      : POSITIVE_INFINITY;
  }

  private static Double parseNaN( InputStream reader ) throws IOException
  {
    String
      lowerCase = "an",
      upperCase = "AN";
    for( int i = 0; i < lowerCase.length(); i++ )
    {
      final int nextChar = reader.read();
      if(    nextChar != lowerCase.charAt( i )
          && nextChar != upperCase.charAt( i ) )
        throw new IOException("Invalid character in \"NaN\"-sequence: '" + (char) nextChar + "'");
    }
    checkEnd(reader.read());
    return NaN;
  }

  private static float toFloat( boolean isNegative, final byte digitCount, long mantissa, int exponent )
  {
    return (float) toDouble( isNegative, digitCount, mantissa, exponent );
  }

  private static strictfp double toDouble( final boolean isNegative, byte digitCount, final long mantissa, int exponent )
  {
    digitCount--;
    final double unsigned;
     //
    // ZERO EXPONENT
   //
    if(   0 == exponent
      ||  0 == mantissa )
      unsigned = mantissa;
    // USE Double-Double PRECISION TO REPRESENT THE MANTISSA
    else {
       //
      // POSITIVE EXPONENTS
     //
      if( 0 < exponent )
        if( MAX_EXPONENT < exponent + digitCount ) // 1e308 = 0.1e309
          unsigned = Double.POSITIVE_INFINITY;
        else /* MAX_EXPONENT >= exponent */ // Double-Double-Arithmetic MULTIPLICATION IN 1 STEP
        {
          //USING Double-Double-Arithmetic
          double
            hi = (double) mantissa,
            lo = mantissa - ( (long) hi );

          if( MAX_EXPONENT + digitCount - 25 < exponent ) //FOR VERY LARGE EXPONENTS: SAFE MULTIPLICATION IN TWO STEPS
          {
            //
            // THE SECOND MULTIPLICATION (2nd) SHOULD BE APPROXIMATELY A QUADRATION i.e.:
            //
            //  mantissa * 10 ^ 1st = 10^ 2nd;
            //  -> 10^(log10(mantissa) + 1st) = 10^2nd;
            //  -> log10(mantissa) + 1st = 2nd;
            //
            //  1st + 2nd = exponent;
            //
            //  log10(mantissa) + 1st = exponent - 1st;
            //  -> 2*1st = exponent - log10(mantissa)
            //  -> 1st = ( exponent - log10( mantissa ) ) / 2
            //
            final int firstStep = ( exponent - digitCount(10, mantissa) + 1 ) / 2;
            exponent -= firstStep;
            double
              facHi = DD_10_POW_HIGH[firstStep],
              facLo = DD_10_POW_LOW [firstStep],
              C = SPLIT * hi,
              hx  = C - hi,
              c = SPLIT * facHi;
            hx  = C - hx;
            double
              tx  = hi - hx,
              hy  = c - facHi;
            C = hi * facHi;
            hy  = c - hy;
            double ty  = facHi - hy;
            c = ( ( ( ( hx * hy - C ) + hx * ty ) + tx * hy ) + tx * ty ) + ( hi * facLo + lo * facHi );
            hi = C + c;
            hx = C - hi;
            lo = c + hx;
          }
          double
            facHi = DD_10_POW_HIGH[exponent],
            facLo = DD_10_POW_LOW [exponent],
            C = SPLIT * hi,
            hx  = C - hi,
            c = SPLIT * facHi;
          hx  = C - hx;
          double
            tx  = hi - hx,
            hy  = c - facHi; 
          C = hi * facHi;
          hy = c - hy;
          double ty = facHi - hy;
          c = ( ( ( ( hx * hy - C ) + hx * ty ) + tx * hy ) + tx * ty ) + ( hi * facLo + lo * facHi );
          double cache = C + c;
          unsigned = Double.isNaN(cache)
            ? Double.POSITIVE_INFINITY
            : cache;
        }
       //
      // NEGATIVE EXPONENTS
     //
      else/* 0 > exponent */
        if( MIN_EXPONENT > exponent + digitCount )//4.9e-324 is just as well as 49e-325 and 490e-326
          unsigned = 0;
        else {
          //exp = abs( exp )
          exponent *= -1;
          // exponent is so large that another division is necessary
          // explaination: when a double-double number becomes smaller
          // than 10^(MIN_EXPONENT+MAX_DIGIT_COUNT_PLUS_ONE) then the
          // low order of the double-double value almost has a value
          // of 10^(MIN_EXPONENT) which means it is too small and
          // does therefore not contain any precision anymode
          if( MIN_DOUBLE_DOUBLE_EXPONENT_ABS < exponent )
            unsigned = fp10Pow(exponent).multiply( new BigDecimal(mantissa) ).doubleValue();
          else {
            double
              hi = (double) mantissa,
              lo = mantissa - ( (long) hi ),
              divHi = DD_10_POW_HIGH[exponent],
              divLo = DD_10_POW_LOW [exponent],
              C = hi / divHi,
              c = SPLIT * C,
              hc  = c - C, 
              u = SPLIT * divHi;
            hc  = c - hc;
            double
              tc  = C - hc,
              hy  = u - divHi,
              U = C * divHi;
            hy  = u - hy;
            double ty  = divHi - hy;
            u = ( ( ( hc * hy - U ) + hc * ty ) + tc * hy ) + tc * ty;
            c = ( ( ( ( hi - U ) - u ) + lo ) - C * divLo ) / divHi;
            unsigned = C + c;
            if( Double.isInfinite(unsigned) )
              throw new Error( "A critical Error org.jEngineering.text.io.JavaDecimalDoubleParser: DoubleDouble Overflow. Please report this Error together with the corresponding input.");
          }
        }
    }
    if( Double.isNaN( unsigned ) )
      throw new Error( "A critical Error org.jEngineering.text.io.JavaDecimalDoubleParser: DoubleDouble NaN. Please report this Error together with the corresponding input.");
    return    isNegative
        ? -unsigned
        :  unsigned;
  }
}