using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public interface IKinectColorFrame : IColorFrame
    {
        FusionColorImageFrame FusionImageFrame { get; }
        byte[] RawPixelData { get; }

        /// <summary>
        /// Up sample color frame with nearest neighbor - replicates pixels
        /// </summary>
        /// <param name="factor">The up sample factor (2=x*2,y*2, 4=x*4,y*4, 8=x*8,y*8, 16=x*16,y*16).</param>
        void UpsampleNearestNeighbor(int[] destBuffer, int factor);
    }
}
