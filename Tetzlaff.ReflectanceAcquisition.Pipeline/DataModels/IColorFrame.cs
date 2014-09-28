using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public interface IColorFrame : IFrame
    {
        byte[] RawPixels { get; }
    }
}
