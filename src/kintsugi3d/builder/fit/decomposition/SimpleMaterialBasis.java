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

import kintsugi3d.builder.io.specular.SpecularFitSerializer;
import kintsugi3d.gl.vecmath.DoubleVector3;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SimpleMaterialBasis implements MutableMaterialBasis
{
    /**
     * Uses LinkedHashMap to preserve insertion order.
     */
    private final LinkedHashMap<String, MutableBasisMaterialInfo> basis;

    private final int specularResolution;

    private int enabledMaterialCount;

    SimpleMaterialBasis(DoubleVector3[] diffuseColors, List<double[]> redBasis, List<double[]> greenBasis, List<double[]> blueBasis)
    {
        this.enabledMaterialCount = redBasis.size();
        this.specularResolution = redBasis.get(0).length - 1;

        this.basis = new LinkedHashMap<>(enabledMaterialCount);

        // If we're creating a new basis without disabled materials, just use b as GPU index.
        for (int b = 0; b < enabledMaterialCount; b++)
        {
            String name = String.format("%02d", b);
            String friendlyName = String.format("Material %d", b);
            this.basis.put(name, MutableBasisMaterialInfo.create(
                name, b, friendlyName, diffuseColors[b], redBasis.get(b), greenBasis.get(b), blueBasis.get(b)));
        }
    }

    private SimpleMaterialBasis(LinkedHashMap<String, MutableBasisMaterialInfo> basis)
    {
        this.basis = basis;
        this.specularResolution = basis.values().stream().findAny().orElseThrow().getResolution();
        this.enabledMaterialCount = (int)basis.values().stream().filter(BasisMaterialInfo::isEnabled).count();
    }

    /**
     * Enabled basis materials will be automatically assigned a GPU index based on the traversal order of names.
     * @param names
     * @param disabledNames
     * @param diffuseColors
     * @param redBasis
     * @param greenBasis
     * @param blueBasis
     * @param <KeyType>
     */
    public <KeyType> SimpleMaterialBasis(
        Map<KeyType, String> names, Map<KeyType, String> disabledNames, Map<KeyType, DoubleVector3> diffuseColors,
        Map<KeyType, double[]> redBasis, Map<KeyType, double[]> greenBasis, Map<KeyType, double[]> blueBasis)
    {
        this.enabledMaterialCount = redBasis.size();
        this.specularResolution = redBasis.values().stream().findAny().orElseThrow().length - 1;

        this.basis = new LinkedHashMap<>(enabledMaterialCount);

        int gpuIndex = 0;
        for (Entry<KeyType, String> entry : names.entrySet())
        {
            KeyType key = entry.getKey();
            String name = entry.getValue();

            basis.put(name, MutableBasisMaterialInfo.create(
                name, gpuIndex, getFriendlyName(name), diffuseColors.get(key),
                redBasis.get(key),  greenBasis.get(key), blueBasis.get(key)));
            gpuIndex++;
        }

        for (Entry<KeyType, String> entry : disabledNames.entrySet())
        {
            KeyType key = entry.getKey();
            String name = entry.getValue();
            basis.put(name, MutableBasisMaterialInfo.create(
                name, gpuIndex, getFriendlyName(name), diffuseColors.get(key),
                redBasis.get(key), greenBasis.get(key), blueBasis.get(key), false));
            gpuIndex++;
        }
    }

    private static String getFriendlyName(String name)
    {
        try
        {
            // Try to simplify material number presentation and include the prefix "Material"
            return String.format("Material %d", Integer.parseInt(name));
        }
        catch (NumberFormatException e)
        {
            return name;
        }
    }

    @Override
    public Collection<? extends IndexAssignableBasisMaterialInfo> getMaterials()
    {
        return basis.values();
    }

    @Override
    public List<BasisMaterialInfo> getIndexableMaterialList()
    {
        BasisMaterialInfo[] result = new BasisMaterialInfo[enabledMaterialCount];
        for (BasisMaterialInfo material : basis.values())
        {
            result[material.getGPUIndex()] = material;
        }

        return List.of(result);
    }

    @Override
    public BasisMaterialInfo getMaterial(String materialName)
    {
        return basis.get(materialName);
    }

    @Override
    public int getMaterialCount()
    {
        return basis.size();
    }

    @Override
    public int getEnabledMaterialCount()
    {
        return enabledMaterialCount;
    }

    @Override
    public int getSpecularResolution()
    {
        return specularResolution;
    }

    @Override
    public IndexAssignableBasisMaterialInfo deleteMaterial(String name)
    {
        MutableBasisMaterialInfo removed = basis.remove(name);
        if (removed != null)
        {
            // Shift indices of materials to the right of the one that was deleted.
            for (MutableBasisMaterialInfo material : basis.values())
            {
                if (material.getGPUIndex() > removed.getGPUIndex())
                {
                    material.setGPUIndex(material.getGPUIndex() - 1);
                }
            }

            if (removed.isEnabled())
            {
                enabledMaterialCount--;
            }
        }
        return removed;
    }

    @Override
    public void save(File outputDirectory, String filenameOverride)
    {
        SpecularFitSerializer.serializeBasisFunctions(specularResolution, this, outputDirectory, filenameOverride);
    }

    /**
     * Makes a deep copy in the sense that it copies each individual basis function.
     * @return
     */
    @Override
    public MutableMaterialBasis copy()
    {
        LinkedHashMap<String, MutableBasisMaterialInfo> copy = new LinkedHashMap<>(basis.size());
        for (var entry : basis.entrySet())
        {
            copy.put(entry.getKey(), entry.getValue().copy());
        }

        return new SimpleMaterialBasis(copy);
    }

    @Override
    public BasisMaterialInfo disableMaterial(String name)
    {
        MutableBasisMaterialInfo material = basis.get(name);

        if (material.isEnabled())
        {
            material.setEnabled(false);
            enabledMaterialCount--;
        }

        return material;
    }

    @Override
    public BasisMaterialInfo enableMaterial(String name)
    {
        MutableBasisMaterialInfo material = basis.get(name);

        if (!material.isEnabled())
        {
            material.setEnabled(true);
            enabledMaterialCount++;
        }

        return material;
    }

    @Override
    public boolean isMaterialEnabled(String name)
    {
        return basis.get(name).isEnabled();
    }
}
