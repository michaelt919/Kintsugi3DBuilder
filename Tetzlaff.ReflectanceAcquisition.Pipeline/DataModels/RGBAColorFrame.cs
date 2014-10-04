using System;
using System.Collections.Generic;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class RGBAColorFrame : IColorFrame
    {
        public int Width { get; private set; }
        public int Height { get; private set; }

        /// <summary>
        /// Data buffer for raw pixels
        /// </summary>
        public byte[] RawPixels { get; private set; }

        public ICameraProjection CameraProjection { get; set; }

        public RGBAColorFrame(int width, int height)
        {
            this.Width = width;
            this.Height = height;
            int colorImageSize = this.Width * this.Height;
            int colorImageByteSize = colorImageSize * sizeof(int);
            RawPixels = new byte[colorImageByteSize];
        }

        public IColorFrame Clone()
        {
            RGBAColorFrame copy = new RGBAColorFrame(this.Width, this.Height);
            copy.CameraProjection = this.CameraProjection == null ? null : 
                new CameraProjection
                {
                    AspectRatio = this.CameraProjection.AspectRatio,
                    HorizontalFieldOfView = this.CameraProjection.HorizontalFieldOfView,
                    VerticalFieldOfView = this.CameraProjection.VerticalFieldOfView,
                    NearPlane = this.CameraProjection.NearPlane,
                    FarPlane = this.CameraProjection.FarPlane
                };
            Array.Copy(this.RawPixels, copy.RawPixels, this.RawPixels.Length);
            return copy;
        }
    }
}
