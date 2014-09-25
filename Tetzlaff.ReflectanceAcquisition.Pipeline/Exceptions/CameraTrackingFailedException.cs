using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Pipeline.Exceptions
{
    /// <summary>
    /// Description of CameraTrackingFailedException
    /// </summary>
    public class CameraTrackingFailedException : Exception, ISerializable
    {
        public CameraTrackingFailedException()
        {
        }

        public CameraTrackingFailedException(string message)
            : base(message)
        {
        }

        public CameraTrackingFailedException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected CameraTrackingFailedException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}