using Microsoft.Kinect;
using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public class KinectFusionDepthFrame : IKinectFusionDepthFrame
    {
        public int Width
        {
            get
            {
                return this.FusionImageFrame.Width;
            }
        }

        public int Height
        {
            get
            {
                return this.FusionImageFrame.Height;
            }
        }

        public float MinDepthClip { get; private set; }
        public float MaxDepthClip { get; private set; }
        public bool MirrorDepth { get; private set; }

        public FusionFloatImageFrame FusionImageFrame { get; private set; }

        public IRawDepthFrameFixed RawDepthFrameFixed { get; private set; }

        public void TakeRawDepthFrameFixed(IRawDepthFrameFixed rawFixedDepthFrame, float minDepthClip, float maxDepthClip, bool mirrorDepth)
        {
            this.RawDepthFrameFixed = rawFixedDepthFrame;

            if (rawFixedDepthFrame != null)
            {
                this.MinDepthClip = minDepthClip;
                this.MaxDepthClip = maxDepthClip;
                this.MirrorDepth = mirrorDepth;

                FusionDepthProcessor.DepthToDepthFloatFrame(
                    this.RawDepthFrameFixed.RawPixels,
                    this.Width,
                    this.Height,
                    this.FusionImageFrame,
                    this.MinDepthClip,
                    this.MaxDepthClip,
                    this.MirrorDepth);
            }
        }

        private IRawDepthFrameFloat _downsampledFloatPixels = null;

        public KinectFusionDepthFrame(int width, int height)
        {
            Contract.Ensures(FusionImageFrame != null);
            FusionImageFrame = new FusionFloatImageFrame(width, height);
            this.RawDepthFrameFixed = null;
        }

        public void Dispose()
        {
            FusionImageFrame.Dispose();
        }

        /// <summary>
        /// Downsample depth pixels with nearest neighbor
        /// </summary>
        /// <param name="dest">The destination depth image.</param>
        /// <param name="factor">The downsample factor (2=x/2,y/2, 4=x/4,y/4, 8=x/8,y/8, 16=x/16,y/16).</param>
        public unsafe void DownsampleNearestNeighbor(IKinectFusionDepthFrame dest, int factor, bool mirror)
        {
            if (this.RawDepthFrameFixed == null)
            {
                throw new RawPixelsNotAvailableException("Raw fixed-point depth pixels must be available in order to downsample.");
            }

            if (null == dest)
            {
                throw new ArgumentException("inputs null");
            }

            if (false == (2 == factor || 4 == factor || 8 == factor || 16 == factor))
            {
                throw new ArgumentException("factor != 2, 4, 8 or 16");
            }

            int downsampleWidth = this.Width / factor;
            int downsampleHeight = this.Height / factor;

            if (dest.Width != downsampleWidth || dest.Height != downsampleHeight)
            {
                throw new ArgumentException("dest != downsampled image size");
            }
            
            if (_downsampledFloatPixels == null ||
                _downsampledFloatPixels.Height != downsampleHeight ||
                _downsampledFloatPixels.Width != downsampleWidth)
            {
                // Lazy initialization
                _downsampledFloatPixels = new RawDepthFrameFloat(downsampleWidth, downsampleHeight);
            }

            if (mirror)
            {
                fixed (ushort* rawDepthPixelPtr = this.RawDepthFrameFixed.RawPixels)
                {
                    ushort* rawDepthPixels = (ushort*)rawDepthPixelPtr;

                    Parallel.For(
                        0,
                        downsampleHeight,
                        y =>
                        {
                            int destIndex = y * downsampleWidth;
                            int sourceIndex = y * this.Width * factor;

                            for (int x = 0; x < downsampleWidth; ++x, ++destIndex, sourceIndex += factor)
                            {
                                // Copy depth value
                                _downsampledFloatPixels.RawPixels[destIndex] = (float)rawDepthPixels[sourceIndex] * 0.001f;
                            }
                        });
                }
            }
            else
            {
                fixed (ushort* rawDepthPixelPtr = this.RawDepthFrameFixed.RawPixels)
                {
                    ushort* rawDepthPixels = (ushort*)rawDepthPixelPtr;

                    // Horizontal flip the color image as the standard depth image is flipped internally in Kinect Fusion
                    // to give a viewpoint as though from behind the Kinect looking forward by default.
                    Parallel.For(
                        0,
                        downsampleHeight,
                        y =>
                        {
                            int flippedDestIndex = (y * downsampleWidth) + (downsampleWidth - 1);
                            int sourceIndex = y * this.Width * factor;

                            for (int x = 0; x < downsampleWidth; ++x, --flippedDestIndex, sourceIndex += factor)
                            {
                                // Copy depth value
                                _downsampledFloatPixels.RawPixels[flippedDestIndex] = (float)rawDepthPixels[sourceIndex] * 0.001f;
                            }
                        });
                }
            }

            dest.FusionImageFrame.CopyPixelDataFrom(this._downsampledFloatPixels.RawPixels);
        }
    }
}
