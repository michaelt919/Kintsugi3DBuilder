using System;
using System.Runtime.Serialization;

namespace Tetzlaff.ReflectanceAcquisition.Kinect.Exceptions
{
    /// <summary>
    /// Description of NoPoseFinderMatchesException
    /// </summary>
    public class NoPoseFinderMatchesException : Exception, ISerializable
    {
        public NoPoseFinderMatchesException()
        {
        }

        public NoPoseFinderMatchesException(string message)
            : base(message)
        {
        }

        public NoPoseFinderMatchesException(string message, Exception innerException)
            : base(message, innerException)
        {
        }

        // This constructor is needed for serialization.
        protected NoPoseFinderMatchesException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}