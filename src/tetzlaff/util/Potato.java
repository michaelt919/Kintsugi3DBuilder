package tetzlaff.util;

import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;

public class Potato
{
    private final int bumpCount;
    private final float bumpSize;
    private final float bumpHeight;
    private final int minTriangles;
    private Bump[] bumps;

    private VertexGeometry geometry;

    private static class Bump
    {
        private float theta;
        private float phi;
        private float size;
        private float height;
    }

    private double computeR(double phi, double theta)
    {
        double r = 1.0;

        for(Bump b : bumps)
        {
            double thetaDiff = theta - b.theta;
            double phiDiff = phi - b.phi;

            double sigmaTheta = Math.PI * b.size / 2;
            double sigmaPhi = sigmaTheta / Math.sin(theta);

            r += b.height / (2 * Math.PI * sigmaTheta * sigmaTheta)
                * Math.exp(-phiDiff * phiDiff / (2 * sigmaPhi * sigmaPhi)
                - thetaDiff * thetaDiff / (2 * sigmaTheta * sigmaTheta));
        }

        return r;
    }

    public Potato(int bumpCount, float bumpSize, float bumpHeight, int minTriangles)
    {
        this.bumpCount = bumpCount;
        this.bumpSize = bumpSize;
        this.bumpHeight = bumpHeight;
        this.minTriangles = minTriangles;

        this.bumps = new Bump[bumpCount];

        for (int i = 0; i < bumpCount; i++)
        {
            bumps[i] = new Bump();
            bumps[i].theta = (float)(Math.random() * 2 * Math.PI);
            bumps[i].phi = (float)Math.acos(Math.random());
            bumps[i].size = bumpSize;
            bumps[i].height = bumpHeight;
        }

        int phiSubdiv = 2 * (int)Math.round(Math.floor(Math.sqrt(0.25 * minTriangles)));
        int thetaSubdiv = (int)Math.round(Math.ceil(minTriangles * 0.5 / phiSubdiv)) + 1;

        List<Vector3> positions = new ArrayList<>(phiSubdiv * (thetaSubdiv - 1) + 2);
        List<Vector2> texCoords = new ArrayList<>((phiSubdiv + 1) * (thetaSubdiv + 1));

        positions.add(new Vector3(0, 0, (float)computeR(0, 0)));

        for (int j = 0; j <= phiSubdiv; j++)
        {
            texCoords.add(new Vector2((float) j / phiSubdiv, 0));
        }

        for (int i = 1; i < thetaSubdiv; i++)
        {
            for (int j = 0; j < phiSubdiv; j++)
            {
                double theta = i * 2 * Math.PI / thetaSubdiv;
                double phi = j * Math.PI / phiSubdiv;

                double r = computeR(phi, theta);
                positions.add(new Vector3(
                    (float)(r * Math.cos(phi) * Math.sin(theta)),
                    (float)(r * Math.sin(phi) * Math.sin(theta)),
                    (float)(r * Math.cos(theta))));

                texCoords.add(new Vector2((float)j / phiSubdiv, (float)i / thetaSubdiv));
            }

            texCoords.add(new Vector2(1.0f, (float)i / thetaSubdiv));
        }

        positions.add(new Vector3(0, 0, -(float)computeR(0, Math.PI)));

        for (int j = 0; j <= phiSubdiv; j++)
        {
            texCoords.add(new Vector2((float) j / phiSubdiv, 1.0f));
        }
    }
}
