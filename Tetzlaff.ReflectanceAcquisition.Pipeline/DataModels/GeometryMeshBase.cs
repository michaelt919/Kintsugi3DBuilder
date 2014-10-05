using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Math;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public abstract class GeometryMeshBase : IMesh<Vertex3D>
    {
        public abstract ReadOnlyCollection<Vertex3D> Vertices { get; protected set; }

        /// <summary>
        /// Centers the mesh around the centroid (the average vertex position)
        /// </summary>
        /// <returns>The offset the mesh is ultimately shifted by</returns>
        public Vector3 CenterMesh()
        {
            Vector3 sumPositions = new Vector3();
            foreach (Vertex3D vertex in Vertices)
            {
                sumPositions = new Vector3
                {
                    X = sumPositions.X + vertex.Position.X,
                    Y = sumPositions.Y + vertex.Position.Y,
                    Z = sumPositions.Z + vertex.Position.Z
                };
            }

            Vector3 centroid = new Vector3
            {
                X = sumPositions.X / Vertices.Count,
                Y = sumPositions.Y / Vertices.Count,
                Z = sumPositions.Z / Vertices.Count
            };

            foreach (Vertex3D vertex in Vertices)
            {
                vertex.Position = new Vector3
                {
                    X = vertex.Position.X - centroid.X,
                    Y = vertex.Position.Y - centroid.Y,
                    Z = vertex.Position.Z - centroid.Z
                };
            }

            return new Vector3
            {
                X = -centroid.X,
                Y = -centroid.Y,
                Z = -centroid.Z
            };
        }

        /// <summary>
        /// Save mesh in binary .STL file
        /// </summary>
        /// <param name="mesh">Calculated mesh object</param>
        /// <param name="writer">Binary file writer</param>
        /// <param name="flipAxes">Flag to determine whether the Y and Z values are flipped on save,
        /// default should be true.</param>
        public void SaveBinaryStlMesh(BinaryWriter writer, bool flipAxes)
        {
            if (null == writer)
            {
                return;
            }

            // Check mesh arguments
            if (0 == Vertices.Count || 0 != Vertices.Count % 3)
            {
                throw new ArgumentException("A mesh must have a vertex count that is a positive multiple of 3.");
            }

            char[] header = new char[80];
            writer.Write(header);

            // Write number of triangles
            int triangles = Vertices.Count / 3;
            writer.Write(triangles);

            // Sequentially write the normal, 3 vertices of the triangle and attribute, for each triangle
            for (int i = 0; i < triangles; i++)
            {
                // Write normal
                var normal = this.Vertices[i * 3].Normal;
                writer.Write(normal.X);
                writer.Write(flipAxes ? -normal.Y : normal.Y);
                writer.Write(flipAxes ? -normal.Z : normal.Z);

                // Write vertices
                for (int j = 0; j < 3; j++)
                {
                    var vertex = this.Vertices[(i * 3) + j].Position;
                    writer.Write(vertex.X);
                    writer.Write(flipAxes ? -vertex.Y : vertex.Y);
                    writer.Write(flipAxes ? -vertex.Z : vertex.Z);
                }

                ushort attribute = 0;
                writer.Write(attribute);
            }
        }

        /// <summary>
        /// Save mesh in ASCII WaveFront .OBJ file
        /// </summary>
        /// <param name="mesh">Calculated mesh object</param>
        /// <param name="writer">The text writer</param>
        /// <param name="flipAxes">Flag to determine whether the Y and Z values are flipped on save,
        /// default should be true.</param>
        public void SaveAsciiObjMesh(TextWriter writer, bool flipAxes)
        {
            if (null == writer)
            {
                return;
            }

            int count = this.Vertices.Count();

            // Check mesh arguments
            if (0 == count || 0 != count % 3)
            {
                throw new ArgumentException("A mesh must have a vertex count that is a positive multiple of 3.");
            }

            // Write the header lines
            writer.WriteLine("#");
            writer.WriteLine("# OBJ file created by Microsoft Kinect Fusion");
            writer.WriteLine("#");

            // Sequentially write the 3 vertices of the triangle, for each triangle
            for (int i = 0; i < this.Vertices.Count; i++)
            {
                var vertex = this.Vertices[i].Position;

                string vertexString = "v " + vertex.X.ToString(CultureInfo.InvariantCulture) + " ";

                if (flipAxes)
                {
                    vertexString += (-vertex.Y).ToString(CultureInfo.InvariantCulture) + " " + (-vertex.Z).ToString(CultureInfo.InvariantCulture);
                }
                else
                {
                    vertexString += vertex.Y.ToString(CultureInfo.InvariantCulture) + " " + vertex.Z.ToString(CultureInfo.InvariantCulture);
                }

                writer.WriteLine(vertexString);
            }

            // Sequentially write the 3 normals of the triangle, for each triangle
            for (int i = 0; i < this.Vertices.Count; i++)
            {
                var normal = this.Vertices[i].Normal;

                string normalString = "vn " + normal.X.ToString(CultureInfo.InvariantCulture) + " ";

                if (flipAxes)
                {
                    normalString += (-normal.Y).ToString(CultureInfo.InvariantCulture) + " " + (-normal.Z).ToString(CultureInfo.InvariantCulture);
                }
                else
                {
                    normalString += normal.Y.ToString(CultureInfo.InvariantCulture) + " " + normal.Z.ToString(CultureInfo.InvariantCulture);
                }

                writer.WriteLine(normalString);
            }

            // Sequentially write the 3 vertex indices of the triangle face, for each triangle
            // Note this is typically 1-indexed in an OBJ file when using absolute referencing!
            for (int i = 0; i < count / 3; i++)
            {
                string baseIndex0 = ((i * 3) + 1).ToString(CultureInfo.InvariantCulture);
                string baseIndex1 = ((i * 3) + 2).ToString(CultureInfo.InvariantCulture);
                string baseIndex2 = ((i * 3) + 3).ToString(CultureInfo.InvariantCulture);

                string faceString = "f " + baseIndex0 + "//" + baseIndex0 + " " + baseIndex1 + "//" + baseIndex1 + " " + baseIndex2 + "//" + baseIndex2;
                writer.WriteLine(faceString);
            }
        }

        /// <summary>
        /// Save mesh in ASCII .PLY file with per-vertex color
        /// </summary>
        /// <param name="mesh">Calculated mesh object</param>
        /// <param name="writer">The text writer</param>
        /// <param name="flipAxes">Flag to determine whether the Y and Z values are flipped on save,
        /// default should be true.</param>
        /// <param name="outputColor">Set this true to write out the surface color to the file when it has been captured.</param>
        public void SaveAsciiPlyMesh(TextWriter writer, bool flipAxes)
        {
            if (null == writer)
            {
                return;
            }

            int count = this.Vertices.Count();

            // Check mesh arguments
            if (0 == count || 0 != count % 3)
            {
                throw new ArgumentException("A mesh must have a vertex count that is a positive multiple of 3.");
            }

            int faces = count / 3;

            // Write the PLY header lines
            writer.WriteLine("ply");
            writer.WriteLine("format ascii 1.0");
            writer.WriteLine("comment file created by Microsoft Kinect Fusion");

            writer.WriteLine("element vertex " + count.ToString(CultureInfo.InvariantCulture));
            writer.WriteLine("property float x");
            writer.WriteLine("property float y");
            writer.WriteLine("property float z");

            writer.WriteLine("element face " + faces.ToString(CultureInfo.InvariantCulture));
            writer.WriteLine("property list uchar int vertex_index");
            writer.WriteLine("end_header");

            // Sequentially write the 3 vertices of the triangle, for each triangle
            for (int i = 0; i < this.Vertices.Count; i++)
            {
                var vertex = this.Vertices[i].Position;

                string vertexString = vertex.X.ToString(CultureInfo.InvariantCulture) + " ";

                if (flipAxes)
                {
                    vertexString += (-vertex.Y).ToString(CultureInfo.InvariantCulture) + " " + (-vertex.Z).ToString(CultureInfo.InvariantCulture);
                }
                else
                {
                    vertexString += vertex.Y.ToString(CultureInfo.InvariantCulture) + " " + vertex.Z.ToString(CultureInfo.InvariantCulture);
                }

                writer.WriteLine(vertexString);
            }

            // Sequentially write the 3 vertex indices of the triangle face, for each triangle, 0-referenced in PLY files
            for (int i = 0; i < faces; i++)
            {
                string baseIndex0 = (i * 3).ToString(CultureInfo.InvariantCulture);
                string baseIndex1 = ((i * 3) + 1).ToString(CultureInfo.InvariantCulture);
                string baseIndex2 = ((i * 3) + 2).ToString(CultureInfo.InvariantCulture);

                string faceString = "3 " + baseIndex0 + " " + baseIndex1 + " " + baseIndex2;
                writer.WriteLine(faceString);
            }
        }
    }
}
