using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Kinect.DataModels;
using Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Exceptions;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Math;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Modules;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Modules
{
    public class KinectFusionPoseAlignment : IPoseAlignmentModule<IKinectFusionDepthFrame, IRawColorFrame, IKinectFusionReconstructionVolume>
    {
        private const int MAX_DEPTH_FRAME_WIDTH = 512;
        private const int MAX_DEPTH_FRAME_HEIGHT = 424;

        /// <summary>
        /// Frame interval we calculate the deltaFromReferenceFrame 
        /// </summary>
        private const int DeltaFrameCalculationInterval = 2;

        /// <summary>
        /// Frame interval we update the camera pose finder database.
        /// </summary>
        private const int CameraPoseFinderProcessFrameCalculationInterval = 5;

        /// <summary>
        /// How many frames after starting tracking will will wait before starting to store
        /// image frames to the pose finder database. Here we set 45 successful frames (1.5s).
        /// </summary>
        private const int MinSuccessfulTrackingFramesForCameraPoseFinder = 45;

        /// <summary>
        /// How many frames after starting tracking will will wait before starting to store
        /// image frames to the pose finder database. Here we set 200 successful frames (~7s).
        /// </summary>
        private const int MinSuccessfulTrackingFramesForCameraPoseFinderAfterFailure = 200;

        /// <summary>
        /// Here we set a high limit on the maximum residual alignment energy where we consider the tracking
        /// to have succeeded. Typically this value would be around 0.2f to 0.3f.
        /// (Lower residual alignment energy after tracking is considered better.)
        /// </summary>
        private const float MaxAlignToReconstructionEnergyForSuccess = 0.27f;

        /// <summary>
        /// Here we set a low limit on the residual alignment energy, below which we reject a tracking
        /// success report and believe it to have failed. Typically this value would be around 0.005f, as
        /// values below this (i.e. close to 0 which is perfect alignment) most likely come from frames
        /// where the majority of the image is obscured (i.e. 0 depth) or mismatched (i.e. similar depths
        /// but different scene or camera pose).
        /// </summary>
        private const float MinAlignToReconstructionEnergyForSuccess = 0.005f;

        /// <summary>
        /// Here we set a high limit on the maximum residual alignment energy where we consider the tracking
        /// with AlignPointClouds to have succeeded. Typically this value would be around 0.005f to 0.006f.
        /// (Lower residual alignment energy after relocalization is considered better.)
        /// </summary>
        private const float MaxAlignPointCloudsEnergyForSuccess = 0.006f;

        /// <summary>
        /// Here we set a low limit on the residual alignment energy, below which we reject a tracking
        /// success report from AlignPointClouds and believe it to have failed. This can typically be around 0.
        /// </summary>
        private const float MinAlignPointCloudsEnergyForSuccess = 0.0f;

        /// <summary>
        /// The maximum number of matched poseCount we consider when finding the camera pose. 
        /// Although the matches are ranked, so we look at the highest probability match first, a higher 
        /// value has a greater chance of finding a good match overall, but has the trade-off of being 
        /// slower. Typically we test up to around the 5 best matches, after which is may be better just
        /// to try again with the next input depth frame if no good match is found.
        /// </summary>
        private const int MaxCameraPoseFinderPoseTests = 5;

        /// <summary>
        /// CameraPoseFinderDistanceThresholdReject is a threshold used following the minimum distance 
        /// calculation between the input frame and the camera pose finder database. This calculated value
        /// between 0 and 1.0f must be less than or equal to the threshold in order to run the pose finder,
        /// as the input must at least be similar to the pose finder database for a correct pose to be
        /// matched.
        /// </summary>
        private const float CameraPoseFinderDistanceThresholdReject = 1.0f; // a value of 1.0 means no rejection

        /// <summary>
        /// CameraPoseFinderDistanceThresholdAccept is a threshold passed to the ProcessFrame 
        /// function in the camera pose finder interface. The minimum distance between the input frame and
        /// the pose finder database must be greater than or equal to this value for a new pose to be 
        /// stored in the database, which regulates how close together poseCount are stored in the database.
        /// </summary>
        private const float CameraPoseFinderDistanceThresholdAccept = 0.1f;

        /// <summary>
        /// Maximum residual alignment energy where tracking is still considered successful
        /// </summary>
        private const int SmoothingKernelWidth = 1; // 0=just copy, 1=3x3, 2=5x5, 3=7x7, here we create a 3x3 kernel

        /// <summary>
        /// Maximum residual alignment energy where tracking is still considered successful
        /// </summary>
        private const float SmoothingDistanceThreshold = 0.04f; // 4cm, could use up to around 0.1f;

        /// <summary>
        /// Maximum translation threshold between successive poses when using AlignPointClouds
        /// </summary>
        private const float MaxTranslationDelta = 0.3f; // 0.15 - 0.3m per frame typical

        /// <summary>
        /// Maximum rotation threshold between successive poses when using AlignPointClouds
        /// </summary>
        private const float MaxRotationDelta = 20.0f; // 10-20 degrees per frame typical

        /// <summary>
        /// The factor to downsample the depth image by for AlignPointClouds
        /// </summary>
        private const int DownsampleFactor = 2;

        /// <summary>
        /// Image Width of depth frame
        /// </summary>
        private int depthWidth = 0;

        /// <summary>
        /// Image height of depth frame
        /// </summary>
        private int depthHeight = 0;

        /// <summary>
        /// The width of the downsampled images for AlignPointClouds
        /// </summary>
        private int downsampledWidth;

        /// <summary>
        /// The height of the downsampled images for AlignPointClouds
        /// </summary>
        private int downsampledHeight;

        /// <summary>
        /// The counter for image process failures
        /// </summary>
        public int TrackingErrorCount { get; private set; }

        /// <summary>
        /// Set true when tracking fails
        /// </summary>
        public bool TrackingFailed { get; private set; }

        /// <summary>
        /// Set true when tracking fails and stays false until integration resumes.
        /// </summary>
        public bool TrackingHasFailedPreviously { get; private set; }

        /// <summary>
        /// Set true when the camera pose finder has stored frames in its database and is able to match camera frames.
        /// </summary>
        public bool CameraPoseFinderAvailable { get; private set; }

        /// <summary>
        /// The counter for image process successes
        /// </summary>
        public int SuccessfulFrameCount { get; private set; }

        /// <summary>
        /// The counter for frames that have been processed
        /// </summary>
        private int processedFrameCount = 0;

        /// <summary>
        /// Intermediate storage for the color data received from the camera in 32bit color, re-sampled to depth image size
        /// </summary>
        private int[] resampledColorImagePixels;

        /// <summary>
        /// Intermediate storage for the smoothed depth float image frame
        /// </summary>
        private KinectFusionDepthFrame smoothDepthFrame;

        /// <summary>
        /// Kinect color re-sampled to be the same size as the depth frame
        /// </summary>
        private KinectFusionColorFrame resampledColorFrame;

        /// <summary>
        /// Per-pixel alignment values
        /// </summary>
        private KinectFusionDepthFrame deltaFromReferenceFrame;

        /// <summary>
        /// Calculated point cloud frame from image integration
        /// </summary>
        private FusionPointCloudImageFrame raycastPointCloudFrame;

        /// <summary>
        /// Calculated point cloud frame from input depth
        /// </summary>
        private FusionPointCloudImageFrame depthPointCloudFrame;

        /// <summary>
        /// Intermediate storage for the depth float data converted from depth image frame
        /// </summary>
        private KinectFusionDepthFrame downsampledDepthFloatFrame;

        /// <summary>
        /// Intermediate storage for the depth float data following smoothing
        /// </summary>
        private KinectFusionDepthFrame downsampledSmoothDepthFloatFrame;

        /// <summary>
        /// Calculated point cloud frame from image integration
        /// </summary>
        private FusionPointCloudImageFrame downsampledRaycastPointCloudFrame;

        /// <summary>
        /// Calculated point cloud frame from input depth
        /// </summary>
        private FusionPointCloudImageFrame downsampledDepthPointCloudFrame;

        /// <summary>
        /// Kinect color delta from reference frame data from AlignPointClouds
        /// </summary>
        private KinectFusionColorFrame downsampledDeltaFromReferenceFrameColorFrame;

        /// <summary>
        /// Pixel buffer of delta from reference frame in 32bit color
        /// </summary>
        private int[] deltaFromReferenceFramePixelsArgb;

        /// <summary>
        /// Alignment energy from AlignDepthFloatToReconstruction for current frame 
        /// </summary>
        private float _alignmentEnergy;
        public float AlignmentEnergy 
        {
            get
            {
                return _alignmentEnergy;
            }
            set
            {
                _alignmentEnergy = value;
            }
        }

        /// <summary>
        /// A camera pose finder to store image frames and poseCount to a database then match the input frames
        /// when tracking is lost to help us recover tracking.
        /// </summary>
        private CameraPoseFinder cameraPoseFinder;

        /// <summary>
        /// Parameter to enable automatic finding of camera pose when lost. This searches back through
        /// the camera pose history where key-frames and camera poseCount have been stored in the camera
        /// pose finder database to propose the most likely pose matches for the current camera input.
        /// </summary>
        public bool AutoFindCameraPoseWhenLost { get; set; }

        public string CameraPoseFinderStatus { get; private set; }

        public KinectFusionPoseAlignment(int depthWidth, int depthHeight)
        {
            this.depthWidth = depthWidth;
            this.depthHeight = depthHeight;

            this.AutoFindCameraPoseWhenLost = true;
            this.TrackingErrorCount = 0;

            // Allocate delta from reference frame
            this.deltaFromReferenceFrame = new KinectFusionDepthFrame(this.depthWidth, this.depthHeight);

            // Allocate point cloud frame
            this.raycastPointCloudFrame = new FusionPointCloudImageFrame(this.depthWidth, this.depthHeight);

            // Allocate re-sampled color at depth image size
            this.resampledColorFrame = new KinectFusionColorFrame(this.depthWidth, this.depthHeight);

            // Allocate point cloud frame created from input depth
            this.depthPointCloudFrame = new FusionPointCloudImageFrame(this.depthWidth, this.depthHeight);

            // Allocate smoothed depth float frame
            this.smoothDepthFrame = new KinectFusionDepthFrame(this.depthWidth, this.depthHeight);

            this.downsampledWidth = depthWidth / DownsampleFactor;
            this.downsampledHeight = depthHeight / DownsampleFactor;

            // Allocate downsampled image frames
            this.downsampledDepthFloatFrame = new KinectFusionDepthFrame(this.downsampledWidth, this.downsampledHeight);

            this.downsampledSmoothDepthFloatFrame = new KinectFusionDepthFrame(this.downsampledWidth, this.downsampledHeight);

            this.downsampledRaycastPointCloudFrame = new FusionPointCloudImageFrame(this.downsampledWidth, this.downsampledHeight);

            this.downsampledDepthPointCloudFrame = new FusionPointCloudImageFrame(this.downsampledWidth, this.downsampledHeight);

            this.downsampledDeltaFromReferenceFrameColorFrame = new KinectFusionColorFrame(this.downsampledWidth, this.downsampledHeight);

            int depthImageSize = depthWidth * depthHeight;

            // Create local color pixels buffer re-sampled to depth size
            this.resampledColorImagePixels = new int[depthImageSize];

            // Create colored pixel array of correct format
            this.deltaFromReferenceFramePixelsArgb = new int[depthImageSize];

            // Create a camera pose finder with default parameters
            CameraPoseFinderParameters cameraPoseFinderParams = CameraPoseFinderParameters.Defaults;
            this.cameraPoseFinder = CameraPoseFinder.FusionCreateCameraPoseFinder(cameraPoseFinderParams);

        }

        public void Dispose()
        {
            if (null != this.deltaFromReferenceFrame)
            {
                this.deltaFromReferenceFrame.Dispose();
            }

            if (null != this.raycastPointCloudFrame)
            {
                this.raycastPointCloudFrame.Dispose();
            }

            if (null != this.resampledColorFrame)
            {
                this.resampledColorFrame.Dispose();
            }

            if (null != this.depthPointCloudFrame)
            {
                this.depthPointCloudFrame.Dispose();
            }

            if (null != this.smoothDepthFrame)
            {
                this.smoothDepthFrame.Dispose();
            }

            if (null != this.downsampledDepthFloatFrame)
            {
                this.downsampledDepthFloatFrame.Dispose();
            }

            if (null != this.downsampledSmoothDepthFloatFrame)
            {
                this.downsampledSmoothDepthFloatFrame.Dispose();
            }

            if (null != this.downsampledRaycastPointCloudFrame)
            {
                this.downsampledRaycastPointCloudFrame.Dispose();
            }

            if (null != this.downsampledDepthPointCloudFrame)
            {
                this.downsampledDepthPointCloudFrame.Dispose();
            }

            if (null != this.downsampledDeltaFromReferenceFrameColorFrame)
            {
                this.downsampledDeltaFromReferenceFrameColorFrame.Dispose();
            }

            if (null != this.cameraPoseFinder)
            {
                this.cameraPoseFinder.Dispose();
            }
        }

        public bool PreviousAlignmentSucceeded
        {
            get
            {
                // Alignment is considered a failure if:
                // 1) tracking failed OR
                // 2) camera pose finder is on and we are still under the MinSuccessfulTrackingFramesForCameraPoseFinderAfterFailure number of successful frames count.
                return !this.TrackingFailed &&
                    (!this.CameraPoseFinderAvailable ||
                    !(this.TrackingHasFailedPreviously && 
                        this.SuccessfulFrameCount < MinSuccessfulTrackingFramesForCameraPoseFinderAfterFailure));
            }
        }

        /// <summary>
        /// Set variables if camera tracking succeeded
        /// </summary>
        public void SetTrackingFailed()
        {
            // Clear successful frame count and increment the track error count
            this.TrackingFailed = true;
            this.TrackingHasFailedPreviously = true;
            this.TrackingErrorCount++;
            this.SuccessfulFrameCount = 0;
        }

        /// <summary>
        /// Set variables if camera tracking succeeded
        /// </summary>
        public void SetTrackingSucceeded()
        {
            // Clear track error count and increment the successful frame count
            this.TrackingFailed = false;
            this.TrackingErrorCount = 0;
            this.SuccessfulFrameCount++;
        }

        /// <summary>
        /// Reset tracking variables
        /// </summary>
        public void ResetTracking()
        {
            this.TrackingFailed = false;
            this.TrackingHasFailedPreviously = false;
            this.TrackingErrorCount = 0;
            this.SuccessfulFrameCount = 0;
            this.CameraPoseFinderStatus = "";

            if (null != this.cameraPoseFinder)
            {
                this.cameraPoseFinder.ResetCameraPoseFinder();
            }
        }

        public ICameraPose AlignFrame(IKinectFusionDepthFrame depthFrame, IRawColorFrame colorFrame, IKinectFusionReconstructionVolume volume, ICameraPose cameraPoseEstimate, bool mirrorDepth)
        {
            // Check if camera pose finder is available
            this.CameraPoseFinderAvailable = this.IsCameraPoseFinderAvailable();

            bool calculateDeltaFrame = this.processedFrameCount % DeltaFrameCalculationInterval == 0;
            bool trackingSucceeded = false;

            // Get updated camera transform from image alignment
            ICameraPose calculatedCameraPos = cameraPoseEstimate;

            this.CameraPoseFinderStatus = "";

            // Here we can either call TrackCameraAlignDepthFloatToReconstruction or TrackCameraAlignPointClouds
            // The TrackCameraAlignPointClouds function typically has higher performance with the camera pose finder 
            // due to its wider basin of convergence, enabling it to more robustly regain tracking from nearby poses
            // suggested by the camera pose finder after tracking is lost.
            if (this.AutoFindCameraPoseWhenLost)
            {
                // Track using AlignPointClouds
                trackingSucceeded = this.TrackCameraAlignPointClouds(calculateDeltaFrame, depthFrame, colorFrame, volume, ref calculatedCameraPos, mirrorDepth);
            }
            else
            {
                // Track using AlignDepthFloatToReconstruction
                trackingSucceeded = this.TrackCameraAlignDepthFloatToReconstruction(calculateDeltaFrame, depthFrame, colorFrame, volume, ref calculatedCameraPos);
            }

            if (!trackingSucceeded && 0 != this.SuccessfulFrameCount)
            {
                this.SetTrackingFailed();

                if (!this.CameraPoseFinderAvailable)
                {
                    throw new CameraTrackingFailedException();
                }
                else
                {
                    try
                    {
                        // Here we try to find the correct camera pose, to re-localize camera tracking.
                        // We can call either the version using AlignDepthFloatToReconstruction or the
                        // version using AlignPointClouds, which typically has a higher success rate.
                        // calculatedCameraPos = this.FindCameraPoseAlignDepthFloatToReconstruction();
                        calculatedCameraPos = this.FindCameraPoseAlignPointClouds(depthFrame, colorFrame, volume);
                    }
                    catch(Exception e)
                    {
                        throw new CameraTrackingFailedException("See inner exception for details.", e);
                    }
                }
            }
            else
            {
                this.SetTrackingSucceeded();
            }

            if (trackingSucceeded)
            {
                // Increase processed frame counter
                this.processedFrameCount++;
            }

            // Update camera pose finder, adding key frames to the database
            if (this.AutoFindCameraPoseWhenLost && !this.TrackingHasFailedPreviously
                && this.SuccessfulFrameCount > MinSuccessfulTrackingFramesForCameraPoseFinder
                && this.processedFrameCount % CameraPoseFinderProcessFrameCalculationInterval == 0)
            {
                this.UpdateCameraPoseFinder(depthFrame, colorFrame, calculatedCameraPos);
            }

            return calculatedCameraPos;
        }

        /// <summary>
        /// Is the camera pose finder initialized and running.
        /// </summary>
        /// <returns>Returns true if available, otherwise false</returns>
        private bool IsCameraPoseFinderAvailable()
        {
            return this.AutoFindCameraPoseWhenLost
                && null != this.cameraPoseFinder
                && this.cameraPoseFinder.GetStoredPoseCount() > 0;
        }

        /// <summary>
        /// Update the camera pose finder data.
        /// </summary>
        private bool UpdateCameraPoseFinder(IKinectFusionDepthFrame depthFrame, IRawColorFrame colorFrame, ICameraPose cameraPose)
        {
            if (null == depthFrame || null == this.resampledColorFrame || null == this.cameraPoseFinder)
            {
                return false;
            }

            this.ResampleColorFrame(colorFrame, this.resampledColorFrame);

            bool poseHistoryTrimmed = false;
            bool addedPose = false;

            // This function will add the pose to the camera pose finding database when the input frame's minimum
            // distance to the existing database is equal to or above CameraPoseFinderDistanceThresholdAccept 
            // (i.e. indicating that the input has become dis-similar to the existing database and a new frame 
            // should be captured). Note that the color and depth frames must be the same size, however, the 
            // horizontal mirroring setting does not have to be consistent between depth and color. It does have
            // to be consistent between camera pose finder database creation and calling FindCameraPose though,
            // hence we always reset both the reconstruction and database when changing the mirror depth setting.
            this.cameraPoseFinder.ProcessFrame(
                depthFrame.FusionImageFrame,
                this.resampledColorFrame.FusionImageFrame,
                cameraPose.Matrix.ToKinectMatrix(),
                CameraPoseFinderDistanceThresholdAccept,
                out addedPose,
                out poseHistoryTrimmed);

            if (poseHistoryTrimmed)
            {
                throw new CameraPoseFinderHistoryFullException();
            }

            return addedPose;
        }

        /// <summary>
        /// Track camera pose by aligning depth float image with reconstruction volume
        /// </summary>
        /// <param name="calculateDeltaFrame">Flag to calculate the delta frame.</param>
        /// <returns>Returns true if tracking succeeded, false otherwise.</returns>
        private bool TrackCameraAlignDepthFloatToReconstruction(
            bool calculateDeltaFrame, 
            IKinectFusionDepthFrame depthFrame,
            IRawColorFrame colorFrame, 
            IKinectFusionReconstructionVolume volume,
            ref ICameraPose cameraPose)
        {
            bool trackingSucceeded = false;

            // Note that here we only calculate the deltaFromReferenceFrame every 
            // DeltaFrameCalculationInterval frames to reduce computation time
            if (calculateDeltaFrame)
            {
                trackingSucceeded = volume.AlignFrameToReconstruction(
                    depthFrame,
                    colorFrame,
                    deltaFromReferenceFrame,
                    out _alignmentEnergy,
                    cameraPose);
            }
            else
            {
                // Don't bother getting the residual delta from reference frame to cut computation time
                trackingSucceeded = volume.AlignFrameToReconstruction(
                    depthFrame,
                    colorFrame,
                    null,
                    out _alignmentEnergy,
                    cameraPose);
            }

            if (!trackingSucceeded || this.AlignmentEnergy > MaxAlignToReconstructionEnergyForSuccess || (this.AlignmentEnergy <= MinAlignToReconstructionEnergyForSuccess && this.SuccessfulFrameCount > 0))
            {
                trackingSucceeded = false;
            }
            else
            {
                // Tracking succeeded, get the updated camera pose
                cameraPose = new CameraPose(volume.CurrentWorldToCameraTransform);
            }

            return trackingSucceeded;
        }

        /// <summary>
        /// Track camera pose using AlignPointClouds
        /// </summary>
        /// <param name="calculateDeltaFrame">A flag to indicate it is time to calculate the delta frame.</param>
        /// <returns>Returns true if tracking succeeded, false otherwise.</returns>
        private bool TrackCameraAlignPointClouds(
            bool calculateDeltaFrame, 
            IKinectFusionDepthFrame depthFrame,
            IRawColorFrame colorFrame, 
            IKinectFusionReconstructionVolume volume, 
            ref ICameraPose cameraPose, 
            bool mirrorDepth)
        {
            bool trackingSucceeded = false;

            depthFrame.DownsampleNearestNeighbor(this.downsampledDepthFloatFrame, DownsampleFactor, mirrorDepth);

            // Smooth the depth frame
            volume.SmoothDepthFrame(this.downsampledDepthFloatFrame, this.downsampledSmoothDepthFloatFrame, SmoothingKernelWidth, SmoothingDistanceThreshold);

            // Calculate point cloud from the smoothed frame
            FusionDepthProcessor.DepthFloatFrameToPointCloud(this.downsampledSmoothDepthFloatFrame.FusionImageFrame, this.downsampledDepthPointCloudFrame);

            // Get the saved pose view by raycasting the volume from the current camera pose
            volume.CalculatePointCloud(this.downsampledRaycastPointCloudFrame, cameraPose);

            ICameraPose initialPose = cameraPose;

            // Note that here we only calculate the deltaFromReferenceFrame every 
            // DeltaFrameCalculationInterval frames to reduce computation time
            if (calculateDeltaFrame)
            {
                Microsoft.Kinect.Fusion.Matrix4 kinectMatrix = cameraPose.Matrix.ToKinectMatrix();
                trackingSucceeded = FusionDepthProcessor.AlignPointClouds(
                    this.downsampledRaycastPointCloudFrame,
                    this.downsampledDepthPointCloudFrame,
                    FusionDepthProcessor.DefaultAlignIterationCount,
                    this.downsampledDeltaFromReferenceFrameColorFrame.FusionImageFrame,
                    ref kinectMatrix);
                cameraPose.Matrix = kinectMatrix.ToPipelineMatrix();

                downsampledDeltaFromReferenceFrameColorFrame.UpsampleNearestNeighbor(deltaFromReferenceFramePixelsArgb, DownsampleFactor);

                // Set calculateDeltaFrame to false as we are rendering it here
                calculateDeltaFrame = false;
            }
            else
            {
                Microsoft.Kinect.Fusion.Matrix4 kinectCameraPose = cameraPose.Matrix.ToKinectMatrix();

                // Don't bother getting the residual delta from reference frame to cut computation time
                trackingSucceeded = FusionDepthProcessor.AlignPointClouds(
                    this.downsampledRaycastPointCloudFrame,
                    this.downsampledDepthPointCloudFrame,
                    FusionDepthProcessor.DefaultAlignIterationCount,
                    null,
                    ref kinectCameraPose);

                cameraPose.Matrix = kinectCameraPose.ToPipelineMatrix();
            }

            if (trackingSucceeded)
            {

                bool failed = GeometryHelper.CameraTransformFailed(
                    initialPose.Matrix,
                    cameraPose.Matrix,
                    MaxTranslationDelta,
                    MaxRotationDelta);

                if (failed)
                {
                    trackingSucceeded = false;
                }
            }

            return trackingSucceeded;
        }

        /// <summary>
        /// Perform camera pose finding when tracking is lost using AlignPointClouds.
        /// This is typically more successful than FindCameraPoseAlignDepthFloatToReconstruction.
        /// </summary>
        /// <returns>Returns true if a valid camera pose was found, otherwise false.</returns>
        private ICameraPose FindCameraPoseAlignPointClouds(IKinectFusionDepthFrame depthFrame, IRawColorFrame colorFrame, IKinectFusionReconstructionVolume volume)
        {
            if (!this.CameraPoseFinderAvailable)
            {
                throw new CameraPoseFinderUnavailableException();
            }

            this.ResampleColorFrame(colorFrame, this.resampledColorFrame);

            MatchCandidates matchCandidates = this.cameraPoseFinder.FindCameraPose(depthFrame.FusionImageFrame, this.resampledColorFrame.FusionImageFrame);

            if (null == matchCandidates)
            {
                throw new NoPoseFinderMatchesException();
            }

            int poseCount = matchCandidates.GetPoseCount();
            float minDistance = matchCandidates.CalculateMinimumDistance();

            if (0 == poseCount || minDistance >= CameraPoseFinderDistanceThresholdReject)
            {
                throw new NotEnoughPoseFinderMatchesException();
            }

            // Smooth the depth frame
            volume.SmoothDepthFrame(depthFrame, this.smoothDepthFrame, SmoothingKernelWidth, SmoothingDistanceThreshold);

            // Calculate point cloud from the smoothed frame
            FusionDepthProcessor.DepthFloatFrameToPointCloud(this.smoothDepthFrame.FusionImageFrame, this.depthPointCloudFrame);

            double smallestEnergy = double.MaxValue;
            int smallestEnergyNeighborIndex = -1;

            int bestNeighborIndex = -1;
            ICameraPose bestCameraPose = new CameraPose(); // Identity

            double bestNeighborAlignmentEnergy = MaxAlignPointCloudsEnergyForSuccess;

            // Run alignment with best matched poseCount (i.e. k nearest neighbors (kNN))
            int maxTests = Math.Min(MaxCameraPoseFinderPoseTests, poseCount);

            var neighbors = matchCandidates.GetMatchPoses();

            for (int n = 0; n < maxTests; n++)
            {
                // Run the camera tracking algorithm with the volume
                // this uses the raycast frame and pose to find a valid camera pose by matching the raycast against the input point cloud
                Microsoft.Kinect.Fusion.Matrix4 poseProposal = neighbors[n];

                // Get the saved pose view by raycasting the volume
                volume.CalculatePointCloud(this.raycastPointCloudFrame, new CameraPose(poseProposal.ToPipelineMatrix()));

                bool success = volume.AlignPointClouds(
                    this.raycastPointCloudFrame,
                    this.depthPointCloudFrame,
                    this.resampledColorFrame,
                    out _alignmentEnergy,
                    ref poseProposal);

                bool relocSuccess = success && this.AlignmentEnergy < bestNeighborAlignmentEnergy && this.AlignmentEnergy > MinAlignPointCloudsEnergyForSuccess;

                if (relocSuccess)
                {
                    bestNeighborAlignmentEnergy = this.AlignmentEnergy;
                    bestNeighborIndex = n;

                    // This is after tracking succeeds, so should be a more accurate pose to store...
                    bestCameraPose = new CameraPose(poseProposal.ToPipelineMatrix());

                    // Update the delta image
                    this.resampledColorFrame.FusionImageFrame.CopyPixelDataTo(this.deltaFromReferenceFramePixelsArgb);
                }

                // Find smallest energy neighbor independent of tracking success
                if (this.AlignmentEnergy < smallestEnergy)
                {
                    smallestEnergy = this.AlignmentEnergy;
                    smallestEnergyNeighborIndex = n;
                }
            }

            matchCandidates.Dispose();

            // Use the neighbor with the smallest residual alignment energy
            // At the cost of additional processing we could also use kNN+Mean camera pose finding here
            // by calculating the mean pose of the best n matched poses and also testing this to see if the 
            // residual alignment energy is less than with kNN.
            if (bestNeighborIndex > -1)
            {
                this.SetReferenceFrame(volume, bestCameraPose);

                // Tracking succeeded!
                this.SetTrackingSucceeded();

                this.CameraPoseFinderStatus = "Camera Pose Finder SUCCESS! Residual energy= " + string.Format(CultureInfo.InvariantCulture, "{0:0.00000}", bestNeighborAlignmentEnergy) + ", " + poseCount + " frames stored, minimum distance=" + minDistance + ", best match index=" + bestNeighborIndex;

                return bestCameraPose;
            }
            else
            {
                bestCameraPose = new CameraPose(neighbors[smallestEnergyNeighborIndex].ToPipelineMatrix());
                this.SetReferenceFrame(volume, bestCameraPose);

                // Camera pose finding failed - return the tracking failed error code
                this.SetTrackingFailed();

                this.CameraPoseFinderStatus = "Camera Pose Finder FAILED! Residual energy=" + string.Format(CultureInfo.InvariantCulture, "{0:0.00000}", smallestEnergy) + ", " + poseCount + " frames stored, minimum distance=" + minDistance + ", best match index=" + smallestEnergyNeighborIndex;
                throw new CameraPoseFinderFailedException(this.CameraPoseFinderStatus);
            }
        }

        /// <summary>
        /// Perform camera pose finding when tracking is lost using AlignDepthFloatToReconstruction.
        /// </summary>
        /// <returns>Returns true if a valid camera pose was found, otherwise false.</returns>
        private ICameraPose FindCameraPoseAlignDepthFloatToReconstruction(IKinectFusionDepthFrame depthFrame, IRawColorFrame colorFrame, IKinectFusionReconstructionVolume volume)
        {
            if (!this.CameraPoseFinderAvailable)
            {
                throw new CameraPoseFinderUnavailableException();
            }

            this.ResampleColorFrame(colorFrame, this.resampledColorFrame);

            MatchCandidates matchCandidates = this.cameraPoseFinder.FindCameraPose(
                depthFrame.FusionImageFrame,
                this.resampledColorFrame.FusionImageFrame);

            if (null == matchCandidates)
            {
                throw new NoPoseFinderMatchesException();
            }

            int poseCount = matchCandidates.GetPoseCount();
            float minDistance = matchCandidates.CalculateMinimumDistance();

            if (0 == poseCount || minDistance >= CameraPoseFinderDistanceThresholdReject)
            {
                throw new NotEnoughPoseFinderMatchesException();
            }

            double smallestEnergy = double.MaxValue;
            int smallestEnergyNeighborIndex = -1;

            int bestNeighborIndex = -1;
            ICameraPose bestCameraPose = new CameraPose();

            double bestNeighborAlignmentEnergy = MaxAlignToReconstructionEnergyForSuccess;

            // Run alignment with best matched poseCount (i.e. k nearest neighbors (kNN))
            int maxTests = Math.Min(MaxCameraPoseFinderPoseTests, poseCount);

            var neighbors = matchCandidates.GetMatchPoses();

            for (int n = 0; n < maxTests; n++)
            {
                // Run the camera tracking algorithm with the volume
                // this uses the raycast frame and pose to find a valid camera pose by matching the depth against the volume
                this.SetReferenceFrame(volume, new CameraPose(neighbors[n].ToPipelineMatrix()));

                bool success = volume.AlignFrameToReconstruction(
                    depthFrame,
                    colorFrame,
                    deltaFromReferenceFrame,
                    out _alignmentEnergy,
                    new CameraPose(neighbors[n].ToPipelineMatrix()));

                // Exclude very tiny alignment energy case which is unlikely to happen in reality - this is more likely a tracking error
                bool relocSuccess = success && this.AlignmentEnergy < bestNeighborAlignmentEnergy && this.AlignmentEnergy > MinAlignToReconstructionEnergyForSuccess;

                if (relocSuccess)
                {
                    bestNeighborAlignmentEnergy = this.AlignmentEnergy;
                    bestNeighborIndex = n;

                    // This is after tracking succeeds, so should be a more accurate pose to store...
                    bestCameraPose = new CameraPose(volume.CurrentWorldToCameraTransform);
                }

                // Find smallest energy neighbor independent of tracking success
                if (this.AlignmentEnergy < smallestEnergy)
                {
                    smallestEnergy = this.AlignmentEnergy;
                    smallestEnergyNeighborIndex = n;
                }
            }

            matchCandidates.Dispose();

            // Use the neighbor with the smallest residual alignment energy
            // At the cost of additional processing we could also use kNN+Mean camera pose finding here
            // by calculating the mean pose of the best n matched poses and also testing this to see if the 
            // residual alignment energy is less than with kNN.
            if (bestNeighborIndex > -1)
            {
                this.SetReferenceFrame(volume, bestCameraPose);

                // Tracking succeeded!
                this.SetTrackingSucceeded();

                this.CameraPoseFinderStatus = "Camera Pose Finder SUCCESS! Residual energy= " + bestNeighborAlignmentEnergy + ", " + poseCount + " frames stored, minimum distance=" + minDistance + ", best match index=" + bestNeighborIndex;
                return bestCameraPose;
            }
            else
            {
                bestCameraPose = new CameraPose(neighbors[smallestEnergyNeighborIndex].ToPipelineMatrix());
                this.SetReferenceFrame(volume, bestCameraPose);

                // Camera pose finding failed - return the tracking failed error code
                this.SetTrackingFailed();

                this.CameraPoseFinderStatus = "Camera Pose Finder FAILED! Residual energy=" + smallestEnergy + ", " + poseCount + " frames stored, minimum distance=" + minDistance + ", best match index=" + smallestEnergyNeighborIndex;
                throw new CameraPoseFinderFailedException(this.CameraPoseFinderStatus);
            }
        }

        /// <summary>
        /// This is used to set the reference frame.
        /// </summary>
        /// <param name="pose">The pose to use.</param>
        private void SetReferenceFrame(IKinectFusionReconstructionVolume volume, ICameraPose pose)
        {
            // Get the saved pose view by raycasting the volume
            volume.CalculatePointCloudAndDepth(this.raycastPointCloudFrame, this.smoothDepthFrame, null, pose);

            // Set this as the reference frame for the next call to AlignDepthFloatToReconstruction
            volume.SetAlignFrameToReconstructionReferenceFrame(this.smoothDepthFrame);
        }

        /// <summary>
        /// Process input color image to make it equal in size to the depth image
        /// </summary>
        private unsafe IKinectFusionColorFrame ResampleColorFrame(IRawColorFrame srcFrame, IKinectFusionColorFrame destFrame)
        {
            if (null == srcFrame)
            {
                throw new ArgumentException("inputs null");
            }

            int originalWidth = srcFrame.Width;
            int originalHeight = srcFrame.Height;
            int resampledWidth = destFrame.Width;
            int resampledHeight = destFrame.Height;
            int resampledHeightPreserveAspectRatio = resampledWidth * originalHeight / originalWidth;

            if (MAX_DEPTH_FRAME_WIDTH < resampledWidth || MAX_DEPTH_FRAME_HEIGHT < resampledHeight)
            {
                throw new ArgumentException("Requested frame too large.");
            }

            if (resampledHeightPreserveAspectRatio > resampledHeight)
            {
                throw new ArgumentException("Requested frame aspect ratio (w:h) too high.");
            }

            float factor = originalWidth / resampledHeightPreserveAspectRatio;
            int verticalOffset = (resampledHeight - resampledHeightPreserveAspectRatio) / 2;

            // Here we make use of unsafe code to just copy the whole pixel as an int for performance reasons, as we do
            // not need access to the individual rgba components.
            fixed (byte* ptrColorPixels = srcFrame.RawPixels)
            {
                int* rawColorPixels = (int*)ptrColorPixels;

                Parallel.For(
                    verticalOffset,
                    resampledHeight - verticalOffset,
                    y =>
                    {
                        int destIndex = y * resampledWidth;

                        for (int x = 0; x < resampledWidth; ++x, ++destIndex)
                        {
                            int srcX = (int)(x * factor);
                            int srcY = (int)(y * factor);
                            int sourceColorIndex = (srcY * srcFrame.Width) + srcX;

                            this.resampledColorImagePixels[destIndex] = rawColorPixels[sourceColorIndex];
                        }
                    });
            }

            destFrame.FusionImageFrame.CopyPixelDataFrom(this.resampledColorImagePixels);

            return destFrame;
        }
    }
}
