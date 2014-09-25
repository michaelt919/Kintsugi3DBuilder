using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions
{
    /// <summary>
    /// Description of CameraPoseFinderFailedException
    /// </summary>
    public class CameraPoseFinderFailedException : Exception, ISerializable
    {
        public CameraPoseFinderFailedException()
        {
        }

        public CameraPoseFinderFailedException(string message)
            : base(message)
        {
        }

        public CameraPoseFinderFailedException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected CameraPoseFinderFailedException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}