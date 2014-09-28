using Microsoft.Kinect;
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
    public class KinectFusionColorFrame : IKinectFusionColorFrame
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

        public FusionColorImageFrame FusionImageFrame { get; private set; }

        /// <summary>
        /// Intermediate storage for the color data used when upsampling
        /// </summary>
        private int[] _rawPixels;

        public KinectFusionColorFrame(int width, int height, int maxUpsampleFactor = 1)
        {
            Contract.Ensures(FusionImageFrame != null);
            FusionImageFrame = new FusionColorImageFrame(width, height);

            int colorImageSize = this.Width * this.Height;
            int colorImageByteSize = colorImageSize * sizeof(int);
            _rawPixels = new int[colorImageSize];
        }

        public KinectFusionColorFrame(FusionColorImageFrame fusionImageFrame, int maxUpsampleFactor = 1)
        {
            Contract.Ensures(FusionImageFrame != null);
            FusionImageFrame = fusionImageFrame;

            int colorImageSize = this.Width * this.Height;
            int colorImageByteSize = colorImageSize * sizeof(int);
            _rawPixels = new int[colorImageSize];
        }

        /// <summary>
        /// Up sample color frame with nearest neighbor - replicates pixels
        /// </summary>
        /// <param name="factor">The up sample factor (2=x*2,y*2, 4=x*4,y*4, 8=x*8,y*8, 16=x*16,y*16).</param>
        public unsafe void UpsampleNearestNeighbor(int[] destBuffer, int factor)
        {
            if (null == destBuffer)
            {
                throw new ArgumentException("inputs null");
            }

            if (false == (2 == factor || 4 == factor || 8 == factor || 16 == factor))
            {
                throw new ArgumentException("factor != 2, 4, 8 or 16");
            }

            int upsampleWidth = this.Width * factor;
            int upsampleHeight = this.Height * factor;

            if (destBuffer.Length < upsampleWidth * upsampleHeight)
            {
                throw new ArgumentException("Destination buffer not large enough.");
            }

            this.FusionImageFrame.CopyPixelDataTo(this._rawPixels);

            int upsampleRowMultiplier = upsampleWidth * factor;

            // Here we make use of unsafe code to just copy the whole pixel as an int for performance reasons, as we do
            // not need access to the individual rgba components.
            fixed (int* rawColorPixelFixedPtr = this._rawPixels)
            {
                int* rawColorPixelPtr = (int*)rawColorPixelFixedPtr;

                // Note we run this only for the source image height pixels to sparsely fill the destination with rows
                Parallel.For(
                    0,
                    this.Height,
                    y =>
                    {
                        int destIndex = y * upsampleRowMultiplier;
                        int sourceColorIndex = y * this.Width;

                        for (int x = 0; x < this.Width; ++x, ++sourceColorIndex)
                        {
                            int color = rawColorPixelPtr[sourceColorIndex];

                            // Replicate pixels horizontally
                            for (int s = 0; s < factor; ++s, ++destIndex)
                            {
                                // Copy color pixel
                                destBuffer[destIndex] = color;
                            }
                        }
                    });
            }

            int sizeOfInt = sizeof(int);
            int rowByteSize = this.Height * sizeOfInt;

            // Duplicate the remaining rows with memcpy
            for (int y = 0; y < this.Height; ++y)
            {
                // iterate only for the smaller number of rows
                int srcRowIndex = upsampleRowMultiplier * y;

                // Duplicate lines
                for (int r = 1; r < factor; ++r)
                {
                    int index = upsampleWidth * ((y * factor) + r);

                    System.Buffer.BlockCopy(
                        destBuffer, srcRowIndex * sizeOfInt, destBuffer, index * sizeOfInt, rowByteSize);
                }
            }
        }

        public void Dispose()
        {
            FusionImageFrame.Dispose();
        }
    }
}
