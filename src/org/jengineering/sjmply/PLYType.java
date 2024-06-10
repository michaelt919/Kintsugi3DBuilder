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
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Objects;

/** Instances of {@link PLYType} and its subclasses represent PLY property types.
 *  
 *  @author Dirk Toewe
 *  
 *  @param <T> The java array type used to represent the entries of this PLY property type.
 */
public abstract class PLYType<T>
{
// STATIC FIELDS
  public static abstract class PLYNum<T> extends PLYType<T>
  {
    PLYNum( Class<T> type, String repr ) { super(type,repr); }

    abstract void write( T array, int index, long value );
  }
  static abstract class PLYInt<T> extends PLYNum<T>
  {
    public final long MIN_VAL, MAX_VAL;
    PLYInt( Class<T> type, String repr, long min, long max ) { super(type,repr); MIN_VAL = min; MAX_VAL = max; }

    public abstract long get( T array, int index );
  }
  public static abstract class PLYUInt<T> extends PLYInt<T>
  {
    PLYUInt( Class<T> type, String repr, long max ) { super(type,repr,0,max); }
  }
  static abstract class PLYFloat<T> extends PLYNum<T>
  {
    PLYFloat( Class<T> type, String repr ) { super( type, repr ); }

    @Override void write( T array, int index, long value ) { write( array, index, (double) value ); }
    abstract void write( T array, int index, double value );
  }
  /** A signed 8-bit integer. Called 'int8' or 'char' in PLY files.
   */
  public static final PLYNum<byte[]> INT8 = new PLYInt<byte[]>(byte[].class, "INT8", Byte.MIN_VALUE, Byte.MAX_VALUE )
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  byte[] arr ) { return elem -> arr[elem] = in.readInt8(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, byte[] arr ) { return elem -> out.writeInt8(arr[elem]); }
    @Override public long get( byte[] arr, int index ) { return arr[index]; }
    @Override void write( byte[] arr, int i, long val )
    {
      if( MIN_VAL > val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer underflow).", val, this) );
      if( MAX_VAL < val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer overflow).",  val, this) );
      arr[i] = (byte) val;
    }
  };
  /** An unsigned 8-bit integer. Called 'uint8' or 'uchar' in PLY files.
   */
  public static final PLYUInt<byte[]> UINT8 = new PLYUInt<byte[]>(byte[].class, "UINT8", (1<<8)-1 )
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  byte[] arr ) { return elem -> arr[elem] = in.readUInt8(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, byte[] arr ) { return elem -> out.writeUInt8(arr[elem]); }
    @Override public long get( byte[] arr, int index ) { return Byte.toUnsignedLong(arr[index]); }
    @Override void write( byte[] arr, int i, long val )
    {
      if( MIN_VAL > val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer underflow).", val, this) );
      if( MAX_VAL < val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer overflow).",  val, this) );
      arr[i] = (byte) val;
    }
  };
  /** A signed 16-bit integer. Called 'int16' or 'short' in PLY files.
   */
  public static final PLYNum<short[]>  INT16 = new PLYInt<short[]>(short[].class, "INT16", Short.MIN_VALUE, Short.MAX_VALUE )
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  short[] arr ) { return elem -> arr[elem] = in.readInt16(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, short[] arr ) { return elem -> out.writeInt16(arr[elem]); }
    @Override public long get( short[] array, int index ) { return array[index]; }
    @Override void write( short[] arr, int i, long val )
    {
      if( MIN_VAL > val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer underflow).", val, this) );
      if( MAX_VAL < val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer overflow).",  val, this) );
      arr[i] = (short) val;
    }
  };
  /** An unsigned 16-bit integer. Called 'uint16' or 'ushort' in PLY files.
   */
  public static final PLYUInt<short[]> UINT16 = new PLYUInt<short[]>(short[].class, "UINT16", (1<<16)-1 )
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  short[] arr ) { return elem -> arr[elem] = in.readUInt16(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, short[] arr ) { return elem -> out.writeUInt16(arr[elem]); }
    @Override public long get( short[] arr, int index ) { return Short.toUnsignedLong(arr[index]); }
    @Override void write( short[] arr, int i, long val )
    {
      if( MIN_VAL > val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer underflow).", val, this) );
      if( MAX_VAL < val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer overflow).",  val, this) );
      arr[i] = (short) val;
    }
  };
  /** A signed 32-bit integer. Called 'int32' or 'int' in PLY files.
   */
  public static final PLYNum<int[]>  INT32 = new PLYInt<int[]>(int[].class, "INT32", Integer.MIN_VALUE, Integer.MAX_VALUE )
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  int[] arr ) { return elem -> arr[elem] = in.readInt32(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, int[] arr ) { return elem -> out.writeInt32(arr[elem]); }
    @Override public long get( int[] array, int index ) { return array[index]; }
    @Override void write( int[] arr, int i, long val )
    {
      if( MIN_VAL > val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer underflow).", val, this) );
      if( MAX_VAL < val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer overflow).",  val, this) );
      arr[i] = (int) val;
    }
  };
  /** An unsigned 32-bit integer. Called 'uint32' or 'uint' in PLY files.
   */
  public static final PLYUInt<int[]> UINT32 = new PLYUInt<int[]>(int[].class, "UINT32", (1L<<32)-1 )
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  int[] arr ) { return elem -> arr[elem] = in.readUInt32(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, int[] arr ) { return elem -> out.writeUInt32(arr[elem]); }
    @Override public long get( int[] arr, int index ) { return Integer.toUnsignedLong(arr[index]); }
    @Override void write( int[] arr, int i, long val )
    {
      if( MIN_VAL > val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer underflow).", val, this) );
      if( MAX_VAL < val ) throw new IllegalArgumentException( format("Cannot convert %d to %s (integer overflow).",  val, this) );
      arr[i] = (int) val;
    }
  };
  /** 32-bit floating point. Called 'float32' or 'float' in PLY files.
   */
  public static final PLYNum<float[]> FLOAT32 = new PLYFloat<float[]>(float[].class,"FLOAT32")
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  float[] arr ) { return elem -> arr[elem] = in.readFloat32(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, float[] arr ) { return elem -> out.writeFloat32(arr[elem]); }
    @Override void write( float[] array, int index, double value ) { array[index] = (float) value; }
  };
  /** 64-bit floating point. Called 'float64' or 'double' in PLY files.
   */
  public static final PLYNum<double[]> FLOAT64 = new PLYFloat<double[]>(double[].class,"FLOAT64")
  {
    @Override PropertyIO propertyReader( DataStreamIn  in,  double[] arr ) { return elem -> arr[elem] = in.readFloat64(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, double[] arr ) { return elem -> out.writeFloat64(arr[elem]); }
    @Override void write( double[] array, int index, double value ) { array[index] = value; }
  };
  public static class PLYSequence<T> extends PLYType<T[]>
  {
  // FIELDS
    /** The type representing the elements in this list.
     */
    public final PLYType<T> elemType;

  // CONSTRUCTORS
    @SuppressWarnings("unchecked")
    PLYSequence( PLYType<T> _elemType, String repr )
    {
      super( (Class<T[]>) Array.newInstance(_elemType.clazz,0).getClass(), repr );
      elemType =_elemType;
    }

    @Override public int hashCode()
    {
      return Integer.rotateLeft( 1, elemType.hashCode() );
    }

  // METHODS
    @Override PropertyIO propertyReader( DataStreamIn  in,  T[] array ) { throw new Error(); }
    @Override PropertyIO propertyWriter( DataStreamOut out, T[] array ) { throw new Error(); }
  }
  /** A PLY property list type.
   * 
   *  @author Dirk Toewe
   *
   *  @param <S> The java array type used to represent the sizes of the entry lists of this PLY property type.
   *  @param <T> The java array type used to represent the entries of this PLY property type.
   */
  public static final class PLYList<S,T> extends PLYSequence<T>
  {
  // FIELDS
    /** The type representing the list sizes.
     */
    public final PLYUInt<S> sizeType;

  // CONSTRUCTORS
    PLYList( PLYUInt<S> _sizeType, PLYType<T> _elemType )
    {
      super(_elemType, "LIST("+_sizeType+","+_elemType.toString() +")" );
      sizeType =_sizeType;
    }

  // METHODS
    @Override public int hashCode()
    {
      return Objects.hash( sizeType, elemType );
    }

    boolean isViewableAs( PLYType<?> type )
    {
      if( type instanceof PLYList )
      {
        PLYList<?,?> list = (PLYList<?,?>) type;
        return sizeType.equals(list.sizeType)
            && elemType.isViewableAs(list.elemType);
      }
      if( type instanceof PLYSequence<?> )
      {
        PLYSequence<?> list = (PLYSequence<?>) type;
        return elemType.isViewableAs(list.elemType);
      }
      return false;
    }

    @Override PropertyIO propertyReader( DataStreamIn in, T[] arr )
    {
      SizeReader readSize;
           if( UINT8  == sizeType ) readSize = () -> Byte .toUnsignedInt( in.readUInt8 () );
      else if( UINT16 == sizeType ) readSize = () -> Short.toUnsignedInt( in.readUInt16() );
      else if( UINT32 == sizeType ) readSize = () ->                      in.readUInt32();
      else throw new AssertionError();

      return elem -> {
        int len = readSize.readSize();
        PropertyIO valReader = elemType.propertyReader( in, arr[elem] = elemType.malloc(len) );
        for( int el=0; el < len; el++ )
          valReader.process(el);
      };
    }

    @Override PropertyIO propertyWriter( DataStreamOut out, T[] arr )
    {
      SizeWriter writeSize;
           if( UINT8  == sizeType ) writeSize = size -> out.writeUInt8 ( (byte ) size );
      else if( UINT16 == sizeType ) writeSize = size -> out.writeUInt16( (short) size );
      else if( UINT32 == sizeType ) writeSize =        out::writeUInt32;
      else throw new AssertionError();

      return elem -> {
        T entry = arr[elem];
        int len = Array.getLength(entry);
        if( len > sizeType.MAX_VAL )
          throw new IOException( format("List cannot be written: Length of %d exceeds size type %s.", len, sizeType) );
        writeSize.writeSize(len);
        PropertyIO valWriter = elemType.propertyWriter(out,entry);
        for( int el=0; el < len; el++ )
          valWriter.process(el);
      };
    }
  }

//  private static final class PLYTuple<T>
//  {
//    
//  }

  public static <T> PLYType<T[]> LIST( PLYType<T> elemType )
  {
    return new PLYSequence<>(elemType,"LIST("+elemType+")");
  }

  public static <S,T> PLYList<S,T> LIST( PLYUInt<S> sizeType, PLYType<T> elemType )
  {
    return new PLYList<>(sizeType,elemType);
  }

// STATIC CONSTRUCTOR

// STATIC METHODS

// FIELDS
  final Class<T> clazz;
  private final String repr;

// CONSTRUCTORS

// METHODS
  private PLYType( Class<T> _clazz, String _repr )
  {
    clazz = requireNonNull(_clazz);
    repr  = requireNonNull(_repr);
  }

  public final boolean equals( PLYType<?> compareTo )
  {
    return compareTo.toString().equals(repr);
  }

  public final boolean equals( Object compareTo )
  {
    return compareTo instanceof PLYType && equals( (PLYType<?>) compareTo );
  }

  boolean isViewableAs( PLYType<?> type )
  {
    return equals(type);
  }

  public String toString() { return repr; }

  /** Creates a new array that allows storage of instances of this PLY property type.
   *  
   *  @param size The size of the allocated array.
   *  @return <code>new T[size]</code>
   */
  public T malloc( int size )
  {
    return cast( Array.newInstance(clazz.getComponentType(), size) );
  }

  /** Type-safely casts an array to this property type's array representation.
   *  
   *  @param array The array to be casted.
   *  @return <code>({@link #T}) array</code>
   */
  public T cast( Object array )
  {
    return clazz.cast(array);
  }

  abstract PropertyIO propertyReader(DataStreamIn  in,  T array);
  abstract PropertyIO propertyWriter(DataStreamOut out, T array);
}