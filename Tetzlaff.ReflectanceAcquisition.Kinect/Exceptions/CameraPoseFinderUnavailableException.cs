using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions
{
    /// <summary>
    /// Description of CameraPoseFinderUnavailableException
    /// </summary>
    public class CameraPoseFinderUnavailableException : Exception, ISerializable
    {
        public CameraPoseFinderUnavailableException()
        {
        }

        public CameraPoseFinderUnavailableException(string message)
            : base(message)
        {
        }

        public CameraPoseFinderUnavailableException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected CameraPoseFinderUnavailableException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}