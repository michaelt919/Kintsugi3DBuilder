package tetzlaff.util;

import java.util.ArrayList;
import java.util.List;
import org.ejml.simple.SimpleMatrix;

import static org.ejml.dense.row.CommonOps_DDRM.*;

public class NonNegativeLeastSquares 
{
	private static SimpleMatrix solvePartial(SimpleMatrix mATA, SimpleMatrix vATb, boolean[] p, int sizeP, List<Integer> mapping, SimpleMatrix sOut)
	{
		for (int index = 0; index < p.length; index++)
		{
			if (p[index])
			{
				mapping.add(index);
			}
		}
		
		// Create versions of A'A and A'b containing only the rows and columns
		// corresponding to the free variables.
		SimpleMatrix mATA_P = new SimpleMatrix(mapping.size(), mapping.size());
		SimpleMatrix vATb_P = new SimpleMatrix(mapping.size(), 1);
		
		for (int i = 0; i < mATA.numCols(); i++)
		{
			if (p[i])
			{
				vATb_P.set(mapping.get(i), vATb.get(i));
				
				for (int j = 0; j < mATA.numCols(); j++)
				{
					if (p[j])
					{
						mATA_P.set(mapping.get(i), mapping.get(j), mATA.get(i, j));
					}
				}
			}
		}
		
		// Solve the system.
		SimpleMatrix s_P = mATA_P.solve(vATb_P);
		
		// Copy the solution for the free variables into a vector containing the full solution,
		// including the variables fixed at zero.
		for (int i = 0; i < p.length; i++)
		{
			sOut.set(i, s_P.get(mapping.get(i)));
		}
		
		return s_P;
	}
	
	public SimpleMatrix solve(SimpleMatrix mA, SimpleMatrix b, double epsilon)
	{
		if (b.numCols() != 1 || b.numRows() != mA.numRows())
		{
			throw new IllegalArgumentException("b must be a column vector with the same number of rows as matrix A.");
		}
		
		if (epsilon <= 0.0)
		{
			throw new IllegalArgumentException("Epsilon must be greater than zero.");
		}
		
		// Precompute matrix products
		SimpleMatrix mATA = new SimpleMatrix(mA.numCols(), mA.numCols());
		SimpleMatrix vATb = new SimpleMatrix(mA.numCols(), 1);
		
		// Low level operations to avoid using unnecessary memory.
		multTransA(mA.getMatrix(), mA.getMatrix(), mATA.getMatrix());
		multTransA(mA.getMatrix(), b.getMatrix(), vATb.getMatrix());
		
		// Keep track of the set of free variables (where p[i] is true)
		// All other variables are fixed at zero.
		boolean[] p = new boolean[mA.numCols()];
		
		// Keep track of the number of free variables.
		int sizeP = 0;
		
		SimpleMatrix x = new SimpleMatrix(mA.numCols(), 1);
		SimpleMatrix w = vATb.copy();
		
		int k = -1;
		double maxW = 0.0;
		
		do
		{
			for (int i = 0; i < w.numRows(); i++)
			{
				double value = w.get(i);
				if (!p[i] && value > maxW)
				{
					k = i;
					maxW = value;
				}
			}
			
			// Iterate until effectively no values of w are positive. 
			if (maxW > epsilon)
			{
				p[k] = true;
				
				SimpleMatrix s = new SimpleMatrix(mATA.numCols(), 1);
				
				// Mapping from the set of free variables to the set of all variables.
				List<Integer> mapping = new ArrayList<Integer>(sizeP + 1);
				
				// Populates the mapping, sovles the system and copies it into s, 
				// and returns a vector containing only the free variables.
				SimpleMatrix s_P = solvePartial(mATA, vATb, p, sizeP + 1, mapping, s);
				
				// Update size of P based on the number of mappings.
				sizeP = mapping.size();
				
				// Make sure that none of the free variables went negative.
				while(elementMin(s_P.getMatrix()) <= 0.0)
				{
					double alpha = 1.0;
					int j = -1;
					for (int i = 0; i < sizeP; i++)
					{
						double sVal = s_P.get(i);
						
						if (sVal <= 0.0)
						{
							double xVal = x.get(mapping.get(i));
							double alphaCandidate = xVal / (xVal - s_P.get(i));
							if (alphaCandidate < alpha)
							{
								alpha = alphaCandidate;
								j = mapping.get(i);
							}
						}
					}
					
					// Several sources seem to indicate that alpha should be negated at this point: 
					// 		alpha = -min(x_i / (x_i - s_i)) where s_i <= 0.
					// (i.e. set alpha = -alpha in this implementation).
					// However, this is not the way it was originally published by Lawson and Hanson,
					// and it doesn't make sense to negate it, since alpha should vary between 0 and 1.
					
					
					// Make sure that at least one previously positive value is set to zero.
					// Because of round-off error, this is not necessarily guaranteed.
					p[j] = false;
					x.set(j, 0.0); 
					
					for (int i = 0; i < x.numRows(); i++)
					{
						if (p[i] && x.get(i) <= 0.0)
						{
							p[i] = false;
							x.set(i, 0.0); // Just in case it went slightly negative due to round-off error.
						}
					}
					
					mapping.clear();
					s.set(0.0); // Set all elements to zero.
					
					// Populates the mapping, sovles the system and copies it into s, 
					// and returns a vector containing only the free variables.
					s_P = solvePartial(mATA, vATb, p, sizeP, mapping, s);
					
					// Update size of P based on the number of mappings.
					sizeP = mapping.size();
				}
				
				x = s;
				w = vATb.minus(mATA.mult(x));
			}
		}
		while(sizeP < p.length && maxW > epsilon); 
		// The second condition makes the loop terminate if the earlier if-statement with the same condition evaluated to false.
		
		return x;
	}
}
