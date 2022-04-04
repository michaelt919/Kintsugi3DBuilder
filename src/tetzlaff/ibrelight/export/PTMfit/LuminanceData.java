package tetzlaff.ibrelight.export.PTMfit;


import tetzlaff.util.ColorList;

public class LuminanceData
{
    private int size;
    private ColorList lumin;
    private ColorList lightdir;
    //private CoefficientData coeffcient;

    public LuminanceData(){
        this.size=0;
        this.lumin=null;
        this.lightdir=null;
    }
    public LuminanceData(ColorList lumin, ColorList lightdir){
        //this.size=lumindata.length;

        this.lumin=lumin;
        this.lightdir=lightdir;
        this.size= lumin.size();
        //this.coeffcient=cululateCoeff();

    }



    public ColorList getLumin() {
        return lumin;
    }
    public ColorList getLightdir() {
        return lightdir;
    }
//    public Float getLumin(int index){
//        return lumin.;
//    }


    public int getsize(){
        return this.size;
    }

}
