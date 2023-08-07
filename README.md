# Kintsugi 3D Builder

## Contributors
- Michael Tetzlaff (University of Minnesota / University of Wisconsin - Stout) - primary developer
- Alex Kautz (University of Rochester) - contributor
- Seth Berrier (University of Wisconsin - Stout) - contributor
- Michael Ludwig (University of Minnesota) - contributor
- Sam Steinkamp (University of Minnesota) - contributor
- Jacob Buelow (University of Wisconsin - Stout) - contributor
- Luke Denney (University of Wisconsin - Stout) - contributor
- Darcy Hannen (University of Wisconsin - Stout) - UX designer
- Gary Meyer (University of Minnesota) - designer and advisor

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

### Install
There are additional requirements for building the installer executable using the `install` maven lifecycle:
- NSIS must be installed on the system. [Download Nullsoft Scriptable Install System](https://nsis.sourceforge.io/Download)
- The NSIS install folder must be added to your `PATH` environment variable.
- A JRE must be located at `jre`. The `install` lifecycle *will fail* without the `jre` folder.