package tetzlaff.gl.helpers;

/**
 * A 3x3 matrix backed by 64-bit floats.  This is an immutable object.
 * @author Michael Tetzlaff
 *
 */
public class DoubleMatrix3 
{
	/**
	 * The matrix data.
	 */
	private double[][] m;
	
	/**
	 * Creates a new matrix by specifying each entry.
	 * @param m11 The entry at row 1, column 1.
	 * @param m12 The entry at row 1, column 2.
	 * @param m13 The entry at row 1, column 3.
	 * @param m21 The entry at row 2, column 1.
	 * @param m22 The entry at row 2, column 2.
	 * @param m23 The entry at row 2, column 3.
	 * @param m31 The entry at row 3, column 1.
	 * @param m32 The entry at row 3, column 2.
	 * @param m33 The entry at row 3, column 3.
	 */
	public DoubleMatrix3(
		double m11, double m12, double m13,
		double m21, double m22, double m23,
		double m31, double m32, double m33)
    {
        m = new double[3][3];
        m[0][0] = m11;
        m[1][0] = m21;
        m[2][0] = m31;
        m[0][1] = m12;
        m[1][1] = m22;
        m[2][1] = m32;
        m[0][2] = m13;
        m[1][2] = m23;
        m[2][2] = m33;
    }
	
	public DoubleMatrix3(Vector3 col1, Vector3 col2, Vector3 col3)
	{
		this (col1.x, col2.x, col3.x,
			  col1.y, col2.y, col3.y,
		      col1.z, col2.z, col3.z );
	}
	
	/**
	 * Creates a 4x4 matrix from a 3x3 matrix by dropping the fourth row and column.
	 * @param m4 The 4x4 matrix.
	 */
	public DoubleMatrix3(Matrix4 m4)
	{
		this(	m4.get(0,0),	m4.get(0,1),	m4.get(0,2),
				m4.get(1,0),	m4.get(1,1),	m4.get(1,2),
				m4.get(2,0),	m4.get(2,1),	m4.get(2,2)		);
	}

	/**
	 * Creates a scale matrix.
	 * @param sx The scale along the x-axis.
	 * @param sy The scale along the y-axis.
	 * @param sz The scale along the z-axis.
	 */
	public DoubleMatrix3(double sx, double sy, double sz) 
	{
		this(	sx, 	0.0, 	0.0,
				0.0,	sy,		0.0,
				0.0,	0.0, 	sz		);
	}
	
	/**
	 * Creates a uniform scale matrix that preserves proportions.
	 * @param s The scale along all axes.
	 */
	public DoubleMatrix3(double s)
	{
		this(s, s, s);
	}

	/**
	 * Creates an identity matrix.
	 */
	public DoubleMatrix3() 
	{
		this(1.0);
	}
	
	/**
	 * Gets a scale matrix.
	 * @param sx The scale along the x-axis.
	 * @param sy The scale along the y-axis.
	 * @param sz The scale along the z-axis.
	 * @return The specified scale matrix.
	 */
	public static DoubleMatrix3 scale(double sx, double sy, double sz)
	{
		return new DoubleMatrix3(sx, sy, sz);
	}
	
	/**
	 * Gets a uniform scale matrix that preserves proportions.
	 * @param s The scale along all axes.
	 * @return The specified scale matrix.
	 */
	public static DoubleMatrix3 scale(double s)
	{
		return new DoubleMatrix3(s);
	}
	
	/**
	 * Gets an identity matrix.
	 * @return An identity matrix.
	 */
	public static DoubleMatrix3 identity()
	{
		return new DoubleMatrix3();
	}
	
	/**
	 * Gets a rotation matrix about the x-axis.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static DoubleMatrix3 rotateX(double radians)
	{
		double sinTheta = Math.sin(radians);
		double cosTheta = Math.cos(radians);
		return new DoubleMatrix3(
			1.0,	0.0, 		0.0,
			0.0,	cosTheta,	-sinTheta,
			0.0,	sinTheta,	cosTheta
		);
	}
	
	/**
	 * Gets a rotation matrix about the y-axis.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static DoubleMatrix3 rotateY(double radians)
	{
		double sinTheta = Math.sin(radians);
		double cosTheta = Math.cos(radians);
		return new DoubleMatrix3(
			cosTheta,	0.0, 	sinTheta,
			0.0,		1.0,	0.0,
			-sinTheta,	0.0,	cosTheta
		);
	}
	
	/**
	 * Gets a rotation matrix about the z-axis.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static DoubleMatrix3 rotateZ(double radians)
	{
		double sinTheta = Math.sin(radians);
		double cosTheta = Math.cos(radians);
		return new DoubleMatrix3(
			cosTheta,	-sinTheta,	0.0,
			sinTheta,	cosTheta,	0.0,
			0.0,		0.0,		1.0
		);
	}
	
	/**
	 * Gets a rotation matrix about an arbitrary axis.
	 * @param axis The axis about which to rotate.
	 * @param radians The amount of rotation, in radians.
	 * @return The specified transformation matrix.
	 */
	public static DoubleMatrix3 rotateAxis(Vector3 axis, double radians)
	{
		double sinTheta = Math.sin(radians);
		double cosTheta = Math.cos(radians);
		double oneMinusCosTheta = 1.0 - cosTheta;
		return new DoubleMatrix3(
				
			axis.x * axis.x * oneMinusCosTheta + cosTheta,			
				axis.x * axis.y * oneMinusCosTheta - axis.z * sinTheta,	
					axis.x * axis.z * oneMinusCosTheta + axis.y * sinTheta,
					
			axis.y * axis.x * oneMinusCosTheta + axis.z * sinTheta,	
				axis.y * axis.y * oneMinusCosTheta + cosTheta,			
					axis.y * axis.z * oneMinusCosTheta - axis.x * sinTheta,
					
			axis.z * axis.x * oneMinusCosTheta - axis.y * sinTheta,	
				axis.z * axis.y * oneMinusCosTheta + axis.x * sinTheta, 
					axis.z * axis.z * oneMinusCosTheta + cosTheta
		);
	}
	
	/**
	 * Gets a rotation matrix from a quaternion.
	 * @param x The first component of the quaternion.
	 * @param y The second component of the quaternion.
	 * @param z The third component of the quaternion.
	 * @param w The fourth (identity) component of the quaternion.
	 * @return The specified transformation matrix.
	 */
	public static DoubleMatrix3 fromQuaternion(double x, double y, double z, double w)
	{
		return new DoubleMatrix3(
			1 - 2*y*y - 2*z*z,	2*x*y - 2*z*w,		2*x*z + 2*y*w,
			2*x*y + 2*z*w,		1 - 2*x*x - 2*z*z,	2*y*z - 2*x*w,
			2*x*z - 2*y*w,		2*y*z + 2*x*w,		1 - 2*x*x - 2*y*y
		);
	}
	
	/**
	 * Convert the given matrix (assumed to be a rotation matrix) to a quaternion.
	 * @return The quaternion packed in a Vector4
	 */
	public DoubleVector4 toQuaternion()
	{
        // Convert rotation matrix to quaternion
        double[] q = new double[4];
        double trace = get(0,0) + get(1,1) + get(2,2);
        if (trace > 0)
        {
            double s = 0.5 / Math.sqrt(trace + 1.0);
            q[3] = 0.25 / s;
            q[0] = (get(1,2) - get(2,1)) * s;
            q[1] = (get(2,0) - get(0,2)) * s;
            q[2] = (get(0,1) - get(1,0)) * s;
        }
        else
        {
            if (get(0,0) > get(1,1) && get(0,0) > get(2,2))
            {
                double s = 2.0 * Math.sqrt(0.0 + get(0,0) - get(1,1) - get(2,2));
                q[3] = (get(1,2) - get(2,1)) / s;
                q[0] = 0.25 * s;
                q[1] = (get(1,0) + get(0,1)) / s;
                q[2] = (get(2,0) + get(0,2)) / s;
            }
            else if (get(1,1) > get(2,2))
            {
                double s = 2.0 * Math.sqrt(0.0 + get(1,1) - get(0,0) - get(2,2));
                q[3] = (get(2,0) - get(0,2)) / s;
                q[0] = (get(1,0) + get(0,1)) / s;
                q[1] = 0.25 * s;
                q[2] = (get(2,1) + get(1,2)) / s;
            }
            else
            {
                double s = 2.0 * Math.sqrt(0.0 + get(2,2) - get(0,0) - get(1,1));
                q[3] = (get(0,1) - get(1,0)) / s;
                q[0] = (get(2,0) + get(0,2)) / s;
                q[1] = (get(2,1) + get(1,2)) / s;
                q[2] = 0.25 * s;
            }
        }
        
        return new DoubleVector4(q[0], q[1], q[2], q[3]);
	}

	/**
	 * Gets a new matrix that is the sum of this matrix and another matrix.
	 * @param other The matrix to add to this one.
	 * @return A new matrix that is the sum of the two matrices.
	 */
	public DoubleMatrix3 plus(DoubleMatrix3 other)
	{
		return new DoubleMatrix3(
			this.m[0][0] + other.m[0][0],	this.m[0][1] + other.m[0][1], this.m[0][2] + other.m[0][2],
			this.m[1][0] + other.m[1][0],	this.m[1][1] + other.m[1][1], this.m[1][2] + other.m[1][2],
			this.m[2][0] + other.m[2][0],	this.m[2][1] + other.m[2][1], this.m[2][2] + other.m[2][2]
		);
	}
	
	/**
	 * Gets a new matrix that is the difference of this matrix and another matrix.
	 * @param other The matrix to subtract from this one.
	 * @return A new matrix that is the difference of the two matrices.
	 */
	public DoubleMatrix3 minus(DoubleMatrix3 other)
	{
		return new DoubleMatrix3(
			this.m[0][0] - other.m[0][0],	this.m[0][1] - other.m[0][1], this.m[0][2] - other.m[0][2],
			this.m[1][0] - other.m[1][0],	this.m[1][1] - other.m[1][1], this.m[1][2] - other.m[1][2],
			this.m[2][0] - other.m[2][0],	this.m[2][1] - other.m[2][1], this.m[2][2] - other.m[2][2]
		);
	}
	
	/**
	 * Gets a new matrix that is the result of multiplying/transforming another matrix by this one.
	 * @param other The matrix to transform.
	 * @return A new matrix that is the result of transforming the other matrix by this one.
	 */
	public DoubleMatrix3 times(DoubleMatrix3 other)
	{
		return new DoubleMatrix3(
			this.m[0][0] * other.m[0][0] + this.m[0][1] * other.m[1][0] + this.m[0][2] * other.m[2][0],	
			this.m[0][0] * other.m[0][1] + this.m[0][1] * other.m[1][1] + this.m[0][2] * other.m[2][1],	
			this.m[0][0] * other.m[0][2] + this.m[0][1] * other.m[1][2] + this.m[0][2] * other.m[2][2],	
			this.m[1][0] * other.m[0][0] + this.m[1][1] * other.m[1][0] + this.m[1][2] * other.m[2][0],	
			this.m[1][0] * other.m[0][1] + this.m[1][1] * other.m[1][1] + this.m[1][2] * other.m[2][1],	
			this.m[1][0] * other.m[0][2] + this.m[1][1] * other.m[1][2] + this.m[1][2] * other.m[2][2],
			this.m[2][0] * other.m[0][0] + this.m[2][1] * other.m[1][0] + this.m[2][2] * other.m[2][0],	
			this.m[2][0] * other.m[0][1] + this.m[2][1] * other.m[1][1] + this.m[2][2] * other.m[2][1],	
			this.m[2][0] * other.m[0][2] + this.m[2][1] * other.m[1][2] + this.m[2][2] * other.m[2][2]
		);
	}
	
	/**
	 * Gets a new vector that is the result of transforming a vector by this matrix.
	 * @param vector The vector to transform.
	 * @return A new vector that is the result of transforming the original vector by this matrix.
	 */
	public DoubleVector3 times(DoubleVector3 vector)
	{
		return new DoubleVector3(
			this.m[0][0] * vector.x + this.m[0][1] * vector.y + this.m[0][2] * vector.z,
			this.m[1][0] * vector.x + this.m[1][1] * vector.y + this.m[1][2] * vector.z,
			this.m[2][0] * vector.x + this.m[2][1] * vector.y + this.m[2][2] * vector.z
		);
	}
	
	/**
	 * Gets a new matrix that is the result of scaling this matrix.
	 * @param factor The factor by which to scale.
	 * @return A new matrix that is the result of scaling this matrix by the specified factor.
	 */
	public DoubleMatrix3 times(double factor)
	{
		return new DoubleMatrix3(
			this.m[0][0] * factor, this.m[0][1] * factor, this.m[0][2] * factor,
			this.m[1][0] * factor, this.m[1][1] * factor, this.m[1][2] * factor,
			this.m[2][0] * factor, this.m[2][1] * factor, this.m[2][2] * factor
		);
	}
	
	/**
	 * Gets a new matrix that is the negation of this matrix.
	 * @return A new matrix with the values of this matrix, but negated.
	 */
	public DoubleMatrix3 negate()
	{
		return new DoubleMatrix3(
			-this.m[0][0], -this.m[0][1], -this.m[0][2],
			-this.m[1][0], -this.m[1][1], -this.m[1][2],
			-this.m[2][0], -this.m[2][1], -this.m[2][2]
		);
	}
	
	/**
	 * Gets a new matrix that is the transpose of this matrix.
	 * @return A new matrix with the values of this matrix, but with rows and columns interchanged.
	 */
	public DoubleMatrix3 transpose()
	{
		return new DoubleMatrix3(
			this.m[0][0], this.m[1][0], this.m[2][0],
			this.m[0][1], this.m[1][1], this.m[2][1],
			this.m[0][2], this.m[1][2], this.m[2][2]
		);
	}
	
	/**
	 * Gets the determinant of this matrix.
	 * @return The matrix determinant.
	 */
	public double determinant()
	{
		return this.m[0][0] * (this.m[1][1] * this.m[2][2] - this.m[2][1] * this.m[1][2])
				- this.m[0][1] * (this.m[1][0] * this.m[2][2] - this.m[2][0] * this.m[1][2])
				+ this.m[0][2] * (this.m[1][0] * this.m[2][1] - this.m[2][0] * this.m[1][1]);
	}
	
	/**
	 * Gets the inverse of this matrix.
	 * @return The matrix inverse.
	 */
	public DoubleMatrix3 inverse()
	{
		double det = this.determinant();
		
		return new DoubleMatrix3(
			(m[1][1] * m[2][2] - m[1][2] * m[2][1]) / det, (m[0][2] * m[2][1] - m[0][1] * m[2][2]) / det, (m[0][1] * m[1][2] - m[0][2] * m[1][1]) / det,
			(m[1][2] * m[2][0] - m[1][0] * m[2][2]) / det, (m[0][0] * m[2][2] - m[0][2] * m[2][0]) / det, (m[0][2] * m[1][0] - m[0][0] * m[1][2]) / det,
			(m[1][0] * m[2][1] - m[1][1] * m[2][0]) / det, (m[0][1] * m[2][0] - m[0][0] * m[2][1]) / det, (m[0][0] * m[1][1] - m[0][1] * m[1][0]) / det);
	}
	
	/**
	 * Gets a particular entry of this matrix.
	 * @param row The row of the entry to retrieve.
	 * @param col The column of the entry to retrieve.
	 * @return The entry at the specified row and column.
	 */
	public double get(int row, int col)
	{
		return this.m[row][col];
	}
	
	/**
	 * Gets a particular row of the matrix.
	 * @param row The index of the row to retrieve.
	 * @return The row vector at the specified index.
	 */
	public DoubleVector3 getRow(int row)
	{
		return new DoubleVector3(this.m[row][0], this.m[row][1], this.m[row][2]);
	}
	
	/**
	 * Gets a particular column of the matrix.
	 * @param row The index of the row to retrieve.
	 * @return The row vector at the specified index.
	 */
	public DoubleVector3 getColumn(int col)
	{
		return new DoubleVector3(this.m[0][col], this.m[1][col], this.m[2][col]);
	}
}
