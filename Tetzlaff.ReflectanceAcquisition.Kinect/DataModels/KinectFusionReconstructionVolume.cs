using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public class KinectFusionReconstructionVolume : IKinectFusionReconstructionVolume
    {
        private ColorReconstruction _reconstruction;
        
        public int MaxAlignmentIterations { get; set; }

        public Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 CurrentWorldToCameraTransform
        {
            get
            {
                return _reconstruction.GetCurrentWorldToCameraTransform().ToPipelineMatrix();
            }
        }

        public Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 CurrentWorldToVolumeTransform
        {
            get
            {
                return _reconstruction.GetCurrentWorldToVolumeTransform().ToPipelineMatrix();
            }
        }

        public KinectFusionReconstructionVolume(ColorReconstruction reconstruction)
        {
            Contract.Ensures(_reconstruction != null);
            _reconstruction = reconstruction;

            MaxAlignmentIterations = FusionDepthProcessor.DefaultAlignIterationCount;
        }

        public bool AlignFrameToReconstruction(IKinectFusionDepthFrame depthFrame, IColorFrame colorFrame, IKinectFusionDepthFrame deltaFromReferenceFrame, out float alignmentEnergy, ICameraPose poseEstimate)
        {
            Microsoft.Kinect.Fusion.Matrix4 kinectMatrix = poseEstimate.Matrix.ToKinectMatrix();
            bool success = _reconstruction.AlignDepthFloatToReconstruction(
                depthFrame.FusionImageFrame,
                MaxAlignmentIterations,
                deltaFromReferenceFrame.FusionImageFrame,
                out alignmentEnergy,
                kinectMatrix);
            poseEstimate.Matrix = kinectMatrix.ToPipelineMatrix();
            return success;
        }

        public void SmoothDepthFrame(IKinectFusionDepthFrame originalDepthFrame, IKinectFusionDepthFrame smoothDepthFrame, int kernelWidth, float distanceThreshold)
        {
            _reconstruction.SmoothDepthFloatFrame(originalDepthFrame.FusionImageFrame, smoothDepthFrame.FusionImageFrame, kernelWidth, distanceThreshold);
        }

        public void CalculatePointCloud(FusionPointCloudImageFrame pointCloudFrame, ICameraPose worldToCameraTransform)
        {
            _reconstruction.CalculatePointCloud(pointCloudFrame, worldToCameraTransform.Matrix.ToKinectMatrix());
        }

        public bool AlignPointClouds(
            FusionPointCloudImageFrame referencePointCloudFrame,
            FusionPointCloudImageFrame observedPointCloudFrame,
            IKinectFusionColorFrame deltaFromReferenceFrame,
            out float alignmentEnergy,
            ref Microsoft.Kinect.Fusion.Matrix4 referenceToObservedTransform)
        {
            return _reconstruction.AlignPointClouds(
                referencePointCloudFrame, 
                observedPointCloudFrame, 
                MaxAlignmentIterations, 
                deltaFromReferenceFrame.FusionImageFrame, 
                out alignmentEnergy,
                ref referenceToObservedTransform);
        }

        public void SetAlignFrameToReconstructionReferenceFrame(IKinectFusionDepthFrame kinectDepthFrame)
        {
            _reconstruction.SetAlignDepthFloatToReconstructionReferenceFrame(kinectDepthFrame.FusionImageFrame);
        }

        public void CalculatePointCloudAndDepth(FusionPointCloudImageFrame fusionPointCloudImageFrame, IKinectFusionDepthFrame kinectDepthFrame, IKinectFusionColorFrame kinectColorFrame, ICameraPose pose)
        {
            _reconstruction.CalculatePointCloudAndDepth(fusionPointCloudImageFrame, kinectDepthFrame.FusionImageFrame, kinectColorFrame.FusionImageFrame, pose.Matrix.ToKinectMatrix());
        }

        public void Dispose()
        {
            _reconstruction.Dispose();
        }

        public void IntegrateFrame(IKinectFusionDepthFrame kinectDepthFrame, short p, ICameraPose cameraPose)
        {
            _reconstruction.IntegrateFrame(kinectDepthFrame.FusionImageFrame, p, cameraPose.Matrix.ToKinectMatrix());
        }

        public void ResetReconstruction(ICameraPose cameraPose)
        {
            _reconstruction.ResetReconstruction(cameraPose.Matrix.ToKinectMatrix());
        }

        public void ResetReconstruction(ICameraPose cameraPose, Pipeline.Math.Matrix4 worldToVolumeTransform)
        {
            _reconstruction.ResetReconstruction(cameraPose.Matrix.ToKinectMatrix(), worldToVolumeTransform.ToKinectMatrix());
        }

        public ColorMesh CalculateMesh(int p)
        {
            return _reconstruction.CalculateMesh(p);
        }

        public void DepthToDepthFloatFrame(ushort[] p1, IKinectFusionDepthFrame kinectDepthFrame, float p2, float p3, bool p4)
        {
            _reconstruction.DepthToDepthFloatFrame(p1, kinectDepthFrame.FusionImageFrame, p2, p3, p4);
        }
    }
}
