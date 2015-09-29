package tetzlaff.imagebasedmicrofacet;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.ulf.ULFDrawable;
import tetzlaff.ulf.ULFLoadOptions;
import tetzlaff.ulf.ULFLoadingMonitor;
import tetzlaff.ulf.ULFRenderer;

public class ImageBasedMicrofacetRenderer<ContextType extends Context<ContextType>> implements ULFDrawable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
	private Program<ContextType> indexProgram;
	private ULFRenderer<ContextType> ulfRenderer;
	private SampledMicrofacetField<ContextType> microfacetField;
	private Trackball viewTrackball;
	private Trackball lightTrackball;
	private ULFLoadingMonitor callback;
	private boolean suppressErrors = false;
	
	public ImageBasedMicrofacetRenderer(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, File xmlFile, File meshFile, ULFLoadOptions loadOptions, 
			Trackball viewTrackball, Trackball lightTrackball)
    {
		this.context = context;
		this.program = program;
		this.viewTrackball = viewTrackball;
		this.lightTrackball = lightTrackball;
    	this.ulfRenderer = new ULFRenderer<ContextType>(context, program, indexProgram, xmlFile, meshFile, loadOptions, viewTrackball);
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
			if (microfacetField.diffuseTexture == null)
			{
				program.setUniform("useDiffuseTexture", false);
			}
			else
			{
				program.setUniform("useDiffuseTexture", true);
				program.setTexture("diffuseMap", microfacetField.diffuseTexture);
			}
			
			if (microfacetField.normalTexture == null)
			{
				program.setUniform("useNormalTexture", false);
			}
			else
			{
				program.setUniform("useNormalTexture", true);
				program.setTexture("normalMap", microfacetField.normalTexture);
			}
	
			program.setUniform("diffuseRemovalAmount", 1.0f);
			program.setUniform("lightPos", 
					new Vector3(lightTrackball.getRotationMatrix().getColumn(2))
						.times(lightTrackball.getScale() / viewTrackball.getScale())
						.plus(ulfRenderer.getLightField().proxy.getCentroid()));
			program.setUniform("lightIntensity", new Vector3(1.0f, 1.0f, 1.0f));
			
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
	
				indexProgram.setUniform("lightPos", 
						new Vector3(lightTrackball.getRotationMatrix().getColumn(2))
							.times(lightTrackball.getScale() / viewTrackball.getScale())
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
		if (microfacetField.diffuseTexture != null)
		{
			microfacetField.diffuseTexture.delete();
		}
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
}
