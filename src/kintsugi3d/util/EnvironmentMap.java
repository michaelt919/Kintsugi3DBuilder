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

import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.util.RadianceImageLoader.Image;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;

/**
 *
 */
public final class EnvironmentMap {
  public static final int DIR_SIDE = 32;
  public static final int NX = 3;
  public static final int NY = 4;
  public static final int NZ = 5;
  public static final int PX = 0;
  public static final int PY = 1;
  public static final int PZ = 2;
  public static final double[] SPEC_EXP = {
      -1.0, 0.5, 1.0, 5.0, 10.0, 20.0, 40.0, 60.0, 100.0, 200.0, 400.0, 600.0, 1000.0, -1.0
  };
  public static final int SPEC_COUNT = SPEC_EXP.length;
  public static final int[] SPEC_SIDE = {
      32, 32, 32, 32, 64, 64, 64, 64, 64, 64, 128, 128, 128, -1
  };
  private final float[][] diff;
  private final float[][] env;
  private final int side;
  private final float[][][] spec;

  //private final List<StructuredImportanceSampler.Zone> zones;

  //private transient double[][] envSH;

  private EnvironmentMap(int side) {
    this.side = side;

    env = new float[6][side * side * 3];
    diff = new float[6][DIR_SIDE * DIR_SIDE * 3];
    spec = new float[SPEC_COUNT][6][];
    for (int m = 0; m < SPEC_COUNT; m++) {
      int s = SPEC_SIDE[m] < 0 ? side : SPEC_SIDE[m];

      for (int i = 0; i < 6; i++) {
        spec[m][i] = new float[s * s * 3];
      }
    }

    //zones = new ArrayList<>();
  }

  public static EnvironmentMap createFromHDRFile(
      File hdrFile/*, boolean computeIrradiance, boolean computeStructuredImportance*/) throws
      IOException {
    Image hdr;
    try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(hdrFile))) {
      hdr = new RadianceImageLoader().read(stream);
    }

//    boolean isPanorama = false;
    EnvironmentMap map;
    if (hdrFile.getName().endsWith("_zvc.hdr")) {
      map = new EnvironmentMap(hdr.width / 3);
      convertCross(map.side, map.env, hdr);
    } else if (hdrFile.getName().endsWith("_pan.hdr")) {
      int side = Math.max(hdr.height, hdr.width) / 4;
      map = new EnvironmentMap(side);
      convertPanorama(map.side, map.env, hdr);
//      isPanorama = true;
    } else {
      int side = Math.max(hdr.height, hdr.width) / 4;
      map = new EnvironmentMap(side);
      convertPanorama(map.side, map.env, hdr);
    }

//    if (computeIrradiance) {
//      map.computeDiffuseIrradiance();
//      map.computeSpecularIrradiance();
//    }
//
//    if (computeStructuredImportance) {
//      if (isPanorama) {
//        map.computeStructuredImportanceFromPano(hdr);
//      } else {
//        map.computeStructuredImportance();
//      }
//    }

    return map;
  }

  public static EnvironmentMap loadFromEnvFile(File cachedData) throws IOException {
    try (
        DataInputStream in = new DataInputStream(
            new BufferedInputStream(new FileInputStream(cachedData)))) {
      int side = in.readInt();
      EnvironmentMap map = new EnvironmentMap(side);
      for (int i = 0; i < 6; i++) {
        for (int j = 0; j < map.env[i].length; j++) {
          map.env[i][j] = in.readFloat();
        }
      }

      int dirSide = in.readInt();
      if (dirSide != DIR_SIDE) {
        throw new IOException("Unexpected diffuse irradiance size: " + dirSide);
      }
      for (int i = 0; i < 6; i++) {
        for (int j = 0; j < map.diff[i].length; j++) {
          map.diff[i][j] = in.readFloat();
        }
      }

      for (int m = 0; m < map.spec.length; m++) {
        for (int i = 0; i < 6; i++) {
          for (int j = 0; j < map.spec[m][i].length; j++) {
            map.spec[m][i][j] = in.readFloat();
          }
        }
      }

//      int numSamples = -1;
//      try {
//        numSamples = in.readInt();
//      } catch (EOFException e) {
//        // skip reading in samples
//      }
//      if (numSamples >= 0 && numSamples <= 20000) {
//        for (int i = 0; i < numSamples; i++) {
//          map.zones.add(StructuredImportanceSampler.Zone.read(in));
//        }
//      }
      return map;
    }
  }

  public static EnvironmentMap loadFromFile(
      File data/*, boolean computeIrradiance, boolean computeStructuredImportance*/) throws
      IOException {
    if (data.getName().endsWith(".hdr")) {
      return createFromHDRFile(data/*, computeIrradiance, computeStructuredImportance*/);
    } 
    else if (data.getName().endsWith(".env")) {
      return loadFromEnvFile(data);
    } 
    else {
      throw new IOException("Unknown extension, must be .hdr or .env");
    }
  }

  public static void main(String... args) throws IOException {
    JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setAcceptAllFileFilterUsed(false);
    fc.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.getName().endsWith(".hdr");
      }

      @Override
      public String getDescription() {
        return "Radiance Environment Maps (.hdr)";
      }
    });
    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      File in = fc.getSelectedFile();

      String nameSansExt = in.getName().substring(0, in.getName().length() - 4);
      String baseName;
      if (nameSansExt.endsWith("_zvc")) {
        baseName = nameSansExt.substring(0, nameSansExt.length() - 4);
      } else if (nameSansExt.endsWith("_pan")) {
        baseName = nameSansExt.substring(0, nameSansExt.length() - 4);
      } else {
        baseName = nameSansExt;
      }

      EnvironmentMap toCache = loadFromFile(in/*, true, true*/);

      File out = new File(in.getParent() + File.separator + baseName + ".env");
      toCache.write(out);
    }
  }

  public static DoubleVector3 sample(float[][] env, int side, DoubleVector3 coord) {
    double sc;
    double tc;
    double ma;
    int face;

    if (Math.abs(coord.x) > Math.abs(coord.y) && Math.abs(coord.x) > Math.abs(coord.z)) {
      if (coord.x >= 0.0) {
        ma = coord.x;
        sc = -coord.z;
        tc = -coord.y;
        face = PX;
      } else {
        ma = -coord.x;
        sc = coord.z;
        tc = -coord.y;
        face = NX;
      }
    } else if (Math.abs(coord.y) > Math.abs(coord.x) && Math.abs(coord.y) > Math.abs(coord.z)) {
      if (coord.y >= 0) {
        ma = coord.y;
        sc = coord.x;
        tc = coord.z;
        face = PY;
      } else {
        ma = -coord.y;
        sc = coord.x;
        tc = -coord.z;
        face = NY;
      }
    } else {
      if (coord.z >= 0) {
        ma = coord.z;
        sc = coord.x;
        tc = -coord.y;
        face = PZ;
      } else {
        ma = -coord.z;
        sc = -coord.x;
        tc = -coord.y;
        face = NZ;
      }
    }

    double s = Math.max(0.0, Math.min(0.5 * (sc / ma + 1.0) * side, side - 2));
    double t = Math.max(0.0, Math.min(0.5 * (tc / ma + 1.0) * side, side - 2));

    int x = (int) Math.floor(s);
    int y = (int) Math.floor(t);
    s -= x;
    t -= y;

    int o1 = y * side * 3 + x * 3;
    int o2 = y * side * 3 + (x + 1) * 3;
    int o3 = (y + 1) * side * 3 + x * 3;
    int o4 = (y + 1) * side * 3 + (x + 1) * 3;

    return new DoubleVector3((1 - t) * ((1 - s) * env[face][o1] + s * env[face][o2]) + t * ((1 - s) * env[face][o3]
            + s * env[face][o4]),
        (1 - t) * ((1 - s) * env[face][o1 + 1] + s * env[face][o2 + 1]) + t * (
            (1 - s) * env[face][o3 + 1] + s * env[face][o4 + 1]),
        (1 - t) * ((1 - s) * env[face][o1 + 2] + s * env[face][o2 + 2]) + t * (
            (1 - s) * env[face][o3 + 2] + s * env[face][o4 + 2]));
  }

  public static double texelCoordPanoramaSolidAngle(int tx, int ty, int width, int height) {
    // scale to [0, 1] range and offset by 0.5 to be point in the texel center
    double s = (tx + 0.5) / width;
    double t = (ty + 0.5) / height;

    double invResolutionS = 0.5 / width;
    double invResolutionT = 0.5 / height;

    double s0 = s - invResolutionS;
    double t0 = t - invResolutionT;
    double s1 = s + invResolutionS;
    double t1 = t + invResolutionT;

    double sa =
        areaElementPanorama(s0, t0) - areaElementPanorama(s0, t1) - areaElementPanorama(s1, t0)
            + areaElementPanorama(s1, t1);
    return Math.abs(sa);
  }

  public static double texelCoordSolidAngle(int tx, int ty, int size) {
    // scale up to [-1, 1] range (inclusive), offset by 0.5 to point to texel center.
    double u = 2.0 * ((tx + 0.5) / (double) size) - 1.0;
    double v = 2.0 * ((ty + 0.5) / (double) size) - 1.0;

    double invResolution = 1.0 / size;

    // StaticUtilities and V are the -1..1 texture coordinate on the current face.
    // Get projected area for this texel
    double x0 = u - invResolution;
    double y0 = v - invResolution;
    double x1 = u + invResolution;
    double y1 = v + invResolution;

    return areaElement(x0, y0) - areaElement(x0, y1) - areaElement(x1, y0) + areaElement(x1, y1);
  }

  public static float[] toPanorama(float[][] env, int side, int newWidth, int newHeight) {
    float[] pano = new float[newWidth * newHeight * 3];
    DoubleVector3 dir = DoubleVector3.ZERO;
    DoubleVector3 color = DoubleVector3.ZERO;
    for (int y = 0; y < newHeight; y++) {
      for (int x = 0; x < newWidth; x++) {
        dir = toVectorFromPanorama(x, y, newWidth, newHeight);

        color = sample(env, side, dir);
        pano[y * newWidth * 3 + x * 3 + 0] = (float)color.x;
        pano[y * newWidth * 3 + x * 3 + 1] = (float)color.y;
        pano[y * newWidth * 3 + x * 3 + 2] = (float)color.z;
      }
    }

    return pano;
  }

  public static void toPanoramaCoord(DoubleVector3 dir, int width, int height, double[] out) {
    // x = cos(theta) cos(phi)
    // y = sin(phi)
    // z = sin(theta) cos(phi)
    double lat = Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))); // -pi/2 to pi/2
    double lon = Math.acos(Math.max(-1.0, Math.min(1.0, dir.x / Math.cos(lat)))); // 0 to pi
    if (dir.z < 0.0) {
      lon = 2.0 * Math.PI - lon; // 0 to 2pi
    }

    // x axis of image is longitude, y axis is latitude
    double px = (width - 1.0) * lon / (2.0 * Math.PI);
    double py = (height - 1.0) * (lat + Math.PI / 2.0) / Math.PI;

    out[0] = px;
    out[1] = py;
  }

  public static DoubleVector3 toVectorFromCubeMap(int face, int tx, int ty, int side) {
    // technically these are sc / |ma| and tc / |ma| but we assert
    // that |ma| = 1
    float sc = 2f * (tx + 0.5f) / (float) side - 1f;
    float tc = 2f * (ty + 0.5f) / (float) side - 1f;
    DoubleVector3 v = DoubleVector3.ZERO;
    switch (face) {
    case PX: // px
      v = new DoubleVector3(1.0, -tc, -sc);
      break;
    case PY: // py
      v = new DoubleVector3(sc, 1.0, tc);
      break;
    case PZ: // pz
      v = new DoubleVector3(sc, -tc, 1.0);
      break;
    case NX: // nx
      v = new DoubleVector3(-1.0, -tc, sc);
      break;
    case NY: // ny
      v = new DoubleVector3(sc, -1.0, -tc);
      break;
    case NZ: // nz
      v = new DoubleVector3(-sc, -tc, -1.0);
      break;
    }
    return v.normalized();
  }

  public static DoubleVector3 toVectorFromPanorama(int lx, int ly, int w, int h) {
    double lat = Math.PI * (ly / (h - 1.0)) - Math.PI / 2.0;
    double lon = 2.0 * Math.PI * (lx / (w - 1.0));
    return new DoubleVector3(Math.cos(lon) * Math.cos(lat), Math.sin(lat), Math.sin(lon) * Math.cos(lat));
  }

//  public void computeDiffuseIrradiance() {
//    computeDiffuseIrradiance(new Convolution.AshikhminDiffuse());
//  }
//
//  public void computeDiffuseIrradiance(Convolution.ConvolutionFunction func) {
//    buildEnvSHSamplesMaybe();
//    Convolution conv = new Convolution(func, DIR_SIDE, diff);
//    conv.convolveSH(envSH, NUM_SH_BANDS);
//    //                conv.convolve(env, side);
//  }
//
//  public TextureCubeMap computeOneSpecularIrradiance(
//      Framework framework, Convolution.ConvolutionFunction func) {
//    buildEnvSHSamplesMaybe();
//    Convolution conv = new Convolution(func, SPEC_SIDE[0], spec[0]);
//    conv.convolveSH(envSH, NUM_SH_BANDS);
//    //        conv.convolve(env, side);
//
//    TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
//    cmb.side(SPEC_SIDE[0]).wrap(Sampler.WrapMode.CLAMP).interpolated();
//    CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
//    data.positiveX(0).from(spec[0][PX]);
//    data.positiveY(0).from(spec[0][PY]);
//    data.positiveZ(0).from(spec[0][PZ]);
//    data.negativeX(0).from(spec[0][NX]);
//    data.negativeY(0).from(spec[0][NY]);
//    data.negativeZ(0).from(spec[0][NZ]);
//
//    return cmb.build();
//  }
//
//  public void computeSpecularIrradiance() {
//    buildEnvSHSamplesMaybe();
//    for (int i = 1; i < spec.length - 1; i++) {
//      double exp = SPEC_EXP[i];
//      int s = SPEC_SIDE[i];
//      Convolution conv = new Convolution(
//          new Convolution.AshikhminIsotropicSpecular(exp), s, spec[i]);
//      conv.convolveSH(envSH, NUM_SH_BANDS);
//    }
//
//    // compute environment map scaled by solid angle, which approximates the highest specularity
//    float[][] scaled = spec[SPEC_COUNT - 1];
//    for (int i = 0; i < 6; i++) {
//      for (int y = 0; y < side; y++) {
//        for (int x = 0; x < side; x++) {
//          float dsa = (float) texelCoordSolidAngle(x, y, side);
//          scaled[i][y * side * 3 + x * 3] = dsa * env[i][y * side * 3 + x * 3];
//          scaled[i][y * side * 3 + x * 3 + 1] = dsa * env[i][y * side * 3 + x * 3 + 1];
//          scaled[i][y * side * 3 + x * 3 + 2] = dsa * env[i][y * side * 3 + x * 3 + 2];
//        }
//      }
//    }
//
//    // duplicate 0th level from 1st
//    for (int i = 0; i < 6; i++) {
//      System.arraycopy(spec[1][i], 0, spec[0][i], 0, spec[1][i].length);
//    }
//  }
//
//  public TextureCubeMap createDiffuseMap(Framework framework) {
//    TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
//    cmb.side(DIR_SIDE).wrap(Sampler.WrapMode.CLAMP).interpolated();
//    CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
//    data.positiveX(0).from(diff[PX]);
//    data.positiveY(0).from(diff[PY]);
//    data.positiveZ(0).from(diff[PZ]);
//    data.negativeX(0).from(diff[NX]);
//    data.negativeY(0).from(diff[NY]);
//    data.negativeZ(0).from(diff[NZ]);
//    return cmb.build();
//  }

//  public Texture2D[] createDualParaboloidMap(Framework framework) {
//    float[] posZ = new float[4 * side * side * 3];
//    float[] negZ = new float[4 * side * side * 3];
//
//    Vector3 negDir = new Vector3();
//    Vector3 posDir = new Vector3();
//
//    Vector3 color = new Vector3();
//
//    double minZ = 10.0;
//    for (int y = 0; y < 2 * side; y++) {
//      for (int x = 0; x < 2 * side; x++) {
//        double s = x / (2.0 * side);
//        double t = y / (2.0 * side);
//
//        double rx = 2.0 * s - 1.0;
//        double ry = 2.0 * t - 1.0;
//        double zPos = 0.5 - 0.5 * (rx * rx + ry * ry);
//        double zNeg = -zPos;
//
//        posDir.set(rx, ry, zPos).normalize();
//        negDir.set(rx, ry, zNeg).normalize();
//
//        minZ = Math.min(minZ, Math.min(Math.abs(negDir.z), Math.abs(posDir.z)));
//
//        sample(negDir, color);
//        color.get(negZ, 2 * side * y * 3 + x * 3);
//
//        sample(posDir, color);
//        color.get(posZ, 2 * side * y * 3 + x * 3);
//      }
//    }
//
//    Texture2DBuilder pos = framework.newTexture2D();
//    pos.width(2 * side).height(2 * side).wrap(Sampler.WrapMode.CLAMP).interpolated();
//    ImageData<? extends TextureBuilder.BasicColorData> posData = pos.rgb();
//
//    posData.mipmap(0).from(posZ);
//    int numMips = (int) Math.floor(Math.log(2 * side) / Math.log(2)) + 1;
//    for (int i = 1; i < numMips; i++) {
//      int s = Math.max(1, (2 * side) >> i);
//      posData.mipmap(i).from(scale(posZ, 2 * side, s));
//    }
//
//    Texture2DBuilder neg = framework.newTexture2D();
//    neg.width(2 * side).height(2 * side).wrap(Sampler.WrapMode.CLAMP).interpolated();
//    ImageData<? extends TextureBuilder.BasicColorData> negData = neg.rgb();
//
//    negData.mipmap(0).from(negZ);
//    for (int i = 1; i < numMips; i++) {
//      int s = Math.max(1, (2 * side) >> i);
//      negData.mipmap(i).from(scale(negZ, 2 * side, s));
//    }
//
//    return new Texture2D[] { neg.build(), pos.build() };
//  }
//
//  public TextureCubeMap createEnvironmentMap(Framework framework) {
//    TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
//    cmb.side(side).wrap(Sampler.WrapMode.CLAMP).interpolated();
//    CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
//
//    // FIXME this really needs to be a well labeled method call in sampler
//    int numMips = (int) Math.floor(Math.log(side) / Math.log(2)) + 1;
//
//    data.positiveX(0).from(env[PX]);
//    data.positiveY(0).from(env[PY]);
//    data.positiveZ(0).from(env[PZ]);
//    data.negativeX(0).from(env[NX]);
//    data.negativeY(0).from(env[NY]);
//    data.negativeZ(0).from(env[NZ]);
//
//    for (int i = 1; i < numMips; i++) {
//      int s = Math.max(1, side >> i);
//      data.positiveX(i).from(scale(env[PX], side, s));
//      data.positiveY(i).from(scale(env[PY], side, s));
//      data.positiveZ(i).from(scale(env[PZ], side, s));
//      data.negativeX(i).from(scale(env[NX], side, s));
//      data.negativeY(i).from(scale(env[NY], side, s));
//      data.negativeZ(i).from(scale(env[NZ], side, s));
//    }
//    return cmb.build();
//  }
//
//  public TextureCubeMap[] createSpecularMaps(Framework framework) {
//    TextureCubeMap[] cubes = new TextureCubeMap[SPEC_COUNT];
//    for (int m = 0; m < SPEC_COUNT; m++) {
//      TextureCubeMapBuilder cmb = framework.newTextureCubeMap();
//      int s = (SPEC_SIDE[m] < 0 ? side : SPEC_SIDE[m]);
//      cmb.side(s).wrap(Sampler.WrapMode.CLAMP).interpolated();
//      CubeImageData<? extends TextureBuilder.BasicColorData> data = cmb.rgb();
//      data.positiveX(0).from(spec[m][PX]);
//      data.positiveY(0).from(spec[m][PY]);
//      data.positiveZ(0).from(spec[m][PZ]);
//      data.negativeX(0).from(spec[m][NX]);
//      data.negativeY(0).from(spec[m][NY]);
//      data.negativeZ(0).from(spec[m][NZ]);
//      cubes[m] = cmb.build();
//    }
//
//    return cubes;
//  }

  public float[][] getData() {
    return env;
  }

  public int getSide() {
    return side;
  }

//  public List<StructuredImportanceSampler.Zone> getZones() {
//    return zones;
//  }

  public void write(File data) throws IOException {
    try (
        DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(data)))) {
      out.writeInt(side);
      for (int i = 0; i < 6; i++) {
        for (int j = 0; j < env[i].length; j++) {
          out.writeFloat(env[i][j]);
        }
      }

      out.writeInt(DIR_SIDE);
      for (int i = 0; i < 6; i++) {
        for (int j = 0; j < diff[i].length; j++) {
          out.writeFloat(diff[i][j]);
        }
      }

      for (float[][] aSpec : spec)
      {
        for (int i = 0; i < 6; i++)
        {
          for (int j = 0; j < aSpec[i].length; j++)
          {
            out.writeFloat(aSpec[i][j]);
          }
        }
      }

//      out.writeInt(zones.size());
//      for (StructuredImportanceSampler.Zone z : zones) {
//        z.write(out);
//      }
    }
  }

//  private void buildEnvSHSamplesMaybe() {
//    if (envSH != null) {
//      return;
//    }
//
//    envSH = SphericalHarmonics.projectFromZones(zones, NUM_SH_BANDS);
//
//    //        SphericalHarmonics.Sample[] samples = SphericalHarmonics.createCubeMapSamples(side, NUM_SH_BANDS);
//    //                SphericalHarmonics.Sample[] samples = SphericalHarmonics.createSamples(144, NUM_SH_BANDS);
//    //        Vector3 color = new Vector3();
//    //        envSH = SphericalHarmonics.projectVector((direction, theta, phi) -> {
//    //            EnvironmentMap.sample(env, side, direction, color);
//    //            return color;
//    //        }, samples);
//  }
//
//  private void computeStructuredImportance() {
//    int width = 3 * side;
//    float[] pano = toPanorama(env, side, width, side);
//    zones.addAll(BinaryImportanceSampler.getSamples(MIN_SA, MAX_DEPTH, pano, width, side));
//  }
//
//  private void computeStructuredImportanceFromPano(RadianceImageLoader.Image img) {
//    zones.addAll(
//        BinaryImportanceSampler.getSamples(MIN_SA, MAX_DEPTH, img.data, img.width, img.height));
//  }
//
//  private void sample(@Const Vector3 coord, Vector3 out) {
//    sample(env, side, coord, out);
//  }

  // from AMD CubeMapGen
  private static double areaElement(double x, double y) {
    return Math.atan2(x * y, Math.sqrt(x * x + y * y + 1));
  }

  private static double areaElementPanorama(double s, double t) {
    // this is the integral of the area of a projected texel sum, given my chosen parameterization of the
    // sphere for the lat-lon panorama maps (namely, the y axis = sin(lat) and lat = t * PI).
    return 2.0 * Math.PI * s * Math.abs(Math.sin(Math.PI * t - Math.PI / 2));
  }

  // FIXME avery says I may have mirrored all the x images (so technically still a valid seamless environment
  // but won't be correct if i'm comparing it to the real place)
  private static void convertCross(int side, float[][] env, Image cross) {
    // px
    for (int y = 0; y < side; y++) {
      for (int x = 0; x < side; x++) {
        int crossX = 3 * side - x - 1;
        int crossY = 3 * side - y - 1;

        int faceOffset = 3 * y * side + 3 * x;
        int crossOffset = 3 * crossY * cross.width + 3 * crossX;
        env[PX][faceOffset] = cross.data[crossOffset];
        env[PX][faceOffset + 1] = cross.data[crossOffset + 1];
        env[PX][faceOffset + 2] = cross.data[crossOffset + 2];
      }
    }

    // py
    for (int y = 0; y < side; y++) {
      for (int x = 0; x < side; x++) {
        int crossX = side + x; // shifted over 1 column
        int crossY = 3 * side + y; // flip y axis

        int faceOffset = 3 * y * side + 3 * x;
        int crossOffset = 3 * crossY * cross.width + 3 * crossX;
        env[PY][faceOffset] = cross.data[crossOffset];
        env[PY][faceOffset + 1] = cross.data[crossOffset + 1];
        env[PY][faceOffset + 2] = cross.data[crossOffset + 2];
      }
    }

    // pz
    for (int y = 0; y < side; y++) {
      for (int x = 0; x < side; x++) {
        int crossX = side + x;
        int crossY = y;

        int faceOffset = 3 * y * side + 3 * x;
        int crossOffset = 3 * crossY * cross.width + 3 * crossX;
        env[PZ][faceOffset] = cross.data[crossOffset];
        env[PZ][faceOffset + 1] = cross.data[crossOffset + 1];
        env[PZ][faceOffset + 2] = cross.data[crossOffset + 2];
      }
    }

    // nx
    for (int y = 0; y < side; y++) {
      for (int x = 0; x < side; x++) {
        int crossX = side - x - 1;
        int crossY = 3 * side - y - 1;

        int faceOffset = 3 * y * side + 3 * x;
        int crossOffset = 3 * crossY * cross.width + 3 * crossX;
        env[NX][faceOffset] = cross.data[crossOffset];
        env[NX][faceOffset + 1] = cross.data[crossOffset + 1];
        env[NX][faceOffset + 2] = cross.data[crossOffset + 2];
      }
    }

    // ny
    for (int y = 0; y < side; y++) {
      for (int x = 0; x < side; x++) {
        int crossX = side + x;
        int crossY = side + y;

        int faceOffset = 3 * y * side + 3 * x;
        int crossOffset = 3 * crossY * cross.width + 3 * crossX;
        env[NY][faceOffset] = cross.data[crossOffset];
        env[NY][faceOffset + 1] = cross.data[crossOffset + 1];
        env[NY][faceOffset + 2] = cross.data[crossOffset + 2];
      }
    }

    // nz
    for (int y = 0; y < side; y++) {
      for (int x = 0; x < side; x++) {
        int crossX = 2 * side - x - 1;
        int crossY = 3 * side - y - 1;

        int faceOffset = 3 * y * side + 3 * x;
        int crossOffset = 3 * crossY * cross.width + 3 * crossX;
        env[NZ][faceOffset] = cross.data[crossOffset];
        env[NZ][faceOffset + 1] = cross.data[crossOffset + 1];
        env[NZ][faceOffset + 2] = cross.data[crossOffset + 2];
      }
    }
  }

  private static void convertPanorama(int side, float[][] env, Image pano) {
    // assumes equirectangular projection
    DoubleVector3 dir = DoubleVector3.ZERO;
    double[] p = new double[2];
    for (int i = 0; i < 6; i++) {
      for (int y = 0; y < side; y++) {
        for (int x = 0; x < side; x++) {
          dir = toVectorFromCubeMap(i, x, y, side);
          // geographic lat/lon spherical coordinates uses elevation angle from xy plane

          toPanoramaCoord(dir, pano.width, pano.height, p);

          double px = p[0];
          double py = p[1];
          double ax = px - Math.floor(px);
          double ay = py - Math.floor(py);

          int o1 =
              3 * (int) (Math.max(0, Math.min(pano.height - 1, Math.floor(py))) * pano.width + Math
                  .max(0, Math.min(pano.width - 1, Math.floor(px))));
          int o2 =
              3 * (int) (Math.max(0, Math.min(pano.height - 1, Math.floor(py))) * pano.width + Math
                  .max(0, Math.min(pano.width - 1, Math.floor(px + 1))));
          int o3 =
              3 * (int) (Math.max(0, Math.min(pano.height - 1, Math.floor(py + 1))) * pano.width
                  + Math.max(0, Math.min(pano.width - 1, Math.floor(px))));
          int o4 =
              3 * (int) (Math.max(0, Math.min(pano.height - 1, Math.floor(py + 1))) * pano.width
                  + Math.max(0, Math.min(pano.width - 1, Math.floor(px + 1))));

          int faceIndex = 3 * (y * side + x);
          env[i][faceIndex] = (float) (
              (1.0 - ay) * ((1.0 - ax) * pano.data[o1] + ax * pano.data[o2]) + ay * (
                  (1.0 - ax) * pano.data[o3] + ax * pano.data[o4]));
          faceIndex++;
          o1++;
          o2++;
          o3++;
          o4++;
          env[i][faceIndex] = (float) (
              (1.0 - ay) * ((1.0 - ax) * pano.data[o1] + ax * pano.data[o2]) + ay * (
                  (1.0 - ax) * pano.data[o3] + ax * pano.data[o4]));
          faceIndex++;
          o1++;
          o2++;
          o3++;
          o4++;
          env[i][faceIndex] = (float) (
              (1.0 - ay) * ((1.0 - ax) * pano.data[o1] + ax * pano.data[o2]) + ay * (
                  (1.0 - ax) * pano.data[o3] + ax * pano.data[o4]));
          faceIndex++;
        }
      }
    }
  }

//  private static float[] scale(float[] env, int side, int newSide) {
//    float[] data = new float[newSide * newSide * 3];
//
//    double ratio = side / (double) newSide;
//    for (int y = 0; y < newSide; y++) {
//      for (int x = 0; x < newSide; x++) {
//        // now iterate over the full image data
//        for (double y2 = ratio * y; y2 < (y + 1) * ratio; y2 += 1.0) {
//          for (double x2 = ratio * x; x2 < (x + 1) * ratio; x2 += 1.0) {
//            // linear interpolation
//            double ax = x2 - Math.floor(x2);
//            double ay = y2 - Math.floor(y2);
//            int o1 = (int) (Math.max(0, Math.min(Math.floor(y2), side - 1)) * side * 3
//                + Math.max(0, Math.min(Math.floor(x2), side - 1)) * 3);
//            int o2 = (int) (Math.max(0, Math.min(Math.floor(y2), side - 1)) * side * 3
//                + Math.max(0, Math.min(Math.floor(x2) + 1, side - 1)) * 3);
//            int o3 = (int) (Math.max(0, Math.min(Math.floor(y2) + 1, side - 1)) * side * 3
//                + Math.max(0, Math.min(Math.floor(x2), side - 1)) * 3);
//            int o4 = (int) (Math.max(0, Math.min(Math.floor(y2) + 1, side - 1)) * side * 3
//                + Math.max(0, Math.min(Math.floor(x2) + 1, side - 1)) * 3);
//
//            data[y * newSide * 3 + x * 3] +=
//                (1.0 - ay) * ((1.0 - ax) * env[o1] + ax * env[o2]) + ay * ((1.0 - ax) * env[o3]
//                    + ax * env[o4]);
//            data[y * newSide * 3 + x * 3 + 1] += //
//                (1.0 - ay) * ((1.0 - ax) * env[o1 + 1] + ax * env[o2 + 1]) + ay * (
//                    (1.0 - ax) * env[o3 + 1] + ax * env[o4 + 1]);
//            data[y * newSide * 3 + x * 3 + 2] += //
//                (1.0 - ay) * ((1.0 - ax) * env[o1 + 2] + ax * env[o2 + 2]) + ay * (
//                    (1.0 - ax) * env[o3 + 2] + ax * env[o4 + 2]);
//          }
//        }
//
//        // average
//        data[y * newSide * 3 + x * 3] /= (ratio * ratio);
//        data[y * newSide * 3 + x * 3 + 1] /= (ratio * ratio);
//        data[y * newSide * 3 + x * 3 + 2] /= (ratio * ratio);
//      }
//    }
//
//    return data;
//  }

//  private static final int MAX_DEPTH = 12;//
//  private static final double MIN_SA = Math.PI * Math.PI / 1444.0;
//  private static final int NUM_SH_BANDS = 10;
}
