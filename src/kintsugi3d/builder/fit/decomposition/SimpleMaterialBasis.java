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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleMaterialBasis implements MaterialBasis
{
    private class BasisData
    {
        private DoubleVector3 diffuseColor;
        private double[] redBasis;
        private double[] greenBasis;
        private double[] blueBasis;
        private int name;
        private boolean enabled;

        public BasisData()
        {
            this.diffuseColor = null;
            this.redBasis = null;
            this.greenBasis = null;
            this.blueBasis = null;
            this.name = -1;
            this.enabled = true;
        }

        public BasisData(DoubleVector3 diffuseColor, double[] redBasis, double[] greenBasis, double[] blueBasis,
                         int name, boolean enabled)
        {
            this.diffuseColor = diffuseColor;
            this.redBasis = redBasis;
            this.greenBasis = greenBasis;
            this.blueBasis = blueBasis;
            this.name = name;
            this.enabled = enabled;
        }

        public DoubleVector3 getDiffuseColor()
        {
            return diffuseColor;
        }

        public void setDiffuseColor(DoubleVector3 diffuseColor)
        {
            this.diffuseColor = diffuseColor;
        }

        public double[] getRedBasis()
        {
            return this.redBasis;
        }

        public void setRedBasis(double[] redBasis)
        {
            this.redBasis = redBasis;
        }

        public double[] getGreenBasis()
        {
            return greenBasis;
        }

        public void setGreenBasis(double[] greenBasis)
        {
            this.greenBasis = greenBasis;
        }

        public double[] getBlueBasis()
        {
            return blueBasis;
        }

        public void setBlueBasis(double[] blueBasis)
        {
            this.blueBasis = blueBasis;
        }

        public int getName()
        {
            return this.name;
        }

        public void setName(int name)
        {
            this.name = name;
        }

        public boolean isEnabled()
        {
            return this.enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }
    }
//    private final List<DoubleVector3> diffuseColors;
//
//    private final List<double[]> redBasis;
//    private final List<double[]> greenBasis;
//    private final List<double[]> blueBasis;
//
//    private final List<String> names;
//
//    private final List<DoubleVector3> disabledDiffuseColors;
//    private final List<double[]> disabledRedBasis;
//    private final List<double[]> disabledGreenBasis;
//    private final List<double[]> disabledBlueBasis;
//
//    private final List<String> disabledNames;

    private final List<BasisData> basisList;
    private final List<BasisData> disabledBasisList;

    private int materialCount;
    private int disabledMaterialCount;
    private final int specularResolution;

    public SimpleMaterialBasis(int materialCount, int specularResolution)
    {
//        diffuseColors = IntStream.range(0, materialCount).mapToObj(b -> DoubleVector3.ZERO).collect(Collectors.toList());
//        redBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).collect(Collectors.toList());
//        greenBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).collect(Collectors.toList());
//        blueBasis = IntStream.range(0, materialCount).mapToObj(b -> new double[specularResolution + 1]).collect(Collectors.toList());
//        names = IntStream.range(0, materialCount).mapToObj(String::valueOf).collect(Collectors.toList());
//
//        disabledDiffuseColors = new ArrayList<>(0);
//        disabledRedBasis = new ArrayList<>(0);
//        disabledGreenBasis = new ArrayList<>(0);
//        disabledBlueBasis = new ArrayList<>(0);
//        disabledNames = new ArrayList<>(0);
//
        this.basisList=new ArrayList<>(materialCount);
        this.basisList.addAll(IntStream.range(0, materialCount).mapToObj(b -> new BasisData()).collect(Collectors.toList()));

        this.disabledBasisList=new ArrayList<>(materialCount);

        this.materialCount = materialCount;
        this.disabledMaterialCount = 0;
        this.specularResolution = specularResolution;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public SimpleMaterialBasis(DoubleVector3[] diffuseColors, List<double[]> redBasis, List<double[]> greenBasis, List<double[]> blueBasis)
    {
        this.materialCount = redBasis.size();
        this.disabledMaterialCount = 0;
        this.specularResolution = redBasis.get(0).length - 1;

//        this.diffuseColors = new ArrayList<>(List.of(diffuseColors));
        this.basisList = new ArrayList<>(materialCount);
        this.basisList.addAll(IntStream.range(0, materialCount).mapToObj(b -> new BasisData()).collect(Collectors.toList()));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setDiffuseColor(diffuseColors[i]));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setRedBasis(redBasis.get(i)));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setGreenBasis(greenBasis.get(i)));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setBlueBasis(blueBasis.get(i)));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setName(i));

        this.disabledBasisList=new ArrayList<>(materialCount);
    }

    public SimpleMaterialBasis(List<BasisData> basisList, List<BasisData> disabledBasisList)
    {
        this.materialCount = basisList.size();
        this.disabledMaterialCount = disabledBasisList.size();
        this.specularResolution = basisList.get(0).getRedBasis().length - 1;
        this.basisList = basisList;
        this.disabledBasisList = disabledBasisList;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public SimpleMaterialBasis(List<Integer> names, List<DoubleVector3> diffuseColors,
                               List<double[]> redBasis, List<double[]> greenBasis, List<double[]> blueBasis,
                               List<Integer> disabledNames, List<DoubleVector3> disabledDiffuseColors,
                               List<double[]> disabledRedBasis, List<double[]> disabledGreenBasis, List<double[]> disabledBlueBasis)
    {
        this.materialCount = redBasis.size();
        this.specularResolution = redBasis.get(0).length - 1;

        this.basisList = new ArrayList<>(materialCount);
        this.basisList.addAll(IntStream.range(0, materialCount).mapToObj(b -> new BasisData()).collect(Collectors.toList()));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setDiffuseColor(diffuseColors.get(i)));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setRedBasis(redBasis.get(i)));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setGreenBasis(greenBasis.get(i)));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setBlueBasis(blueBasis.get(i)));
        IntStream.range(0, materialCount).forEach(i -> basisList.get(i).setName(names.get(i)));

        this.disabledBasisList = new ArrayList<>(materialCount);
        this.disabledMaterialCount = disabledRedBasis.size();
        this.disabledBasisList.addAll(IntStream.range(0, disabledMaterialCount).mapToObj(b -> new BasisData()).collect(Collectors.toList()));
        IntStream.range(0, disabledMaterialCount).forEach(i -> disabledBasisList.get(i).setDiffuseColor(diffuseColors.get(i)));
        IntStream.range(0, disabledMaterialCount).forEach(i -> disabledBasisList.get(i).setRedBasis(redBasis.get(i)));
        IntStream.range(0, disabledMaterialCount).forEach(i -> disabledBasisList.get(i).setGreenBasis(greenBasis.get(i)));
        IntStream.range(0, disabledMaterialCount).forEach(i -> disabledBasisList.get(i).setBlueBasis(blueBasis.get(i)));
        IntStream.range(0, disabledMaterialCount).forEach(i -> disabledBasisList.get(i).setName(disabledNames.get(i)));
        this.disabledBasisList.forEach(b -> b.setEnabled(false));
    }

    @Override
    public int getName(int b)
    {
        return basisList.get(b).getName();
    }

    @Override
    public String getDisplayName(int cardIndex)
    {
        return Integer.toString(getAllBasisData().get(cardIndex).getName());
    }

    private List<BasisData> getAllBasisData()
    {
        List<BasisData> combinedList = new ArrayList<>(basisList.size() + disabledBasisList.size());
        combinedList.addAll(basisList);
        for (BasisData b : disabledBasisList)
        {
            combinedList.add(b.getName(), b);
        }
        return combinedList;
    }

    @Override
    public DoubleVector3 getDiffuseColor(int b)
    {
//        Optional<BasisData> inEnabledList = basisList.stream().filter(basisData -> basisData.getName() == b).findFirst();
//        if (inEnabledList.isPresent())
//        {
//            return basisList.get(basisList.indexOf(inEnabledList.get())).getDiffuseColor();
//        }
//        else
//        {
//            int index = disabledBasisList.indexOf(disabledBasisList.stream().filter(basisData -> basisData.getName() == b).findFirst().get());
//            return disabledBasisList.get(index).getDiffuseColor();
//        }
        return getAllBasisData().get(b).getDiffuseColor();
    }

    @Override
    public List<DoubleVector3> getDiffuseColors()
    {
        return getAllBasisData().stream().map(BasisData::getDiffuseColor).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public double evaluateSpecularRed(int b, int m)
    {
//        int index = names.indexOf(Integer.toString(b));
//        if (index != -1)
//        {
//            return redBasis.get(index)[m];
//        }
//        else
//        {
//            index = disabledNames.indexOf(Integer.toString(b));
//            return disabledRedBasis.get(index)[m];
//        }
        return getAllBasisData().get(b).getRedBasis()[m];
    }

    @Override
    public double evaluateEnabledSpecularRed(int b, int m)
    {
        return basisList.get(b).getRedBasis()[m];
    }

    @Override
    public double evaluateSpecularGreen(int b, int m)
    {
//        int index = names.indexOf(Integer.toString(b));
//        if (index != -1)
//        {
//            return greenBasis.get(index)[m];
//        }
//        else
//        {
//            index = disabledNames.indexOf(Integer.toString(b));
//            return disabledGreenBasis.get(index)[m];
//        }
        return getAllBasisData().get(b).getGreenBasis()[m];
    }

    @Override
    public double evaluateEnabledSpecularGreen(int b, int m)
    {
        return basisList.get(b).getGreenBasis()[m];
    }

    @Override
    public double evaluateSpecularBlue(int b, int m)
    {
//        int index = names.indexOf(Integer.toString(b));
//        if (index != -1)
//        {
//            return blueBasis.get(index)[m];
//        }
//        else
//        {
//            index = disabledNames.indexOf(Integer.toString(b));
//            return disabledBlueBasis.get(index)[m];
//        }
        return getAllBasisData().get(b).getBlueBasis()[m];
    }

    @Override
    public double evaluateEnabledSpecularBlue(int b, int m)
    {
        return basisList.get(b).getBlueBasis()[m];
    }

    @Override
    public int getMaterialCount()
    {
        return materialCount;
    }

    @Override
    public int getDisabledMaterialCount()
    {
        return disabledMaterialCount;
    }

    @Override
    public int getSpecularResolution()
    {
        return specularResolution;
    }

    @Override
    public void deleteMaterial(int b)
    {
//        redBasis.remove(b);
//        greenBasis.remove(b);
//        blueBasis.remove(b);
//        diffuseColors.remove(b);
//        names.remove(b);
        Optional<BasisData> inEnabledList = basisList.stream().filter(basisData -> basisData.getName() == b).findFirst();
        if (inEnabledList.isPresent())
        {
            basisList.remove(inEnabledList.get());
        }
        else
        {
            Optional<BasisData> inDisabledList = disabledBasisList.stream().filter(basisData -> basisData.getName() == b).findFirst();
            inDisabledList.ifPresent(disabledBasisList::remove);
        }
        materialCount--;
    }

    @Override
    public void save(File outputDirectory, String filenameOverride)
    {
        SpecularFitSerializer.serializeBasisFunctions(basisList.size() + disabledBasisList.size(), specularResolution, this, outputDirectory, filenameOverride);
    }

    @Override
    public MaterialBasis copy()
    {
//        return new SimpleMaterialBasis(List.copyOf(basisList), List.copyOf(disabledBasisList));
        return new SimpleMaterialBasis(new ArrayList<>(basisList), new ArrayList<>(disabledBasisList));
//        return new SimpleMaterialBasis(diffuseColors.toArray(DoubleVector3[]::new),
//            List.copyOf(redBasis), List.copyOf(greenBasis), List.copyOf(blueBasis));
    }

    @Override
    public void disableMaterial(int b)
    {
        Optional<BasisData> inEnabledList = basisList.stream().filter(basisData -> basisData.getName() == b).findFirst();
        if (inEnabledList.isPresent())
        {
            int index = Math.min(b, disabledBasisList.size());
            // copy to disabled lists
            disabledBasisList.add(inEnabledList.get());
            disabledBasisList.get(disabledBasisList.size() - 1).setEnabled(false);
            disabledBasisList.sort(Comparator.comparing(BasisData::getName));
            // remove from enabled lists
            basisList.remove(inEnabledList.get());
            materialCount--;
            disabledMaterialCount++;
        }
    }

    @Override
    public void enableMaterial(int b)
    {
        // get persistent name
        Optional<BasisData> inDisabledList = disabledBasisList.stream().filter(basisData -> basisData.getName() == b).findFirst();
        if (inDisabledList.isPresent())
        {
            // add to enabled lists
            basisList.add(inDisabledList.get());
            basisList.get(basisList.size() - 1).setEnabled(true);
            basisList.sort(Comparator.comparing(BasisData::getName));
            // remove from disabled lists
            disabledBasisList.remove(inDisabledList.get());
            materialCount++;
            disabledMaterialCount--;
        }
    }

    @Override
    public boolean getIsEnabled(int b)
    {
        return getAllBasisData().get(b).isEnabled();
    }

    /**
     * Sets an element of the red basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of red basis function b, at element m.
     */
    public void setRed(int b, int m, double value)
    {
        getAllBasisData().get(b).getRedBasis()[m] = value;
    }

    /**
     * Sets an element of the green basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of green basis function b, at element m.
     */
    public void setGreen(int b, int m, double value)
    {
        getAllBasisData().get(b).getGreenBasis()[m] = value;
    }

    /**
     * Sets an element of the blue basis function
     * @param b the basis function to modify
     * @param m the discrete element of the basis function to modify
     * @param value the new value of blue basis function b, at element m.
     */
    public void setBlue(int b, int m, double value)
    {
        getAllBasisData().get(b).getBlueBasis()[m] = value;
    }
}
