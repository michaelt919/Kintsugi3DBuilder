using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions
{
    /// <summary>
    /// Description of NotEnoughPoseFinderMatchesException
    /// </summary>
    public class NotEnoughPoseFinderMatchesException : Exception, ISerializable
    {
        public NotEnoughPoseFinderMatchesException()
        {
        }

        public NotEnoughPoseFinderMatchesException(string message)
            : base(message)
        {
        }

        public NotEnoughPoseFinderMatchesException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected NotEnoughPoseFinderMatchesException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}