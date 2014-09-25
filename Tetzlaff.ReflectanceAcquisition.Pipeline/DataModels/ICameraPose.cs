using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Math;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public interface ICameraPose
    {
        Matrix4 Matrix { get; set; }
    }
}
