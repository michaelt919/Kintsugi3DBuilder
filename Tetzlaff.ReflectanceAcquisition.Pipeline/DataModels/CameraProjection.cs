using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class CameraProjection : ICameraProjection
    {
        public double NearPlane { get; set; }
        public double FarPlane { get; set; }
        public double AspectRatio { get; set; }
        public double HorizontalFieldOfView { get; set; }
        public double VerticalFieldOfView { get; set; }

        public bool Equals(ICameraProjection other)
        {
            return this.NearPlane == other.NearPlane &&
                this.FarPlane == other.FarPlane &&
                this.AspectRatio == other.AspectRatio &&
                this.HorizontalFieldOfView == other.HorizontalFieldOfView &&
                this.VerticalFieldOfView == other.VerticalFieldOfView;
        }
    }
}
