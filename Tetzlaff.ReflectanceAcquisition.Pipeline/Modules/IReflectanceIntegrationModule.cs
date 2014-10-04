using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.Modules
{
    public interface IReflectanceIntegrationModule<ColorFrameType, ReflectanceModelType>
        where ColorFrameType : IFrame
        where ReflectanceModelType : IReflectanceModel
    {

    }
}
