using Microsoft.Kinect.Fusion;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.DataModels
{
    public class KinectDepthFrame : IKinectDepthFrame
    {
        private const int MIN_DOWNSAMPLE_FACTOR = 2;

        /// <summary>
        /// Pixel buffer of depth float frame with pixel data in float format for downsampling
        /// </summary>
        private float[] _downsampledPixels;

        public FusionFloatImageFrame FusionImageFrame { get; private set; }

        public int Width
        {
            get
            {
                return FusionImageFrame.Width;
            }
        }

        public int Height
        {
            get
            {
                return FusionImageFrame.Height;
            }
        }

        private ushort[] _rawPixelData;
        //public ReadOnlyCollection<ushort> RawPixelData
        //{
        //    get
        //    {
        //        return Array.AsReadOnly<ushort>(_rawPixelData);
        //    }
        //}
        public ushort[] Pixels
        {
            get
            {
                return _rawPixelData;
            }
        }

        public KinectDepthFrame(int width, int height)
        {
            Contract.Ensures(FusionImageFrame != null);
            FusionImageFrame = new FusionFloatImageFrame(width, height);

            int depthImageSize = this.Width * this.Height;
            int downsampledDepthImageSize = depthImageSize / (MIN_DOWNSAMPLE_FACTOR * MIN_DOWNSAMPLE_FACTOR);
            _rawPixelData = new ushort[depthImageSize];
            _downsampledPixels = new float[downsampledDepthImageSize];
        }

        public KinectDepthFrame(FusionFloatImageFrame fusionImageFrame)
        {
            Contract.Ensures(FusionImageFrame != null);
            FusionImageFrame = fusionImageFrame;

            int depthImageSize = this.Width * this.Height;
            int downsampledDepthImageSize = depthImageSize / (MIN_DOWNSAMPLE_FACTOR * MIN_DOWNSAMPLE_FACTOR);
            _rawPixelData = new ushort[depthImageSize];
            _downsampledPixels = new float[downsampledDepthImageSize];
        }

        /// <summary>
        /// Downsample depth pixels with nearest neighbor
        /// </summary>
        /// <param name="dest">The destination depth image.</param>
        /// <param name="factor">The downsample factor (2=x/2,y/2, 4=x/4,y/4, 8=x/8,y/8, 16=x/16,y/16).</param>
        public unsafe void DownsampleNearestNeighbor(IKinectDepthFrame dest, int factor, bool mirror)
        {
            if (null == dest || null == this._downsampledPixels)
            {
                throw new ArgumentException("inputs null");
            }

            if (false == (2 == factor || 4 == factor || 8 == factor || 16 == factor))
            {
                throw new ArgumentException("factor != 2, 4, 8 or 16");
            }

            if (factor < MIN_DOWNSAMPLE_FACTOR)
            {
                throw new ArgumentException("Downsample factor too small.");
            }

            int downsampleWidth = this.Width / factor;
            int downsampleHeight = this.Height / factor;

            if (dest.Width != downsampleWidth || dest.Height != downsampleHeight)
            {
                throw new ArgumentException("dest != downsampled image size");
            }

            if (mirror)
            {
                fixed (ushort* rawDepthPixelPtr = this._rawPixelData)
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
                                this._downsampledPixels[destIndex] = (float)rawDepthPixels[sourceIndex] * 0.001f;
                            }
                        });
                }
            }
            else
            {
                fixed (ushort* rawDepthPixelPtr = this._rawPixelData)
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
                                this._downsampledPixels[flippedDestIndex] = (float)rawDepthPixels[sourceIndex] * 0.001f;
                            }
                        });
                }
            }

            dest.FusionImageFrame.CopyPixelDataFrom(this._downsampledPixels);
        }

        public void Dispose()
        {
            FusionImageFrame.Dispose();
        }
    }
}
