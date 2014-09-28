using System;
using System.Collections.Generic;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public class RawColorFrame : IRawColorFrame
    {
        public int Width { get; private set; }

        public int Height { get; private set; }

        /// <summary>
        /// Data buffer for raw pixels
        /// </summary>
        public byte[] RawPixels { get; private set; }

        public RawColorFrame(int width, int height, int maxUpsampleFactor = 1)
        {
            this.Width = width;
            this.Height = height;
            int colorImageSize = this.Width * this.Height;
            int colorImageByteSize = colorImageSize * sizeof(int);
            RawPixels = new byte[colorImageByteSize];
        }
    }
}
