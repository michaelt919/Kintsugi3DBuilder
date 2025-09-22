package kintsugi3d.builder.fit.settings;

public class BasisSettings
{
    private int basisCount = 8;
    private int basisResolution = 90;
    private boolean smithMaskingShadowingEnabled = true;

    /**
     * @return The number of basis functions to use for the specular lobe.
     */
    public int getBasisCount()
    {
        return basisCount;
    }

    /**
     * @param basisCount The number of basis functions to use for the specular lobe.
     */
    public void setBasisCount(int basisCount)
    {
        if (basisCount <= 0)
        {
            throw new IllegalArgumentException("Basis count must be greater than zero.");
        }
        else
        {
            this.basisCount = basisCount;
        }
    }

    /**
     * @return The number of discrete values in the definition of the specular lobe.
     */
    public int getBasisResolution()
    {
        return basisResolution;
    }

    /**
     * @param basisResolution The number of discrete values in the definition of the specular lobe.
     */
    public void setBasisResolution(int basisResolution)
    {
        if (basisResolution <= 0)
        {
            throw new IllegalArgumentException("Basis resolution must be greater than zero.");
        }
        else
        {
            this.basisResolution = basisResolution;
        }
    }

    /**
     * Whether or not to use height-correlated Smith for masking / shadowing.  Default is true.
     * @return
     */
    public boolean isSmithMaskingShadowingEnabled()
    {
        return smithMaskingShadowingEnabled;
    }

    /**
     * Whether or not to use height-correlated Smith for masking / shadowing.  Default is true.
     * @param smithMaskingShadowingEnabled
     */
    public void setSmithMaskingShadowingEnabled(boolean smithMaskingShadowingEnabled)
    {
        this.smithMaskingShadowingEnabled = smithMaskingShadowingEnabled;
    }
}
