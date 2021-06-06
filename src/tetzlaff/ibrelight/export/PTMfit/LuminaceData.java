package tetzlaff.ibrelight.export.PTMfit;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.util.ColorList;

public class LuminaceData {
    private int size;
    private ColorList lumin;
    private Float[] lightdir;
    //private CoefficientData coeffcient;

    public LuminaceData(){
        this.size=0;
        this.lumin=null;
        this.lightdir=null;
    }
    public LuminaceData(ColorList lumin, Float[] lightdir){
        //this.size=lumindata.length;

        this.lumin=lumin;
        this.lightdir=lightdir;
        this.size= lumin.size();
        //this.coeffcient=cululateCoeff();

    }



    public ColorList getLumin() {
        return lumin;
    }
    public Float[] getLightdir() {
        return lightdir;
    }
//    public Float getLumin(int index){
//        return lumin.;
//    }


    public int getsize(){
        return this.size;
    }



//    public double dot(LuminaceData t2) {
//        double[] t1lumin=this.getLumin();
//        double[] t2lumin=this.getLumin();
//        double result=0;
//            for(int i=0;i<size;i++){
//                result+=t1lumin[i]*t2lumin[i];
//            }
//
//        return result;
//
//    }

//    public CoefficientData cululateCoeff(){
//        double[][] ppdata=new double[6][6];
//        double[][] pldata=new double[1][this.size];
//        double[] coeffdata= new double[6];
//        for(int i=0;i<this.size;i++){
//            for(int r=0;r<6;r++){
//                for(int c=0;c<6;c++){
//                    ppdata[r][c]+=p[i][r]*p[i][c];
//                }
//            }
//        }
//        for(int i=0;i<this.size;i++){
//            for(int k=0;k<6;k++){
//                //to do
//                pldata[0][k]+=p[i][k]*lumin.getLumin(i);
//            }
//        }
//
//        SimpleMatrix PTP=new SimpleMatrix(6,6, DMatrixRMaj.class);
//        SimpleMatrix PTL=new SimpleMatrix(1,this.size, DMatrixRMaj.class);
//
//        for(int r=0;r<6;r++){
//            for(int c=0;c<6;c++){
//                PTP.set(r,c,ppdata[r][c]);
//            }
//        }
//        for(int c=0;c<6;c++){
//            PTL.set(0,c,pldata[0][c]);
//        }
//
//        SimpleMatrix PTPinvert = PTP.invert();
//        SimpleMatrix C=PTPinvert.mult(PTL);
//
//
//        for(int i=0;i<6;i++){
//            coeffdata[i]=C.get(1,i);
//        }
//        CoefficientData coeff= new CoefficientData(coeffdata);
//
//        return coeff;
//    }

}
