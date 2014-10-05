using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class PoseAlignedFrame<FrameType> : IFrame
        where FrameType : IFrame
    {
        protected FrameType BaseFrame { get; private set; }

        public int Width
        {
            get
            {
                return BaseFrame.Width;
            }
        }

        public int Height
        {
            get
            {
                return BaseFrame.Height;
            }
        }

        public ICameraPose CameraPose { get; set; }

        public PoseAlignedFrame(FrameType frame, ICameraPose cameraPose)
        {
            BaseFrame = frame;
            CameraPose = cameraPose;
        }
    }
}
