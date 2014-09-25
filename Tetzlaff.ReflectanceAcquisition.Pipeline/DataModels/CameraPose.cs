using System;
using System.Collections.Generic;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Math;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public class CameraPose : ICameraPose
    {
        public Matrix4 Matrix { get; set; }

        public CameraPose()
        {
            Matrix = Matrix4.Identity;
        }

        public CameraPose(Matrix4 matrix)
        {
            Contract.Ensures(Matrix != null);
            Matrix = matrix;
        }
    }
}
