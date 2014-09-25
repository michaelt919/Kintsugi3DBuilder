using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class AlignedDepthFrame : IDepthFrame
    {
        private IDepthFrame _baseDepthFrame;

        public int Width
        {
            get
            {
                return _baseDepthFrame.Width;
            }
        }

        public int Height
        {
            get
            {
                return _baseDepthFrame.Height;
            }
        }

        public ICameraPose CameraPose { get; set; }

        public AlignedDepthFrame(IDepthFrame depthFrame, ICameraPose cameraPose)
        {
            _baseDepthFrame = depthFrame;
            CameraPose = cameraPose;
        }
    }
}
