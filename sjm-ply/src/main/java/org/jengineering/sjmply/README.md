# sjmply

sjmply is a simple Java-based model for [PLY files](https://en.wikipedia.org/wiki/PLY_(file_format)).

## Reading a PLY File
```Java
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jengineering.sjmply.PLY;

Path path = Paths.get( System.getProperty("user.home"), "Documents/3d_models/bunny.ply" );
PLY bunny = PLY.load(path);
System.out.println(bunny);
```

## Visualizing a PLY File
sjmply offers convenience methods that generate standalone HTML visualizations of a PLY file,
either as triangle mesh or point cloud (scatter plot). The visualization uses [Plotly](Plot.ly).
As of now these methods are only meant for debugging and quick visualization. They do not support
the full configurability and features that Plotly itself offers.

In order to minimize the HTML file size, the data is compressed as zip archive.
[JSZip](https://stuk.github.io/jszip/) is used to decompress the data during loading.

### Mesh
```Java
import org.jengineering.sjmply.PLY_Plotly;

PLY_Plotly.Mesh3d_show("Bunneh",bunny);
```

### Point Cloud
```Java
import org.jengineering.sjmply.PLY_Plotly;

PLY_Plotly.Scatter3d_show("Bunneh",bunny);
```

## Accessing Elements and Properties
The data in PLY files is organized in element lists. In an element list, all elements
have the same properties. The element lists and their properties are declared in the
file header. The format can be extended with additional element lists and properties.

Supported property types are:
  * int8, int16, int32 (char, short, int)
  * uint8, uint16, uint32 (uchar, ushort, uint)
  * float32, float64 (float, double)
  * list

The list type has an associated size type and an element type that need to be declare alongside it,
e.g. `property list uint8 uint32 vertex_indices` is a property of name `vertex_indices`
whose type is a list with a size type of `uint8` and an element type `uint32`. It is
unclear to the author of sjmply whether or not nested lists (e.g. `list uint8 list uint8 float32`)
are allowed according to the PLY file specification. sjmply however supports them,
other PLY file libraries might not. Elements are accessed by their name using the `elements()` method.
```Java
PLYElementList
  vertex = bunny.elements("vertex"),
  face = bunny.elements("face");
System.out.println(vertex);
System.out.println(face);
```

sjmply stores property data in form or primitive arrays. The primitive types are mapped to the Java
primitive types of the same bit count. Those array can be accessed using the `property()` method.
```Java
import java.util.Arrays;
import static org.jengineering.sjmply.PLYType.*;

float[] x = vertex.property(FLOAT32,"x");
int[][] vertex_indices = face.property(LIST(UINT32),"vertex_indices");
System.out.println( Arrays.toString(x) );
System.out.println( Arrays.deepToString(vertex_indices) );
```
The specified type must match the exact property type. Calling `property(LIST(FLOAT64), ...)` on a 
`LIST(FLOAT32)` property will fail. An `IllegalArgumentException` is thrown.

The array returned by the `property()` method is the underlying data array. Changes to the array changes
the content of the element list. The entry `arr[i]` of an array `arr` represents the property value of the
(i+1)th element.

Keep in mind that there are no unsigned integer types in Java. Unsigned value have to be handled accordingly.
It is recommended to convert them to a higher bit count signed integer, e.g. using the [Byte.toUnsignedInt](https://docs.oracle.com/javase/8/docs/api/java/lang/Byte.html#toUnsignedInt-byte-), [Short.toUnsignedInt](https://docs.oracle.com/javase/8/docs/api/java/lang/Short.html#toUnsignedInt-short-) or the [Integer.toUnsignedLong](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html#toUnsignedLong-int-) method.

The `propertyAs()` method allows to canonically read property data. It always returns a new array which contains
the data converted to the requested type.
```Java
double[] x_double = vertex.propertyAs(FLOAT64,"x");
short[][] vertex_indices_short = face.propertyAs(LIST(UINT16),"vertex_indices");
```
Conversion between integer types are allowed, as long as there is no integer over- or underflow during conversion.
Any number type can be converted to a floating point type potentially causing loss of data. A conversion from
float to integer is not supported.

## Converting a Property
In order to canonicalize PLY files, it may become necessary to convert different typs of the same property
to a uniform type. The `convertProperty` method allows just that.
```Java
bunny.elements("face").convertProperty( "vertex_indices", LIST(UINT8,UINT16) );
System.out.println( bunny.elements("face") );
```
While converting a property from one list type to another the size type has to be specified. Calling
`convertProperty( "vertex_indices", LIST(UINT16) )` will not even compile.

## Writing a PLY File
A PLY instance can be written to a file using its `save()` method.
```Java
Path out = Paths.get("/tmp/bunny.ply");
bunny.save(out);
```

## Creating a PLY File
PLY instance can created in-memory using their constructor.
```Java
PLY ply = new PLY();
System.out.println(ply);
```

## Adding an Element List
New element lists can be directly added to the `elements` map of a PLY instance.
```Java
PLYElementList edge = new PLYElementList(1337);
ply.elements.put("edge",edge);
System.out.println(ply);
```

The size of an element has to be specified while creating it. In this case a list of 1337 "edges" was created.

## Adding a Property
The `addProperty()` methods adds a new property to an element list.
```Java
edge.addProperty(LIST(UINT8,UINT32),"vertex_indices");
System.out.println(ply);
```
While adding a property from one list type to another the size type has to be specified. Calling
`addProperty(LIST(UINT32),"vertex_indices")` will not even compile.

## Changing the Output Format
The output format of a PLY instance is determined by its `format` field. To change it use the
`setFormat()` method.
```Java
import static org.jengineering.sjmply.PLYFormat.*;

bunny.setFormat(ASCII);
System.out.println(bunny);
```
