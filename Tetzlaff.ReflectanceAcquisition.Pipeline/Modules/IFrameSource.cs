using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.Modules
{
    public interface IFrameSource<DepthFrameType, ColorFrameType>
        where DepthFrameType : IDepthFrame
        where ColorFrameType : IColorFrame
    {
        DepthFrameType DepthFrame { get; }
        ColorFrameType ColorFrame { get; }

        event Action DepthFrameReady;
        event Action ColorFrameReady;

        void RefreshDepthFrame();
        void RefreshColorFrame();
    }
}
