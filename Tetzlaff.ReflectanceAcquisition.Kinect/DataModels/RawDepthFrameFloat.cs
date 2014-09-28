using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public class RawDepthFrameFloat : IRawDepthFrameFloat
    {
        public int Width { get; private set; }
        public int Height { get; private set; }

        /// <summary>
        /// Data buffer for raw pixels
        /// </summary>
        public float[] RawPixels { get; private set; }

        public RawDepthFrameFloat(int width, int height)
        {
            this.Width = width;
            this.Height = height;
            int depthImageSize = this.Width * this.Height;
            RawPixels = new float[depthImageSize];
        }
    }
}
