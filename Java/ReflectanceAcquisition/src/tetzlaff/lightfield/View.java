package tetzlaff.lightfield;

import tetzlaff.gl.helpers.Matrix4;

public class View<ImageType>
{
	public final Matrix4 cameraPose;
	public final Projection projection;
	public final ImageType image;
	
	public View(Matrix4 cameraPose, Projection projection, ImageType image) 
	{
		this.cameraPose = cameraPose;
		this.projection = projection;
		this.image = image;
	}
}
