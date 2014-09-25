using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions
{
    /// <summary>
    /// Description of Exception1
    /// </summary>
    public class CameraPoseFinderHistoryFullException : Exception, ISerializable
    {
        public CameraPoseFinderHistoryFullException()
        {
        }

        public CameraPoseFinderHistoryFullException(string message)
            : base(message)
        {
        }

        public CameraPoseFinderHistoryFullException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected CameraPoseFinderHistoryFullException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}