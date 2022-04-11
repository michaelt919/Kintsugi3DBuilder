package tetzlaff.ibrelight.export.PTMfit;


import tetzlaff.util.ColorList;

public class LuminanceData
{
    private int size;
    private ColorList lumin;
    private ColorList lightdir;

    public LuminanceData(ColorList lumin, ColorList lightdir){
        this.lumin=lumin;
        this.lightdir=lightdir;
        this.size= lumin.size();
    }

    public ColorList getLumin() {
        return lumin;
    }
    public ColorList getLightdir() {
        return lightdir;
    }

    public int getsize(){
        return this.size;
    }

}
