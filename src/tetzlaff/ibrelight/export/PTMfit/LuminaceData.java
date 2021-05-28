package tetzlaff.ibrelight.export.PTMfit;
public class LuminaceData {
    private int size;
    private double[] lumin;
    private double x;
    private double y;


    public LuminaceData(){
        this.size=0;
        this.lumin=null;
        x=0;
        y=0;
    }
    public LuminaceData(double[] lumindata,double x,double y){
        this.size=lumindata.length;
        this.lumin=lumindata;
        x=x;
        y=y;
    }
    public LuminaceData(int size,double x,double y){
        this.size=size;
        this.lumin=new double[size];
        x=x;
        y=y;
    }

    public double[] getLumin() {
        return lumin;
    }
    public double getLumin(int index) {
        return lumin[index];
    }

    public int getsize(){
        return this.size;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setLumin(int index, double value) {
        this.lumin[index] = value;
    }

    public double dot(LuminaceData t2) {
        double[] t1lumin=this.getLumin();
        double[] t2lumin=this.getLumin();
        double result=0;
            for(int i=0;i<size;i++){
                result+=t1lumin[i]*t2lumin[i];
            }

        return result;

    }
}
