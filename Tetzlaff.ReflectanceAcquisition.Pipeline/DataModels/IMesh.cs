using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Tetzlaff.ReflectanceAcquisition.Pipeline.Math;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.DataModels
{
    public interface IMesh<VertexType>
    {
        ReadOnlyCollection<VertexType> Vertices { get; }
    }
}
