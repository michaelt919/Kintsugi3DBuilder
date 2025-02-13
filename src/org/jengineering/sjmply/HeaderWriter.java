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

import java.io.IOException;
import java.io.OutputStream;
import java.util.TreeMap;

import static java.nio.charset.StandardCharsets.US_ASCII;

/** {@link OutputStream} wrapper that allows line-wise writing of PLY header data, sneakily
 *  inserts the comments into the correct lines.
 *  
 *  @author Dirk Toewe
 */
class HeaderWriter
{
// FIELDS
  private final OutputStream out;
  private final int[] commentLines;
  private final String[] comments;
  private int
    iComment = 0,
    iLine = 1;
  

// CONSTRUCTORS
  public HeaderWriter( OutputStream _out, PLY ply ) throws IOException
  {
    out =_out;

    TreeMap<Integer,String> commentMap = new TreeMap<>();
    ply.comments .forEach( (k,v) -> commentMap.put(k,"comment " +v) );
    ply.obj_infos.forEach( (k,v) -> commentMap.put(k,"obj_info "+v) );

    if( commentMap.size() != ply.comments.size() + ply.obj_infos.size() )
      throw new IOException("For at least one line both a comment and an obj_info is defined.");

    if( commentMap.keySet().stream().anyMatch( line -> line < 3 ) )
      throw new IOException("Not comments are allowed before line 3.");

    commentLines = commentMap.keySet().stream().mapToInt( x -> x ).toArray();
    comments     = commentMap.values().stream().toArray(String[]::new);
  }

// METHODS
  private void _writeln( String line ) throws IOException
  {
    out.write( line.getBytes(US_ASCII) );
    out.write('\n');
    iLine += 1;
  }

  void writeln( String line ) throws IOException
  {
    while( iComment < commentLines.length && commentLines[iComment] == iLine )
      _writeln(comments[iComment++]);
    assert iComment == commentLines.length || iLine < commentLines[iComment];
    _writeln(line);
  }

  /** If there are any unwritten comments left, this methods writes them out
   *  line-by-line. Finally this methods add a line with the string "end_header\n".
   */
  void end_header() throws IOException
  {
    while( iLine < commentLines.length )
      _writeln(comments[iComment++]);
    _writeln("end_header");
  }
}