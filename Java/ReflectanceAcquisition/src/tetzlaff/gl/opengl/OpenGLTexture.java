/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import tetzlaff.gl.Texture;

abstract class OpenGLTexture implements Texture<OpenGLContext>, OpenGLFramebufferAttachment
{
	protected final OpenGLContext context;
	
	private int textureId;
	
	OpenGLTexture(OpenGLContext context) 
	{
		this.context = context;
		this.textureId = glGenTextures();
		this.context.openGLErrorCheck();
	}
	
	@Override
	public OpenGLContext getContext()
	{
		return this.context;
	}
	
	abstract int getOpenGLTextureTarget();
	abstract int getLevelCount();
	
	void bind()
	{
		glBindTexture(this.getOpenGLTextureTarget(), this.textureId);
		this.context.openGLErrorCheck();
	}
	
	int getTextureId()
	{
		return this.textureId;
	}
	
	void bindToTextureUnit(int textureUnitIndex)
	{
		if (textureUnitIndex < 0)
		{
			throw new IllegalArgumentException("Texture unit index cannot be negative.");
		}
		else if (textureUnitIndex > this.context.getMaxCombinedTextureImageUnits())
		{
			throw new IllegalArgumentException("Texture unit index (" + textureUnitIndex + ") is greater than the maximum allowed index (" + 
					(this.context.getMaxCombinedTextureImageUnits()-1) + ").");
		}
		glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
		this.context.openGLErrorCheck();
		this.bind();
	}
	
	@Override
	public void delete() 
	{
		glDeleteTextures(this.textureId);
		this.context.openGLErrorCheck();
	}

	@Override
	public void attachToDrawFramebuffer(int attachment, int level) 
	{
		if (level < 0)
		{
			throw new IllegalArgumentException("Texture level cannot be negative.");
		}
		if (level > this.getLevelCount())
		{
			throw new IllegalArgumentException("Illegal level index: " + level + ".  The texture only has " + this.getLevelCount() + " levels.");
		}
		glFramebufferTexture(GL_DRAW_FRAMEBUFFER, attachment, this.textureId, level);
		this.context.openGLErrorCheck();
	}

	@Override
	public void attachToReadFramebuffer(int attachment, int level) 
	{
		glFramebufferTexture(GL_READ_FRAMEBUFFER, attachment, this.textureId, level);
		this.context.openGLErrorCheck();
	}
}
