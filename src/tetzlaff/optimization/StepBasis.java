package tetzlaff.optimization;

public class StepBasis implements Basis
{
    private final double metallicity;

    public StepBasis(double metallicity)
    {
        // TODO: Does metallicity / diffuse stuff actually belong here?
        this.metallicity = metallicity;
    }

    @Override
    public double evaluate(int basisFunction, int location)
    {
        return location <= basisFunction ? 1.0 : 0.0;
    }

    @Override
    public void contributeToFittingSystem(int locationCurrent, int locationPrev, int instanceCount, MatrixBuilderSums sums, MatrixSystem fittingSystem)
    {
        // Number of rows / columns in LHS = instanceCount * (stepCount + 1)
        // The +1 comes from the diffuse term.
        int stepCount = fittingSystem.lhs.numRows() / instanceCount - 1;

        // Add the running total to elements of the ATA matrix and the ATy vector corresponding to the newly visited m
        // as well as any m-values skipped over.
        // These elements also need to get some more contributions that have blending weights that are yet to be visited,
        // but that will be handled later, when a sample is visited for some matrix elements, or the next time m changes for others.
        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            int i = instanceCount * (locationPrev + 1) + b1;

            // Update ATy vector for blended term.
            // Contribution due to previous m-value scaled by blending weight t to account for linear interpolation.
            // Accumulation due to greater m-values should already have been added to the vector the last time an m-value changed
            fittingSystem.addToRHS(i, 0, sums.getWeightedAnalyticTimesObservedBlended(0, b1));
            fittingSystem.addToRHS(i, 1, sums.getWeightedAnalyticTimesObservedBlended(1, b1));
            fittingSystem.addToRHS(i, 2, sums.getWeightedAnalyticTimesObservedBlended(2, b1));

            // This loop usually would only run once, but could run multiple times if we skipped a few m values.
            for (int m1 = locationPrev + 1; m1 <= locationCurrent; m1++)
            {
                i = instanceCount * (m1 + 1) + b1;

                // Update ATy vector
                fittingSystem.addToRHS(i, 0, sums.getWeightedAnalyticTimesObserved(0, b1));
                fittingSystem.addToRHS(i, 1, sums.getWeightedAnalyticTimesObserved(1, b1));
                fittingSystem.addToRHS(i, 2, sums.getWeightedAnalyticTimesObserved(2, b1));

                // Update ATA matrix
                for (int b2 = 0; b2 < instanceCount; b2++)
                {
                    // Top right and bottom left partitions of the matrix:
                    // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    fittingSystem.addToLHS(i, b2,
                            metallicity * sums.getWeightedAnalyticSquared(b1, b2)
                                    + (1 - metallicity) * sums.getWeightedAnalytic(b1, b2) / Math.PI);
                    fittingSystem.addToLHS(b2, i,
                            metallicity * sums.getWeightedAnalyticSquared(b2, b1)
                                    + (1 - metallicity) * sums.getWeightedAnalytic(b2, b1) / Math.PI);

                    // Bottom right partition of the matrix: row and column both correspond to specular.

                    // Handle "corner" case where m1 = m2 (don't want to repeat with row and column swapped as elements would then be duplicated).
                    int j = instanceCount * (m1 + 1) + b2;
                    fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquared(b1, b2));

                    // Visit every element of the microfacet distribution that comes after m1.
                    // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
                    for (int m2 = m1 + 1; m2 < stepCount; m2++)
                    {
                        j = instanceCount * (m2 + 1) + b2;

                        // Add the current value of the running total to the appropriate location in the matrix.
                        // The matrix is symmetric so we also need to swap row and column and update that way.
                        fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquared(b1, b2));
                        fittingSystem.addToLHS(j, i, sums.getWeightedAnalyticSquared(b2, b1));
                    }
                }
            }
        }

        // Add the total of recently visited samples with blending weights to elements of the ATA matrix corresponding to the old m.
        for (int b1 = 0; b1 < instanceCount; b1++)
        {
            int i = instanceCount * (locationPrev + 1) + b1;

            for (int b2 = 0; b2 < instanceCount; b2++)
            {

                // Top right and bottom left partitions of the matrix:
                // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                // The matrix is symmetric so we also need to swap row and column and update that way.
                fittingSystem.addToLHS(i, b2,
                        metallicity * sums.getWeightedAnalyticSquaredBlended(b1, b2)
                                + (1 - metallicity) * sums.getWeightedAnalyticBlended(b1, b2) / Math.PI);
                fittingSystem.addToLHS(b2, i,
                        metallicity * sums.getWeightedAnalyticSquaredBlended(b1, b2)
                                + (1 - metallicity) * sums.getWeightedAnalyticBlended(b1, b2) / Math.PI);

                // Bottom right partition of the matrix: row and column both correspond to specular:
                // Update "corner" element with squared blending weight.
                int j = instanceCount * (locationPrev + 1) + b2;
                fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquaredBlendedSquared(b1, b2));

                // Visit every element of the microfacet distribution that comes after mPrevious.
                // This is because the form of ATA is such that the values in the matrix are determined by the lower of the two m-values.
                for (int m2 = locationPrev + 1; m2 < stepCount; m2++)
                {
                    j = instanceCount * (m2 + 1) + b2;

                    // Add the current value of the running total with blending (linear interpolation) weights to the appropriate location in the matrix.
                    // The matrix is symmetric so we also need to swap row and column and update that way.
                    fittingSystem.addToLHS(i, j, sums.getWeightedAnalyticSquaredBlended(b1, b2));
                    fittingSystem.addToLHS(j, i, sums.getWeightedAnalyticSquaredBlended(b2, b1));
                }
            }
        }
    }
}
