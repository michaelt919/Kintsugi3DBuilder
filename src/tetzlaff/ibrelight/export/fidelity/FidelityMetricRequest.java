package tetzlaff.ibrelight.export.fidelity;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.stream.IntStream;

import tetzlaff.gl.Context;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.CubicHermiteSpline;

public class FidelityMetricRequest implements IBRRequest
{
    private static final boolean DEBUG = true;

    //private final static boolean USE_RENDERER_WEIGHTS = true;
    private static final boolean USE_PERCEPTUALLY_LINEAR_ERROR = false;

    private static final boolean VIEW_IMPORTANCE_ENABLED = false;
    private static final boolean VIEW_IMPORTANCE_PREDICTION_ENABLED = false;
    private static final boolean CUMULATIVE_ERROR_CALCULATION_ENABLED = false;
    private static final boolean SECOND_VIEW_ANALYSIS_ENABLED = true;

    private final File fidelityExportPath;
    private final File fidelityVSETFile;
    private final File maskFile;
    private final ReadonlySettingsModel settings;

    public FidelityMetricRequest(File exportPath, File targetVSETFile, File maskFile, ReadonlySettingsModel settings)
    {
        this.fidelityExportPath = exportPath;
        this.fidelityVSETFile = targetVSETFile;
        this.maskFile = maskFile;
        this.settings = settings;
    }

    private static double estimateErrorQuadratic(double baseline, double slope, double peak, double distance)
    {
        if (Double.isFinite(peak))
        {
            double peakDistance = 2 * peak / slope;
            if (distance > peakDistance)
            {
                return baseline + peak;
            }
            else
            {
                return baseline + slope * distance - slope * slope * distance * distance / (4 * peak);
            }
        }
        else
        {
            return baseline + slope * distance;
        }
    }

    private static double estimateErrorFromSplines(List<Vector3> directions, List<CubicHermiteSpline> splines, Vector3 targetDirection, double targetDistance)
    {
        PriorityQueue<SimpleEntry<Double, CubicHermiteSpline>> splineQueue
            = new PriorityQueue<>(Comparator.<SimpleEntry<Double, CubicHermiteSpline>>comparingDouble(SimpleEntry::getKey).reversed());

        for (int i = 0; i < directions.size(); i++)
        {
            double distance = Math.acos(Math.max(-1.0, Math.min(1.0f, directions.get(i).dot(targetDirection))));
            splineQueue.add(new SimpleEntry<>(distance, splines.get(i)));
            if (splineQueue.size() > 5)
            {
                splineQueue.remove();
            }
        }

        double thresholdInv = Math.min(500000.0, 1.0 / splineQueue.remove().getKey());

        double sum = 0.0;
        double sumWeights = 0.0;
        while (!splineQueue.isEmpty())
        {
            SimpleEntry<Double, CubicHermiteSpline> next = splineQueue.remove();
            double weight = Math.min(1000000.0, 1.0 / next.getKey()) - thresholdInv;
            sum += weight * next.getValue().applyAsDouble(targetDistance);
            sumWeights += weight;
        }

        return sum / sumWeights;
    }
    @Override
    public <ContextType extends Context<ContextType>>
        void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
            throws IOException
    {
        IBRResources<ContextType> resources = renderable.getResources();

        System.out.println();

        Vector3[] viewDirections = IntStream.range(0, resources.viewSet.getCameraPoseCount())
            .mapToObj(i -> resources.viewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
                .minus(resources.geometry.getCentroid()).normalized())
            .toArray(Vector3[]::new);

        double[] slopes = new double[resources.viewSet.getCameraPoseCount()];
        double[] peaks = new double[resources.viewSet.getCameraPoseCount()];
        double[] baselines = new double[resources.viewSet.getCameraPoseCount()];
        CubicHermiteSpline[] errorFunctions = new CubicHermiteSpline[resources.viewSet.getCameraPoseCount()];

        ViewSet targetViewSet = null;
        Vector3[] targetDirections = null;
        double[] targetBaselines = null;
        double[] targetSlopes = null;
        double[] targetPeaks = null;

        File debugDirectory;
        if (DEBUG)
        {
            debugDirectory = new File(fidelityExportPath.getParentFile(), "debug");
            debugDirectory.mkdir();
        }

        if (fidelityVSETFile != null && fidelityVSETFile.exists())
        {
            ViewSet fidelityViewSet = ViewSet.loadFromVSETFile(fidelityVSETFile);

            targetViewSet = fidelityViewSet;
            targetDirections = IntStream.range(0, targetViewSet.getCameraPoseCount())
                .mapToObj(i -> fidelityViewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
                    .minus(resources.geometry.getCentroid()).normalized())
                .toArray(Vector3[]::new);

            // Determine a function describing the error of each quadratic view by blending the slope and peak parameters from the known views.
            targetBaselines = new double[targetViewSet.getCameraPoseCount()];
            targetSlopes = new double[targetViewSet.getCameraPoseCount()];
            targetPeaks = new double[targetViewSet.getCameraPoseCount()];
        }

        try (PrintStream out = new PrintStream(fidelityExportPath);
            FidelityEvaluationTechnique<ContextType> fidelityTechnique =
                new TextureFitFidelityTechnique<>(USE_PERCEPTUALLY_LINEAR_ERROR))
//                USE_RENDERER_WEIGHTS ? new IBRFidelityTechnique<ContextType>()
//                    : new LinearSystemFidelityTechnique<ContextType>(USE_PERCEPTUALLY_LINEAR_ERROR, debugDirectory))
        {
            fidelityTechnique.initialize(resources, settings, 256);
            fidelityTechnique.setMask(maskFile);

            if (VIEW_IMPORTANCE_ENABLED)
            {
                System.out.println("View Importance:");

                double[][] viewDistances = new double[resources.viewSet.getCameraPoseCount()][resources.viewSet.getCameraPoseCount()];

                for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
                {
                    for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                    {
                        viewDistances[i][j] = Math.acos(Math.max(-1.0, Math.min(1.0f, viewDirections[i].dot(viewDirections[j]))));
                    }
                }

                if (callback != null)
                {
                    callback.setMaximum(resources.viewSet.getCameraPoseCount());
                    callback.setProgress(0.0);
                }

                out.println("#Name\tBaseline\tSlope\tPeak\tMinDistance\tError\t(CumError)");

                for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
                {
                    System.out.println(resources.viewSet.getImageFileName(i));
                    out.print(resources.viewSet.getImageFileName(i) + '\t');

                    double lastMinDistance = 0.0;
                    double minDistance;

                    List<Double> distances = new ArrayList<>(resources.viewSet.getCameraPoseCount());
                    List<Double> errors = new ArrayList<>(resources.viewSet.getCameraPoseCount());

                    baselines[i] = fidelityTechnique.evaluateBaselineError(i, DEBUG ?
                        new File(debugDirectory, "baseline_" + renderable.getActiveViewSet().getImageFileName(i))
                        : null);

                    distances.add(0.0);
                    errors.add(baselines[i]);

                    List<Integer> activeViewIndexList;

                    do
                    {
                        activeViewIndexList = new ArrayList<>(resources.viewSet.getCameraPoseCount());

                        minDistance = Float.MAX_VALUE;
                        for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                        {
                            if (i != j && viewDistances[i][j] > lastMinDistance)
                            {
                                minDistance = Math.min(minDistance, viewDistances[i][j]);
                                activeViewIndexList.add(j);
                                fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);
                            }
                        }

                        if (!activeViewIndexList.isEmpty())
                        {
                            distances.add(minDistance);

                            errors.add(fidelityTechnique.evaluateError(i,
                                DEBUG && activeViewIndexList.size() == resources.viewSet.getCameraPoseCount() - 1 ?
                                    new File(debugDirectory, renderable.getActiveViewSet().getImageFileName(i))
                                    : null));

                            lastMinDistance = minDistance;
                        }
                    }
                    while (VIEW_IMPORTANCE_PREDICTION_ENABLED && Double.isFinite(errors.get(errors.size() - 1)) && !activeViewIndexList.isEmpty() && minDistance < /*0*/ Math.PI / 4);

                    if (VIEW_IMPORTANCE_PREDICTION_ENABLED)
                    {
                        double[] errorArray;
                        double[] distanceArray;

                        errorArray = errors.stream().mapToDouble(error -> error).toArray();
                        distanceArray = distances.stream().mapToDouble(distance -> distance).toArray();

                        errorFunctions[i] = new CubicHermiteSpline(distanceArray, errorArray, true);

                        // Fit the error v. distance data to a quadratic with a few constraints.
                        // First, the quadratic must pass through the origin.
                        // Second, the slope at the origin must be positive.
                        // Finally, the "downward" slope of the quadratic will be clamped to the quadratic's maximum value
                        // to ensure that the function is monotonically increasing or constant.
                        // (So only half of the quadratic will actually be used.)
                        double peak = -1.0;
                        double slope = -1.0;
                        double maxDistance = distances.get(distances.size() - 1);
                        double prevMaxDistance;

                        // Every time we fit a quadratic, the data that would have been clamped on the downward slope messes up the fit.
                        // So we should keep redoing the fit without that data affecting the initial slope and only affecting the peak value.
                        // This continues until convergence (no new data points are excluded from the quadratic).
                        do
                        {
                            double sumSquareDistances = 0.0;
                            double sumCubeDistances = 0.0;
                            double sumFourthDistances = 0.0;
                            double sumErrorDistanceProducts = 0.0;
                            double sumErrorSquareDistanceProducts = 0.0;

                            double sumHighErrorDiffs = 0.0;
                            int countHighErrorDiffs = 0;

                            for (int k = 0; k < distances.size(); k++)
                            {
                                double distance = distances.get(k);
                                double errorDiff = Math.max(0.0, errors.get(k) - baselines[i]);

                                if (distance < maxDistance)
                                {
                                    double distanceSq = distance * distance;

                                    sumSquareDistances += distanceSq;
                                    sumCubeDistances += distance * distanceSq;
                                    sumFourthDistances += distanceSq * distanceSq;
                                    sumErrorDistanceProducts += errorDiff * distance;
                                    sumErrorSquareDistanceProducts += errorDiff * distanceSq;
                                }
                                else
                                {
                                    sumHighErrorDiffs += errorDiff;
                                    countHighErrorDiffs++;
                                }
                            }

                            double prevPeak = peak;
                            double prevSlope = slope;

                            // Fit error vs. distance to a quadratic using least squares: a*x^2 + slope * x = error
                            double d = sumCubeDistances * sumCubeDistances - sumFourthDistances * sumSquareDistances;
                            double a = (sumCubeDistances * sumErrorDistanceProducts - sumSquareDistances * sumErrorSquareDistanceProducts) / d;

                            slope = (sumCubeDistances * sumErrorSquareDistanceProducts - sumFourthDistances * sumErrorDistanceProducts) / d;

                            if (slope <= 0.0 || !Double.isFinite(slope) || countHighErrorDiffs > errors.size() - 5)
                            {
                                if (prevSlope < 0.0)
                                {
                                    // If its the first iteration, use a linear function
                                    // peak=0 is a special case for designating a linear function
                                    peak = 0.0;
                                    slope = sumErrorDistanceProducts / sumSquareDistances;
                                }
                                else
                                {
                                    // Revert to the previous peak and slope
                                    slope = prevSlope;
                                    peak = prevPeak;
                                }
                            }
                            else
                            {
                                // Peak can be determined from a and the slope.
                                double leastSquaresPeak = slope * slope / (-4 * a);

                                if (Double.isFinite(leastSquaresPeak) && leastSquaresPeak > 0.0)
                                {
                                    if (countHighErrorDiffs == 0)
                                    {
                                        peak = leastSquaresPeak;
                                    }
                                    else
                                    {
                                        // Do a weighted average between the least-squares peak and the average of all the errors that would be on the downward slope of the quadratic,
                                        // but are instead clamped to the maximum of the quadratic.
                                        // Clamp the contribution of the least-squares peak to be no greater than twice the average of the other values.
                                        peak = (Math.min(2 * sumHighErrorDiffs / countHighErrorDiffs, leastSquaresPeak)
                                            * (errors.size() - countHighErrorDiffs) + sumHighErrorDiffs)
                                            / errors.size();
                                    }
                                }
                                else if (prevPeak < 0.0)
                                {
                                    // If its the first iteration, use a linear function
                                    // peak=0 is a special case for designating a linear function
                                    peak = 0.0;
                                    slope = sumErrorDistanceProducts / sumSquareDistances;
                                }
                                else
                                {
                                    // Revert to the previous peak and slope
                                    slope = prevSlope;
                                    peak = prevPeak;
                                }
                            }

                            // Update the max distance and previous max distance.
                            prevMaxDistance = maxDistance;
                            maxDistance = 2 * peak / slope;
                        }
                        while (maxDistance < prevMaxDistance && peak > 0.0);

                        if (errors.size() >= 2)
                        {
                            out.println(baselines[i] + "\t" + slope + '\t' + peak + '\t' + minDistance + '\t' + errors.get(1));
                        }

                        System.out.println("Baseline: " + baselines[i]);
                        System.out.println("Slope: " + slope);
                        System.out.println("Peak: " + peak);
                        System.out.println();

                        slopes[i] = slope;
                        peaks[i] = peak;

                        for (Double distance : distances)
                        {
                            out.print(distance + "\t");
                        }
                        out.println();

                        for (Double error : errors)
                        {
                            out.print(error + "\t");
                        }
                        out.println();

                        out.println();
                    }

                    if (callback != null)
                    {
                        callback.setProgress(i);
                    }
                }
            }

            if (SECOND_VIEW_ANALYSIS_ENABLED)
            {
                if (callback != null)
                {
                    callback.setMaximum(resources.viewSet.getCameraPoseCount());
                    callback.setProgress(0.0);
                }

                out.println();
                out.println("Second view analysis:");
                out.println();

                for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
                {
                    if (i != resources.viewSet.getPrimaryViewIndex())
                    {
                        fidelityTechnique.updateActiveViewIndexList(Arrays.asList(resources.viewSet.getPrimaryViewIndex(), i));

                        for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                        {
                            if (i == j)
                            {
                                fidelityTechnique.evaluateError(j, new File(debugDirectory, "pairs_" + resources.viewSet.getImageFileName(j)));
                            }
                            else
                            {
                                fidelityTechnique.evaluateError(j);
                            }
                        }
                    }

                    if (callback != null)
                    {
                        callback.setProgress(i);
                    }
                }
            }

            if (CUMULATIVE_ERROR_CALCULATION_ENABLED && targetViewSet != null)
            {
                out.println();
                out.println("Expected error for views in target view set:");
                out.println();

                if (callback != null)
                {
                    callback.setMaximum(resources.viewSet.getCameraPoseCount());
                    callback.setProgress(0);
                }

                if (VIEW_IMPORTANCE_PREDICTION_ENABLED)
                {
                    for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                    {
                        double weightedBaselineSum = 0.0;
                        double weightedSlopeSum = 0.0;
                        double weightSum = 0.0;
                        double weightedPeakSum = 0.0;
                        double peakWeightSum = 0.0;

                        for (int k = 0; k < slopes.length; k++)
                        {
                            double weight = 1 / Math.max(0.000001, 1.0 -
                                Math.pow(Math.max(0.0, targetDirections[i].dot(viewDirections[k])), this.settings.getWeightExponent()))
                                - 1.0;

                            if (peaks[k] > 0)
                            {
                                weightedPeakSum += weight * peaks[k];
                                peakWeightSum += weight;
                            }

                            weightedBaselineSum += weight * baselines[k];
                            weightedSlopeSum += weight * slopes[k];
                            weightSum += weight;
                        }

                        targetBaselines[i] = weightedBaselineSum / weightSum;
                        targetSlopes[i] = weightedSlopeSum / weightSum;
                        targetPeaks[i] = peakWeightSum == 0.0 ? 0.0 : weightedPeakSum / peakWeightSum;
                    }
                }

                double[] targetDistances;
                double[] targetErrors = new double[targetViewSet.getCameraPoseCount()];

                targetDistances = IntStream.range(0, targetViewSet.getCameraPoseCount())
                    .mapToDouble(i -> Double.MAX_VALUE)
                    .toArray();

                boolean[] originalUsed = new boolean[resources.viewSet.getCameraPoseCount()];

                List<Integer> activeViewIndexList = new ArrayList<>(resources.viewSet.getCameraPoseCount());
                fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);

                // Print views that are only in the original view set and NOT in the target view set
                // This also initializes the distances for the target views.
                for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                {
                    // First determine if the view is in the target view set
                    boolean found = false;
                    for (int i = 0; !found && i < targetViewSet.getCameraPoseCount(); i++)
                    {
                        if (targetViewSet.getImageFileName(i).contains(resources.viewSet.getImageFileName(j).split("\\.")[0]))
                        {
                            found = true;
                        }
                    }

                    if (!found)
                    {
                        // If it isn't, then print it to the file
                        originalUsed[j] = true;
                        out.print(resources.viewSet.getImageFileName(j).split("\\.")[0]
                            + '\t' + baselines[j] + '\t' + slopes[j] + '\t' + peaks[j] + "\tn/a\t");

                        out.print(fidelityTechnique.evaluateError(j) + "\t");

                        activeViewIndexList.add(j);
                        fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);

                        double cumError = IntStream.range(0, resources.viewSet.getCameraPoseCount())
                            .filter(k -> !originalUsed[k] || !fidelityTechnique.isGuaranteedInterpolating())
                            .mapToDouble(fidelityTechnique::evaluateError)
                            .sum();

                        out.println(cumError);

                        // Make debug image
                        if (DEBUG)
                        {
                            fidelityTechnique.evaluateError(j,
                                new File(debugDirectory, "cum_" + activeViewIndexList.size() + '_'
                                    + resources.viewSet.getImageFileName(j)));
                        }

                        // Then update the distances for all of the target views
                        for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                        {
                            targetDistances[i] = Math.min(targetDistances[i],
                                Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(viewDirections[j])))));
                        }
                    }

                    if (callback != null)
                    {
                        callback.setProgress(activeViewIndexList.size());
                    }
                }

                if (VIEW_IMPORTANCE_PREDICTION_ENABLED)
                {
                    // Now update the errors for all of the target views
                    for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                    {
                        if (fidelityTechnique.isGuaranteedMonotonic())
                        {
                            targetErrors[i] = estimateErrorFromSplines(Arrays.asList(viewDirections), Arrays.asList(errorFunctions), targetDirections[i], targetDistances[i]);
                        }
                        else
                        {
                            targetErrors[i] = estimateErrorQuadratic(targetBaselines[i], targetSlopes[i], targetPeaks[i], targetDistances[i]);
                        }
                    }
                }

                boolean[] targetUsed = new boolean[targetErrors.length];

                int unusedOriginalViews = (int) IntStream.range(0, resources.viewSet.getCameraPoseCount())
                    .filter(j -> !originalUsed[j])
                    .count();

                // Views that are in both the target view set and the original view set
                // Go through these views in order of importance so that when loaded viewset = target viewset, it generates a ground truth ranking.
                while (unusedOriginalViews > 0)
                {
                    //                    double maxError = -1.0;
                    int nextViewTargetIndex = -1;
                    int nextViewOriginalIndex = -1;

                    // Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
                    //                    for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                    //                    {
                    //                        for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                    //                        {
                    //                            if (targetViewSet.getImageFileName(i).contains(resources.viewSet.getImageFileName(j).split("\\.")[0]))
                    //                            {
                    //                                // Can't be previously used and must have more error than any other view
                    //                                if (!originalUsed[j] && targetErrors[i] > maxError)
                    //                                {
                    //                                    maxError = targetErrors[i];
                    //                                    nextViewTargetIndex = i;
                    //                                    nextViewOriginalIndex = j;
                    //                                }
                    //                            }
                    //                        }
                    //                    }

                    double minTotalError = Double.MAX_VALUE;

                    int activeViewCount = activeViewIndexList.size();
                    activeViewIndexList.add(-1);

                    for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                    {
                        if (!targetUsed[i])
                        {
                            for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                            {
                                if (targetViewSet.getImageFileName(i).contains(resources.viewSet.getImageFileName(j).split("\\.")[0]))
                                {
                                    activeViewIndexList.set(activeViewCount, j);
                                    fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);

                                    double totalError = 0.0;

                                    if (fidelityTechnique.isGuaranteedInterpolating())
                                    {
                                        for (int k = 0; k < targetViewSet.getCameraPoseCount(); k++)
                                        {
                                            if (k != i && !targetUsed[k])
                                            {
                                                for (int l = 0; l < resources.viewSet.getCameraPoseCount(); l++)
                                                {
                                                    if (targetViewSet.getImageFileName(k).contains(
                                                        resources.viewSet.getImageFileName(l).split("\\.")[0]))
                                                    {
                                                        totalError += fidelityTechnique.evaluateError(l);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        totalError = IntStream.range(0, resources.viewSet.getCameraPoseCount())
                                            .mapToDouble(fidelityTechnique::evaluateError)
                                            .sum();
                                    }

                                    if (totalError < minTotalError)
                                    {
                                        nextViewTargetIndex = i;
                                        nextViewOriginalIndex = j;
                                        minTotalError = totalError;
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    // Print the view to the file
                    out.print(targetViewSet.getImageFileName(nextViewTargetIndex).split("\\.")[0] + '\t' + targetBaselines[nextViewTargetIndex]
                        + '\t' + targetSlopes[nextViewTargetIndex] + '\t' + targetPeaks[nextViewTargetIndex]
                        + '\t' + targetDistances[nextViewTargetIndex] + '\t' + targetErrors[nextViewTargetIndex] + '\t');

                    // Flag that its been used
                    targetUsed[nextViewTargetIndex] = true;
                    originalUsed[nextViewOriginalIndex] = true;
                    activeViewIndexList.set(activeViewCount, nextViewOriginalIndex);

                    // Make debug image
                    if (DEBUG)
                    {
                        fidelityTechnique.evaluateError(nextViewOriginalIndex,
                            new File(debugDirectory, "cum_" + activeViewIndexList.size() + '_'
                                + resources.viewSet.getImageFileName(nextViewOriginalIndex)));
                    }

                    double expectedTotalError = 0.0;

                    // Update all of the other target distances and errors that haven't been used yet
                    for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                    {
                        // Don't update previously used views
                        if (!targetUsed[i])
                        {
                            // distance
                            targetDistances[i] = Math.min(targetDistances[i],
                                Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[nextViewTargetIndex])))));

                            if (VIEW_IMPORTANCE_PREDICTION_ENABLED)
                            {
                                // error
                                if (fidelityTechnique.isGuaranteedMonotonic())
                                {
                                    targetErrors[i] = estimateErrorFromSplines(Arrays.asList(viewDirections),
                                        Arrays.asList(errorFunctions), targetDirections[i], targetDistances[i]);
                                }
                                else
                                {
                                    targetErrors[i] = estimateErrorQuadratic(targetBaselines[i], targetSlopes[i], targetPeaks[i], targetDistances[i]);
                                }
                                expectedTotalError += targetErrors[i];
                            }
                        }
                    }

                    out.println(expectedTotalError + "\t" + minTotalError);

                    // Count how many views from the original view set haven't been used.
                    unusedOriginalViews = 0;
                    unusedOriginalViews += IntStream.range(0, resources.viewSet.getCameraPoseCount())
                        .filter(j -> !originalUsed[j])
                        .count();

                    if (callback != null)
                    {
                        callback.setProgress(activeViewIndexList.size());
                    }
                }

                if (VIEW_IMPORTANCE_PREDICTION_ENABLED)
                {
                    // Views that are in the target view set and NOT in the original view set
                    int unused;
                    double maxErrorDiff;
                    do
                    {
                        unused = 0;
                        maxErrorDiff = -1.0;
                        int maxErrorIndex = -1;

                        // Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
                        for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                        {
                            // Can't be previously used and must have more error than any other view
                            if (!targetUsed[i])
                            {
                                // Keep track of number of unused views at the same time
                                unused++;

                                if (targetErrors[i] - targetBaselines[i] > maxErrorDiff)
                                {
                                    maxErrorDiff = targetErrors[i] - targetBaselines[i];
                                    maxErrorIndex = i;
                                }
                            }
                        }

                        if (maxErrorIndex >= 0)
                        {
                            // Print the view to the file
                            out.print(targetViewSet.getImageFileName(maxErrorIndex).split("\\.")[0] + '\t' + targetBaselines[maxErrorIndex] +
                                '\t' + targetSlopes[maxErrorIndex] + '\t' + targetPeaks[maxErrorIndex] + '\t' +
                                targetDistances[maxErrorIndex] + '\t' + targetErrors[maxErrorIndex] + '\t');

                            // Flag that its been used
                            targetUsed[maxErrorIndex] = true;
                            unused--;

                            double cumError = 0.0;

                            // Update all of the other target distances and errors
                            for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
                            {
                                // Don't update previously used views
                                if (!targetUsed[i])
                                {
                                    // distance
                                    targetDistances[i] = Math.min(targetDistances[i],
                                        Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[maxErrorIndex])))));

                                    // error
                                    if (fidelityTechnique.isGuaranteedMonotonic())
                                    {
                                        targetErrors[i] = estimateErrorFromSplines(Arrays.asList(viewDirections),
                                            Arrays.asList(errorFunctions), targetDirections[i], targetDistances[i]);
                                    }
                                    else
                                    {
                                        targetErrors[i] = estimateErrorQuadratic(targetBaselines[i], targetSlopes[i], targetPeaks[i], targetDistances[i]);
                                    }
                                    cumError += targetErrors[i];
                                }
                            }

                            out.println(cumError);
                        }
                    }
                    while (maxErrorDiff > 0.0 && unused > 0);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
