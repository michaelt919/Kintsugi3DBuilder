# Kintsugi 3D Builder

Developed by a team at the University of Wisconsin – Stout with support from the Minneapolis Institute of Art (Mia) and Cultural Heritage Imaging (CHI) under a grant from the National Endowment for the Humanities (NEH), Kintsugi 3D is capable of synthesizing empirically-based roughness, specularity, normal, and diffuse textures from an image set containing photographs of an object captured with flash-on-camera illumination. Kintsugi 3D is an evolution of its predecessor, IBRelight, with a user experience redesigned to specifically target the application of building textures and materials for use in lightweight 3D viewer applications. What sets Kintsugi 3D apart from other workflows that produce specularity maps is that all the textures produced are empirically-based: they are derived directly from photographic data using classical optimization methods, and the reconstruction error from this optimization process can be recorded as metadata for the object as documentation of the fidelity of the digitized form. 

Kintsugi 3D also features its own Viewer application (https://github.com/UWStout/Kintsugi3DViewer) for public access to finished digitizations in the highest possible quality, using a custom shader designed specifically for materials derived from photographs. The goal of this viewer is to support the rest of the Kintsugi 3D platform with a lightweight app for public access to this robust reproduction quality, while striving for feature parity with comparable viewers such as Sketchfab or Smithsonian Voyager. Kintsugi 3D also supports exporting in standard texture formats to support existing efforts using established viewers like Sketchfab or Voyager. The simplicity and open access of the Kintsugi 3D platform makes this available even to institutions without the infrastructure or support to otherwise develop such hands-on physical or digital interactive experiences.

Kintsugi 3D still relies on Agisoft Metashape (or potentially other photogrammetry alternatives in the future) for camera alignment and 3D reconstruction; it merely replaces the final stage of texture generation. As such, it is an extension, not a replacement, for established photogrammetry solutions. However, Kintsugi 3D does change the photogrammetry pipeline in certain significant ways. Professional photographers at many institutions currently capture image sets that utilize white backgrounds with uniform lighting on the object in each image set. While this makes it possible to easily mask images from the contrast between the background and object, it also makes achieving the necessary uniform illumination for accurate textures more challenging, and empirically deriving specularity from such images is not possible. There is also a risk of color issues in the textures due to bounce lighting or interreflections  that, among other things, dull out colors, reducing the texture fidelity. 

In contrast, Kintsugi 3D, like its predecessor IBRelight, uses a photographic technique that leverages a flash mounted on the camera, There are two primary modes of capture: against a black background in a studio environment, or in-gallery. These two options reduce unintentional bounce light and offer accessibility and flexibility in terms of how the photos are taken, while providing essential reflectivity data for Kintsugi 3D to reconstruct specular maps, which most other photogrammetry workflows cannot replicate. 

## References
Lou Brown, Charles Walbridge, and Michael Tetzlaff, “Kintsugi 3D: An Empirically-Based Photogrammetry Production Pipeline,” *IS&T Archiving Conference*, 2024, pp. 76-80.

Michael Tetzlaff, “High-Fidelity Specular SVBRDF Acquisition from Flash Photographs,” *IEEE Transactions on Visualization and Computer Graphics (TVCG)*, vol. 30, no. 4, 2024, pp. 1885-1896.

Tyler Garcia, Zhangchi Lyu, and Michael Tetzlaff, “An Online Model Viewer for Cultural Heritage in Unity 3D,” *IS&T Archiving Conference*, 2022, pp. 50-55.

Giljoo Nam, Joo Ho Lee, Diego Gutierrez, and Min H. Kim, “Practical SVBRDF Acquisition of 3D Objects with Unstructured Flash Photography,” *ACM Transactions on Graphics*, vol. 37, no. 6, 2018, pp. 267:1-267:12.

Michael Tetzlaff, Gary Meyer, and Alex Kautz, “IBRelight: An Image-Based 3D Renderer for Cultural Heritage,” *IS&T Archiving Conference*, 2018, pp. 93-98.

## Contributors
- Michael Tetzlaff (University of Minnesota / University of Wisconsin - Stout) - primary developer
- Alex Kautz (University of Rochester) - contributor
- Seth Berrier (University of Wisconsin - Stout) - contributor
- Michael Ludwig (University of Minnesota) - contributor
- Sam Steinkamp (University of Minnesota) - contributor
- Jacob Buelow (University of Wisconsin - Stout) - contributor
- Luke Denney (University of Wisconsin - Stout) - contributor
- Isaac Tesch (University of Wisconsin - Stout) - contributor
- Nathaniel Willius (University of Wisconsin - Stout) - contributor
- Darcy Hannen (University of Wisconsin - Stout) - UX designer
- Gary Meyer (University of Minnesota) - designer and advisor

## Copyright and License

Copyright (c) Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney 2023

Copyright (c) The Regents of the University of Minnesota 2019

Licensed under GPLv3 
( http://www.gnu.org/licenses/gpl-3.0.html )

Kintsugi 3D Builder is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Kintsugi 3D Builder is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 
Requests for source code, comments, or bug reports should be sent to
Michael Tetzlaff ( tetzlaffm@uwstout.edu )

## Build Requirements
### Package
Kintsugi 3D builder can be compiled using the maven build system with no external requirements,
however in order to use the `package` maven lifecycle to build the app to a executable jar, exe and distributable zip folder,
a redistributable Java runtime should be located in the `jre` folder at the repository root. The `package` lifecycle will not fail without this,
but no JRE will be bundled in the distribution zip file.
For MacOS builds, a MacOS Java (x64) runtime should be located in the `jre-macos-x64` at the repository root.  

### Install
There are additional requirements for building the installer executable using the `install` maven lifecycle:
- NSIS must be installed on the system. [Download Nullsoft Scriptable Install System](https://nsis.sourceforge.io/Download)
- The NSIS install folder must be added to your `PATH` environment variable.
- A JRE must be located at `jre`. The `install` lifecycle *will fail* without the `jre` folder.
- The Kintsugi 3D Viewer installer, `Kintsugi3DViewer-setup.exe` must be located at `viewer/Kintsugi3DViewer-setup.exe`. The `install` lifecycle *will fail* without this.
