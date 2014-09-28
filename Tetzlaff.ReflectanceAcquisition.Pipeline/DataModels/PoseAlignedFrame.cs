using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class PoseAlignedFrame : IFrame
    {
        private IFrame _baseFrame;

        public int Width
        {
            get
            {
                return _baseFrame.Width;
            }
        }

        public int Height
        {
            get
            {
                return _baseFrame.Height;
            }
        }

        public ICameraPose CameraPose { get; set; }

        public PoseAlignedFrame(IFrame frame, ICameraPose cameraPose)
        {
            _baseFrame = frame;
            CameraPose = cameraPose;
        }
    }
}
