/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.util;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;

/**
 * Based on the "potato" shape from "Specular reflections and the estimation of shape from binocular disparity" by Muryy et al., 2013
 */
public class Potato
{
    private final Bump[] bumps;

    private final List<Vector3> positions;
    private final List<Vector2> texCoords;
    private final List<List<IntVector2>> faces;

    private static class Bump
    {
        private Vector3 direction;
        private float size;
        private float height;
    }

    private double computeR(Vector3 direction)
    {
        double r = 1.0;

        for (Bump b : bumps)
        {
            double diff = Math.acos(direction.dot(b.direction));
            double sigma = Math.PI * b.size / 12;
            r += b.height / (6 * Math.PI * sigma * sigma) * Math.exp(-diff * diff / (2 * sigma * sigma));
        }

        return r;
    }

    public Potato(int bumpCount, float bumpSize, float bumpHeight, int minTriangles)
    {
        this.bumps = new Bump[bumpCount];

        for (int i = 0; i < bumpCount; i++)
        {
            bumps[i] = new Bump();
            bumps[i].size = bumpSize;
            bumps[i].height = bumpHeight;

            double phi = Math.random() * 2 * Math.PI;
            double theta = Math.acos(2 * Math.random() - 1);
            double sinTheta = Math.sin(theta);
            bumps[i].direction = new Vector3(
                (float) (Math.cos(phi) * sinTheta),
                (float) (Math.sin(phi) * sinTheta),
                (float) Math.cos(theta));
        }

        int phiSubdiv = 2 * (int) Math.round(Math.floor(Math.sqrt(0.25 * minTriangles)));
        int thetaSubdiv = (int) Math.round(Math.ceil(minTriangles * 0.5 / phiSubdiv)) + 1;

        positions = new ArrayList<>(phiSubdiv * (thetaSubdiv - 1) + 2);
        texCoords = new ArrayList<>((phiSubdiv + 1) * (thetaSubdiv - 1) + 2);
        faces = new ArrayList<>(2 * phiSubdiv * (thetaSubdiv - 1));

        positions.add(new Vector3(0, 0, (float) computeR(new Vector3(0, 0, 1))));
        texCoords.add(new Vector2(0.5f, 0.0f));

        for (int j = 0; j < phiSubdiv - 1; j++)
        {
            faces.add(Collections.unmodifiableList(Arrays.asList(
                new IntVector2(0, 0),
                new IntVector2(j + 1, j + 1),
                new IntVector2(j + 2, j + 2))));
        }

        faces.add(Collections.unmodifiableList(Arrays.asList(
            new IntVector2(0, 0),
            new IntVector2(phiSubdiv, phiSubdiv),
            new IntVector2(1, phiSubdiv + 1))));

        for (int i = 1; i < thetaSubdiv; i++)
        {
            double theta = i * Math.PI / thetaSubdiv;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int j = 0; j < phiSubdiv; j++)
            {
                double phi = j * 2 * Math.PI / phiSubdiv;

                Vector3 direction = new Vector3(
                    (float) (Math.cos(phi) * sinTheta),
                    (float) (Math.sin(phi) * sinTheta),
                    (float) cosTheta);

                positions.add(direction.times((float)computeR(direction)));

                texCoords.add(new Vector2((float) (((double) j / phiSubdiv - 0.5) * sinTheta + 0.5), (float) i / thetaSubdiv));

                if (i < thetaSubdiv - 1 && j < phiSubdiv - 1)
                {
                    faces.add(Collections.unmodifiableList(Arrays.asList(
                        new IntVector2(j + (i - 1) * phiSubdiv + 1, j + (i - 1) * (phiSubdiv + 1) + 1),
                        new IntVector2(j + i * phiSubdiv + 1, j + i * (phiSubdiv + 1) + 1),
                        new IntVector2(j + i * phiSubdiv + 2, j + i * (phiSubdiv + 1) + 2))));

                    faces.add(Collections.unmodifiableList(Arrays.asList(
                        new IntVector2(j + (i - 1) * phiSubdiv + 2, j + (i - 1) * (phiSubdiv + 1) + 2),
                        new IntVector2(j + (i - 1) * phiSubdiv + 1, j + (i - 1) * (phiSubdiv + 1) + 1),
                        new IntVector2(j + i * phiSubdiv + 2, j + i * (phiSubdiv + 1) + 2))));
                }
            }

            texCoords.add(new Vector2((float)(sinTheta * 0.5 + 0.5), (float) i / thetaSubdiv));

            if (i < thetaSubdiv - 1)
            {
                faces.add(Collections.unmodifiableList(Arrays.asList(
                    new IntVector2(i * phiSubdiv, i * (phiSubdiv + 1) - 1),
                    new IntVector2((i + 1) * phiSubdiv, (i + 1) * (phiSubdiv + 1) - 1),
                    new IntVector2(i * phiSubdiv + 1, (i + 1) * (phiSubdiv + 1)))));

                faces.add(Collections.unmodifiableList(Arrays.asList(
                    new IntVector2((i - 1) * phiSubdiv + 1, i * (phiSubdiv + 1)),
                    new IntVector2(i * phiSubdiv, i * (phiSubdiv + 1) - 1),
                    new IntVector2(i * phiSubdiv + 1, (i + 1) * (phiSubdiv + 1)))));
            }
        }

        positions.add(new Vector3(0, 0, -(float) computeR(new Vector3(0, 0, -1))));
        texCoords.add(new Vector2(0.5f, 1.0f));

        for (int j = 0; j < phiSubdiv - 1; j++)
        {
            faces.add(Collections.unmodifiableList(Arrays.asList(
                new IntVector2(j + (thetaSubdiv - 2) * phiSubdiv + 2, j + (thetaSubdiv - 2) * (phiSubdiv + 1) + 2),
                new IntVector2(j + (thetaSubdiv - 2) * phiSubdiv + 1, j + (thetaSubdiv - 2) * (phiSubdiv + 1) + 1),
                new IntVector2((thetaSubdiv - 1) * phiSubdiv + 1, (thetaSubdiv - 1) * (phiSubdiv + 1) + 1))));
        }

        faces.add(Collections.unmodifiableList(Arrays.asList(
            new IntVector2((thetaSubdiv - 2) * phiSubdiv + 1, (thetaSubdiv - 1) * (phiSubdiv + 1)),
            new IntVector2((thetaSubdiv - 1) * phiSubdiv, (thetaSubdiv - 1) * (phiSubdiv + 1) - 1),
            new IntVector2((thetaSubdiv - 1) * phiSubdiv + 1, (thetaSubdiv - 1) * (phiSubdiv + 1) + 1))));
    }

    public List<Vector3> getPositions()
    {
        return Collections.unmodifiableList(positions);
    }

    public List<Vector2> getTexCoords()
    {
        return Collections.unmodifiableList(texCoords);
    }

    public List<List<IntVector2>> getFaces()
    {
        return Collections.unmodifiableList(faces);
    }

    public void writeToStream(PrintStream out)
    {
        for (Vector3 pos : positions)
        {
            out.println("v\t" + pos.x + '\t' + pos.y + '\t' + pos.z);
        }

        for (Vector2 tex : texCoords)
        {
            out.println("vt\t" + tex.x + '\t' + tex.y);
        }

        for (List<IntVector2> f : faces)
        {
            out.print('f');

            for (IntVector2 v : f)
            {
                out.print("\t" + (v.x + 1) + '/' + (v.y + 1));
            }

            out.println();
        }
    }

    public static void main(String[] args)
    {
        try(PrintStream out = new PrintStream(args[4], StandardCharsets.UTF_8))
        {
            new Potato(Integer.parseInt(args[0]), Float.parseFloat(args[1]), Float.parseFloat(args[2]), Integer.parseInt(args[3]))
                .writeToStream(out);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
