using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Math;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public interface IKinectFusionReconstructionVolume : IReconstructionVolume
    {
        int MaxAlignmentIterations { get; set; }
        Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 CurrentWorldToCameraTransform { get; }

        bool AlignFrameToReconstruction(IKinectFusionDepthFrame depthFrame, IColorFrame colorFrame, IKinectFusionDepthFrame deltaFromReferenceFrame, out float alignmentEnergy, ICameraPose poseEstimate);
        void SmoothDepthFrame(IKinectFusionDepthFrame originalDepthFrame, IKinectFusionDepthFrame smoothDepthFrame, int kernelWidth, float distanceThreshold);
        void CalculatePointCloud(FusionPointCloudImageFrame pointCloudFrame, ICameraPose worldToCameraTransform);
        bool AlignPointClouds(
            FusionPointCloudImageFrame referencePointCloudFrame,
            FusionPointCloudImageFrame observedPointCloudFrame,
            IKinectFusionColorFrame deltaFromReferenceFrame,
            out float alignmentEnergy,
            ref Microsoft.Kinect.Fusion.Matrix4 referenceToObservedTransform);
        void SetAlignFrameToReconstructionReferenceFrame(IKinectFusionDepthFrame kinectDepthFrame);
        void CalculatePointCloudAndDepth(FusionPointCloudImageFrame fusionPointCloudImageFrame, IKinectFusionDepthFrame kinectDepthFrame, IKinectFusionColorFrame kinectColorFrame, ICameraPose worldToCameraTransform);
        void ResetReconstruction(ICameraPose cameraPose);
        void ResetReconstruction(ICameraPose cameraPose, Pipeline.Math.Matrix4 worldToVolumeTransform);
        ColorMesh CalculateMesh(int p);
        void DepthToDepthFloatFrame(ushort[] p1, IKinectFusionDepthFrame kinectDepthFrame, float p2, float p3, bool p4);
        void IntegrateFrame(IKinectFusionDepthFrame depthFrame, short p, ICameraPose cameraPose);
    }
}
