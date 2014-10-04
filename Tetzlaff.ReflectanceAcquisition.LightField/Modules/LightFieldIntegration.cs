using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.LightField.DataModels;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Modules;

namespace Tetzlaff.ReflectanceAcquisition.LightField.Modules
{
    public class LightFieldIntegration : IReflectanceIntegrationModule<IColorFrame, ILightField>
    {
        public void IntegrateReflectance(IColorFrame colorFrame, ILightField lightField, ICameraPose cameraPose, double alignmentEnergy)
        {
            lightField.AddView(colorFrame, cameraPose, alignmentEnergy);
        }
    }
}
