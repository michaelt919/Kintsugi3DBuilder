package tetzlaff.ibrelight.export.prioritize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.export.fidelity.FidelityEvaluationTechnique;
import tetzlaff.ibrelight.export.fidelity.LinearSystemFidelityTechnique;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class PrioritizedViewSetRequest implements IBRRequest
{
    private static final boolean DEBUG = true;

    private static final boolean USE_PERCEPTUALLY_LINEAR_ERROR = true;

    private final File exportPath;
    private final File maskFile;
    private final ReadonlySettingsModel settings;

    public PrioritizedViewSetRequest(File exportPath, File maskFile, ReadonlySettingsModel settings)
    {
        this.exportPath = exportPath;
        this.maskFile = maskFile;
        this.settings = settings;
    }

    @Override
    public <ContextType extends Context<ContextType>>
        void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
            throws IOException
    {
        IBRResources<ContextType> resources = renderable.getResources();

        File debugDirectory;
        if (DEBUG)
        {
            debugDirectory = new File(exportPath.getParentFile(), "debug");
            debugDirectory.mkdir();
        }

        try (PrintStream debugOut = new PrintStream(new File(exportPath.getParentFile(), "debug.txt"));
            FidelityEvaluationTechnique<ContextType> fidelityTechnique =
                new LinearSystemFidelityTechnique<>(USE_PERCEPTUALLY_LINEAR_ERROR, debugDirectory))
        {
            fidelityTechnique.initialize(resources, settings, 256);
            fidelityTechnique.setMask(maskFile);

            // A list storing the indices in the original view set, which will eventually be ranked by priority.
            List<Integer> permutationIndices = IntStream.range(0, resources.viewSet.getCameraPoseCount()).boxed().collect(Collectors.toList());

            // Keeps track of the number of views remaining to be selected.
            int viewCount = permutationIndices.size();

            if (callback != null)
            {
                callback.setMaximum(permutationIndices.size());
                callback.setProgress(0);
            }

            // Basically performing a selection sort as a greedy algorithm to rank the views by priority
            while (viewCount > 1)
            {
                int nextViewIndex = -1; // The index within the permutationIndices list (not the index within the original view set).
                double minTotalError = Double.MAX_VALUE;

                for (int i = 0; i < viewCount; i++) // Consider each of the views that haven't yet been selected.
                {
                    // Get all the view indices that haven't been ranked, not including the one that's currently being considered.
                    int current = i;
                    List<Integer> activeViewIndexList = IntStream.range(0, viewCount)
                        .filter(k -> k != current)
                        .mapToObj(permutationIndices::get)
                        .collect(Collectors.toList());

                    fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);

                    double totalError;

                    if (fidelityTechnique.isGuaranteedInterpolating())
                    {
                        totalError = Stream.concat(Stream.of(permutationIndices.get(current)),
                                permutationIndices.subList(viewCount, permutationIndices.size()).stream())
                            .mapToDouble(fidelityTechnique::evaluateError)
                            .sum();
                    }
                    else
                    {
                        totalError = IntStream.range(0, permutationIndices.size())
                            .mapToDouble(fidelityTechnique::evaluateError)
                            .sum();
                    }

                    if (totalError < minTotalError)
                    {
                        nextViewIndex = i;
                        minTotalError = totalError;
                    }
                }

                // Swap the next view and the one in the next spot in the permutation index list.
                int tmp = permutationIndices.get(viewCount - 1);
                permutationIndices.set(viewCount - 1, permutationIndices.get(nextViewIndex));
                permutationIndices.set(nextViewIndex, tmp);

                // Print the view to the debug file
                debugOut.println(resources.viewSet.getImageFileName(permutationIndices.get(viewCount - 1)).split("\\.")[0]
                    + '\t' + minTotalError);

                viewCount--;

                if (callback != null)
                {
                    callback.setProgress(permutationIndices.size() - viewCount);
                }
            }

            try(FileOutputStream out = new FileOutputStream(exportPath))
            {
                resources.viewSet.createPermutation(permutationIndices).writeVSETFileToStream(out);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
