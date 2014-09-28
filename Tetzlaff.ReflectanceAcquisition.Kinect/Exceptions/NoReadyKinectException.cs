using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions
{
    /// <summary>
    /// Description of NoReadyKinectException
    /// </summary>
    public class NoReadyKinectException : Exception, ISerializable
    {
        public NoReadyKinectException()
        {
        }

        public NoReadyKinectException(string message)
            : base(message)
        {
        }

        public NoReadyKinectException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected NoReadyKinectException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}