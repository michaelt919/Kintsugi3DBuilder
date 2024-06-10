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

import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static java.lang.System.arraycopy;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import org.jengineering.sjmply.PLYType.PLYFloat;
import org.jengineering.sjmply.PLYType.PLYInt;
import org.jengineering.sjmply.PLYType.PLYList;
import org.jengineering.sjmply.PLYType.PLYNum;
import org.jengineering.sjmply.PLYType.PLYSequence;

/** A representation of an PLY series of elements (e.g. vertex, face, edge).
 *  The properties of these elements are themselves represented as an array
 *  series. The i-th entry of that array represents the property value for
 *  the i-th element. The property series can be viewed as the columns of
 *  a table wherein the rows would represent an element.
 *  
 *  @see <a href="http://paulbourke.net/dataformats/ply/">PLY - Polygon File Format</a>
 *  @see <a href="https://en.wikipedia.org/wiki/PLY_(file_format)">PLY (file format) - Wikipedia</a>
 *  
 *  @author Dirk Toewe
 */
public class PLYElementList
{
// STATIC FIELDS

// STATIC CONSTRUCTOR

// STATIC METHODS
  private static Object cloneDeep( Object arr )
  {
    int len = Array.getLength(arr);
    Object result = Array.newInstance(arr.getClass().getComponentType(), len);
    if( result instanceof Object[] )
    {
      Object[]
        src = (Object[]) arr,
        res = (Object[]) result;
      for( int i=0; i < res.length; i++ )
        res[i] = cloneDeep(src[i]);
    }
    else
      arraycopy(arr,0,result,0,len);
    return result;
  }

  /** Provided every {@link PLYElementList} in lists has the same properties, this
   *  method creates a new PLYElementList which contains the elements of every list
   *  in lists, in the order they appear.
   *  
   *  @param lists
   *  @return
   */
  public static PLYElementList stack( PLYElementList... lists )
  {
    Map<String,PLYType<?>> properties = lists[0].properties;
    for( int i=1; i < lists.length; i++ )
    {
      Map<String,PLYType<?>> properties_i = lists[i].properties;
      if( ! properties .equals( properties_i ) )
      {
        List<String> symDiff = Stream.concat(
          properties  .keySet().stream(),
          properties_i.keySet().stream()
        ).distinct()
         .filter( k -> ! Objects.equals(
            properties  .get(k),
            properties_i.get(k)
          ))
         .collect( toList() );
        throw new IllegalArgumentException( format("list[0] and lists[%d] have non matching properties or property types: %s.", i, symDiff) );
      }
    }

    int size = toIntExact( stream(lists).mapToInt( list -> list.size ).sum() );
    PLYElementList result = new PLYElementList(size);

    properties.forEach( (k,v) -> result._addProperty(v,k) );

    int row = 0;
    for( PLYElementList list: lists )
    {
      for( String prop: properties.keySet() )
      {
        Object
          src =   list.data.get(prop),
          dest= result.data.get(prop);
        if( src instanceof Object[] )
        {
          Object[]
             srcArr = (Object[]) src,
            destArr = (Object[]) dest;
          for( int i=0; i < list.size; i++ )
            destArr[row+i] = cloneDeep(srcArr[i]);
        }
        else
          arraycopy(src,0,dest,row,list.size);
      }
      row += list.size;
    }
    assert row == result.size;

    return result;
  }

// FIELDS
  /** The number of elements in this element series.
   */
  public final int size;
  private final Map<String,PLYType<?>>_properties = new LinkedHashMap<>();
  /** A read-only view of all properties and their type.
   */
  public final Map<String,PLYType<?>> properties = unmodifiableMap(_properties );
  private Map<String,Object> data = new HashMap<>();

// CONSTRUCTORS
  public PLYElementList( int _size )
  {
    size =_size;
  }

// METHODS
  /** Returns the property by the given name.
   * 
   *  @param type The type of the requested property.
   *  @param name The name of the requested property.
   *  @return The property values series.
   *  @throws NoSuchElementException If there is no property by the given name in this element series.
   */
  public <T> T property( PLYType<? extends T> type, String name ) throws NoSuchElementException
  {
    PLYType<?> resultType = properties.get(name);
    if( null == resultType )
      throw new NoSuchElementException();

    if( ! resultType.isViewableAs(type) )
      throw new IllegalArgumentException( format("%s cannot be viewed as %s.",resultType,type) );

    Object result = data.get(name);
    return type.clazz.cast(result);
  }

  /** Returns a copy the property by the given name cast to the requested type.
   * 
   *  @param type The requested result type property. The property will be cast if neccessary.
   *  @param name The name of the requested property.
   *  @return The property values series.
   *  @throws NoSuchElementException If there is no property by the given name in this element series.
   */
  public <T> T propertyAs( PLYType<? extends T> type, String name ) throws NoSuchElementException
  {
    PLYType<?> srcType =_properties.get(name);

    if( null == srcType )
      throw new NoSuchElementException();

    T result = type.malloc(size);

    _copyOver(property(srcType,name), srcType, result, type);

    return result;
  }
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private <S,T> void _copyOver( Object src, PLYType<?> srcType, Object result, PLYType<?> type )
  {
    assert srcType.clazz.isInstance(src);
    assert    type.clazz.isInstance(result);
    if( srcType instanceof PLYSequence )
      if( ! (type instanceof PLYSequence) )
        throw new IllegalArgumentException( format("Cannot convert %s to %s.",srcType,type) );
      else {
        PLYType
          srcTyp = ( (PLYSequence) srcType ).elemType,
             typ = ( (PLYSequence)    type ).elemType;
        for( int i = Array.getLength(src)-1; i >= 0; i-- )
        {
          Object src_i = Array.get(src,i);
          if( null == src_i )
            continue;
          Object result_i = typ.malloc( Array.getLength(src_i) );
          Array.set(result,i,result_i);
          _copyOver(src_i, srcTyp, result_i, typ);
        }
      }
    else if( srcType instanceof PLYInt )
      if( ! (type instanceof PLYNum) )
        throw new IllegalArgumentException( format("Cannot convert %s to %s.",srcType,type) );
      else {
        PLYInt srcTyp = (PLYInt) srcType;
        PLYNum typ = (PLYNum) type;
        for( int i = Array.getLength(src)-1; i >= 0; i-- )
          typ.write(result,i, srcTyp.get(src,i) );
      }
    else if( srcType instanceof PLYFloat )
      if( ! (type instanceof PLYFloat) )
        throw new IllegalArgumentException( format("Cannot convert %s to %s.",srcType,type) );
      else {
        PLYFloat typ = (PLYFloat) type;
        for( int i = Array.getLength(src)-1; i >= 0; i-- )
          typ.write(result,i, Array.getDouble(src,i) );
      }
    else
      throw new AssertionError();
  }

  /** Creates a new property series in this element series.
   *  
   *  @param type The type of the created property.
   *  @param name The type of the created property.
   *  @return The newly create property value series.
   *  @throws IllegalStateException If this element series already contains a property by that name.
   */
  public <T> T addProperty( PLYNum<? extends T> type, String name ) throws IllegalStateException { return _addProperty(type,name); }
  /** Creates a new property series in this element series.
   *  
   *  @param type The type of the created property.
   *  @param name The type of the created property.
   *  @return The newly create property value series.
   *  @throws IllegalStateException If this element series already contains a property by that name.
   */
  public <T> T[] addProperty( PLYList<?,? extends T> type, String name ) throws IllegalStateException { return _addProperty(type,name);  }
  <T> T _addProperty( PLYType<? extends T> type, String name ) throws IllegalStateException
  {
    if( ! (type instanceof PLYNum) && ! (type instanceof PLYList) )
      throw new AssertionError();

    if( name.codePoints().anyMatch(Character::isWhitespace) )
      throw new IllegalArgumentException("'name' cannot contain whitespaces.");

    if(_properties.containsKey(name) )
      throw new IllegalStateException( format("Already contains property '%s'.",name) );

    _properties.put(name,type);
    T result = type.malloc(size);
    data.put(name,result);
    return result;
  }

  public <T> T   convertProperty( String property, PLYNum<   ? extends T> newType ) { return _convertProperty(property,newType); }
  public <T> T[] convertProperty( String property, PLYList<?,? extends T> newType ) { return _convertProperty(property,newType); }
  private <T> T _convertProperty( String property, PLYType<  ? extends T> newType )
  {
    if( ! (newType instanceof PLYNum) && ! (newType instanceof PLYList) )
      throw new AssertionError();

    if( ! _properties.containsKey(property) )
      throw new NoSuchElementException();

    T result = propertyAs(newType,property);
    _properties.put(property,newType);
    data.put(property,result);
    return result;
  }

  /** Removes the property by the given name from this element series.
   *  
   *  @param name The name of the property to be removed.
   */
  public void removeProperty( String name )
  {
    if( null == data.remove(name) )
      throw new NoSuchElementException();

    _properties.remove(name);
  }

  public PLYElementList clone()
  {
    PLYElementList result = new PLYElementList(size);
    result._properties.putAll(_properties);
    data.forEach( (k,v) -> result.data.put( k, cloneDeep(v) ) );
    return result;
  }

  @Override public String toString()
  {
    return properties.isEmpty() ? "" : "{\n" + str("  ") + "\n}";
  }

  String str( String indent )
  {
    return properties.entrySet().stream().map(
      entry ->
          entry.getKey()
        + ": "
        + entry.getValue()
        + " = ["
        + str(indent+"  ", entry.getValue(), data.get(entry.getKey()) )
        + "]"
    ).collect( joining(",\n"+indent,indent,"") );
  }

  private String str( String indent, PLYType<?> type, Object series )
  {
    if( null == series )
      return null;
    assert type.clazz.isInstance(series);
    if( type instanceof PLYList )
    {
      PLYType<?> eltype = ( (PLYList<?,?>) type ).elemType;
      int
        len = Array.getLength(series),
        max = 9;
      Stream<Object> result;
      IntFunction<String> toStr =  i -> "["+str(indent+"  ", eltype, Array.get(series,i) )+"]";
      if( len <= max )
        result = range(0,len).mapToObj(toStr);
      else {
        result = Stream.concat( range(0,max/2).mapToObj(toStr), Stream.of("...") );
        result = Stream.concat( result, range(len-max/2,len).mapToObj(toStr) );
      }
      return result.map(Objects::toString).collect(
        joining(
          ",\n"+indent,
           "\n"+indent,
           "\n"+indent.substring( 0, indent.length()-2 )
        )
      ); 
    }
    int
      len = Array.getLength(series),
      max = 15;
    Stream<Object> result;
    @SuppressWarnings("unchecked")
    BiFunction<Object,Integer,Object>
      getElem = type instanceof PLYInt<?>
        ? ( (PLYInt<Object>) type )::get
        : Array::get;
    if( len <= max )
      result = range(0,len).mapToObj( i -> getElem.apply(series,i) );
    else {
      result = Stream.concat( range(0,max/2).mapToObj( i -> getElem.apply(series,i) ), Stream.of("...") );
      result = Stream.concat( result, range(len-max/2,len).mapToObj( i -> getElem.apply(series,i) ) );
    }
    return result.map(Objects::toString).collect( joining(", ", " "," ") );
  }

  <T> PropertyIO propertyReader( DataStreamIn  in,  PLYType<T> type, String name ) { return type.propertyReader(in, property(type,name) ); }
  <T> PropertyIO propertyWriter( DataStreamOut out, PLYType<T> type, String name ) { return type.propertyWriter(out,property(type,name) ); }
}