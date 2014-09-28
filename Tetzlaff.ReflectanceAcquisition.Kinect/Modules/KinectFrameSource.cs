using Microsoft.Kinect;
using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Kinect.DataModels;
using Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Modules;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Modules
{
    public class KinectFrameSource : IFrameSource<IKinectFusionDepthFrame, IRawColorFrame>
    {
        /// <summary>
        /// This is necessary to allow us to swap buffers transparently
        /// </summary>
        private class _RawColorFrameWrapper : IRawColorFrame
        {
            public IRawColorFrame BaseFrame { get; set; }

            public _RawColorFrameWrapper(IRawColorFrame baseFrame)
            {
                this.BaseFrame = baseFrame;
            }

            public byte[] RawPixels
            {
                get
                {
                    return BaseFrame.RawPixels;
                }
            }

            public int Width
            {
                get
                {
                    return BaseFrame.Width;
                }
            }

            public int Height
            {
                get
                {
                    return BaseFrame.Height;
                }
            }
        }

        private KinectFusionDepthFrame _depthFrameFront;
        private IRawDepthFrameFixed _rawDepthFrameFront;
        private IRawDepthFrameFixed _rawDepthFrameBack;

        /// <summary>
        /// Intermediate storage for the depth float data converted from depth image frame
        /// </summary>
        public IKinectFusionDepthFrame DepthFrame
        { 
            get
            {
                return _depthFrameFront;
            }
        }

        private _RawColorFrameWrapper _colorFrameFrontWrapper;
        private IRawColorFrame _colorFrameBack;

        /// <summary>
        /// Intermediate storage for the color data
        /// </summary>
        public IRawColorFrame ColorFrame
        {
            get
            {
                return _colorFrameFrontWrapper;
            }
        }

        /// <summary>
        /// Lock object for raw pixel access
        /// </summary>
        private object _backFrameLock = new object();

        /// <summary>
        /// Image Width of depth frame
        /// </summary>
        public int DepthWidth { get; private set; }

        /// <summary>
        /// Image height of depth frame
        /// </summary>
        public int DepthHeight { get; private set; }

        /// <summary>
        /// Count of pixels in the depth frame
        /// </summary>
        public int DepthPixelCount { get; private set; }

        /// <summary>
        /// Horizontal field of view for the depth frame
        /// </summary>
        public float DepthHorizontalFieldOfView { get; private set; }

        /// <summary>
        /// Image width of color frame
        /// </summary>
        public int ColorWidth { get; private set; }

        /// <summary>
        /// Image height of color frame
        /// </summary>
        public int ColorHeight { get; private set; }

        /// <summary>
        /// Count of pixels in the color frame
        /// </summary>
        public int ColorPixelCount { get; private set; }

        /// <summary>
        /// Horizontal field of view for the color frame
        /// </summary>
        public float ColorHorizontalFieldOfView { get; private set; }

        /// <summary>
        /// Gets or sets the timestamp of the current depth frame
        /// </summary>
        public TimeSpan RelativeTime { get; set; }

        /// <summary>
        /// Depth image is mirrored
        /// </summary>
        public bool MirrorDepth { get; set; }

        /// <summary>
        /// Minimum depth distance threshold in meters. Depth pixels below this value will be
        /// returned as invalid (0). Min depth must be positive or 0.
        /// </summary>
        public float MinDepthClip { get; set; }

        /// <summary>
        /// Maximum depth distance threshold in meters. Depth pixels above this value will be
        /// returned as invalid (0). Max depth must be greater than 0.
        /// </summary>
        public float MaxDepthClip { get; set; }

        /// <summary>
        /// Active Kinect sensor
        /// </summary>
        private KinectSensor _sensor;

        /// <summary>
        /// Reader for depth/color/body index frames
        /// </summary>
        private MultiSourceFrameReader _reader;

        public KinectFrameSource() : this(KinectSensor.GetDefault())
        {
        }

        public KinectFrameSource(KinectSensor sensor)
        {
            this._sensor = sensor;

            if (this._sensor == null)
            {
                throw new NoReadyKinectException();
            }

            this.MinDepthClip = FusionDepthProcessor.DefaultMinimumDepth;
            this.MaxDepthClip = FusionDepthProcessor.DefaultMaximumDepth;

            // open the sensor
            this._sensor.Open();

            this._reader = this._sensor.OpenMultiSourceFrameReader(FrameSourceTypes.Depth | FrameSourceTypes.Color);

            FrameDescription depthFrameDescription = this._sensor.DepthFrameSource.FrameDescription;
            this.DepthWidth = depthFrameDescription.Width;
            this.DepthHeight = depthFrameDescription.Height;
            this.DepthPixelCount = this.DepthWidth * this.DepthHeight;
            this.DepthHorizontalFieldOfView = depthFrameDescription.HorizontalFieldOfView;

            FrameDescription colorFrameDescription = this._sensor.ColorFrameSource.FrameDescription;
            this.ColorWidth = colorFrameDescription.Width;
            this.ColorHeight = colorFrameDescription.Height;
            this.ColorPixelCount = this.ColorWidth * this.ColorHeight;
            this.ColorHorizontalFieldOfView = colorFrameDescription.HorizontalFieldOfView;

            // Allocate depth frame
            this._depthFrameFront = new KinectFusionDepthFrame(this.DepthWidth, this.DepthHeight);
            this._rawDepthFrameFront = new RawDepthFrameFixed(this.DepthWidth, this.DepthHeight);
            this._rawDepthFrameBack = new RawDepthFrameFixed(this.DepthWidth, this.DepthHeight);

            // Allocate color frame
            IRawColorFrame colorFrameFront = new RawColorFrame(this.ColorWidth, this.ColorHeight);
            this._colorFrameFrontWrapper = new _RawColorFrameWrapper(colorFrameFront);
            this._colorFrameBack = new RawColorFrame(this.ColorWidth, this.ColorHeight);

            // Add an event handler to be called whenever depth and color both have new data
            this._reader.MultiSourceFrameArrived += this.Reader_MultiSourceFrameArrived;
        }

        public void Dispose()
        {
            if (this._reader != null)
            {
                this._reader.Dispose();
                this._reader = null;
            }

            if (null != this._sensor)
            {
                this._sensor.Close();
                this._sensor = null;
            }

            if (null != this.DepthFrame)
            {
                this._depthFrameFront.Dispose();
            }
        }

        public event Action DepthFrameReady;
        public event Action ColorFrameReady;

        public void RefreshDepthFrame()
        {
            lock(_backFrameLock)
            {
                _depthFrameFront.TakeRawDepthFrameFixed(
                    this._rawDepthFrameBack,
                    this.MinDepthClip,
                    this.MaxDepthClip,
                    this.MirrorDepth);
                IRawDepthFrameFixed tmp = this._rawDepthFrameFront;
                this._rawDepthFrameFront = this._rawDepthFrameBack;
                this._rawDepthFrameBack = tmp;
            }
        }

        public void RefreshColorFrame()
        {
            lock(_backFrameLock)
            {
                IRawColorFrame tmp = this._colorFrameFrontWrapper.BaseFrame;
                _colorFrameFrontWrapper.BaseFrame = this._colorFrameBack;
                this._colorFrameBack = tmp;
            }
        }

        /// <summary>
        /// Event handler for multiSourceFrame arrived event
        /// </summary>
        /// <param name="sender">object sending the event</param>
        /// <param name="e">event arguments</param>
        private void Reader_MultiSourceFrameArrived(object sender, MultiSourceFrameArrivedEventArgs e)
        {
            bool validDepth = false;
            bool validColor = false;

            MultiSourceFrameReference frameReference = e.FrameReference;

            MultiSourceFrame multiSourceFrame = null;
            DepthFrame newDepthFrame = null;
            ColorFrame newColorFrame = null;

            try
            {
                multiSourceFrame = frameReference.AcquireFrame();

                if (multiSourceFrame != null)
                {
                    ColorFrameReference colorFrameReference = multiSourceFrame.ColorFrameReference;
                    DepthFrameReference depthFrameReference = multiSourceFrame.DepthFrameReference;

                    newColorFrame = colorFrameReference.AcquireFrame();
                    newDepthFrame = depthFrameReference.AcquireFrame();

                    if ((newDepthFrame != null) && (newColorFrame != null))
                    {
                        // Save frame timestamp
                        this.RelativeTime = newDepthFrame.RelativeTime;

                        FrameDescription colorFrameDescription = newColorFrame.FrameDescription;
                        int colorWidth = colorFrameDescription.Width;
                        int colorHeight = colorFrameDescription.Height;

                        if (colorWidth == this.ColorFrame.Width && colorHeight == this.ColorFrame.Height)
                        {
                            newColorFrame.CopyConvertedFrameDataToArray(this._colorFrameBack.RawPixels, ColorImageFormat.Bgra);
                            validColor = true;
                        }

                        FrameDescription depthFrameDescription = newDepthFrame.FrameDescription;
                        int depthWidth = depthFrameDescription.Width;
                        int depthHeight = depthFrameDescription.Height;

                        if (depthWidth == this.DepthFrame.Width && depthHeight == this.DepthFrame.Height)
                        {
                            newDepthFrame.CopyFrameDataToArray(this._rawDepthFrameBack.RawPixels);
                            validDepth = true;
                        }
                    }
                }
            }
            catch (Exception)
            {
                // ignore if the frame is no longer available
            }
            finally
            {
                // MultiSourceFrame, DepthFrame, ColorFrame, BodyIndexFrame are IDispoable
                if (newDepthFrame != null)
                {
                    newDepthFrame.Dispose();
                    newDepthFrame = null;
                }

                if (newColorFrame != null)
                {
                    newColorFrame.Dispose();
                    newColorFrame = null;
                }

                if (multiSourceFrame != null)
                {
                    multiSourceFrame = null;
                }
            }

            if (validDepth)
            {
                DepthFrameReady();
            }

            if (validColor)
            {
                ColorFrameReady();
            }
        }
    }
}
