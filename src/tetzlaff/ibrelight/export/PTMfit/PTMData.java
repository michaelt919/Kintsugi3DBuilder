package tetzlaff.ibrelight.export.PTMfit;

public class PTMData {
    private final int rownumber;
    private final double[][] p;

    public PTMData() {
        rownumber = 0;
        p = new double[0][];
    }
    public PTMData(double[][] matrixP){
        this.rownumber=matrixP.length;
        this.p=matrixP;
    }
    public int getRownumber(){return this.rownumber;}

    public double[] getProw(int index){return this.p[index]; }

    

}
