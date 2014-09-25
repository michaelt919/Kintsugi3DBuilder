using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.Modules
{
    public interface IFrameSource<FrameType> where FrameType : IDepthFrame
    {
        void CopyToFrame(FrameType frame);
    }
}
