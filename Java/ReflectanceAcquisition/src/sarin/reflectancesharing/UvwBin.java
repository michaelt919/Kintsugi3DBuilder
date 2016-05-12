package sarin.reflectancesharing;

public class UvwBin {
	private final static float SCALE = 10.0f;
	
	public int u, v, w;
	
	public UvwBin( float u, float v, float w ){
		this.u = (int) Math.round(SCALE * u );
		this.v = (int) Math.round(SCALE * v );
		this.w = (int) Math.round(SCALE * w );
	}
	
	public boolean equals( Object o ){
		if( !(o instanceof UvwBin )){
			return false;
		}
		else{
			UvwBin obj = (UvwBin) o;
			return obj.u == u && obj.v == v && obj.w == w;
		}
	}
	
	public String toString() {
		return String.format("(%.4f, %.4f, %.4f)", u / SCALE, v / SCALE, w / SCALE);
	}
	
	public int hashCode(){
		int result = 17;
		result = 31 * result + Integer.hashCode(u);
		result = 31 * result + Integer.hashCode(v);
		result = 31 * result + Integer.hashCode(w);
		return result;
	}
	
}
