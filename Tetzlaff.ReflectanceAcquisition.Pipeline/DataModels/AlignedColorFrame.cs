using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class AlignedColorFrame
    {
        private IColorFrame _baseColorFrame;

        public int Width
        {
            get
            {
                return _baseColorFrame.Width;
            }
        }

        public int Height
        {
            get
            {
                return _baseColorFrame.Height;
            }
        }

        public ICameraPose CameraPose { get; set; }

        public AlignedColorFrame(IColorFrame colorFrame, ICameraPose cameraPose)
        {
            _baseColorFrame = colorFrame;
            CameraPose = cameraPose;
        }
    }
}
