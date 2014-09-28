using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class DoubleBufferedColorFrame<ColorFrameType> : IColorFrame
        where ColorFrameType : IColorFrame
    {
        public int Width { get; private set; }
        public int Height { get; private set; }

        public byte[] RawPixels
        {
            get
            {
                return FrontBuffer.RawPixels;
            }
        }

        public ColorFrameType FrontBuffer { get; private set; }
        public ColorFrameType BackBuffer { get; private set; }

        public DoubleBufferedColorFrame(ColorFrameType frontBuffer, ColorFrameType backBuffer)
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
            ColorFrameType tmp = FrontBuffer;
            FrontBuffer = BackBuffer;
            BackBuffer = tmp;
        }
    }
}
