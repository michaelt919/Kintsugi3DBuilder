package tetzlaff.ibrelight.export.PTMfit;
public class CoefficientData {
    private double[] coeff;
    private double x;
    private double y;
    public CoefficientData(double[] coeff, double x,double y){

        this.y = y;
        this.x = x;
        this.coeff = coeff;
    }

    public double getY() {
        return y;
    }

    public double getX() {
        return x;
    }

    public double[] getCoeff() {
        return coeff;
    }
}
