using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public interface ICameraProjection
    {
        double NearPlane { get; set; }
        double FarPlane { get; set; }
        double AspectRatio { get; set; }
        double HorizontalFieldOfView { get; set; }
        double VerticalFieldOfView { get; set; }
    }
}
