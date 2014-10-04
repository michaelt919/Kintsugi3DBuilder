using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.LightField.DataModels
{
    public interface ILightField : IReflectanceModel
    {
        /// <summary>
        /// The number of frames in the view set
        /// </summary>
        int ViewCount { get; }

        void AddView(IColorFrame view, ICameraPose cameraPose, double alignmentEnergy);
        void FinalizeReflectance();
    }
}
