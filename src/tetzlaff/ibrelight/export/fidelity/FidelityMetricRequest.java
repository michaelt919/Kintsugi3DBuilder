package tetzlaff.ibrelight.export.fidelity;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.IntStream;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class FidelityMetricRequest implements IBRRequest
{
    private static final boolean DEBUG = false;

    private static final boolean USE_PERCEPTUALLY_LINEAR_ERROR = true;

    private static final boolean VIEW_IMPORTANCE_ENABLED = true;
    private static final boolean VIEW_IMPORTANCE_PREDICTION_ENABLED = false;
    private static final boolean CUMULATIVE_ERROR_CALCULATION_ENABLED = false;

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

    private static double estimateError(
        Vector3 targetDirection, Vector3[] viewDirections, double[][] error, double[][] errorDistances, Double targetDistanceOverride)
    {
        double[] distances = new double[viewDirections.length];

        // Keep the furthest saved view at the top of the priority queue to be replaced if a closer view appears
        PriorityQueue<Integer> nearestViews = new PriorityQueue<>(5,
            Comparator.<Integer>comparingDouble(i -> distances[i]).reversed());

        for (int i = 0; i < viewDirections.length; i++)
        {
            distances[i] = Math.max(0.0001, Math.acos(Math.max(-1.0, Math.min(1.0f, viewDirections[i].dot(targetDirection)))));
            if (distances[i] < Math.PI / 2) // must be less than a 90 degree angle
            {
                if (nearestViews.size() < 5)
                {
                    nearestViews.offer(i);
                }
                else if (distances[i] < distances[nearestViews.peek()])
                {
                    // replace old head with the new view
                    nearestViews.poll();
                    nearestViews.offer(i);
                }
            }
        }

        if (nearestViews.isEmpty()) // No usable views
        {
            return 1.0;
        }
        else if (nearestViews.size() <= 2) // Only a single view will be used to estimate error
        {
            if (nearestViews.size() == 2)
            {
                nearestViews.poll(); // Remove the head, which is the further away of the two
            }

            int i = nearestViews.poll();

            // Find the first applicable error estimate.
            // The distance to the nearest view for the conditions under which the reference error was computed
            // must be at least as great as the distance from the target view to the closest available view.
            int j = 0;
            while(j < error[i].length && errorDistances[i][j] < (targetDistanceOverride != null ? targetDistanceOverride : distances[i]))
            {
                j++;
            }

            if (j < error[i].length)
            {
                return error[i][j];
            }
            else
            {
                return 1.0;
            }
        }
        else // Weighted average of up to four views
        {
            double thresholdDistance = distances[nearestViews.poll()];

            double minDistance = targetDistanceOverride != null
                ? targetDistanceOverride
                : nearestViews.stream() // Find the distance to the closest available view.
                    .map(i -> distances[i])
                    .min(Comparator.naturalOrder())
                    .orElse(Math.PI / 2);

            double sumError = 0.0;
            double sumWeights = 0.0;

            for (int i : nearestViews)
            {
                // Find the first applicable error estimate.
                // The distance to the nearest view for the conditions under which the reference error was computed
                // must be at least as great as the distance from the target view to the closest available view.
                int j = 0;
                while(j < error[i].length && errorDistances[i][j] < minDistance)
                {
                    j++;
                }

                if (j < error[i].length && errorDistances[i][j] < thresholdDistance)
                {
                    // Buehler weighting formula
                    double weight = 1 / errorDistances[i][j] - 1 / thresholdDistance;
                    sumError += weight * error[i][j];
                    sumWeights += weight;
                }
            }

            if (sumWeights == 0.0)
            {
                return 1.0;
            }
            else
            {
                return sumError / sumWeights;
            }
        }
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

        File debugDirectory = null;
        if (DEBUG)
        {
            debugDirectory = new File(fidelityExportPath.getParentFile(), "debug");
            debugDirectory.mkdir();
        }

        ViewSet targetViewSet;
        Vector3[] targetDirections;

        if (fidelityVSETFile != null && fidelityVSETFile.exists())
        {
            ViewSet fidelityViewSet = ViewSet.loadFromVSETFile(fidelityVSETFile);

            targetViewSet = fidelityViewSet;
            targetDirections = IntStream.range(0, targetViewSet.getCameraPoseCount())
                .mapToObj(i -> fidelityViewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
                    .minus(resources.geometry.getCentroid()).normalized())
                .toArray(Vector3[]::new);
        }
        else
        {
            targetViewSet = null;
            targetDirections = null;
        }

        double[][] errors = new double[resources.viewSet.getCameraPoseCount()][];
        double[][] errorDistances = new double[resources.viewSet.getCameraPoseCount()][];

        try (PrintStream out = new PrintStream(fidelityExportPath);
            FidelityEvaluationTechnique<ContextType> fidelityTechnique =
                new HeuristicFidelityTechnique<>(USE_PERCEPTUALLY_LINEAR_ERROR, debugDirectory))
                //new LinearSystemFidelityTechnique<>(USE_PERCEPTUALLY_LINEAR_ERROR, debugDirectory))
        {
            fidelityTechnique.initialize(resources, settings, 256);
            fidelityTechnique.setMask(maskFile);

            if (VIEW_IMPORTANCE_ENABLED)
            {
                out.println("View Importance:");
                out.println();

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

                for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
                {
                    System.out.println(resources.viewSet.getImageFileName(i));
                    out.println(resources.viewSet.getImageFileName(i));

                    double lastMinDistance = 0.0;
                    double minDistance;

                    List<Double> distanceList = new ArrayList<>(resources.viewSet.getCameraPoseCount());
                    List<Double> errorList = new ArrayList<>(resources.viewSet.getCameraPoseCount());

                    List<Integer> activeViewIndexList;

                    double newError = Double.POSITIVE_INFINITY;

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

                        String[] filenameParts = resources.viewSet.getImageFileName(i).split("\\.");
                        filenameParts[filenameParts.length - 1] = "png";

                        if (!activeViewIndexList.isEmpty())
                        {
                            distanceList.add(minDistance);

                            newError = fidelityTechnique.evaluateError(i,
                                DEBUG && activeViewIndexList.size() == resources.viewSet.getCameraPoseCount() - 1
                                    ? new File(debugDirectory, String.join(".", filenameParts)) : null);

                            errorList.add(newError);

                            lastMinDistance = minDistance;
                        }
                    }
                    while (Double.isFinite(newError) && !activeViewIndexList.isEmpty() && minDistance < Math.PI / 2);

                    errors[i] = errorList.stream().mapToDouble(x -> x).toArray();
                    errorDistances[i] = distanceList.stream().mapToDouble(x -> x).toArray();

                    for (Double distance : distanceList)
                    {
                        out.print(distance + "\t");
                    }

                    out.println();

                    for (Double error : errorList)
                    {
                        out.print(error + "\t");
                    }

                    out.println();
                    out.println();

                    if (callback != null)
                    {
                        callback.setProgress(i);
                    }
                }

                if (targetDirections != null)
                {
                    double fidelityMetric = 1.0 -
                        Arrays.stream(targetDirections)
                            .mapToDouble(targetDirection -> estimateError(targetDirection, viewDirections, errors, errorDistances, null))
                            .average()
                            .orElse(0.0);
                    out.println();
                    out.println("---------------------------");
                    out.println("Fidelity: " + fidelityMetric);
                    out.println("---------------------------");
                    out.println();
                }
            }

            if (CUMULATIVE_ERROR_CALCULATION_ENABLED)
            {
                out.println();

                if (callback != null)
                {
                    callback.setMaximum(resources.viewSet.getCameraPoseCount());
                    callback.setProgress(0);
                }

                List<Integer> activeViewIndexList = new ArrayList<>(resources.viewSet.getCameraPoseCount());
                fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);

                // Print views that are in the original view set
                // This also initializes the distances for the target views.
                for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                {
                    out.print(resources.viewSet.getImageFileName(j) + '\t');

                    activeViewIndexList.add(j);
                    fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);

                    int current = j;
                    double cumError = IntStream.range(0, resources.viewSet.getCameraPoseCount())
                        .filter(k -> k > current || !fidelityTechnique.isGuaranteedInterpolating())
                        .mapToDouble(fidelityTechnique::evaluateError)
                        .sum();

                    out.println(cumError);

                    // Make debug image
                    if (DEBUG)
                    {
                        String[] filenameParts = resources.viewSet.getImageFileName(j).split("\\.");
                        filenameParts[filenameParts.length - 1] = "png";

                        fidelityTechnique.evaluateError(j,
                            new File(debugDirectory, "cum_" + activeViewIndexList.size() + '_'
                                + String.join(".", filenameParts)));
                    }

                    if (callback != null)
                    {
                        callback.setProgress(activeViewIndexList.size());
                    }
                }
            }

            if (targetDirections != null && VIEW_IMPORTANCE_PREDICTION_ENABLED)
            {
                double[] targetDistances = IntStream.range(0, targetDirections.length)
                    .mapToDouble(i -> Double.MAX_VALUE)
                    .toArray();

                // Initialize all target distances
                for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
                {
                    for (int i = 0; i < targetDirections.length; i++)
                    {
                        targetDistances[i] = Math.min(targetDistances[i],
                            Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(viewDirections[j])))));
                    }
                }

                boolean[] targetUsed = new boolean[targetDirections.length];

                // Now initialize the predicted errors for all of the remaining target views
                double[] targetErrors = Arrays.stream(targetDirections)
                    .mapToDouble(targetDirection -> estimateError(targetDirection, viewDirections, errors, errorDistances, null))
                    .toArray();

                // Views that are in the target view set and NOT in the original view set
                int unused;
                double maxError;
                do
                {
                    unused = 0;
                    maxError = -1.0;
                    int maxErrorIndex = -1;

                    // Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
                    for (int i = 0; i < targetDirections.length; i++)
                    {
                        // Can't be previously used and must have more error than any other view
                        if (!targetUsed[i])
                        {
                            // Keep track of number of unused views at the same time
                            unused++;

                            if (targetErrors[i] > maxError)
                            {
                                maxError = targetErrors[i];
                                maxErrorIndex = i;
                            }
                        }
                    }

                    if (maxErrorIndex >= 0)
                    {
                        // Print the view to the file
                        out.print(targetViewSet.getImageFileName(maxErrorIndex) + '\t');

                        // Flag that its been used
                        targetUsed[maxErrorIndex] = true;
                        unused--;

                        double cumError = 0.0;

                        // Update all of the other target distances and errors
                        for (int i = 0; i < targetDirections.length; i++)
                        {
                            // Don't update previously used views
                            if (!targetUsed[i])
                            {
                                // distance
                                targetDistances[i] = Math.min(targetDistances[i],
                                    Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[maxErrorIndex])))));

                                // TODO account for the fact that there may be many target views in the same high-information region
                                // TODO and predict the point at which it would be better to switch to another region
                                targetErrors[i] = estimateError(targetDirections[i], viewDirections, errors, errorDistances, targetDistances[i]);
                                cumError += targetErrors[i];
                            }
                        }

                        out.println(cumError);
                    }
                }
                while (maxError > 0.0 && unused > 0);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
