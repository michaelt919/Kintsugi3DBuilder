//------------------------------------------------------------------------------
// <copyright file="MainWindow.xaml.cs" company="Microsoft">
//     Copyright (c) Microsoft Corporation.  All rights reserved.
// </copyright>
//------------------------------------------------------------------------------

namespace Tetzlaff.ReflectanceAcquisition.App
{
    using System;
    using System.Collections.Generic;
    using System.ComponentModel;
    using System.Globalization;
    using System.IO;
    using System.Threading;
    using System.Threading.Tasks;
    using System.Windows;
    using System.Windows.Data;
    using System.Windows.Media;
    using System.Windows.Media.Imaging;
    using System.Windows.Media.Media3D;
    using System.Windows.Threading;
    using Microsoft.Kinect;
    using Microsoft.Kinect.Fusion;
    using Wpf3DTools;
    using Tetzlaff.ReflectanceAcquisition.Kinect.Modules;
    using Tetzlaff.ReflectanceAcquisition.Kinect.DataModels;
    using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;
    using Tetzlaff.ReflectanceAcquisition.Pipeline.Exceptions;
    using Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions;
    using Tetzlaff.ReflectanceAcquisition.LightField.DataModels;
using Tetzlaff.ReflectanceAcquisition.LightField.Modules;

    /// <summary>
    /// The implementation of the MainWindow class.
    /// </summary>
    public partial class MainWindow : Window, INotifyPropertyChanged, IDisposable
    {
        /// <summary>
        /// The reconstruction volume processor type. This parameter sets whether AMP or CPU processing
        /// is used. Note that CPU processing will likely be too slow for real-time processing.
        /// </summary>
        private const ReconstructionProcessor ProcessorType = ReconstructionProcessor.Amp;

        /// <summary>
        /// The zero-based device index to choose for reconstruction processing if the 
        /// ReconstructionProcessor AMP options are selected.
        /// Here we automatically choose a device to use for processing by passing -1, 
        /// </summary>
        private const int DeviceToUse = -1;

        /// <summary>
        /// If set true, will automatically reset the reconstruction when MaxTrackingErrors have occurred
        /// </summary>
        private const bool AutoResetReconstructionWhenLost = false;

        /// <summary>
        /// Max tracking error count, will reset the reconstruction if tracking errors
        /// reach the number
        /// </summary>
        private const int MaxTrackingErrors = 100;

        /// <summary>
        /// Time threshold to reset the reconstruction if tracking can't be restored within it.
        /// This value is valid if GPU is used
        /// </summary>
        private const int ResetOnTimeStampSkippedMillisecondsGPU = 2000;

        /// <summary>
        /// Time threshold to reset the reconstruction if tracking can't be restored within it.
        /// This value is valid if CPU is used
        /// </summary>
        private const int ResetOnTimeStampSkippedMillisecondsCPU = 6000;

        /// <summary>
        /// Width of raw depth stream
        /// </summary>
        private const int RawDepthWidth = 512;

        /// <summary>
        /// Height of raw depth stream
        /// </summary>
        private const int RawDepthHeight = 424;

        /// <summary>
        /// Width of raw color stream
        /// </summary>
        private const int RawColorWidth = 1920;

        /// <summary>
        /// Height of raw color stream
        /// </summary>
        private const int RawColorHeight = 1080;

        /// <summary>
        /// The height of raw depth stream if keep the w/h ratio as 4:3
        /// </summary>
        private const int RawDepthHeightWithSpecialRatio = 384;

        /// <summary>
        /// Event interval for FPS timer
        /// </summary>
        private const int FpsInterval = 5;

        /// <summary>
        /// Event interval for status bar timer
        /// </summary>
        private const int StatusBarInterval = 1;

        /// <summary>
        /// Force a point cloud calculation and render at least every 100 milliseconds.
        /// </summary>
        private const int RenderIntervalMilliseconds = 100;

        /// <summary>
        /// The frame interval where we integrate color.
        /// Capturing color has an associated processing cost, so we do not have to capture every frame here.
        /// </summary>
        private const int ColorIntegrationInterval = 1;

        /// <summary>
        /// Volume Cube and WPF3D Origin coordinate cross axis 3D graphics line thickness in screen pixels
        /// </summary>
        private const int LineThickness = 2;

        /// <summary>
        /// WPF3D Origin coordinate cross 3D graphics axis size in m
        /// </summary>
        private const float OriginCoordinateCrossAxisSize = 0.1f;

        /// <summary>
        /// Volume Cube 3D graphics line color
        /// </summary>
        private static System.Windows.Media.Color volumeCubeLineColor = System.Windows.Media.Color.FromArgb(200, 0, 200, 0);   // Green, partly transparent

        /// <summary>
        /// If set true, will automatically reset the reconstruction when the timestamp changes by
        /// ResetOnTimeStampSkippedMillisecondsGPU or ResetOnTimeStampSkippedMillisecondsCPU for the 
        /// different processor types respectively. This is useful for automatically resetting when
        /// scrubbing through a .XEF file or on loop of a .XEF file during playback. Note that setting
        /// this true may cause constant resets on slow machines that cannot process frames in less
        /// time that the reset threshold. If this occurs, set to false or increase the timeout.
        /// </summary>
        /// <remarks>
        /// We now try to find the camera pose, however, setting this false will no longer auto reset on .XEF file playback
        /// </remarks>
        private bool autoResetReconstructionOnTimeSkip = false; 

        /// <summary>
        /// Track whether Dispose has been called
        /// </summary>
        private bool disposed;

        /// <summary>
        /// Saving mesh flag
        /// </summary>
        private bool savingMesh;

        /// <summary>
        /// To display shaded surface normals frame instead of shaded surface frame
        /// </summary>
        private bool displayNormals;

        /// <summary>
        /// Capture, integrate and display color when true
        /// </summary>
        private bool captureColor;

        /// <summary>
        /// Pause or resume image integration
        /// </summary>
        private bool pauseIntegration = true;

        /// <summary>
        /// Whether render from the live Kinect camera pose or virtual camera pose
        /// </summary>
        private bool kinectView = true;

        /// <summary>
        /// Whether render the volume 3D graphics overlay
        /// </summary>
        private bool volumeGraphics;

        /// <summary>
        /// The counter for frames that have been processed
        /// </summary>
        private int processedFrameCount = 0;

        /// <summary>
        /// Timestamp of last depth frame
        /// </summary>
        private TimeSpan lastFrameTimestamp;

        /// <summary>
        /// Timer to count FPS
        /// </summary>
        private DispatcherTimer fpsTimer;

        /// <summary>
        /// Timer stamp of last computation of FPS
        /// </summary>
        private DateTime lastFPSTimestamp = DateTime.UtcNow;

        /// <summary>
        /// Timer stamp of last raycast and render
        /// </summary>
        private DateTime lastRenderTimestamp = DateTime.UtcNow;

        /// <summary>
        /// Timer used for ensuring status bar message will be displayed at least one second
        /// </summary>
        private DispatcherTimer statusBarTimer;

        /// <summary>
        /// Timer stamp of last update of status message
        /// </summary>
        private DateTime lastStatusTimestamp;

        /// <summary>
        /// A high priority message queue for status message
        /// </summary>
        private Queue<string> statusMessageQueue = new Queue<string>();
        
        // TODO: For mid-refactor testing, pull out later
        private KinectFrameSource frameSource = null;
        private KinectFusionPoseAlignment poseAlignmentModule = null;
        private KinectFusionGeometryIntegration geometryIntegrationModule = null;
        private LightFieldIntegration lightFieldIntegration = null;

        /// <summary>
        /// The Kinect Fusion volume, enabling color reconstruction
        /// </summary>
        private KinectFusionReconstructionVolume volume;

        private LightFieldDirectory lightField;

        /// <summary>
        /// Kinect color mapped into depth frame
        /// </summary>
        private FusionColorImageFrame resampledColorFrameDepthAligned;

        /// <summary>
        /// Calculated point cloud frame from image integration
        /// </summary>
        private FusionPointCloudImageFrame raycastPointCloudFrame;

        /// <summary>
        /// Shaded surface frame from shading point cloud frame
        /// </summary>
        private FusionColorImageFrame shadedSurfaceFrame;

        /// <summary>
        /// Shaded surface normals frame from shading point cloud frame
        /// </summary>
        private FusionColorImageFrame shadedSurfaceNormalsFrame;

        /// <summary>
        /// Bitmap contains depth float frame data for rendering
        /// </summary>
        private WriteableBitmap depthFloatFrameBitmap;

        /// <summary>
        /// Bitmap contains delta from reference frame data for rendering
        /// </summary>
        private WriteableBitmap deltaFromReferenceFrameBitmap;

        /// <summary>
        /// Bitmap contains shaded surface frame data for rendering
        /// </summary>
        private WriteableBitmap shadedSurfaceFrameBitmap;

        /// <summary>
        /// Pixel buffer of depth float frame with pixel data in float format
        /// </summary>
        private float[] depthFloatFrameDepthPixels;

        /// <summary>
        /// Pixel buffer of delta from reference frame with pixel data in float format
        /// </summary>
        private float[] deltaFromReferenceFrameFloatPixels;

        /// <summary>
        /// Pixel buffer of depth float frame with pixel data in 32bit color
        /// </summary>
        private int[] depthFloatFramePixelsArgb;

        /// <summary>
        /// Pixels buffer of shaded surface frame in 32bit color
        /// </summary>
        private int[] shadedSurfaceFramePixelsArgb;

        /// <summary>
        /// Pixel buffer of color frame from Kinect sensor
        /// </summary>
        private int[] colorFramePixelsArgb;

        /// <summary>
        /// Mapping of depth pixels into color image
        /// </summary>
        private ColorSpacePoint[] colorCoordinates;

        /// <summary>
        /// Mapped color pixels in depth frame of reference
        /// </summary>
        private int[] resampledColorImagePixelsAlignedToDepth;

        /// <summary>
        /// The worker thread to process the depth and color data
        /// </summary>
        private Thread workerThread = null;

        /// <summary>
        /// Event to stop worker thread
        /// </summary>
        private ManualResetEvent workerThreadStopEvent;

        /// <summary>
        /// Event to notify that depth data is ready for process
        /// </summary>
        private ManualResetEvent depthReadyEvent;

        /// <summary>
        /// Event to notify that color data is ready for process
        /// </summary>
        private ManualResetEvent colorReadyEvent;

        /// <summary>
        /// Lock object for volume re-creation and meshing
        /// </summary>
        private object volumeLock = new object();

        /// <summary>
        /// The volume cube 3D graphical representation
        /// </summary>
        private ScreenSpaceLines3D volumeCube;

        /// <summary>
        /// The volume cube 3D graphical representation
        /// </summary>
        private ScreenSpaceLines3D volumeCubeAxisX;

        /// <summary>
        /// The volume cube 3D graphical representation
        /// </summary>
        private ScreenSpaceLines3D volumeCubeAxisY;

        /// <summary>
        /// The volume cube 3D graphical representation
        /// </summary>
        private ScreenSpaceLines3D volumeCubeAxisZ;

        /// <summary>
        /// The axis-aligned coordinate cross X axis
        /// </summary>
        private ScreenSpaceLines3D axisX;

        /// <summary>
        /// The axis-aligned coordinate cross Y axis
        /// </summary>
        private ScreenSpaceLines3D axisY;

        /// <summary>
        /// The axis-aligned coordinate cross Z axis
        /// </summary>
        private ScreenSpaceLines3D axisZ;

        /// <summary>
        /// Indicate whether the 3D view port has added the volume cube
        /// </summary>
        private bool haveAddedVolumeCube = false;

        /// <summary>
        /// Indicate whether the 3D view port has added the origin coordinate cross
        /// </summary>
        private bool haveAddedCoordinateCross = false;

        /// <summary>
        /// Flag boolean set true to force the reconstruction visualization to be updated after graphics camera movements
        /// </summary>
        private bool viewChanged = true;

        /// <summary>
        /// The virtual 3rd person camera view that can be controlled by the mouse
        /// </summary>
        private GraphicsCamera virtualCamera;

        /// <summary>
        /// The virtual 3rd person camera view that can be controlled by the mouse - start rotation
        /// </summary>
        private Quaternion virtualCameraStartRotation = Quaternion.Identity;

        /// <summary>
        /// The virtual 3rd person camera view that can be controlled by the mouse - start translation
        /// </summary>
        private Point3D virtualCameraStartTranslation = new Point3D();  // 0,0,0

        /// <summary>
        /// Flag to signal to worker thread to reset the reconstruction
        /// </summary>
        private bool resetReconstruction = false;

        /// <summary>
        /// Flag to signal to worker thread to re-create the reconstruction
        /// </summary>
        private bool recreateReconstruction = false;

        /// <summary>
        /// The transformation between the world and camera view coordinate system
        /// </summary>
        private ICameraPose currentCameraPose;

        /// <summary>
        /// The default transformation between the world and volume coordinate system
        /// </summary>
        private Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 defaultWorldToVolumeTransform;

        /// <summary>
        /// The reconstruction volume voxel density in voxels per meter (vpm)
        /// 1000mm / 256vpm = ~3.9mm/voxel
        /// </summary>
        private float voxelsPerMeter = 256.0f;

        /// <summary>
        /// The reconstruction volume voxel resolution in the X axis
        /// At a setting of 256vpm the volume is 384 / 256 = 1.5m wide
        /// </summary>
        private int voxelsX = 384;

        /// <summary>
        /// The reconstruction volume voxel resolution in the Y axis
        /// At a setting of 256vpm the volume is 384 / 256 = 1.5m high
        /// </summary>
        private int voxelsY = 384;

        /// <summary>
        /// The reconstruction volume voxel resolution in the Z axis
        /// At a setting of 256vpm the volume is 384 / 256 = 1.5m deep
        /// </summary>
        private int voxelsZ = 384;

        /// <summary>
        /// Parameter to translate the reconstruction based on the minimum depth setting. When set to
        /// false, the reconstruction volume +Z axis starts at the camera lens and extends into the scene.
        /// Setting this true in the constructor will move the volume forward along +Z away from the
        /// camera by the minimum depth threshold to enable capture of very small reconstruction volume
        /// by setting a non-identity world-volume transformation in the ResetReconstruction call.
        /// Small volumes should be shifted, as the Kinect hardware has a minimum sensing limit of ~0.35m,
        /// inside which no valid depth is returned, hence it is difficult to initialize and track robustly  
        /// when the majority of a small volume is inside this distance.
        /// </summary>
        private bool translateResetPoseByMinDepthThreshold = true;

        /// <summary>
        /// The color mapping of the rendered reconstruction visualization. 
        /// </summary>
        private Matrix4 worldToBGRTransform = new Matrix4();

        /// <summary>
        /// The virtual camera pose - updated whenever the user interacts and moves the virtual camera.
        /// </summary>
        private ICameraPose virtualCameraPose = new CameraPose();

        private Thread saveViewThread = null;

        private string outputDirectory;

        //private bool ForceLightField1To1AspectRatio = true; // Required for LFMorphing code base

        /// <summary>
        /// Initializes a new instance of the MainWindow class.
        /// </summary>
        public MainWindow()
        {
            this.InitializeComponent();
        }

        /// <summary>
        /// Finalizes an instance of the MainWindow class.
        /// This destructor will run only if the Dispose method does not get called.
        /// </summary>
        ~MainWindow()
        {
            this.Dispose(false);
        }

        #region Properties

        /// <summary>
        /// Property change event
        /// </summary>
        public event PropertyChangedEventHandler PropertyChanged;

        /// <summary>
        /// Gets or sets a value indicating whether to display surface normals.
        /// </summary>
        public bool DisplayNormals
        {
            get
            {
                return this.displayNormals;
            }

            set
            {
                this.displayNormals = value;
                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("DisplayNormals"));
                }
            }
        }

        /// <summary>
        /// Gets or sets a value indicating whether to capture color.
        /// </summary>
        public bool CaptureColor
        {
            get
            {
                return this.captureColor;
            }

            set
            {
                this.captureColor = value;

                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("CaptureColor"));
                }
            }
        }

        /// <summary>
        /// Gets or sets a value indicating whether to pause integration.
        /// </summary>
        public bool PauseIntegration
        {
            get
            {
                return this.pauseIntegration;
            }

            set
            {
                this.pauseIntegration = value;
                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("PauseIntegration"));
                }
            }
        }

        /// <summary>
        /// Gets or sets a value indicating whether to mirror depth.
        /// </summary>
        public bool MirrorDepth
        {
            get
            {
                return this.frameSource == null ? false : this.frameSource.MirrorDepth;
            }

            set
            {
                if (this.frameSource != null)
                {
                    this.frameSource.MirrorDepth = value;
                    if (null != this.PropertyChanged)
                    {
                        this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("MirrorDepth"));
                    }

                    this.resetReconstruction = true;
                }
            }
        }

        /// <summary>
        /// Gets or sets a value indicating whether enable "Kinect View".
        /// </summary>
        public bool KinectView
        {
            get
            {
                return this.kinectView;
            }

            set
            {
                this.kinectView = value;

                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("KinectView"));
                }

                // Decide whether render the volume cube
                if (this.kinectView)
                {
                    this.virtualCamera.CameraTransformationChanged -= this.OnVirtualCameraTransformationChanged;
                    this.virtualCamera.Detach(this.shadedSurfaceImage);

                    if (this.volumeGraphics)
                    {
                        // Do not render the frustum when in Kinect camera view with volume graphics active
                        this.virtualCamera.RemoveFrustum3DGraphics();
                    }
                }
                else
                {
                    this.virtualCamera.Attach(this.shadedSurfaceImage);
                    this.virtualCamera.CameraTransformationChanged += this.OnVirtualCameraTransformationChanged;

                    if (this.volumeGraphics)
                    {
                        // Re-render the frustum if we exit the Kinect camera view with volume graphics active
                        this.virtualCamera.AddFrustum3DGraphics();
                    }

                    // Reset the virtual camera
                    this.virtualCamera.Reset();
                }

                this.viewChanged = true;

                this.GraphicsViewport.InvalidateVisual();
            }
        }

        /// <summary>
        /// Gets or sets a value indicating whether to show volume graphics.
        /// </summary>
        public bool VolumeGraphics
        {
            get
            {
                return this.volumeGraphics;
            }

            set
            {
                this.volumeGraphics = value;

                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("VolumeGraphics"));
                }

                if (this.volumeGraphics)
                {
                    // Add the graphics to the visual tree

                    // Create axis-aligned coordinate cross 3D graphics at the WPF3D/reconstruction world origin
                    // Red is the +X axis, Green is the +Y axis, Blue is the +Z axis in the WPF3D coordinate system
                    // Note that this coordinate cross shows the WPF3D graphics coordinate system
                    // (right hand, erect so +Y up and +X right, +Z out of screen), rather than the reconstruction 
                    // volume coordinate system (right hand, rotated so +Y is down and +X is right, +Z into screen ).
                    this.CreateAxisAlignedCoordinateCross3DGraphics(new Point3D(0, 0, 0), OriginCoordinateCrossAxisSize, LineThickness);

                    // Create volume cube 3D graphics in WPF3D. The front top left corner is the actual origin of the volume
                    // voxel coordinate system, and shown with an overlaid coordinate cross.
                    // Red is the +X axis, Green is the +Y axis, Blue is the +Z axis in the voxel coordinate system
                    this.CreateCube3DGraphics(volumeCubeLineColor, LineThickness, new Vector3D(0, 0, 0));

                    this.AddVolumeCube3DGraphics();
                    this.AddAxisAlignedCoordinateCross3DGraphics();

                    if (!this.kinectView)
                    {
                        // Do not render the frustum when in Kinect camera view with volume graphics active
                        this.virtualCamera.AddFrustum3DGraphics();
                    }

                    // Add callback which is called every time WPF renders
                    System.Windows.Media.CompositionTarget.Rendering += this.CompositionTargetRendering;
                }
                else
                {
                    // Remove the graphics from the visual tree
                    this.DisposeVolumeCube3DGraphics();
                    this.DisposeAxisAlignedCoordinateCross3DGraphics();

                    this.virtualCamera.RemoveFrustum3DGraphics();

                    // Remove callback which is called every time WPF renders
                    System.Windows.Media.CompositionTarget.Rendering -= this.CompositionTargetRendering;
                }

                this.viewChanged = true;

                this.GraphicsViewport.InvalidateVisual();
            }
        }

        /// <summary>
        /// Gets or sets a value indicating whether to use the camera pose finder.
        /// </summary>
        public bool UseCameraPoseFinder
        {
            get
            {
                return poseAlignmentModule != null && poseAlignmentModule.AutoFindCameraPoseWhenLost;
            }
            set
            {
                if (poseAlignmentModule != null)
                {
                    poseAlignmentModule.AutoFindCameraPoseWhenLost = value;
                    if (null != this.PropertyChanged)
                    {
                        this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("UseCameraPoseFinder"));
                    }
                }
            }
        }

        /// <summary>
        /// Gets or sets the minimum clip depth.
        /// </summary>
        public double MinDepthClip
        {
            get
            {
                return (double)(this.frameSource == null ? FusionDepthProcessor.DefaultMinimumDepth : this.frameSource.MinDepthClip);
            }

            set
            {
                if (this.frameSource != null)
                {
                    this.frameSource.MinDepthClip = (float)value;
                    if (null != this.PropertyChanged)
                    {
                        this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("MinDepthClip"));
                    }
                }
            }
        }

        /// <summary>
        /// Gets or sets the maximum clip depth.
        /// </summary>
        public double MaxDepthClip
        {
            get
            {
                return (double)(this.frameSource == null ? FusionDepthProcessor.DefaultMaximumDepth : this.frameSource.MaxDepthClip);
            }

            set
            {
                if (this.frameSource != null)
                {
                    this.frameSource.MaxDepthClip = (float)value;
                    if (null != this.PropertyChanged)
                    {
                        this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("MaxDepthClip"));
                    }
                }
            }
        }

        /// <summary>
        /// Gets or sets the integration weight.
        /// </summary>
        public double IntegrationWeight
        {
            get
            {
                return geometryIntegrationModule == null ? 0.0 : (double)geometryIntegrationModule.IntegrationWeight;
            }

            set
            {
                if (geometryIntegrationModule != null)
                {
                    geometryIntegrationModule.IntegrationWeight = (short)(value + 0.5);
                    if (null != this.PropertyChanged)
                    {
                        this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("IntegrationWeight"));
                    }
                }
            }
        }

        /// <summary>
        /// Gets or sets the voxels per meter value.
        /// </summary>
        public double VoxelsPerMeter
        {
            get
            {
                return (double)this.voxelsPerMeter;
            }

            set
            {
                this.voxelsPerMeter = (float)value;
                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("VoxelsPerMeter"));
                }
            }
        }

        /// <summary>
        /// Gets or sets the X-axis volume resolution.
        /// </summary>
        public double VoxelsX
        {
            get
            {
                return (double)this.voxelsX;
            }

            set
            {
                this.voxelsX = (int)(value + 0.5);
                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("VoxelsX"));
                }
            }
        }

        /// <summary>
        /// Gets or sets the Y-axis volume resolution.
        /// </summary>
        public double VoxelsY
        {
            get
            {
                return (double)this.voxelsY;
            }

            set
            {
                this.voxelsY = (int)(value + 0.5);
                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("VoxelsY"));
                }
            }
        }

        /// <summary>
        /// Gets or sets the Z-axis volume resolution.
        /// </summary>
        public double VoxelsZ
        {
            get
            {
                return (double)this.voxelsZ;
            }

            set
            {
                this.voxelsZ = (int)(value + 0.5);
                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("VoxelsZ"));
                }
            }
        }

        /// <summary>
        /// Gets a value indicating whether rendering is overdue 
        /// (i.e. time interval since last render > RenderIntervalMilliseconds)
        /// </summary>
        public bool IsRenderOverdue
        {
            get
            {
                return (DateTime.UtcNow - this.lastRenderTimestamp).TotalMilliseconds >= RenderIntervalMilliseconds;
            }
        }

        #endregion

        /// <summary>
        /// Dispose resources
        /// </summary>
        public void Dispose()
        {
            this.Dispose(true);

            // This object will be cleaned up by the Dispose method.
            GC.SuppressFinalize(this);
        }

        /// <summary>
        /// Frees all memory associated with the ReconstructionVolume and FusionImageFrames.
        /// </summary>
        /// <param name="disposing">Whether the function was called from Dispose.</param>
        protected virtual void Dispose(bool disposing)
        {
            if (!this.disposed)
            {
                if (disposing)
                {
                    if (null != this.depthReadyEvent)
                    {
                        this.depthReadyEvent.Dispose();
                    }

                    if (null != this.colorReadyEvent)
                    {
                        this.colorReadyEvent.Dispose();
                    }

                    if (null != this.workerThreadStopEvent)
                    {
                        this.workerThreadStopEvent.Dispose();
                    }

                    this.RemoveVolumeCube3DGraphics();
                    this.DisposeVolumeCube3DGraphics();

                    this.RemoveAxisAlignedCoordinateCross3DGraphics();
                    this.DisposeAxisAlignedCoordinateCross3DGraphics();

                    if (null != this.virtualCamera)
                    {
                        this.virtualCamera.CameraTransformationChanged -= this.OnVirtualCameraTransformationChanged;
                        this.virtualCamera.Detach(this.shadedSurfaceImage);     // Stop getting mouse events from the image
                        this.virtualCamera.Dispose();
                    }

                    this.SafeDisposeFusionResources();

                    poseAlignmentModule.Dispose();

                    if (null != this.volume)
                    {
                        this.volume.Dispose();
                    }
                }
            }

            this.disposed = true;
        }

        /// <summary>
        /// Render Fusion color frame to UI
        /// </summary>
        /// <param name="colorFrame">Fusion color frame</param>
        /// <param name="colorPixels">Pixel buffer for fusion color frame</param>
        /// <param name="bitmap">Bitmap contains color frame data for rendering</param>
        /// <param name="image">UI image component to render the color frame</param>
        private static void RenderColorImage(FusionColorImageFrame colorFrame, ref int[] colorPixels, ref WriteableBitmap bitmap, System.Windows.Controls.Image image)
        {
            if (null == image || null == colorFrame)
            {
                return;
            }

            if (null == colorPixels || colorFrame.PixelDataLength != colorPixels.Length)
            {
                // Create pixel array of correct format
                colorPixels = new int[colorFrame.PixelDataLength];
            }

            if (null == bitmap || colorFrame.Width != bitmap.Width || colorFrame.Height != bitmap.Height)
            {
                // Create bitmap of correct format
                bitmap = new WriteableBitmap(colorFrame.Width, colorFrame.Height, 96.0, 96.0, PixelFormats.Bgr32, null);

                // Set bitmap as source to UI image object
                image.Source = bitmap;
            }

            // Copy pixel data to pixel buffer
            colorFrame.CopyPixelDataTo(colorPixels);

            // Write pixels to bitmap
            bitmap.WritePixels(
                        new Int32Rect(0, 0, colorFrame.Width, colorFrame.Height),
                        colorPixels,
                        bitmap.PixelWidth * sizeof(int),
                        0);
        }

        /// <summary>
        /// Execute startup tasks
        /// </summary>
        /// <param name="sender">object sending the event</param>
        /// <param name="e">event arguments</param>
        private void WindowLoaded(object sender, RoutedEventArgs e)
        {
            OpenViewSetFile();

            int deviceMemoryKB = 0;

            // Check to ensure suitable DirectX11 compatible hardware exists before initializing Kinect Fusion
            try
            {
                string deviceDescription = string.Empty;
                string deviceInstancePath = string.Empty;

                FusionDepthProcessor.GetDeviceInfo(ProcessorType, DeviceToUse, out deviceDescription, out deviceInstancePath, out deviceMemoryKB);
            }
            catch (IndexOutOfRangeException)
            {
                // Thrown when index is out of range for processor type or there is no DirectX11 capable device installed.
                // As we set -1 (auto-select default) for the DeviceToUse above, this indicates that there is no DirectX11 
                // capable device. The options for users in this case are to either install a DirectX11 capable device 
                // (see documentation for recommended GPUs) or to switch to non-real-time CPU based reconstruction by 
                // changing ProcessorType to ReconstructionProcessor.Cpu
                this.statusBarText.Text = Properties.Resources.NoDirectX11CompatibleDeviceOrInvalidDeviceIndex;
                return;
            }
            catch (DllNotFoundException)
            {
                this.statusBarText.Text = Properties.Resources.MissingPrerequisite;
                return;
            }
            catch (InvalidOperationException ex)
            {
                this.statusBarText.Text = ex.Message;
                return;
            }

            VoxelsXSlider.Maximum = 512;
            VoxelsYSlider.Maximum = 512;
            VoxelsZSlider.Maximum = 512;

            try
            {
                this.frameSource = new KinectFrameSource();
                if (null != this.PropertyChanged)
                {
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("MinDepthClip"));
                    this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("MaxDepthClip"));
                }
                this.frameSource.ColorFrameReady += () =>
                {
                    // Signal worker thread to process
                    this.colorReadyEvent.Set(); 
                };
                this.frameSource.DepthFrameReady += () =>
                {
                    // Signal worker thread to process
                    this.depthReadyEvent.Set(); 
                };
            }
            catch (NoReadyKinectException)
            {
                this.statusBarText.Text = Properties.Resources.NoReadyKinect;
            }

            // Setup the graphics rendering

            // Create virtualCamera for non-Kinect viewpoint rendering
            // Default position is translated along Z axis, looking back at the origin
            this.virtualCameraStartTranslation = new Point3D(0, 0, this.voxelsZ / this.voxelsPerMeter);
            this.virtualCamera = new GraphicsCamera(this.virtualCameraStartTranslation, this.virtualCameraStartRotation, (float)Width / (float)Height);

            // Attach this virtual camera to the viewport
            this.GraphicsViewport.Camera = this.virtualCamera.Camera;

            // Start worker thread for depth processing
            this.StartWorkerThread();

            // Start fps timer
            this.fpsTimer = new DispatcherTimer(DispatcherPriority.Send);
            this.fpsTimer.Interval = new TimeSpan(0, 0, FpsInterval);
            this.fpsTimer.Tick += this.FpsTimerTick;
            this.fpsTimer.Start();

            // Set last fps timestamp as now
            this.lastFPSTimestamp = DateTime.UtcNow;

            // Start status bar timer
            this.statusBarTimer = new DispatcherTimer(DispatcherPriority.Send);
            this.statusBarTimer.Interval = new TimeSpan(0, 0, StatusBarInterval);
            this.statusBarTimer.Tick += this.StatusBarTimerTick;
            this.statusBarTimer.Start();

            this.lastStatusTimestamp = DateTime.Now;

            // Allocate frames for Kinect Fusion now a sensor is present
            this.AllocateKinectFusionResources();

            poseAlignmentModule = new KinectFusionPoseAlignment(this.frameSource.DepthWidth, this.frameSource.DepthHeight);
            if (null != this.PropertyChanged)
            {
                this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("UseCameraPoseFinder"));
            }

            geometryIntegrationModule = new KinectFusionGeometryIntegration();
            if (null != this.PropertyChanged)
            {
                this.PropertyChanged.Invoke(this, new PropertyChangedEventArgs("IntegrationWeight"));
            }

            lightFieldIntegration = new LightFieldIntegration();

            // Create the camera frustum 3D graphics in WPF3D
            this.virtualCamera.CreateFrustum3DGraphics(this.GraphicsViewport, this.frameSource.DepthWidth, this.frameSource.DepthHeight);

            // Set recreate reconstruction flag
            this.recreateReconstruction = true;

            // Show introductory message
            this.ShowStatusMessage(Properties.Resources.IntroductoryMessage);
        }

        /// <summary>
        /// Execute shutdown tasks
        /// </summary>
        /// <param name="sender">Object sending the event</param>
        /// <param name="e">Event arguments</param>
        private void WindowClosing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            this.lightField.FinalizeReflectance();

            // Stop timer
            if (null != this.fpsTimer)
            {
                this.fpsTimer.Stop();
                this.fpsTimer.Tick -= this.FpsTimerTick;
            }

            if (null != this.statusBarTimer)
            {
                this.statusBarTimer.Stop();
                this.statusBarTimer.Tick -= this.StatusBarTimerTick;
            }

            // Remove the camera frustum 3D graphics from WPF3D
            this.virtualCamera.DisposeFrustum3DGraphics();

            // Stop worker thread
            this.StopWorkerThread();
        }

        /// <summary>
        /// Handler for FPS timer tick
        /// </summary>
        /// <param name="sender">Object sending the event</param>
        /// <param name="e">Event arguments</param>
        private void FpsTimerTick(object sender, EventArgs e)
        {
            if (!this.savingMesh)
            {
                if (null == this.frameSource)
                {
                    // Show "No ready Kinect found!" on status bar
                    this.statusBarText.Text = Properties.Resources.NoReadyKinect;
                }
                else
                {
                    // Calculate time span from last calculation of FPS
                    double intervalSeconds = (DateTime.UtcNow - this.lastFPSTimestamp).TotalSeconds;

                    // Calculate and show fps on status bar
                    this.fpsText.Text = string.Format(
                        System.Globalization.CultureInfo.InvariantCulture,
                        Properties.Resources.Fps,
                        (double)this.processedFrameCount / intervalSeconds);
                }
            }

            // Reset frame counter
            this.processedFrameCount = 0;
            this.lastFPSTimestamp = DateTime.UtcNow;
        }

        /// <summary>
        /// Reset FPS timer and counter
        /// </summary>
        private void ResetFps()
        {
            // Restart fps timer
            if (null != this.fpsTimer)
            {
                this.fpsTimer.Stop();
                this.fpsTimer.Start();
            }

            // Reset frame counter
            this.processedFrameCount = 0;
            this.lastFPSTimestamp = DateTime.UtcNow;
        }

        /// <summary>
        /// Handler for status bar timer tick
        /// </summary>
        /// <param name="sender">Object sending the event</param>
        /// <param name="e">Event arguments</param>
        private void StatusBarTimerTick(object sender, EventArgs e)
        {
            if (this.statusMessageQueue.Count > 0)
            {
                this.statusBarText.Text = this.statusMessageQueue.Dequeue();

                // Update the last timestamp of status message
                this.lastStatusTimestamp = DateTime.Now;
            }
        }

        /// <summary>
        /// Start the work thread to process incoming depth data
        /// </summary>
        private void StartWorkerThread()
        {
            if (null == this.workerThread)
            {
                // Initialize events
                this.depthReadyEvent = new ManualResetEvent(false);
                this.colorReadyEvent = new ManualResetEvent(false);
                this.workerThreadStopEvent = new ManualResetEvent(false);

                // Create worker thread and start
                this.workerThread = new Thread(this.WorkerThreadProc);
                this.workerThread.Start();
            }
        }

        /// <summary>
        /// Stop worker thread
        /// </summary>
        private void StopWorkerThread()
        {
            if (null != this.workerThread)
            {
                // Set stop event to stop thread
                this.workerThreadStopEvent.Set();

                // Wait for exit of thread
                this.workerThread.Join();
            }
        }

        /// <summary>
        /// Worker thread in which depth data is processed
        /// </summary>
        private void WorkerThreadProc()
        {
            WaitHandle[] events = new WaitHandle[2] { this.workerThreadStopEvent, this.depthReadyEvent };
            while (true)
            {
                int index = WaitHandle.WaitAny(events);

                if (0 == index)
                {
                    // Stop event has been set. Exit thread
                    break;
                }

                // Reset depth ready event
                this.depthReadyEvent.Reset();

                // Pass data to process
                this.Process();
            }
        }

        /// <summary>
        /// The main Kinect Fusion process function
        /// </summary>
        private void Process()
        {
            if (this.recreateReconstruction)
            {
                lock (this.volumeLock)
                {
                    this.recreateReconstruction = false; 
                    this.RecreateReconstruction();
                }
            }

            if (this.resetReconstruction)
            {
                this.resetReconstruction = false;
                this.ResetReconstruction();
            }

            if (null != this.volume && !this.savingMesh)
            {
                try
                {
                    // Convert depth to float and render depth frame
                    this.ProcessDepthAndColorData();

                    try
                    {
                        // Track camera pose
                        ICameraPose calculatedCameraPos = poseAlignmentModule.AlignFrame(
                            this.frameSource.DepthFrame,
                            this.frameSource.ColorFrame,
                            this.volume,
                            this.currentCameraPose,
                            this.MirrorDepth
                        );

                        this.ShowStatusMessageLowPriority(poseAlignmentModule.CameraPoseFinderStatus);

                        if (poseAlignmentModule.TrackingHasFailedPreviously)
                        {
                            this.ShowStatusMessageLowPriority("Kinect Fusion camera tracking RECOVERED! Residual energy=" + string.Format(CultureInfo.InvariantCulture, "{0:0.00000}", poseAlignmentModule.AlignmentEnergy));
                        }

                        this.currentCameraPose = calculatedCameraPos;

                        // Only save the view if there isn't already an active view saving thread
                        if (saveViewThread == null || !saveViewThread.IsAlive)
                        {
                            //if (AddViewSetEntry(calculatedCameraPos.Matrix.ToKinectMatrix())) // Only save the image if the entry was successfuly added to the vset file
                            {
                                canProceed = false;
                                saveViewThread = new Thread(new ThreadStart(this.SaveLightFieldView));
                                saveViewThread.Start();

                                // Wait for the view saving thread to get started before moving on.
                                while (!canProceed) Thread.Sleep(1);
                            }
                        }

                        if (this.kinectView)
                        {
                            Dispatcher.BeginInvoke((Action)(() => this.UpdateVirtualCameraTransform()));
                        }
                        else
                        {
                            // Just update the frustum
                            Dispatcher.BeginInvoke((Action)(() => this.virtualCamera.UpdateFrustumTransformMatrix4(this.currentCameraPose.Matrix.ToKinectMatrix())));
                        }

                        // Increase processed frame counter
                        this.processedFrameCount++;
                    }
                    catch (CameraTrackingFailedException e)
                    {
                        if (e.InnerException != null && e.InnerException is NotEnoughPoseFinderMatchesException)
                        {
                            this.ShowStatusMessage(Properties.Resources.PoseFinderNotEnoughMatches);
                        }
                        else if (e.InnerException != null && e.InnerException is CameraPoseFinderFailedException)
                        {
                            this.ShowStatusMessage(e.Message);
                        }
                        else
                        {
                            // Show tracking error on status bar
                            this.ShowStatusMessageLowPriority(Properties.Resources.CameraTrackingFailed);
                        }

                        if (AutoResetReconstructionWhenLost &&
                            poseAlignmentModule.TrackingErrorCount >= MaxTrackingErrors)
                        {
                            // Bad tracking
                            this.ShowStatusMessage(Properties.Resources.ResetVolumeAuto);

                            // Automatically Clear Volume and reset tracking if tracking fails
                            this.ResetReconstruction();
                        }
                    }
                    catch (CameraPoseFinderHistoryFullException)
                    {
                        this.ShowStatusMessage(Properties.Resources.PoseFinderPoseHistoryFull);
                    }

                    // Only continue if we do not have tracking errors
                    if (0 == poseAlignmentModule.TrackingErrorCount)
                    {
                        // Integrate depth
                        this.IntegrateData();

                        // Check to see if another depth frame is already available. 
                        // If not we have time to calculate a point cloud and render, 
                        // but if so we make sure we force a render at least every 
                        // RenderIntervalMilliseconds.
                        if (!this.depthReadyEvent.WaitOne(0) || this.IsRenderOverdue)
                        {
                            // Raycast and render
                            this.RenderReconstruction();
                        }
                    }
                }
                catch (InvalidOperationException ex)
                {
                    this.ShowStatusMessage(ex.Message);
                }
            }
        }

        /// <summary>
        /// Process the depth input for camera tracking
        /// </summary>
        private void ProcessDepthAndColorData()
        {
            // To enable playback of a .xef file through Kinect Studio and reset of the reconstruction
            // if the .xef loops, we test for when the frame timestamp has skipped a large number. 
            // Note: this will potentially continually reset live reconstructions on slow machines which
            // cannot process a live frame in less time than the reset threshold. Increase the number of
            // milliseconds if this is a problem.
            if (this.autoResetReconstructionOnTimeSkip)
            {
                this.CheckResetTimeStamp(this.frameSource.RelativeTime);
            }

            this.frameSource.RefreshDepthFrame();
            this.frameSource.RefreshColorFrame();

            // Render depth float frame
            this.Dispatcher.BeginInvoke(
                (Action)
                (() =>
                    this.RenderDepthFloatImage(
                        ref this.depthFloatFrameBitmap,
                        this.depthFloatImage)));
            this.Dispatcher.BeginInvoke(
                (Action)
                (() =>
                    this.RenderKinectColorImage(
                        ref this.deltaFromReferenceFrameBitmap,
                        this.deltaFromReferenceImage)));
        }

        /// <summary>
        /// Perform volume depth data integration
        /// </summary>
        private void IntegrateData()
        {
            bool integrateData = !this.PauseIntegration && poseAlignmentModule.PreviousAlignmentSucceeded;

            // Integrate the frame to volume
            if (integrateData)
            {
                // Reset this flag as we are now integrating data again
                if (poseAlignmentModule.TrackingHasFailedPreviously)
                {
                    poseAlignmentModule.ResetTracking();
                }

                geometryIntegrationModule.IntegrateGeometry(this.frameSource.DepthFrame, this.volume, this.currentCameraPose);
            }
        }

        /// <summary>
        /// Render the reconstruction
        /// </summary>
        private void RenderReconstruction()
        {
            if (null == this.volume || this.savingMesh || null == this.raycastPointCloudFrame
                || null == this.shadedSurfaceFrame || null == this.shadedSurfaceNormalsFrame)
            {
                return;
            }

            // If KinectView option has been set, use the worldToCameraTransform, else use the virtualCamera transform
            ICameraPose cameraView = this.KinectView ? this.currentCameraPose : this.virtualCameraPose;

            //if (this.captureColor)
            //{
            //    this.volume.CalculatePointCloud(this.raycastPointCloudFrame, this.shadedSurfaceFrame, cameraView);
            //}
            //else
            {
                this.volume.CalculatePointCloud(this.raycastPointCloudFrame, cameraView);

                // Shade point cloud frame for rendering
                FusionDepthProcessor.ShadePointCloud(
                    this.raycastPointCloudFrame,
                    cameraView.Matrix.ToKinectMatrix(),
                    this.worldToBGRTransform,
                    this.displayNormals ? null : this.shadedSurfaceFrame,
                    this.displayNormals ? this.shadedSurfaceNormalsFrame : null);
            }

            // Update the rendered UI image
            Dispatcher.BeginInvoke((Action)(() => this.ReconstructFrameComplete()));

            this.lastRenderTimestamp = DateTime.UtcNow;
        }

        /// <summary>
        /// Called when a ray-casted view of the reconstruction is available for display in the UI 
        /// </summary>
        private void ReconstructFrameComplete()
        {
            // Render shaded surface frame or shaded surface normals frame
            RenderColorImage(
                /*this.captureColor ? this.shadedSurfaceFrame :*/ (this.displayNormals ? this.shadedSurfaceNormalsFrame : this.shadedSurfaceFrame),
                ref this.shadedSurfaceFramePixelsArgb,
                ref this.shadedSurfaceFrameBitmap,
                this.shadedSurfaceImage);
        }

        /// <summary>
        /// Render Fusion depth float frame to UI
        /// </summary>
        /// <param name="bitmap">Bitmap contains depth float frame data for rendering</param>
        /// <param name="image">UI image component to render depth float frame to</param>
        private void RenderDepthFloatImage(ref WriteableBitmap bitmap, System.Windows.Controls.Image image)
        {
            if (null == this.frameSource.DepthFrame)
            {
                return;
            }

            if (null == bitmap || this.frameSource.DepthFrame.Width != bitmap.Width || this.frameSource.DepthFrame.Height != bitmap.Height)
            {
                // Create bitmap of correct format
                bitmap = new WriteableBitmap(this.frameSource.DepthFrame.Width, this.frameSource.DepthFrame.Height, 96.0, 96.0, PixelFormats.Bgr32, null);

                // Set bitmap as source to UI image object
                image.Source = bitmap;
            }

            this.frameSource.DepthFrame.FusionImageFrame.CopyPixelDataTo(this.depthFloatFrameDepthPixels);

            // Calculate color of pixels based on depth of each pixel
            float range = 4.0f;
            float oneOverRange = (1.0f / range) * 256.0f;
            float minRange = 0.0f;

            Parallel.For(
            0,
            this.frameSource.DepthFrame.Height,
            y =>
            {
                int index = y * this.frameSource.DepthFrame.Width;
                for (int x = 0; x < this.frameSource.DepthFrame.Width; ++x, ++index)
                {
                    float depth = this.depthFloatFrameDepthPixels[index];
                    int intensity = (depth >= minRange) ? ((byte)((depth - minRange) * oneOverRange)) : 0;

                    this.depthFloatFramePixelsArgb[index] = (255 << 24) | (intensity << 16) | (intensity << 8) | intensity; // set blue, green, red
                }
            });

            // Copy colored pixels to bitmap
            bitmap.WritePixels(
                        new Int32Rect(0, 0, this.frameSource.DepthFrame.Width, this.frameSource.DepthFrame.Height),
                        this.depthFloatFramePixelsArgb,
                        bitmap.PixelWidth * sizeof(int),
                        0);
        }

        /// <summary>
        /// Render Fusion color frame to UI
        /// </summary>
        /// <param name="bitmap">Bitmap contains color float frame data for rendering</param>
        /// <param name="image">UI image component to render color float frame to</param>
        private void RenderKinectColorImage(ref WriteableBitmap bitmap, System.Windows.Controls.Image image)
        {
            if (null == this.frameSource.ColorFrame.RawPixels)
            {
                return;
            }

            if (null == bitmap || this.frameSource.ColorWidth != bitmap.Width || this.frameSource.ColorHeight != bitmap.Height)
            {
                // Create bitmap of correct format
                bitmap = new WriteableBitmap(this.frameSource.ColorWidth, this.frameSource.ColorHeight, 96.0, 96.0, PixelFormats.Bgr32, null);

                // Set bitmap as source to UI image object
                image.Source = bitmap;
            }

            Parallel.For(
                0,
                this.frameSource.ColorHeight,
                y =>
                {
                    int index = y * this.frameSource.ColorWidth;
                    for (int x = 0; x < this.frameSource.ColorWidth; ++x, ++index)
                    {
                        int bufferIndexBase = 4 * (y * this.frameSource.ColorWidth + this.frameSource.ColorWidth - x - 1);
                        this.colorFramePixelsArgb[index] = (255 << 24) | (this.frameSource.ColorFrame.RawPixels[bufferIndexBase + 2] << 16) | (this.frameSource.ColorFrame.RawPixels[bufferIndexBase + 1] << 8) | this.frameSource.ColorFrame.RawPixels[bufferIndexBase + 0]; // set blue, green, red
                    }
                });

            // Copy colored pixels to bitmap
            bitmap.WritePixels(
                        new Int32Rect(0, 0, this.frameSource.ColorWidth, this.frameSource.ColorHeight),
                        this.colorFramePixelsArgb,
                        bitmap.PixelWidth * sizeof(int),
                        0);
        }

        /// <summary>
        /// Called on each render of WPF (usually around 60Hz)
        /// </summary>
        /// <param name="sender">Object sending the event</param>
        /// <param name="e">Event arguments</param>
        private void CompositionTargetRendering(object sender, EventArgs e)
        {
            // If the viewChanged flag is used so we only raycast the volume when something changes
            // When reconstructing we call RenderReconstruction manually for every integrated depth frame (see ReconstructDepthData)
            if (this.viewChanged)
            {
                this.viewChanged = false;
                this.RenderReconstruction();
            }
        }

        /// <summary>
        /// Event raised when the mouse updates the graphics camera transformation for the virtual camera
        /// Here we set the viewChanged flag to true, to cause a volume render when the WPF composite update event occurs
        /// </summary>
        /// <param name="sender">Event generator</param>
        /// <param name="e">Event parameter</param>
        private void OnVirtualCameraTransformationChanged(object sender, EventArgs e)
        {
            // Update the stored virtual camera pose
            this.virtualCameraPose = new CameraPose(this.virtualCamera.WorldToCameraMatrix4.ToPipelineMatrix());
            this.viewChanged = true;
        }

        /// <summary>
        /// Check if the gap between 2 frames has reached reset time threshold. If yes, reset the reconstruction
        /// </summary>
        /// <param name="frameTimestamp">The frame's timestamp.</param>
        private void CheckResetTimeStamp(TimeSpan frameTimestamp)
        {
            if (!this.lastFrameTimestamp.Equals(TimeSpan.Zero))
            {
                long timeThreshold = (ReconstructionProcessor.Amp == ProcessorType) ? ResetOnTimeStampSkippedMillisecondsGPU : ResetOnTimeStampSkippedMillisecondsCPU;

                // Calculate skipped milliseconds between 2 frames
                long skippedMilliseconds = (long)frameTimestamp.Subtract(this.lastFrameTimestamp).Duration().TotalMilliseconds;

                if (skippedMilliseconds >= timeThreshold)
                {
                    this.ShowStatusMessage(Properties.Resources.ResetVolume);
                    this.resetReconstruction = true;
                }
            }

            // Set timestamp of last frame
            this.lastFrameTimestamp = frameTimestamp;
        }

        /// <summary>
        /// Reset reconstruction object to initial state
        /// </summary>
        private void ResetReconstruction()
        {
            if (null == this.frameSource)
            {
                return;
            }

            // Reset tracking error counter
            poseAlignmentModule.ResetTracking();

            // Set the world-view transform to identity, so the world origin is the initial camera location.
            this.currentCameraPose = new CameraPose();

            // Reset volume
            if (null != this.volume)
            {
                try
                {
                    // Translate the reconstruction volume location away from the world origin by an amount equal
                    // to the minimum depth threshold. This ensures that some depth signal falls inside the volume.
                    // If set false, the default world origin is set to the center of the front face of the 
                    // volume, which has the effect of locating the volume directly in front of the initial camera
                    // position with the +Z axis into the volume along the initial camera direction of view.
                    if (this.translateResetPoseByMinDepthThreshold)
                    {
                        Tetzlaff.ReflectanceAcquisition.Pipeline.Math.Matrix4 worldToVolumeTransform = this.defaultWorldToVolumeTransform;

                        // Translate the volume in the Z axis by the minDepthClip distance
                        float minDist = (this.frameSource.MinDepthClip < this.frameSource.MaxDepthClip) ? this.frameSource.MinDepthClip : this.frameSource.MaxDepthClip;
                        worldToVolumeTransform.M43 -= minDist * this.voxelsPerMeter;

                        this.volume.ResetReconstruction(this.currentCameraPose, worldToVolumeTransform); 
                    }
                    else
                    {
                        this.volume.ResetReconstruction(this.currentCameraPose);
                    }

                    poseAlignmentModule.ResetTracking();
                    this.ResetColorImage();
                }
                catch (InvalidOperationException)
                {
                    this.ShowStatusMessage(Properties.Resources.ResetFailed);
                }
            }

            // Update manual reset information to status bar
            this.ShowStatusMessage(Properties.Resources.ResetVolume);
        }

        /// <summary>
        /// Re-create the reconstruction object
        /// </summary>
        /// <returns>Indicate success or failure</returns>
        private bool RecreateReconstruction()
        {
            // Check if sensor has been initialized
            if (null == this.frameSource)
            {
                return false;
            }

            if (null != this.volume)
            {
                this.volume.Dispose();
                this.volume = null;
            }

            try
            {
                ReconstructionParameters volParam = new ReconstructionParameters(this.voxelsPerMeter, this.voxelsX, this.voxelsY, this.voxelsZ);

                // Set the world-view transform to identity, so the world origin is the initial camera location.
                this.currentCameraPose = new CameraPose();

                this.volume = new KinectFusionReconstructionVolume(ColorReconstruction.FusionCreateReconstruction(volParam, ProcessorType, DeviceToUse, this.currentCameraPose.Matrix.ToKinectMatrix()));

                this.defaultWorldToVolumeTransform = this.volume.CurrentWorldToVolumeTransform;

                if (this.translateResetPoseByMinDepthThreshold)
                {
                    this.ResetReconstruction();
                }
                else
                {
                    poseAlignmentModule.ResetTracking();
                    this.ResetColorImage();
                }

                // Map world X axis to blue channel, Y axis to green channel and Z axis to red channel,
                // normalizing each to the range [0, 1]. We also add a shift of 0.5 to both X,Y channels
                // as the world origin starts located at the center of the front face of the volume,
                // hence we need to map negative x,y world vertex locations to positive color values.
                this.worldToBGRTransform = Matrix4.Identity;
                this.worldToBGRTransform.M11 = this.voxelsPerMeter / this.voxelsX;
                this.worldToBGRTransform.M22 = this.voxelsPerMeter / this.voxelsY;
                this.worldToBGRTransform.M33 = this.voxelsPerMeter / this.voxelsZ;
                this.worldToBGRTransform.M41 = 0.5f;
                this.worldToBGRTransform.M42 = 0.5f;
                this.worldToBGRTransform.M44 = 1.0f;

                // Update the graphics volume cube rendering
                if (this.volumeGraphics)
                {
                    Dispatcher.BeginInvoke(
                        (Action)(() =>
                            {
                                // re-create the volume cube display with the new size
                                this.RemoveVolumeCube3DGraphics();
                                this.DisposeVolumeCube3DGraphics();
                                this.CreateCube3DGraphics(volumeCubeLineColor, LineThickness, new Vector3D(0, 0, 0));
                                this.AddVolumeCube3DGraphics();
                            }));
                }

                // Signal that a render is required
                this.viewChanged = true;

                return true;
            }
            catch (ArgumentException)
            {
                this.volume = null;
                this.ShowStatusMessage(Properties.Resources.VolumeResolution);
            }
            catch (InvalidOperationException ex)
            {
                this.volume = null;
                this.ShowStatusMessage(ex.Message);
            }
            catch (DllNotFoundException)
            {
                this.volume = null;
                this.ShowStatusMessage(Properties.Resources.MissingPrerequisite);
            }
            catch (OutOfMemoryException)
            {
                this.volume = null;
                this.ShowStatusMessage(Properties.Resources.OutOfMemory);
            }

            return false;
        }

        /// <summary>
        /// Reset the mapped color image on reset or re-create of volume
        /// </summary>
        private void ResetColorImage()
        {
            if (null != this.resampledColorFrameDepthAligned && null != this.resampledColorImagePixelsAlignedToDepth)
            {
                // Clear the mapped color image
                Array.Clear(this.resampledColorImagePixelsAlignedToDepth, 0, this.resampledColorImagePixelsAlignedToDepth.Length);
                this.resampledColorFrameDepthAligned.CopyPixelDataFrom(this.resampledColorImagePixelsAlignedToDepth);
            }
        }

        /// <summary>
        /// Handler for click event from "Reset Virtual Camera" button
        /// </summary>
        /// <param name="sender">Event sender</param>
        /// <param name="e">Event arguments</param>
        private void ResetCameraButtonClick(object sender, RoutedEventArgs e)
        {
            if (null != this.virtualCamera)
            {
                this.virtualCamera.Reset();
                this.viewChanged = true;
            }
        }

        /// <summary>
        /// Handler for click event from "Reset Reconstruction" button
        /// </summary>
        /// <param name="sender">Event sender</param>
        /// <param name="e">Event arguments</param>
        private void ResetReconstructionButtonClick(object sender, RoutedEventArgs e)
        {
            if (null == this.frameSource)
            {
                return;
            }

            // Signal the worker thread to reset the volume
            this.resetReconstruction = true;

            // Update manual reset information to status bar
            this.ShowStatusMessage(Properties.Resources.ResetVolume);
        }

        /// <summary>
        /// Handler for click event from "Create Mesh" button
        /// </summary>
        /// <param name="sender">Event sender</param>
        /// <param name="e">Event arguments</param>
        private void CreateMeshButtonClick(object sender, RoutedEventArgs e)
        {
            // Mark the start time of saving mesh
            DateTime beginning = DateTime.UtcNow;

            try
            {
                this.ShowStatusMessage(Properties.Resources.SavingMesh);

                GeometryMeshBase mesh = null;

                lock (this.volumeLock)
                {
                    this.savingMesh = true;

                    if (null == this.volume)
                    {
                        this.ShowStatusMessage(Properties.Resources.MeshNullVolume);
                        return;
                    }

                    mesh = this.volume.CalculateMesh(1);
                }

                if (null == mesh)
                {
                    this.ShowStatusMessage(Properties.Resources.ErrorSaveMesh);
                    return;
                }

                Microsoft.Win32.SaveFileDialog dialog = new Microsoft.Win32.SaveFileDialog();
                dialog.InitialDirectory = this.outputDirectory;

                if (true == this.stlFormat.IsChecked)
                {
                    dialog.FileName = "MeshedReconstruction.stl";
                    dialog.Filter = "STL Mesh Files|*.stl|All Files|*.*";
                }
                else if (true == this.objFormat.IsChecked)
                {
                    dialog.FileName = "MeshedReconstruction.obj";
                    dialog.Filter = "OBJ Mesh Files|*.obj|All Files|*.*";
                }
                else
                {
                    dialog.FileName = "MeshedReconstruction.ply";
                    dialog.Filter = "PLY Mesh Files|*.ply|All Files|*.*";
                }

                if (true == dialog.ShowDialog())
                {
                    if (true == this.stlFormat.IsChecked)
                    {
                        using (BinaryWriter writer = new BinaryWriter(dialog.OpenFile()))
                        {
                            // Default to flip Y,Z coordinates on save
                            mesh.SaveBinaryStlMesh(writer, true);
                        }
                    }
                    else if (true == this.objFormat.IsChecked)
                    {
                        using (StreamWriter writer = new StreamWriter(dialog.FileName))
                        {
                            // Default to flip Y,Z coordinates on save
                            mesh.SaveAsciiObjMesh(writer, true);
                        }
                    }
                    else
                    {
                        using (StreamWriter writer = new StreamWriter(dialog.FileName))
                        {
                            // Default to flip Y,Z coordinates on save
                            mesh.SaveAsciiPlyMesh(writer, true);
                        }
                    }

                    this.ShowStatusMessage(Properties.Resources.MeshSaved);
                }
                else
                {
                    this.ShowStatusMessage(Properties.Resources.MeshSaveCanceled);
                }
            }
            catch (ArgumentException)
            {
                this.ShowStatusMessage(Properties.Resources.ErrorSaveMesh);
            }
            catch (InvalidOperationException)
            {
                this.ShowStatusMessage(Properties.Resources.ErrorSaveMesh);
            }
            catch (IOException)
            {
                this.ShowStatusMessage(Properties.Resources.ErrorSaveMesh);
            }
            catch (OutOfMemoryException)
            {
                this.ShowStatusMessage(Properties.Resources.ErrorSaveMeshOutOfMemory);
            }
            finally
            {
                // Update timestamp of last frame to avoid auto reset reconstruction
                this.lastFrameTimestamp += DateTime.UtcNow - beginning;

                this.savingMesh = false;
            }
        }

        /// <summary>
        /// Handler for volume setting changing event
        /// </summary>
        /// <param name="sender">Event sender</param>
        /// <param name="e">Event argument</param>
        private void VolumeSettingsChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            // Signal the worker thread to recreate the volume
            this.recreateReconstruction = true;
        }

        /// <summary>
        /// Update the virtual camera transform from this process.
        /// </summary>
        private void UpdateVirtualCameraTransform()
        {
            // Update just the virtual camera pose from the tracked camera
            // We do not update the frustum here, as we do not render it when in Kinect camera view.
            this.virtualCamera.WorldToCameraMatrix4 = this.currentCameraPose.Matrix.ToKinectMatrix();
        }

        /// <summary>
        /// Create an axis-aligned coordinate cross for rendering in the WPF3D coordinate system. 
        /// Red is the +X axis, Green is the +Y axis, Blue is the +Z axis
        /// </summary>
        /// <param name="crossOrigin">The origin of the coordinate cross in world space.</param>
        /// <param name="axisSize">The size of the axis in m.</param>
        /// <param name="thickness">The thickness of the lines in screen pixels.</param>
        private void CreateAxisAlignedCoordinateCross3DGraphics(Point3D crossOrigin, float axisSize, int thickness)
        {
            this.axisX = new ScreenSpaceLines3D();

            this.axisX.Points = new Point3DCollection();
            this.axisX.Points.Add(crossOrigin);
            this.axisX.Points.Add(new Point3D(crossOrigin.X + axisSize, crossOrigin.Y, crossOrigin.Z));

            this.axisX.Thickness = 2;
            this.axisX.Color = System.Windows.Media.Color.FromArgb(200, 255, 0, 0);   // Red (X)

            this.axisY = new ScreenSpaceLines3D();

            this.axisY.Points = new Point3DCollection();
            this.axisY.Points.Add(crossOrigin);
            this.axisY.Points.Add(new Point3D(crossOrigin.X, crossOrigin.Y + axisSize, crossOrigin.Z));

            this.axisY.Thickness = 2;
            this.axisY.Color = System.Windows.Media.Color.FromArgb(200, 0, 255, 0);   // Green (Y)

            this.axisZ = new ScreenSpaceLines3D();

            this.axisZ.Points = new Point3DCollection();
            this.axisZ.Points.Add(crossOrigin);
            this.axisZ.Points.Add(new Point3D(crossOrigin.X, crossOrigin.Y, crossOrigin.Z + axisSize));

            this.axisZ.Thickness = thickness;
            this.axisZ.Color = System.Windows.Media.Color.FromArgb(200, 0, 0, 255);   // Blue (Z)
        }

        /// <summary>
        /// Add the coordinate cross axes to the visual tree
        /// </summary>
        private void AddAxisAlignedCoordinateCross3DGraphics()
        {
            if (this.haveAddedCoordinateCross)
            {
                return;
            }

            if (null != this.axisX)
            {
                this.GraphicsViewport.Children.Add(this.axisX);

                this.haveAddedCoordinateCross = true;
            }

            if (null != this.axisY)
            {
                this.GraphicsViewport.Children.Add(this.axisY);
            }

            if (null != this.axisZ)
            {
                this.GraphicsViewport.Children.Add(this.axisZ);
            }
        }

        /// <summary>
        /// Remove the coordinate cross axes from the visual tree
        /// </summary>
        private void RemoveAxisAlignedCoordinateCross3DGraphics()
        {
            if (null != this.axisX)
            {
                this.GraphicsViewport.Children.Remove(this.axisX);
            }

            if (null != this.axisY)
            {
                this.GraphicsViewport.Children.Remove(this.axisY);
            }

            if (null != this.axisZ)
            {
                this.GraphicsViewport.Children.Remove(this.axisZ);
            }

            this.haveAddedCoordinateCross = false;
        }

        /// <summary>
        /// Dispose the coordinate cross axes from the visual tree
        /// </summary>
        private void DisposeAxisAlignedCoordinateCross3DGraphics()
        {
            if (this.haveAddedCoordinateCross)
            {
                this.RemoveAxisAlignedCoordinateCross3DGraphics();
            }

            if (null != this.axisX)
            {
                this.axisX.Dispose();
                this.axisX = null;
            }

            if (null != this.axisY)
            {
                this.axisY.Dispose();
                this.axisY = null;
            }

            if (null != this.axisZ)
            {
                this.axisZ.Dispose();
                this.axisZ = null;
            }
        }

        /// <summary>
        /// Create an axis-aligned volume cube for rendering.
        /// </summary>
        /// <param name="color">The color of the volume cube.</param>
        /// <param name="thickness">The thickness of the lines in screen pixels.</param>
        /// <param name="translation">World to volume translation vector.</param>
        private void CreateCube3DGraphics(System.Windows.Media.Color color, int thickness, Vector3D translation)
        {
            // Scaler for cube size
            float cubeSizeScaler = 1.0f;

            // Before we created a volume which contains the head
            // Here we create a graphical representation of this volume cube
            float oneOverVpm = 1.0f / this.voxelsPerMeter;

            // This cube is world axis aligned
            float cubeSideX = this.voxelsX * oneOverVpm * cubeSizeScaler;
            float halfSideX = cubeSideX * 0.5f;

            float cubeSideY = this.voxelsY * oneOverVpm * cubeSizeScaler;
            float halfSideY = cubeSideY * 0.5f;

            float cubeSideZ = this.voxelsZ * oneOverVpm * cubeSizeScaler;
            float halfSideZ = cubeSideZ * 0.5f;

            // The translation vector is from the origin to the volume front face
            // And here we describe the translation Z as from the origin to the cube center
            // So we continue to translate half volume size align Z
            translation.Z -= halfSideZ / cubeSizeScaler;

            this.volumeCube = new ScreenSpaceLines3D();
            this.volumeCube.Points = new Point3DCollection();

            // Front face
            // TL front - TR front
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, halfSideY + translation.Y, -halfSideZ + translation.Z));

            // TR front - BR front
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, -halfSideY + translation.Y, -halfSideZ + translation.Z));

            // BR front - BL front
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, -halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, -halfSideY + translation.Y, -halfSideZ + translation.Z));

            // BL front - TL front
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, -halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, -halfSideZ + translation.Z));

            // Rear face
            // TL rear - TR rear
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));

            // TR rear - BR rear
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, -halfSideY + translation.Y, halfSideZ + translation.Z));

            // BR rear - BL rear
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, -halfSideY + translation.Y, halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, -halfSideY + translation.Y, halfSideZ + translation.Z));

            // BL rear - TL rear
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, -halfSideY + translation.Y, halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));

            // Connecting lines
            // TL front - TL rear
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));

            // TR front - TR rear
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));

            // BR front - BR rear
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, -halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(halfSideX + translation.X, -halfSideY + translation.Y, halfSideZ + translation.Z));

            // BL front - BL rear
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, -halfSideY + translation.Y, -halfSideZ + translation.Z));
            this.volumeCube.Points.Add(new Point3D(-halfSideX + translation.X, -halfSideY + translation.Y, halfSideZ + translation.Z));

            this.volumeCube.Thickness = thickness;
            this.volumeCube.Color = color;

            this.volumeCubeAxisX = new ScreenSpaceLines3D();

            this.volumeCubeAxisX.Points = new Point3DCollection();
            this.volumeCubeAxisX.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));
            this.volumeCubeAxisX.Points.Add(new Point3D(-halfSideX + 0.1f + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));

            this.volumeCubeAxisX.Thickness = thickness + 2;
            this.volumeCubeAxisX.Color = System.Windows.Media.Color.FromArgb(200, 255, 0, 0);   // Red (X)

            this.volumeCubeAxisY = new ScreenSpaceLines3D();

            this.volumeCubeAxisY.Points = new Point3DCollection();
            this.volumeCubeAxisY.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));
            this.volumeCubeAxisY.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY - 0.1f + translation.Y, halfSideZ + translation.Z));

            this.volumeCubeAxisY.Thickness = thickness + 2;
            this.volumeCubeAxisY.Color = System.Windows.Media.Color.FromArgb(200, 0, 255, 0);   // Green (Y)

            this.volumeCubeAxisZ = new ScreenSpaceLines3D();

            this.volumeCubeAxisZ.Points = new Point3DCollection();
            this.volumeCubeAxisZ.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, halfSideZ + translation.Z));
            this.volumeCubeAxisZ.Points.Add(new Point3D(-halfSideX + translation.X, halfSideY + translation.Y, halfSideZ - 0.1f + translation.Z));

            this.volumeCubeAxisZ.Thickness = thickness + 2;
            this.volumeCubeAxisZ.Color = System.Windows.Media.Color.FromArgb(200, 0, 0, 255);   // Blue (Z)
        }

        /// <summary>
        /// Add the volume cube and axes to the visual tree
        /// </summary>
        private void AddVolumeCube3DGraphics()
        {
            if (this.haveAddedVolumeCube)
            {
                return;
            }

            if (null != this.volumeCube)
            {
                this.GraphicsViewport.Children.Add(this.volumeCube);

                this.haveAddedVolumeCube = true;
            }

            if (null != this.volumeCubeAxisX)
            {
                this.GraphicsViewport.Children.Add(this.volumeCubeAxisX);
            }

            if (null != this.volumeCubeAxisY)
            {
                this.GraphicsViewport.Children.Add(this.volumeCubeAxisY);
            }

            if (null != this.volumeCubeAxisZ)
            {
                this.GraphicsViewport.Children.Add(this.volumeCubeAxisZ);
            }
        }

        /// <summary>
        /// Remove the volume cube and axes from the visual tree
        /// </summary>
        private void RemoveVolumeCube3DGraphics()
        {
            if (null != this.volumeCube)
            {
                this.GraphicsViewport.Children.Remove(this.volumeCube);
            }

            if (null != this.volumeCubeAxisX)
            {
                this.GraphicsViewport.Children.Remove(this.volumeCubeAxisX);
            }

            if (null != this.volumeCubeAxisY)
            {
                this.GraphicsViewport.Children.Remove(this.volumeCubeAxisY);
            }

            if (null != this.volumeCubeAxisZ)
            {
                this.GraphicsViewport.Children.Remove(this.volumeCubeAxisZ);
            }

            this.haveAddedVolumeCube = false;
        }

        /// <summary>
        /// Dispose the volume cube and axes
        /// </summary>
        private void DisposeVolumeCube3DGraphics()
        {
            if (this.haveAddedVolumeCube)
            {
                this.RemoveVolumeCube3DGraphics();
            }

            if (null != this.volumeCube)
            {
                this.volumeCube.Dispose();
                this.volumeCube = null;
            }

            if (null != this.volumeCubeAxisX)
            {
                this.volumeCubeAxisX.Dispose();
                this.volumeCubeAxisX = null;
            }

            if (null != this.volumeCubeAxisY)
            {
                this.volumeCubeAxisY.Dispose();
                this.volumeCubeAxisY = null;
            }

            if (null != this.volumeCubeAxisZ)
            {
                this.volumeCubeAxisZ.Dispose();
                this.volumeCubeAxisZ = null;
            }
        }

        /// <summary>
        /// Show high priority messages in status bar
        /// </summary>
        /// <param name="message">Message to show on status bar</param>
        private void ShowStatusMessage(string message)
        {
            this.Dispatcher.BeginInvoke(
                (Action)(() =>
                {
                    this.ResetFps();

                    if ((DateTime.Now - this.lastStatusTimestamp).Seconds >= StatusBarInterval)
                    {
                        this.statusBarText.Text = message;
                        this.lastStatusTimestamp = DateTime.Now;
                    }
                    else
                    {
                        this.statusMessageQueue.Enqueue(message);
                    }
                }));
        }

        /// <summary>
        /// Show low priority messages in the status bar. Low priority messages do not reset the fps counter,
        /// and will only be displayed when high priority message has at least StatusBarInterval seconds shown to the user.
        /// Messages that comes in burst or appear extremely frequently should be considered low priority.
        /// </summary>
        /// <param name="message">Message to show on status bar</param>
        private void ShowStatusMessageLowPriority(string message)
        {
            this.Dispatcher.BeginInvoke(
                (Action)(() =>
                {
                    // Make sure the high prioirty messages has at least StatusBarInterval seconds shown to the user.
                    if ((DateTime.Now - this.lastStatusTimestamp).Seconds >= StatusBarInterval)
                    {
                        this.statusBarText.Text = message;
                    }
                }));
        }

        /// <summary>
        /// Allocate the frame buffers and memory used in the process for Kinect Fusion
        /// </summary>
        private void AllocateKinectFusionResources()
        {
            this.SafeDisposeFusionResources();

            // Allocate color frame for color data from Kinect mapped into depth frame
            this.resampledColorFrameDepthAligned = new FusionColorImageFrame(this.frameSource.DepthWidth, this.frameSource.DepthHeight);

            // Allocate point cloud frame
            this.raycastPointCloudFrame = new FusionPointCloudImageFrame(this.frameSource.DepthWidth, this.frameSource.DepthHeight);

            // Allocate shaded surface frame
            this.shadedSurfaceFrame = new FusionColorImageFrame(this.frameSource.DepthWidth, this.frameSource.DepthHeight);

            // Allocate shaded surface normals frame
            this.shadedSurfaceNormalsFrame = new FusionColorImageFrame(this.frameSource.DepthWidth, this.frameSource.DepthHeight);

            int depthImageSize = this.frameSource.DepthWidth * this.frameSource.DepthHeight;
            int colorImageSize = this.frameSource.ColorWidth * this.frameSource.ColorHeight;

            // Create float pixel array
            this.depthFloatFrameDepthPixels = new float[depthImageSize];

            // Create float pixel array
            this.deltaFromReferenceFrameFloatPixels = new float[depthImageSize];

            // Create colored pixel array of correct format
            this.depthFloatFramePixelsArgb = new int[depthImageSize];

            // Create colored pixel array of correct format
            this.colorFramePixelsArgb = new int[colorImageSize];

            // Allocate the depth-color mapping points
            this.colorCoordinates = new ColorSpacePoint[depthImageSize];

            // Allocate color points re-sampled to depth size mapped into depth frame of reference
            this.resampledColorImagePixelsAlignedToDepth = new int[depthImageSize];
       }

        /// <summary>
        /// Dispose fusion resources safely
        /// </summary>
        private void SafeDisposeFusionResources()
        {
            if (null != this.resampledColorFrameDepthAligned)
            {
                this.resampledColorFrameDepthAligned.Dispose();
            }

            if (null != this.raycastPointCloudFrame)
            {
                this.raycastPointCloudFrame.Dispose();
            }

            if (null != this.shadedSurfaceFrame)
            {
                this.shadedSurfaceFrame.Dispose();
            }

            if (null != this.shadedSurfaceNormalsFrame)
            {
                this.shadedSurfaceNormalsFrame.Dispose();
            }
        }

        /// <summary>
        /// A variable used to block the main thread until the image saving thread is done with any shared resources
        /// </summary>
        private bool canProceed = true;

        /// <summary>
        /// Saves the current image in the color buffer as a view for the light field being created
        /// </summary>
        private void SaveLightFieldView()
        {
            if (this.captureColor)
            {
                // Quickly copy the color buffer directly so that it can be released and other threads can use it
                IColorFrame colorFrameCopy = frameSource.ColorFrame.Clone();

                // Indicate that the main thread can proceed, since we have copied the view index and the color buffer.
                canProceed = true;

                // Yield so that this thread doesn't hog the CPU, hurting the framerate
                Thread.Yield();

                lightFieldIntegration.IntegrateReflectance(colorFrameCopy, lightField, currentCameraPose, poseAlignmentModule.AlignmentEnergy);
            }
            else canProceed = true; // Let the main thread proceed if we aren't saving the image
        }

        ///// <summary>
        ///// Add an entry for the current camera pose to the view set (vset) file
        ///// </summary>
        ///// <param name="m">The camera-to-world transformation matrix</param>
        ///// <returns>true if added successfully, false if no entry was added</returns>
        //private bool AddViewSetEntry(Matrix4 m)
        //{
        //    if (this.captureColor)
        //    {
        //        return lightFieldIntegration.IntegrateReflectance();
        //    }
        //    else return false;
        //}

        /// <summary>
        /// Prompts the user for a directory in which to create a light field, and opens the corresponding view set (vset) file to be written to.
        /// Also opens a log file for debugging purposes.
        /// </summary>
        private void OpenViewSetFile()
        {
            System.Windows.Forms.FolderBrowserDialog dialog = new System.Windows.Forms.FolderBrowserDialog();
            dialog.Description = "Select a light field output directory:";
            if (dialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                outputDirectory = dialog.SelectedPath;
                this.lightField = new LightFieldDirectory(outputDirectory);
                this.lightField.Force1To1AspectRatio = this.ForceLightField1To1AspectRatio;
            }
        }
    }
}
