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

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jengineering.sjmply.PLYType.*;

import static java.lang.String.format;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.jengineering.sjmply.PLYFormat.ASCII;
import static org.jengineering.sjmply.PLYFormat.BINARY_BIG_ENDIAN;
import static org.jengineering.sjmply.PLYType.FLOAT32;
import static org.jengineering.sjmply.PLYType.FLOAT64;
import static org.jengineering.sjmply.PLYType.INT16;
import static org.jengineering.sjmply.PLYType.INT32;
import static org.jengineering.sjmply.PLYType.INT8;
import static org.jengineering.sjmply.PLYType.LIST;
import static org.jengineering.sjmply.PLYType.UINT16;
import static org.jengineering.sjmply.PLYType.UINT32;
import static org.jengineering.sjmply.PLYType.UINT8;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jengineering.sjmply.PLYType.PLYList;
import org.jengineering.sjmply.PLYType.PLYSequence;
import org.jengineering.sjmply.PLYType.PLYUInt;
import static org.jengineering.sjmply.PLYType.*;

/** An in-memory representation of a <a href="https://en.wikipedia.org/wiki/PLY_(file_format)">PLY File</a>. PLY files are used to 
 *  
 *  @author Dirk Toewe
 */
public class PLY
{
// STATIC FIELDS
  /** A set of supported PLY format versions. Currently, only version "1.0" is supported.
   */
 public static final Set<String> SUPPORTED_VERSIONS = unmodifiableSet( new HashSet<>( asList("1.0") ) );

// STATIC CONSTRUCTOR
 private static final Pattern
   MATCH_LINE2     = Pattern.compile("\\s*format\\s+(?<FORMAT>\\S+)\\s+(?<VERSION>\\S+)\\s*"),
   MATCH_COMMENT   = Pattern.compile("\\s*comment\\s(?<COMMENT>.*)"),
   MATCH_ELEMENT   = Pattern.compile("\\s*element\\s+(?<NAME>\\S+)\\s+(?<SIZE>\\d+)\\s*"),
   MATCH_PROPERTY  = Pattern.compile("\\s*property\\s+(?<TYPE>(list\\s+(u(int(8|16|32)?|char|short))\\s+)*(u?(int(8|16|32)?|char|short)|double|float(32|64)?))\\s+(?<NAME>\\S+)\\s*"),
   MATCH_END       = Pattern.compile("\\s*end_header\\s*"),
   MATCH_OBJ_INFO  = Pattern.compile("\\s*obj_info\\s(?<OBJINFO>.*)");

// STATIC METHODS
  private static PLYType<?> parseType( String type )
  {
    String[] tokens = type.split("\\s+");
    assert 0 != tokens.length % 2;
    Function<String,PLYType<?>> parse = typ -> {
      switch(typ) {
        case "uchar" : case  "uint8" : return  UINT8;  case  "char" : case "int8" : return INT8;
        case "ushort": case  "uint16": return  UINT16; case  "short": case "int16": return INT16;
        case "uint"  : case  "uint32": return  UINT32; case  "int"  : case "int32": return INT32;
        case "float" : case "float32": return FLOAT32;
        case "double": case "float64": return FLOAT64;
      }
      throw new Error();
    };
    PLYType<?> result = parse.apply( tokens[tokens.length-1] );
    for( int i = tokens.length-2; i > 0; i-- )
      result = LIST( (PLYUInt<?>) parse.apply(tokens[i]), result );
    return result;
  }

  /** Reads a PLY file from an InputStream. When no exception is thrown, the
   *  next read() call on the InputStream returns exactly the first byte after
   *  the PLY file.
   *  
   *  <p> It is highly recommended that the InputStream is to be buffered.
   *  
   *  <p> The method skips/ignores empty lines. The official PLY specification however does
   *  not seem to allow empty lines.
   */
  public static PLY read( InputStream in ) throws IOException
  {
    HeaderInputStream header = new HeaderInputStream(in);
    CharSequence line = header.readLine();
    if( ! "ply".contentEquals(line) )
      throw new IOException( format("[Line 1] 'ply' expected at file start but '%s' found.",line) );

    line = header.readLine();
    Matcher match = MATCH_LINE2.matcher(line);
    if( ! match.matches() )
      throw new IOException( format("[Line %d] Illegal format specification: '%s'.", header.lineCount(), line) );

    PLYFormat format = PLYFormat.valueOf( match.group("FORMAT").toUpperCase() );
    PLY result = new PLY( format, match.group("VERSION") );

     //
    // READ HEADER
   //
    parse_header: while(true)
    {
      line = header.readLine();
      parse_line: while(true) {
        if( ( match = MATCH_ELEMENT.matcher(line) ).matches() )
        {
          int size = Integer.valueOf( match.group("SIZE") );
          PLYElementList elems = new PLYElementList(size);
          result.elements.put( match.group("NAME"), elems );
          while(true) {
            line = header.readLine();
                 if(   ( match = MATCH_COMMENT .matcher(line) ).matches() ) result.comments.put(header.lineCount(), match.group("COMMENT") );
            else if( ! ( match = MATCH_PROPERTY.matcher(line) ).matches() ) continue parse_line;
            else
              elems._addProperty( parseType( match.group("TYPE") ), match.group("NAME") );
          }
        }
        else if(           MATCH_END     .matcher(line)  .matches() ) break parse_header;
        else if( ( match = MATCH_COMMENT .matcher(line) ).matches() ) result.comments .put(header.lineCount(), match.group("COMMENT") );
        else if( ( match = MATCH_OBJ_INFO.matcher(line) ).matches() ) result.obj_infos.put(header.lineCount(), match.group("OBJINFO") );
        else
          throw new IOException( format("[Line %d] could not parse '%s'", header.lineCountTotal(), line) );
        break;
      }
    }

     //
    // READ DATA
   //
    DataStreamIn input;
    switch(format) {
      case ASCII               : input = new DataStreamInASCII(header); break;
      case BINARY_BIG_ENDIAN   : input = new DataStreamInBE(in); break;
      case BINARY_LITTLE_ENDIAN: input = new DataStreamInLE(in); break;
      default: throw new NullPointerException();
    }

    for( Entry<String,PLYElementList> entry: result.elements.entrySet() )
    {
      PLYElementList list = entry.getValue();
      String[] properties = list.properties.keySet().stream().toArray(String[]::new);
      PropertyIO[] readers = list.properties.entrySet().stream()
        .map( e -> list.propertyReader(input, e.getValue(), e.getKey() ) )
        .toArray(PropertyIO[]::new);

      for( int elem=0; elem < list.size; elem++ )
      {
        for( int prop=0; prop < properties.length; prop++ )
          try {
            readers[prop].process(elem);
          }
          catch( Exception ex ) {
            String msg = format("Failed to read %s for %s#%d.", properties[prop], entry.getKey(), elem);
            if( ASCII == format )
              msg = format("[Line %d]", ( (DataStreamInASCII) input ).lineCountTotal() ) + msg;
            throw new IOException( msg, ex );
          }
        input.endElement();
      }
    }

    return result;
  }

  public static PLY load( Path path ) throws IOException
  {
    try( InputStream in = new BufferedInputStream( newInputStream(path) ) )
    {
      PLY result = read(in);
      int chr = in.read();
      switch( result.getFormat() )
      {
        default: throw new AssertionError();
        case ASCII:
          while( Character.isWhitespace(chr) )
            chr = in.read();
        case BINARY_BIG_ENDIAN:
        case BINARY_LITTLE_ENDIAN:
          if( 0 <= chr )
            throw new IOException("File is longer than expected.");
      }
      return result;
    }
  }

  /**
   * Loads a PLY file from inside a zip file.
   * @param zipFolder The File object of the zip file
   * @param targetFileName The name of the PLY file that is zipped inside zip file
   * @return PLY object
   * @throws IOException
   */
  public static PLY loadFromZip(File zipFolder, String targetFileName) throws IOException {

    // Make an input stream from zipFolder
    try(ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFolder))) {

      // Target zipIn to be on the targetFileName entry
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        if (entry.getName().equals(targetFileName)) {
          break;
        }
        entry = zipIn.getNextEntry();
      }
      // Throw error if unable to find PLY file in zip
      if (entry == null) {
        throw new IOException("File not found in zip");
      }

      // Read the file
      try (InputStream in = new BufferedInputStream(zipIn)) {
        PLY result = read(in);
        int chr = in.read();
        switch (result.getFormat()) {
          default:
            throw new AssertionError();
          case ASCII:
            while (Character.isWhitespace(chr))
              chr = in.read();
          case BINARY_BIG_ENDIAN:
          case BINARY_LITTLE_ENDIAN:
            if (0 <= chr)
              throw new IOException("File is longer than expected.");
        }

        return result;
      }
    }
  }



// FIELDS
  private PLYFormat format;
  private String version;
  public final SortedMap<Integer,String>
    comments = new TreeMap<>(),
    obj_infos= new TreeMap<>();
  /** Map all elements lists and their names.
   */
  public final Map<String,PLYElementList> elements = new LinkedHashMap<String,PLYElementList>()
  {
    /** Version number used for serialization. 
     */
    private static final long serialVersionUID = 1864304791018866966L;

    @Override public PLYElementList put( String key, PLYElementList val )
    {
      if( key.codePoints().anyMatch(Character::isWhitespace) )
        throw new IllegalArgumentException("'key' cannot contain whitespaces.");
      requireNonNull(val,"'val' cannot be null.");
      return super.put(key,val);
    }
  };

// CONSTRUCTORS
  public PLY() {
    this(BINARY_BIG_ENDIAN,"1.0");
  }
  public PLY( PLYFormat format, String version )
  {
    setFormat(format);
    setVersion(version);
  }

// METHODS
  /** Returns the file format. The file format has no effect on the in-memory representation.
   *  It does however give information about a loaded file's format after reading. When writing
   *  the PLY object to a file or an {@link OutputStream}, it also specifies the output format. 
   *  
   *  @return The file format.
   */
  public PLYFormat getFormat() { return format; }
  /** Set the file format, changing the way in which the PLY object is saved/written
   *  to file/{@link OutputStream}s.
   *  
   *  @param _format The new file format.
   *  @throws NullPointerException If null == _format
   */
  public void setFormat( PLYFormat _format ) { format = requireNonNull(_format); }

  public String getVersion() { return version; }
  public void setVersion( String _version ) throws IllegalArgumentException
  {
    _version =_version.trim();
    if( ! SUPPORTED_VERSIONS.contains(_version ) )
      throw new IllegalArgumentException( format("Version '%s' not supported. Supported versions: '%s'.",_version, SUPPORTED_VERSIONS) );
    version =_version;
  }

  /** Returns the element list by the given name from this PLY object.
   *
   *  @param name The name of the requested element list.
   *  @return The element list by the name <code>name</code>.
   *  @throws NoSuchElementException If there is no element list by the given name.
   */
  public PLYElementList elements( String name ) throws NoSuchElementException
  {
    PLYElementList result = elements.get(name);
    if( null == result )
      throw new NoSuchElementException();
    return result;
  }

  @Override public String toString()
  {
    return format(
        "\n{"
      + "\n  header: {"
      + "\n    version: \"%s\","
      + "\n    format: \"%s\","
      + "\n    comments: {%s},"
      + "\n    obj_info: {%s}"
      + "\n  },"
      + "\n  data: {"
      + "\n%s"
      + "\n  }"
      + "\n}",
      version,
      format,
      comments.isEmpty()
        ? ""
        : comments.entrySet().stream().map(
            entry -> entry.getKey() + ": \"" + entry.getValue() + '"'
          ).collect( joining(",\n      ", "\n      ", "\n    ") ),
      obj_infos.isEmpty()
        ? ""
        : obj_infos.entrySet().stream().map(
            entry -> entry.getKey() + ": \"" + entry.getValue() + '"'
          ).collect( joining(",\n      ", "\n      ", "\n    ") ),
      elements.entrySet().stream().map(
        entry -> "    "
          + entry.getKey()
          + ": \n"
          + entry.getValue().str("      ")
          + "\n    }"
      ).collect( joining(",\n") )
    );
  }

  public void save( Path path ) throws IOException
  {
    try( BufferedOutputStream out = new BufferedOutputStream( newOutputStream(path) ) )
    {
      write(out);
    }
  }

  private static String typeString( PLYType<?> type )
  {
    if( type instanceof PLYList )
    {
      PLYList<?,?> list = (PLYList<?,?>) type;
      return "list " + typeString(list.sizeType) + " " + typeString(list.elemType);
    }
    assert ! (type instanceof PLYSequence<?>);
    return type.toString().toLowerCase();
  }

  public void write( OutputStream out ) throws IOException
  {
    HeaderWriter header = new HeaderWriter(out,this);
    header.writeln("ply");
    header.writeln( format("format %s %s", format.toString().toLowerCase(), version) );
    for( Entry<String,PLYElementList> elem: elements.entrySet() )
    {
      PLYElementList list = elem.getValue();
      header.writeln( format("element %s %s", elem.getKey(), list.size) );
      for( Entry<String,PLYType<?>> property: list.properties.entrySet() )
        header.writeln( format("property %s %s", typeString( property.getValue() ), property.getKey() ) );
    }
    header.end_header();

    DataStreamOut output;
    switch(format) {
      case ASCII               : output = new DataStreamOutASCII(out); break;
      case BINARY_BIG_ENDIAN   : output = new DataStreamOutBE(out); break;
      case BINARY_LITTLE_ENDIAN: output = new DataStreamOutLE(out); break;
      default: throw new NullPointerException();
    }

    for( Entry<String,PLYElementList> entry: elements.entrySet() )
    {
      PLYElementList list = entry.getValue();
      String[] properties = list.properties.keySet().stream().toArray(String[]::new);
      PropertyIO[] writers = list.properties.entrySet().stream()
        .map( e -> list.propertyWriter(output, e.getValue(), e.getKey() ) )
        .toArray(PropertyIO[]::new);

      for( int elem=0; elem < list.size; elem++ )
      {
        for( int prop=0; prop < properties.length; prop++ )
          try {
            writers[prop].process(elem);
          }
          catch( Exception ex ) {
            throw new IOException( format("Failed to write %s for %s#%d.", properties[prop], entry.getKey(), elem), ex );
          }
        output.endElement();
      }
    }
  }
}