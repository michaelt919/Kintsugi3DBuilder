## [Download latest release](https://github.com/michaelt919/Kintsugi3DBuilder/releases/latest)
## [Documentation](https://michaelt919.github.io/Kintsugi3DBuilder/Kintsugi3DDocumentation.pdf)
## [CHI forums](https://forums.culturalheritageimaging.org/forum/48-kintsugi-3d/)

## Install instructions:

On Windows, download and run "Kintsugi3DBuilder-\<version\>-setup.exe" to install Kintsugi 3D.

On Mac OS, download and open "Kintsugi3DBuilder-\<version\>-macos.dmg" and copy "Kintsugi 3D Viewer" and "Kintsugi 3D Builder" into the Applications folder.
For the time being, Kintsugi 3D is not signed or notarized and must be manually allowed through Mac OS's "Gatekeeper" feature.  Instructions for how to do this are included in the documentation linked above.

## Support

Kintsugi 3D Builder is beta software and may contain bugs or other issues.  We welcome feedback on your experience with the software so that we can continue improving it.  Comments or bug reports should be directed to Michael Tetzlaff ([tetzlaffm@uwstout.edu](mailto:tetzlaffm@uwstout.edu)) or posted on the CHI forums (linked above).

## Overview
Developed by a team at the University of Wisconsin – Stout with support from the Minneapolis Institute of Art (Mia) and Cultural Heritage Imaging (CHI) under a grant from the National Endowment for the Humanities (NEH), Kintsugi 3D is a novel software platform for synthesizing empirically-based roughness, specularity, normal, and diffuse textures from an image set containing photographs of an object captured with flash-on-camera illumination. What sets Kintsugi 3D apart from other workflows that produce specularity maps is that all the textures produced are empirically-based: they are derived directly from photographic data using classical optimization methods, and the reconstruction error from this optimization process can be recorded as metadata for the object as documentation of the fidelity of the digitized form.

Kintsugi 3D is an evolution of its predecessor, IBRelight: a tool developed as part of Michael Tetzlaff’s (Kintsugi 3D project lead) doctoral thesis that originally had a use case of being a tool for cultural heritage professionals to generate images and videos from photogrammetric models by reprojecting the original photographs onto the 3D model.  What was unique about IBRelight was its use of a flash-on-camera photography technique that made it possible to change the lighting in software based on which flash images were selected for blending.  However, IBRelight had substantial hardware requirements which were a barrier preventing it from being used for general-purpose dissemination of digitized heritage objects.  By implementing a texture processing technique originally described by Nam et al. and refined by Tetzlaff, IBRelight has evolved into Kintsugi 3D Builder, with a user experience redesigned to specifically target the application of building textures and materials for use in lightweight 3D viewer applications.

The Kintsugi 3D platform features its own [Viewer application](https://github.com/UWStout/Kintsugi3DViewer) for public access to finished digitizations in the highest possible quality, using a custom shader designed specifically for materials derived from photographs. The goal of this viewer is to support the rest of the Kintsugi 3D platform with a lightweight app for public access to this robust reproduction quality, while striving for feature parity with comparable viewers such as Sketchfab or Smithsonian Voyager. Kintsugi 3D also supports exporting in standard texture formats to support existing efforts using established viewers like Sketchfab or Voyager. The simplicity and open access of the Kintsugi 3D platform makes this available even to institutions without the infrastructure or support to otherwise develop such hands-on physical or digital interactive experiences.

Kintsugi 3D still relies on Agisoft Metashape (or potentially other photogrammetry alternatives in the future) for camera alignment and 3D reconstruction; it merely replaces the final stage of texture generation. As such, it is an extension, not a replacement, for established photogrammetry solutions. However, Kintsugi 3D does change the photogrammetry pipeline in certain significant ways. Professional photographers at many institutions currently capture image sets that utilize white backgrounds with uniform lighting on the object in each image set. While this makes it possible to easily mask images from the contrast between the background and object, it also makes achieving the necessary uniform illumination for accurate textures more challenging, and empirically deriving specularity from such images is not possible. There is also a risk of color issues in the textures due to bounce lighting or interreflections that, among other things, dull out colors, reducing the texture fidelity.

In contrast, Kintsugi 3D, like its predecessor IBRelight, uses a photographic technique that leverages a flash mounted on the camera. There are two primary modes of capture: against a black background in a studio environment, or in-gallery. These two options reduce unintentional bounce light and offer accessibility and flexibility in terms of how the photos are taken, while providing essential reflectivity data for Kintsugi 3D to reconstruct specular maps, which most other photogrammetry workflows cannot replicate.

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
- Ian Anderson (University of Wisconsin - Stout) - contributor
- Zoe Cuthrell (University of Wisconsin - Stout) - contributor
- Blane Suess (University of Wisconsin - Stout) - contributor
- Isaac Tesch (University of Wisconsin - Stout) - contributor
- Nathaniel Willius (University of Wisconsin - Stout) - contributor
- Darcy Hannen (University of Wisconsin - Stout) - UX designer
- Isabel Smith (University of Wisconsin - Stout) - UX designer
- Elliot Duffy (University of Wisconsin - Stout) - UX designer
- Augusto Freitas (University of Wisconsin - Stout) - UX designer
- Gary Meyer (University of Minnesota) - designer and advisor

Kintsugi 3D was developed with support from a grant from the National Endowment for the Humanities (NEH PR-290101-23).

Additional student research support came from a Minority Student Research grant from the National Science Foundation through the Wisconsin Louis Stokes Alliance for Minority Participation (WiscAMP) program.

Special thanks to Carla Schroer and Mark Mudge from Cultural Heritage Imaging and Charles Walbridge, Lou Brown, Pierre Ware, and Dan Dennehy from the Minneapolis Institute of Art (Mia) for their involvement on this project.

## Copyright and License

Copyright (c) Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius 2024\
Copyright (c) The Regents of the University of Minnesota 2019

Licensed under [GPLv3](http://www.gnu.org/licenses/gpl-3.0.html)

Kintsugi 3D Builder is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
Kintsugi 3D Builder is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
