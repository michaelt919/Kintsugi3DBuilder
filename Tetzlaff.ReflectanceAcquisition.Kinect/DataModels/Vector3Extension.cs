using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public static class Vector3Extension
    {
        public static Microsoft.Kinect.Fusion.Vector3 ToKinectVector(
            this Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Vector3 vec)
        {
            return new Microsoft.Kinect.Fusion.Vector3
            {
                X = vec.X,
                Y = vec.Y,
                Z = vec.Z
            };
        }

        public static Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Vector3 ToPipelineVector(
            this Microsoft.Kinect.Fusion.Vector3 vec)
        {
            return new Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Vector3
            {
                X = vec.X,
                Y = vec.Y,
                Z = vec.Z
            };
        }
    }
}
