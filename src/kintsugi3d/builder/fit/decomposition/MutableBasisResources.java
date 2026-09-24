/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.texture.TextureResolution;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.io.specular.SpecularFitSerializer;
import kintsugi3d.gl.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MutableBasisResources<ContextType extends Context<ContextType>> extends BasisResourcesBase<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(MutableBasisResources.class);

    private MutableMaterialBasis basis;

    private final BasisWeightResources<ContextType> weightResources;

    public MutableBasisResources(ContextType context, MutableMaterialBasis basis, TextureResolution textureResolution)
    {
        // Load both enabled and disabled materials.  Enabled materials should always precede disabled materials.
        super(context, basis.getMaterialCount(), basis.getSpecularResolution());
        this.basis = basis;

        // Assign GPU indices before loading weightmaps
        refreshGraphicsResources();

        this.weightResources = new BasisWeightResources<>(context,
            textureResolution.width, textureResolution.height, basis);
    }

    private MutableBasisResources(ContextType context, MutableMaterialBasis basis, File priorSolutionDirectory)
    {
        // Load both enabled and disabled materials.  Enabled materials should always precede disabled materials.
        super(context, basis.getMaterialCount(), basis.getSpecularResolution());
        this.basis = basis;

        // Assign GPU indices before loading weightmaps
        refreshGraphicsResources();

        BasisWeightResources<ContextType> loadedWeightResources = null;
        try
        {
            loadedWeightResources = BasisWeightResources.loadFromPriorSolution(context, priorSolutionDirectory, basis);
        }
        catch (IOException e)
        {
            LOG.error("Error loading weightmap resources from {}", priorSolutionDirectory, e);
        }
        this.weightResources = loadedWeightResources;

    }

    @Override
    public MutableMaterialBasis getBasis()
    {
        return basis;
    }

    public BasisWeightResources<ContextType> getWeightResources()
    {
        return weightResources;
    }

    public void setBasis(MutableMaterialBasis basis)
    {
        this.basis = basis;
        refreshGraphicsResources();
    }

    public BasisMaterialInfo deleteBasisMaterial(String name)
    {
        // Delete the basis materials themselves and the corresponding weight maps
        BasisMaterialInfo deleted = basis.deleteMaterial(name);

        if (weightResources != null)
        {
            weightResources.deleteWeightMap(deleted.getGPUIndex());

            // Need to refresh GPU indices before proceeding.
            refreshGraphicsResources();

            try
            {
                ViewSet viewSet = Global.io().getLoadedViewSet();
                File supportingFilesDir = viewSet.getSupportingFilesDirectory();

                // Refresh thumbnails since names will have shifted (brute force but fine since this shouldn't take long)
                new BasisImageCreator<>(getContext(), 2 * getBasisResolution() + 1)
                    .createImages(this, viewSet.getThumbnailImageDirectory());

                // Save basis functions and weight maps to prevent inconsistency with thumbnails if the user forgets to save manually.
                save(supportingFilesDir);
                weightResources.saveUnpacked("PNG", supportingFilesDir);
            }
            catch (IOException e)
            {
                LOG.error("An filesystem error occurred while deleting the basis material.", e);
            }
        }
        return deleted;
    }

    private BasisMaterialInfo disableBasisMaterial(String name)
    {
        return basis.disableMaterial(name);
    }

    private BasisMaterialInfo enableBasisMaterial(String name)
    {
        return basis.enableMaterial(name);
    }

    public BasisMaterialInfo toggleBasisMaterial(String name)
    {
        BasisMaterialInfo result;
        if (basis.isMaterialEnabled(name))
        {
            result = disableBasisMaterial(name);
        }
        else
        {
            result = enableBasisMaterial(name);
        }

        refreshGraphicsResources();

        return result;
    }

    /**
     * Loads basis functions from a prior solution.
     * Does not load diffuse basis colors, so a diffuse map should instead be optimized to cover diffuse.
     * @param priorSolutionDirectory The directory from which to load a prior solution.
     * @throws IOException If a part of the solution cannot be loaded form file.
     */
    public static <ContextType extends Context<ContextType>> MutableBasisResources<ContextType> loadFromPriorSolution(
        ContextType context, File priorSolutionDirectory)
    {
        try
        {
            MutableMaterialBasis basis =
                SpecularFitSerializer.deserializeBasisFunctions(priorSolutionDirectory);

            if (basis != null)
            {
                return new MutableBasisResources<>(context, basis, priorSolutionDirectory);
            }
            else
            {
                return null;
            }
        }
        catch (IOException e)
        {
            LOG.error("Error loading basis functions from {}", priorSolutionDirectory, e);
            return null;
        }
    }
}
