package org.jengineering.sjmply;
/* Copyright 2016 Dirk Toewe
 * 
 * This file was generated using org.jengineering.sjmply.
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

import static java.lang.Float.floatToRawIntBits;
import static java.lang.String.format;
import static java.nio.file.Files.newOutputStream;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.jengineering.sjmply.PLYType.FLOAT32;
import static org.jengineering.sjmply.PLYType.LIST;
import static org.jengineering.sjmply.PLYType.UINT32;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.IntConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Utility methods to create <a href="https://plot.ly/">Plotly</a> 3D plots of PLY files.
 *  
 *  @author Dirk Toewe
 */
public class PLY_Plotly
{
// FIELDS

// CONSTRUCTORS

// METHODS

// STATIC FIELDS
  private static final Base64.Encoder B64_ENCODER = Base64.getMimeEncoder(1024, new byte[]{'\\','\n',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' '} );

// STATIC CONSTRUCTOR

// STATIC METHODS
  public static void Scatter3d_show( String title, PLY ply ) throws IOException
  {
    Path tmp = Files.createTempFile("PLY_Scatter3D_", ".html");
    Scatter3d_save(title,ply,tmp);
    Desktop.getDesktop().browse( tmp.toUri() );
  }

  public static void Scatter3d_save( String title, PLY ply, Path path ) throws IOException
  {
    try( OutputStream out = newOutputStream(path) )
    {
      Scatter3d_write(title,ply,out);
    }
  }

  public static void Scatter3d_write( String title, PLY ply, OutputStream out ) throws IOException
  {
    OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    writer.write( Scatter3d_html(title,ply).toString() );
    writer.flush();
  }

  /** Returns HTML file containing a Plotly Scatter3d plot of the given {@link PLY} object as {@link CharSequence}.
   *  
   *  @param title The plot and HTML document title.
   *  @param ply   The {@link PLY} object for which a 3d mesh view is created.
   *  @return An HTML document as {@link CharSequence}, which contains a 3d scatter plot of ply's vertices.
   */
  public static CharSequence Scatter3d_html( String title, PLY ply ) throws IOException
  {
    int[][] vertex_indices = ply.elements("face").propertyAs(LIST(UINT32),"vertex_indices");

    assert stream(vertex_indices).allMatch(
      face -> {
        for( int i=0; i < face.length; i++ )
          if( 0 > face[i] )
            return false;
        return true;
      }
    );

    for( int i=0; i < vertex_indices.length; i++ )
      if( vertex_indices[i].length != 3 )
        throw new IllegalArgumentException( format("face/vertex_indices[%d] has an unsupported length of %d.",i,vertex_indices[i].length) );

    StringBuilder data = new StringBuilder();
    OutputStream ascii_out = new OutputStream()
    {
      @Override public void write( int ascii_char ) { data.appendCodePoint(ascii_char); }
    };
    try( ZipOutputStream zip = new ZipOutputStream( B64_ENCODER.wrap(ascii_out) ) )
    {
      zip.setLevel(9);
      byte[] cache = new byte[4];
      IntConsumer cacheInt = bits -> {
        cache[0] = (byte) (bits >> 0);
        cache[1] = (byte) (bits >> 8);
        cache[2] = (byte) (bits >>16);
        cache[3] = (byte) (bits >>24);
      };

      for( String xyz: asList("x","y","z") )
      {
        zip.putNextEntry( new ZipEntry("/vertex/"+xyz) );

        for( float dat: ply.elements("vertex").propertyAs(FLOAT32,xyz) )
        {
          cacheInt.accept( floatToRawIntBits(dat) );
          zip.write(cache);
        }
      }
    }
    catch( IOException e ) {
      throw new Error(e);
    }

    return format(
          "<!DOCTYPE html>"
      + "\n<html lang=\"en\">"
      + "\n  <!--"
      + "\n    This file was generated using org.jengineering.sjmply."
      + "\n"
      + "\n    org.jengineering.sjmply is free software: you can redistribute it and/or"
      + "\n    modify it under the terms of the GNU General Public License as published"
      + "\n    by the Free Software Foundation, either version 3 of the License, or"
      + "\n    (at your option) any later version."
      + "\n"
      + "\n    org.jengineering.sjmply is distributed in the hope that it will be useful,"
      + "\n    but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY"
      + "\n    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License"
      + "\n    for more details."
      + "\n"
      + "\n    You should have received a copy of the GNU General Public License"
      + "\n    along with org.jengineering.sjmply. If not, see <http://www.gnu.org/licenses/>."
      + "\n   -->"
      + "\n  <head>"
      + "\n    <meta charset=\"utf-8\">"
      + "\n    <title>%s</title>"
      + "\n    <script type=\"text/javascript\" src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>"
      + "\n    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/jszip/3.1.3/jszip.min.js\"></script>"
      + "\n  </head>"
      + "\n  <body>"
      + "\n    <script type=\"text/javascript\">"
      + "\n    {"
      + "\n      'use strict';"
      + "\n      let div = document.createElement('div');"
      + "\n      div.style = 'width: 100%%; height: 1024px';"
      + "\n      div.innerHTML = 'Unzipping Data...';"
      + "\n      document.body.appendChild(div);"
      + "\n"
      + "\n      function isBigEndian()"
      + "\n      {"
      + "\n        let"
      + "\n          buf = new ArrayBuffer(2),"
      + "\n          uInt8 = new Uint8Array(buf),"
      + "\n          uInt16 = new Uint16Array(buf);"
      + "\n        uInt8[0] = 0x55;"
      + "\n        uInt8[1] = 0xAA;"
      + "\n        if(uInt16[0] === 0xAA55) return false;"
      + "\n        if(uInt16[0] === 0x55AA) return true;"
      + "\n        throw new Error();"
      + "\n      }"
      + "\n"
      + "\n      let"
      + "\n        zip = new JSZip().loadAsync("
      + "\n          '%s',"
      + "\n          { base64: true }"
      + "\n        ),"
      + "\n        data = ["
      + "\n          zip.then( zip => zip.file('/vertex/x').async('arraybuffer') ).then( buf => new Float32Array(buf) ),"
      + "\n          zip.then( zip => zip.file('/vertex/y').async('arraybuffer') ).then( buf => new Float32Array(buf) ),"
      + "\n          zip.then( zip => zip.file('/vertex/z').async('arraybuffer') ).then( buf => new Float32Array(buf) ),"
      + "\n        ];"
      + "\n"
      + "\n      // flip byte order if necessary"
      + "\n      if( isBigEndian() )"
      + "\n        for( let i=data.length-1; i >=0; i-- )"
      + "\n          data[i] = data[i].then("
      + "\n            arr => {"
      + "\n              let view = new DataView(arr.buffer);"
      + "\n              for( let j=arr.length-1; j >= 0; j-- )"
      + "\n                view.setUint32(4*j, view.getUint32(4*j,true), false);"
      + "\n              return arr;"
      + "\n            }"
      + "\n          );"
      + "\n"
      + "\n      // this is unfortunately neccessary since Plotly cannot handle typed arrays"
      + "\n      for( let i=data.length-1; i >=0; i-- )"
      + "\n        data[i] = data[i].then( arr => Array.from(arr) );"
      + "\n"
      + "\n      let"
      + "\n        progress = 0,"
      + "\n        updateProgress = progress => div.innerHTML = 'Unzipping Data ('+progress+'/'+data.length+')...';"
      + "\n      updateProgress(0);"
      + "\n"
      + "\n      for( let i=data.length-1; i >=0; i-- )"
      + "\n        data[i].then( x => updateProgress(++progress) );"
      + "\n"        
      + "\n      Promise.all(data).then("
      + "\n        xyz => {"
      + "\n          let"
      + "\n            [x,y,z] = xyz,"
      + "\n            data = [{"
      + "\n              type: 'scatter3d',"
      + "\n              mode: 'markers',"
      + "\n              marker: { size: 1 },"
      + "\n              x: x,"
      + "\n              y: y,"
      + "\n              z: z,"
      + "\n            }],"
      + "\n            layout = {"
      + "\n              title: '%s',"
      + "\n              scene: {"
      + "\n                aspectratio: {x: 1, y: 1, z: 1},"
      + "\n                aspectmode: 'data'"
      + "\n              }"
      + "\n            };"
      + "\n          div.innerHTML = '';"
      + "\n          Plotly.plot(div, data, layout, { showLink: false, modeBarButtonsToRemove: ['sendDataToCloud'] });"
      + "\n        }"
      + "\n      );"
      + "\n    }"
      + "\n    </script>"
      + "\n  </body>"
      + "\n</html>"
      , title
      , data
      , title
    );
  }

  public static void Mesh3d_show( String title, PLY ply ) throws IOException
  {
    Path tmp = Files.createTempFile("PLY_Mesh3D_", ".html");
    Mesh3d_save(title,ply,tmp);
    Desktop.getDesktop().browse( tmp.toUri() );
  }

  public static void Mesh3d_save( String title, PLY ply, Path path ) throws IOException
  {
    try( OutputStream out = newOutputStream(path) )
    {
      Mesh3d_write(title,ply,out);
    }
  }

  public static void Mesh3d_write( String title, PLY ply, OutputStream out ) throws IOException
  {
    OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    writer.write( Mesh3d_html(title,ply).toString() );
    writer.flush();
  }

  /** Returns HTML file containing a Plotly Mesh3d plot of the given {@link PLY} object as {@link CharSequence}.
   *  
   *  @param title The plot and HTML document title.
   *  @param ply   The {@link PLY} object for which a 3d mesh view is created.
   *  @return An HTML document as {@link CharSequence}, which contains a 3d mesh view of ply's faces.
   */
  public static CharSequence Mesh3d_html( CharSequence title, PLY ply )
  {
    int[][] vertex_indices = ply.elements("face").propertyAs(LIST(UINT32),"vertex_indices");

    assert stream(vertex_indices).allMatch(
      face -> {
        for( int i=0; i < face.length; i++ )
          if( 0 > face[i] )
            return false;
        return true;
      }
    );

    for( int i=0; i < vertex_indices.length; i++ )
      if( vertex_indices[i].length != 3 )
        throw new IllegalArgumentException( format("face/vertex_indices[%d] has an unsupported length of %d.",i,vertex_indices[i].length) );

    StringBuilder data = new StringBuilder();
    OutputStream ascii_out = new OutputStream()
    {
      @Override public void write( int ascii_char ) { data.appendCodePoint(ascii_char); }
    };
    try( ZipOutputStream zip = new ZipOutputStream( B64_ENCODER.wrap(ascii_out) ) )
    {
      zip.setLevel(9);
      byte[] cache = new byte[4];
      IntConsumer cacheInt = bits -> {
        cache[0] = (byte) (bits >> 0);
        cache[1] = (byte) (bits >> 8);
        cache[2] = (byte) (bits >>16);
        cache[3] = (byte) (bits >>24);
      };

      for( String xyz: asList("x","y","z") )
      {
        zip.putNextEntry( new ZipEntry("/vertex/"+xyz) );

        for( float dat: ply.elements("vertex").propertyAs(FLOAT32,xyz) )
        {
          cacheInt.accept( floatToRawIntBits(dat) );
          zip.write(cache);
        }
      }

      String[] ijk = {"i","j","k"};
      for( int l=0; l < ijk.length; l++ )
      {
        zip.putNextEntry( new ZipEntry("/face/"+ijk[l]) );

        for( int[] face: vertex_indices )
        {
          cacheInt.accept(face[l]);
          zip.write(cache);
        }
      }
    }
    catch( IOException e ) {
      throw new Error(e);
    }

    return format(
          "<!DOCTYPE html>"
      + "\n<html lang=\"en\">"
      + "\n  <!--"
      + "\n    This file was generated using org.jengineering.sjmply."
      + "\n"
      + "\n    org.jengineering.sjmply is free software: you can redistribute it and/or"
      + "\n    modify it under the terms of the GNU General Public License as published"
      + "\n    by the Free Software Foundation, either version 3 of the License, or"
      + "\n    (at your option) any later version."
      + "\n"
      + "\n    org.jengineering.sjmply is distributed in the hope that it will be useful,"
      + "\n    but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY"
      + "\n    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License"
      + "\n    for more details."
      + "\n"
      + "\n    You should have received a copy of the GNU General Public License"
      + "\n    along with org.jengineering.sjmply. If not, see <http://www.gnu.org/licenses/>."
      + "\n   -->"
      + "\n  <head>"
      + "\n    <meta charset=\"utf-8\">"
      + "\n    <title>%s</title>"
      + "\n    <script type=\"text/javascript\" src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>"
      + "\n    <script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/jszip/3.1.3/jszip.min.js\"></script>"
      + "\n  </head>"
      + "\n  <body>"
      + "\n    <script type=\"text/javascript\">"
      + "\n    {"
      + "\n      'use strict';"
      + "\n      let div = document.createElement('div');"
      + "\n      div.style = 'width: 100%%; height: 1024px';"
      + "\n      div.innerHTML = 'Unzipping Data...';"
      + "\n      document.body.appendChild(div);"
      + "\n"
      + "\n      function isBigEndian()"
      + "\n      {"
      + "\n        let"
      + "\n          buf = new ArrayBuffer(2),"
      + "\n          uInt8 = new Uint8Array(buf),"
      + "\n          uInt16 = new Uint16Array(buf);"
      + "\n        uInt8[0] = 0x55;"
      + "\n        uInt8[1] = 0xAA;"
      + "\n        if(uInt16[0] === 0xAA55) return false;"
      + "\n        if(uInt16[0] === 0x55AA) return true;"
      + "\n        throw new Error();"
      + "\n      }"
      + "\n"
      + "\n      let"
      + "\n        zip = new JSZip().loadAsync("
      + "\n          '%s',"
      + "\n          { base64: true }"
      + "\n        ),"
      + "\n        data = ["
      + "\n          zip.then( zip => zip.file('/vertex/x').async('arraybuffer') ).then( buf => new Float32Array(buf) ),"
      + "\n          zip.then( zip => zip.file('/vertex/y').async('arraybuffer') ).then( buf => new Float32Array(buf) ),"
      + "\n          zip.then( zip => zip.file('/vertex/z').async('arraybuffer') ).then( buf => new Float32Array(buf) ),"
      + "\n          zip.then( zip => zip.file('/face/i'  ).async('arraybuffer') ).then( buf => new  Uint32Array(buf) ),"
      + "\n          zip.then( zip => zip.file('/face/j'  ).async('arraybuffer') ).then( buf => new  Uint32Array(buf) ),"
      + "\n          zip.then( zip => zip.file('/face/k'  ).async('arraybuffer') ).then( buf => new  Uint32Array(buf) )"
      + "\n        ];"
      + "\n"
      + "\n      // flip byte order if necessary"
      + "\n      if( isBigEndian() )"
      + "\n        for( let i=data.length-1; i >=0; i-- )"
      + "\n          data[i] = data[i].then("
      + "\n            arr => {"
      + "\n              let view = new DataView(arr.buffer);"
      + "\n              for( let j=arr.length-1; j >= 0; j-- )"
      + "\n                view.setUint32(4*j, view.getUint32(4*j,true), false);"
      + "\n              return arr;"
      + "\n            }"
      + "\n          );"
      + "\n"
      + "\n      // this is unfortunately neccessary since Plotly cannot handle typed arrays"
      + "\n      for( let i=data.length-1; i >=0; i-- )"
      + "\n        data[i] = data[i].then( arr => Array.from(arr) );"
      + "\n"
      + "\n      let"
      + "\n        progress = 0,"
      + "\n        updateProgress = progress => div.innerHTML = 'Unzipping Data ('+progress+'/'+data.length+')...';"
      + "\n      updateProgress(0);"
      + "\n"
      + "\n      for( let i=data.length-1; i >=0; i-- )"
      + "\n        data[i].then( x => updateProgress(++progress) );"
      + "\n"        
      + "\n      Promise.all(data).then("
      + "\n        xyzijk => {"
      + "\n          let"
      + "\n            [x,y,z,i,j,k] = xyzijk,"
      + "\n            data = [{"
      + "\n              type: 'mesh3d',"
      + "\n              x: x, y: y, z: z,"
      + "\n              i: i, j: j, k: k,"
      + "\n            }],"
      + "\n            layout = {"
      + "\n              title: '%s',"
      + "\n              scene: {"
      + "\n                aspectratio: {x: 1, y: 1, z: 1},"
      + "\n                aspectmode: 'data'"
      + "\n              }"
      + "\n            };"
      + "\n          div.innerHTML = '';"
      + "\n          Plotly.plot(div, data, layout, { showLink: false, modeBarButtonsToRemove: ['sendDataToCloud'] });"
      + "\n        }"
      + "\n      );"
      + "\n    }"
      + "\n    </script>"
      + "\n  </body>"
      + "\n</html>"
      , title
      , data
      , title
    );
  }
}