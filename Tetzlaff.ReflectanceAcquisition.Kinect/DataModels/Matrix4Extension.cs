using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public static class Matrix4Extension
    {
        public static Microsoft.Kinect.Fusion.Matrix4 ToKinectMatrix(
            this Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 mat)
        {
            return new Microsoft.Kinect.Fusion.Matrix4
            {
                M11 = mat.M11,
                M12 = mat.M12,
                M13 = mat.M13,
                M14 = mat.M14,
                M21 = mat.M21,
                M22 = mat.M22,
                M23 = mat.M23,
                M24 = mat.M24,
                M31 = mat.M31,
                M32 = mat.M32,
                M33 = mat.M33,
                M34 = mat.M34,
                M41 = mat.M41,
                M42 = mat.M42,
                M43 = mat.M43,
                M44 = mat.M44
            };
        }

        public static Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 ToPipelineMatrix(
            this Microsoft.Kinect.Fusion.Matrix4 mat)
        {
            return new Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4
            {
                M11 = mat.M11,
                M12 = mat.M12,
                M13 = mat.M13,
                M14 = mat.M14,
                M21 = mat.M21,
                M22 = mat.M22,
                M23 = mat.M23,
                M24 = mat.M24,
                M31 = mat.M31,
                M32 = mat.M32,
                M33 = mat.M33,
                M34 = mat.M34,
                M41 = mat.M41,
                M42 = mat.M42,
                M43 = mat.M43,
                M44 = mat.M44
            };
        }
    }
}
