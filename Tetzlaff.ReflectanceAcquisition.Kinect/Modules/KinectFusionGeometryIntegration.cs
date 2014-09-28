using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Kinect.DataModels;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Modules;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Modules
{
    public class KinectFusionGeometryIntegration : IGeometryIntegrationModule<IKinectFusionDepthFrame, IKinectFusionReconstructionVolume>
    {
        public short IntegrationWeight { get; set; }

        public KinectFusionGeometryIntegration()
        {
            this.IntegrationWeight = FusionDepthProcessor.DefaultIntegrationWeight;
        }

        public void IntegrateGeometry(IKinectFusionDepthFrame depthFrame, IKinectFusionReconstructionVolume reconstructionVolume, ICameraPose cameraPose)
        {
            reconstructionVolume.IntegrateFrame(
                depthFrame,
                this.IntegrationWeight,
                cameraPose);
        }
    }
}
