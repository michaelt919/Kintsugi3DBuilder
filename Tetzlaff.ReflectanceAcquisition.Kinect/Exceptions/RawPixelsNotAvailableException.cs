using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions
{
    /// <summary>
    /// Description of RawPixelsNotAvailableException
    /// </summary>
    public class RawPixelsNotAvailableException : Exception, ISerializable
    {
        public RawPixelsNotAvailableException()
        {
        }

        public RawPixelsNotAvailableException(string message)
            : base(message)
        {
        }

        public RawPixelsNotAvailableException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected RawPixelsNotAvailableException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}