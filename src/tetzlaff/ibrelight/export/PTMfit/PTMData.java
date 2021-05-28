package tetzlaff.ibrelight.export.PTMfit;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

public class PTMData {
    private final int rownumber;
    private final double[][] p;
    private final LuminaceData lumin;
    public PTMData() {
        LuminaceData lumin1;
        rownumber = 0;
        p = new double[0][];
        lumin1 =new LuminaceData();
        this.lumin = lumin1;
    }
    public PTMData(double[][] matrixP,LuminaceData lumin1){
        this.rownumber=matrixP.length;
        this.p=matrixP;
        this.lumin = lumin1;
    }
    public int getRownumber(){return this.rownumber;}

    public double[] getProw(int index){return this.p[index]; }
    public LuminaceData getRealLumin(){
        return this.lumin;
    }
    public CoefficientData cululateCoeff(){
        double[][] ppdata=new double[6][6];
        double[][] pldata=new double[1][this.rownumber];
        double[] coeffdata= new double[6];
        for(int i=0;i<this.rownumber;i++){
            for(int r=0;r<6;r++){
                for(int c=0;c<6;c++){
                    ppdata[r][c]+=p[i][r]*p[i][c];
                }
            }
        }
        for(int i=0;i<this.rownumber;i++){
            for(int k=0;k<6;k++){
                pldata[0][k]+=p[i][k]*lumin.getLumin(i);
            }
        }

        SimpleMatrix PTP=new SimpleMatrix(6,6, DMatrixRMaj.class);
        SimpleMatrix PTL=new SimpleMatrix(1,this.rownumber, DMatrixRMaj.class);

        for(int r=0;r<6;r++){
            for(int c=0;c<6;c++){
                PTP.set(r,c,ppdata[r][c]);
            }
        }
        for(int c=0;c<6;c++){
            PTL.set(0,c,pldata[0][c]);
        }

        SimpleMatrix PTPinvert = PTP.invert();
        SimpleMatrix C=PTPinvert.mult(PTL);


        for(int i=0;i<6;i++){
            coeffdata[i]=C.get(1,i);
        }
        CoefficientData coeff= new CoefficientData(coeffdata,this.lumin.getX(),this.lumin.getY());

        return coeff;
    }

}
