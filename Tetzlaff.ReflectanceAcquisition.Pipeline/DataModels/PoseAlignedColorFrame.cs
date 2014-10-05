using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class PoseAlignedColorFrame : PoseAlignedFrame<IColorFrame>, IColorFrame
    {
        public PoseAlignedColorFrame(IColorFrame frame, ICameraPose cameraPose) : 
            base(frame, cameraPose)
        {
        }

        public byte[] RawPixels
        {
            get 
            {
                return this.BaseFrame.RawPixels;
            }
        }

        public ICameraProjection CameraProjection
        {
            get
            {
                return this.BaseFrame.CameraProjection;
            }
            set
            {
                this.BaseFrame.CameraProjection = value;
            }
        }

        public IColorFrame Clone()
        {
            return this.BaseFrame.Clone();
        }
    }
}
