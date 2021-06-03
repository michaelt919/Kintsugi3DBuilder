package tetzlaff.ibrelight.export.PTMfit;
public class CoefficientData {
    private double[] coeff;

    public CoefficientData(double[] coeff, double x,double y){

        this.coeff = coeff;
    }



    public double[] getCoeff() {
        return coeff;
    }

    public void setCoeff(double[] coeff) {
        this.coeff = coeff;
    }
    public void setCoeff(int index, double value) {
        this.coeff[index]=value;
    }
}
