using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Math;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public class KinectFusionGeometryMesh : GeometryMeshBase
    {
        public override ReadOnlyCollection<Vertex3D> Vertices { get; protected set; }

        public KinectFusionGeometryMesh(Microsoft.Kinect.Fusion.ColorMesh baseMesh)
        {
            ReadOnlyCollection<Microsoft.Kinect.Fusion.Vector3> kinectVertices = baseMesh.GetVertices();
            ReadOnlyCollection<Microsoft.Kinect.Fusion.Vector3> kinectNormals = baseMesh.GetNormals();

            Vertex3D[] vertices = new Vertex3D[kinectVertices.Count];
            for (int i = 0; i < kinectVertices.Count; i++)
            {
                vertices[i] = new Vertex3D
                {
                    Position = kinectVertices[i].ToPipelineVector(),
                    Normal = kinectNormals[i].ToPipelineVector()
                };
            }
            this.Vertices = new ReadOnlyCollection<Vertex3D>(vertices);
        }
    }
}
