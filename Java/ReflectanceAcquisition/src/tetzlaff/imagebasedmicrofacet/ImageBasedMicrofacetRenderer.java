package tetzlaff.imagebasedmicrofacet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.ulf.ULFDrawable;
import tetzlaff.ulf.ULFLoadOptions;
import tetzlaff.ulf.ULFLoadingMonitor;
import tetzlaff.ulf.ULFRenderer;

public class ImageBasedMicrofacetRenderer<ContextType extends Context<ContextType>> implements ULFDrawable<ContextType>, TrackballLightModel
{
	private ContextType context;
	private Program<ContextType> program;
	private Program<ContextType> indexProgram;
	private ULFRenderer<ContextType> ulfRenderer;
	private SampledMicrofacetField<ContextType> microfacetField;
	private int activeTrackball = 0;
	private List<Trackball> trackballs;
	private List<Vector3> lightColors;
	private ULFLoadingMonitor callback;
	private boolean suppressErrors = false;
	
	public ImageBasedMicrofacetRenderer(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, File xmlFile, File meshFile, ULFLoadOptions loadOptions, 
			List<Trackball> trackballs)
    {
		this.context = context;
		this.program = program;
		this.trackballs = trackballs;
		this.trackballs.get(0).setEnabled(true);
		this.lightColors = new ArrayList<Vector3>(trackballs.size());
		this.lightColors.add(new Vector3(1.0f, 1.0f, 1.0f));
		for (int i = 1; i < this.trackballs.size(); i++)
		{
			this.trackballs.get(i).setEnabled(false);
			this.lightColors.add(new Vector3(0.0f, 0.0f, 0.0f));
		}
    	this.ulfRenderer = new ULFRenderer<ContextType>(context, program, indexProgram, xmlFile, meshFile, loadOptions, trackballs.get(0));
    }

	@Override
	public void initialize() 
	{
		ulfRenderer.initialize();
		try
		{
			microfacetField = new SampledMicrofacetField<ContextType>(ulfRenderer.getLightField(), 
					new File(new File(ulfRenderer.getGeometryFile().getParentFile(), "textures"), "diffuse.png"),
					new File(new File(ulfRenderer.getGeometryFile().getParentFile(), "textures"), "normal.png"), context);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		if (callback != null)
		{
			callback.loadingComplete();
		}
	}

	@Override
	public void update() 
	{
	}

	@Override
	public void draw() 
	{
		try
		{
			if (microfacetField.normalTexture == null)
			{
				program.setUniform("useNormalTexture", false);
			}
			else
			{
				program.setUniform("useNormalTexture", true);
				program.setTexture("normalMap", microfacetField.normalTexture);
			}
	
			for (int i = 0; i < trackballs.size(); i++)
			{
				Matrix4 lightMatrix = Matrix4.lookAt(
	    				new Vector3(0.0f, 0.0f, 5.0f / trackballs.get(i).getScale()), 
	    				new Vector3(0.0f, 0.0f, 0.0f),
	    				new Vector3(0.0f, 1.0f, 0.0f)
	    			) // View
	    			.times(trackballs.get(i).getRotationMatrix())
	    			.times(Matrix4.translate(ulfRenderer.getLightField().proxy.getCentroid().negated()));
				
				program.setUniform("lightPos[" + i + "]", new Matrix3(lightMatrix).transpose().times(new Vector3(lightMatrix.getColumn(3).negated())));
				program.setUniform("lightIntensity[" + i + "]", this.lightColors.get(i));
				program.setUniform("virtualLightCount", Math.min(8, trackballs.size()));
			}
			
			if (indexProgram != null)
			{
				if (microfacetField.normalTexture == null)
				{
					indexProgram.setUniform("useNormalTexture", false);
				}
				else
				{
					indexProgram.setUniform("useNormalTexture", true);
					indexProgram.setTexture("normalMap", microfacetField.normalTexture);
				}
	
				// TODO multiple lights
				indexProgram.setUniform("lightPos", 
						new Vector3(trackballs.get(0).getRotationMatrix().getColumn(2))
							.times(trackballs.get(0).getScale() / trackballs.get(0).getScale())
							.plus(ulfRenderer.getLightField().proxy.getCentroid()));
				indexProgram.setUniform("lightIntensity", new Vector3(1.0f, 1.0f, 1.0f));
			}
			
			ulfRenderer.draw();
		}
		catch(Exception e)
		{
			if (!suppressErrors)
			{
				e.printStackTrace();
				suppressErrors = true; // Prevent excessive errors
			}
		}
	}

	@Override
	public void cleanup() 
	{
		ulfRenderer.cleanup();
		if (microfacetField.normalTexture != null)
		{
			microfacetField.normalTexture.delete();
		}
	}

	@Override
	public void setOnLoadCallback(ULFLoadingMonitor callback) 
	{
		this.callback = callback;
	}

	@Override
	public float getGamma() 
	{
		return ulfRenderer.getGamma();
	}

	@Override
	public float getWeightExponent() 
	{
		return ulfRenderer.getWeightExponent();
	}

	@Override
	public boolean isOcclusionEnabled() 
	{
		return ulfRenderer.isOcclusionEnabled();
	}

	@Override
	public float getOcclusionBias() 
	{
		return ulfRenderer.getOcclusionBias();
	}

	@Override
	public boolean getHalfResolution() 
	{
		return ulfRenderer.getHalfResolution();
	}

	@Override
	public void setGamma(float gamma) 
	{
		ulfRenderer.setGamma(gamma);
	}

	@Override
	public void setWeightExponent(float weightExponent)
	{
		ulfRenderer.setWeightExponent(weightExponent);
	}

	@Override
	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		ulfRenderer.setOcclusionEnabled(occlusionEnabled);
	}

	@Override
	public void setOcclusionBias(float occlusionBias) 
	{
		ulfRenderer.setOcclusionBias(occlusionBias);
	}

	@Override
	public void setHalfResolution(boolean halfResEnabled) 
	{
		ulfRenderer.setHalfResolution(halfResEnabled);
	}

	@Override
	public void setMultisampling(boolean multisamplingEnabled) 
	{
		ulfRenderer.setMultisampling(multisamplingEnabled);
	}

	@Override
	public boolean isViewIndexCacheEnabled() 
	{
		return ulfRenderer.isViewIndexCacheEnabled();
	}

	@Override
	public void setViewIndexCacheEnabled(boolean viewIndexCacheEnabled) 
	{
		ulfRenderer.setViewIndexCacheEnabled(viewIndexCacheEnabled);
	}

	@Override
	public void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException 
	{
		ulfRenderer.requestResample(width, height, targetVSETFile, exportPath);
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		ulfRenderer.setProgram(program);
		suppressErrors = false;
	}

	@Override
	public void setIndexProgram(Program<ContextType> program) 
	{
		this.indexProgram = program;
		ulfRenderer.setIndexProgram(program);
		suppressErrors = false;
	}

	@Override
	public int getActiveTrackball() 
	{
		return this.activeTrackball;
	}

	@Override
	public void setActiveTrackball(int i) 
	{
		this.trackballs.get(getActiveTrackball()).setEnabled(false);
		this.activeTrackball = i;
		ulfRenderer.setTrackball(this.trackballs.get(i));
		this.trackballs.get(i).setEnabled(true);
	}

	@Override
	public Vector3 getActiveLightColor() 
	{
		return lightColors.get(getActiveTrackball());
	}

	@Override
	public void setActiveLightColor(Vector3 color) 
	{
		lightColors.set(getActiveTrackball(), color);
	}
}
