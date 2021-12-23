package tetzlaff.optimization;

public interface Basis
{
    double evaluate(int basisFunction, int location);
    void contributeToFittingSystem(int locationCurrent, int locationPrev, int instanceCount, MatrixBuilderSums sums, MatrixSystem fittingSystem);
}
