using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.Modules
{
    public interface IPoseAlignmentModule<DepthFrameType, ColorFrameType, ReconstructionVolumeType>
        where DepthFrameType : IFrame
        where ColorFrameType : IFrame
        where ReconstructionVolumeType : IReconstructionVolume
    {
        ICameraPose AlignFrame(DepthFrameType depthFrame, ColorFrameType colorFrame, ReconstructionVolumeType volume, ICameraPose cameraPoseEstimate, bool mirrorDepth);
    }
}
