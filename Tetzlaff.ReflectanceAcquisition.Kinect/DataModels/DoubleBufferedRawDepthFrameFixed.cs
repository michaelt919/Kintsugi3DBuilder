using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public class DoubleBufferedRawDepthFrameFixed<DepthFrameType> : IRawDepthFrameFixed
        where DepthFrameType : IRawDepthFrameFixed
    {
        public int Width { get; private set; }
        public int Height { get; private set; }

        public ushort[] RawPixels
        {
            get
            {
                return FrontBuffer.RawPixels;
            }
        }

        public DepthFrameType FrontBuffer { get; private set; }
        public DepthFrameType BackBuffer { get; private set; }

        public DoubleBufferedRawDepthFrameFixed(DepthFrameType frontBuffer, DepthFrameType backBuffer)
        {
            if (frontBuffer.Width != backBuffer.Width || frontBuffer.Height != backBuffer.Height)
            {
                throw new ArgumentException("Front and back buffers must be the same size.");
            }

            this.Width = frontBuffer.Width;
            this.Height = frontBuffer.Height;
            this.FrontBuffer = frontBuffer;
            this.BackBuffer = backBuffer;
        }

        public void SwapBuffers()
        {
            DepthFrameType tmp = FrontBuffer;
            FrontBuffer = BackBuffer;
            BackBuffer = tmp;
        }
    }
}
