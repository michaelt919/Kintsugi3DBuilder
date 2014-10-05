using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public interface ICameraProjection : IEquatable<ICameraProjection>
    {
        double NearPlane { get; }
        double FarPlane { get; }
        double AspectRatio { get; }
        double HorizontalFieldOfView { get; }
        double VerticalFieldOfView { get; }
    }
}
