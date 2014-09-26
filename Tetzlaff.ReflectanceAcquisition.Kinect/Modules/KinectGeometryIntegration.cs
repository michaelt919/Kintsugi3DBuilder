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
    public class KinectGeometryIntegration : IGeometryIntegrationModule<IKinectDepthFrame, IKinectReconstructionVolume>
    {
        public short IntegrationWeight { get; set; }

        public KinectGeometryIntegration()
        {
            this.IntegrationWeight = FusionDepthProcessor.DefaultIntegrationWeight;
        }

        public void IntegrateGeometry(IKinectDepthFrame depthFrame, IKinectReconstructionVolume reconstructionVolume, ICameraPose cameraPose)
        {
            reconstructionVolume.IntegrateFrame(
                depthFrame,
                this.IntegrationWeight,
                cameraPose);
        }
    }
}
