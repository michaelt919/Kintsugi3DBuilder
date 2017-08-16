package tetzlaff.gl;

/**
 * Enumerates the possible hardware compression schemes that can be requested from the GL.
 * The actual compression algorithm used can be freely chosen by the GL, 
 * but it should ensure that all the requested components are available within the specified bits-per-pixel (bpp) limits.
 * Note: BPP values are an asymptotic measure and may vary slightly, particularly with smaller textures. 
 * @author Michael Tetzlaff
 *
 */
public enum CompressionFormat 
{
    /**
     * A single unsigned red channel should be compressed at 4 bits-per-pixel.
     */
    RED_4BPP,

    /**
     * A single signed red channel should be compressed at 4 bits-per-pixel.
     */
    SIGNED_RED_4BPP,

    /**
     * Two unsigned channels, red and green, should be compressed independently at 4 bits-per-pixel each, for a total of 8 bits-per-pixel.
     */
    RED_4BPP_GREEN_4BPP,

    /**
     * Two signed channels, red and green, should be compressed independently at 4 bits-per-pixel each, for a total of 8 bits-per-pixel.
     */
    SIGNED_RED_4BPP_GREEN_4BPP,

    /**
     * Three unsigned channels for red, green, and blue should be compressed together, in a linear color space, at 4 bits-per-pixel total.
     */
    RGB_4BPP,

    /**
     * Three unsigned channels for red, green, and blue should be compressed together, in the sRGB color space, at 4 bits-per-pixel total.
     */
    SRGB_4BPP,

    /**
     * Three unsigned channels for red, green, and blue should be compressed together in a linear color space,
     * along with a punch-through alpha bit, at 4 bits-per-pixel total.
     */
    RGB_PUNCHTHROUGH_ALPHA1_4BPP,

    /**
     * Three unsigned channels for red, green, and blue should be compressed together in the sRGB color space,
     * along with a punch-through alpha bit, at 4 bits-per-pixel total.
     */
    SRGB_PUNCHTHROUGH_ALPHA1_4BPP,

    /**
     * Three unsigned channels for red, green, and blue should be compressed together, in a linear color space, at 4 bits-per-pixel,
     * along with with an independently compressed alpha channel at 4 bits-per-pixel, for a total of 8 bits-per-pixel.
     */
    RGB_4BPP_ALPHA_4BPP,

    /**
     * Three unsigned channels for red, green, and blue should be compressed together, in the sRGB color space, at 4 bits-per-pixel,
     * along with with an independently compressed alpha channel at 4 bits-per-pixel, for a total of 8 bits-per-pixel.
     */
    SRGB_4BPP_ALPHA_4BPP
}
