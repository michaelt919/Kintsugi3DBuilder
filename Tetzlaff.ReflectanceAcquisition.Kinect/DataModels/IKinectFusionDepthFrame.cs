using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public interface IKinectFusionDepthFrame : IDepthFrame
    {
        FusionFloatImageFrame FusionImageFrame { get; }

        void DownsampleNearestNeighbor(IKinectFusionDepthFrame dest, int factor, bool mirror);
    }
}
