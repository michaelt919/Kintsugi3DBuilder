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
package tetzlaff.gl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import tetzlaff.helpers.ZipWrapper;

/**
 * An interface for a three-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture3D<ContextType extends Context<ContextType>> extends Texture<ContextType>
{
	/**
	 * Gets the width of the texture.
	 * @return The width of the texture.
	 */
	int getWidth();
	
	/**
	 * Gets the height of the texture.
	 * @return The height of the texture.
	 */
	int getHeight();
	
	/**
	 * Gets the depth of the texture (or for 2D texture arrays, the number of 2D textures in the array).
	 * @return The depth of the texture.
	 */
	int getDepth();
	
	/**
	 * Loads a layer of the texture from an arbitrary input stream.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param fileStream An input stream containing the image in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical ais.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical, boolean rotate90) throws IOException;
	
	/**
	 * Loads a layer of the texture from a ZIP file.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param zipFile Represents a location within a ZIP file containing the image in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, ZipWrapper zipFile, boolean flipVertical, boolean rotate90) throws IOException;
	
	/**
	 * Loads a layer of the texture from an ordinary file.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param file A file containing the image in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, File file, boolean flipVertical, boolean rotate90) throws IOException;
	
	/**
	 * Loads a layer of the texture from an ordinary file.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param img The image already read into memory.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, BufferedImage img, boolean flipVertical, boolean rotate90) throws IOException;
	
	/**
	 * Loads a layer of the texture from an arbitrary input stream along with a separate stream containing an alpha mask.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param imageStream An input stream containing the image in a format supported by Java's ImageIO library.
	 * @param maskStream An input stream containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical, boolean rotate90) throws IOException;
	
	/**
	 * Loads a layer of the texture from a ZIP file along with a separate alpha mask.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param imageZip Represents a location within a ZIP file containing the image in a format supported by Java's ImageIO library.
	 * @param maskZip Represents a location within a ZIP file containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, ZipWrapper imageZip, ZipWrapper maskZip, boolean flipVertical, boolean rotate90) throws IOException;
	
	/**
	 * Loads a layer of the texture from an ordinary file along with an alpha mask in a separate file.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param imageFile A file containing the image in a format supported by Java's ImageIO library.
	 * @param maskFile A file containing the alpha mask in a format supported by Java's ImageIO library.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical, boolean rotate90) throws IOException;

	/**
	 * Loads a layer of the texture from a ZIP file along with a separate alpha mask.
	 * @param layerIndex The index of the layer to be loaded.
	 * @param img The image already loaded from a file ready to be used.
	 * @param mask The alpha mask already loaded from a file ready to be used.
	 * @param flipVertical Whether or not to automatically flip all of the pixels vertically to resolve discrepancies with respect to the orientation of the vertical axis.
	 * @param rotate90 Whether or not to automatically rotate all of the pixels by 90 deg to fix portrait vs landscape photographs
	 * @throws IOException Upon a File I/O problem while loading the image.
	 */
	void loadLayer(int layerIndex, BufferedImage img, BufferedImage mask, boolean flipVertical, boolean rotate90) throws IOException;
	
	/**
	 * Requests that mipmaps be automatically regenerated before this texture is read from again.
	 */
	void generateMipmaps();
	
	/**
	 * Retrieves a layer of the texture for use as an attachment of a framebuffer object.
	 * @param layerIndex The index of the layer to be used.
	 * @return An attachment object which can be attached to a compatible framebuffer object.
	 */
	FramebufferAttachment<ContextType> getLayerAsFramebufferAttachment(int layerIndex);
}
