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
    public interface IKinectReconstructionVolume : IReconstructionVolume
    {
        int MaxAlignmentIterations { get; set; }
        Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 CurrentWorldToCameraTransform { get; }

        bool AlignFrameToReconstruction(IKinectDepthFrame depthFrame, IKinectColorFrame colorFrame, IKinectDepthFrame deltaFromReferenceFrame, out float alignmentEnergy, ICameraPose poseEstimate);
        void SmoothDepthFrame(IKinectDepthFrame originalDepthFrame, IKinectDepthFrame smoothDepthFrame, int kernelWidth, float distanceThreshold);
        void CalculatePointCloud(FusionPointCloudImageFrame pointCloudFrame, ICameraPose worldToCameraTransform);
        bool AlignPointClouds(
            FusionPointCloudImageFrame referencePointCloudFrame,
            FusionPointCloudImageFrame observedPointCloudFrame,
            IKinectColorFrame deltaFromReferenceFrame,
            out float alignmentEnergy,
            ref Microsoft.Kinect.Fusion.Matrix4 referenceToObservedTransform);
        void SetAlignFrameToReconstructionReferenceFrame(IKinectDepthFrame kinectDepthFrame);
        void CalculatePointCloudAndDepth(FusionPointCloudImageFrame fusionPointCloudImageFrame, IKinectDepthFrame kinectDepthFrame, IKinectColorFrame kinectColorFrame, ICameraPose worldToCameraTransform);
    }
}
