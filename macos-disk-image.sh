#!/usr/bin/env bash
#
# Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
# Copyright (c) 2019 The Regents of the University of Minnesota
#
# Licensed under GPLv3
# ( http://www.gnu.org/licenses/gpl-3.0.html )
#
# This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
# This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
#

FRIENDLY_NAME=$1
BUILD_DIRECTORY=$2
K3DBUILDER_ZIP_PATH=$3
K3D_STAGING_FOLDER=$4
OUTPUT_DMG=$5

rm -rf "$K3D_STAGING_FOLDER"
unzip -o "$K3DBUILDER_ZIP_PATH" -d "$BUILD_DIRECTORY" # Builder: unzip with overwriting (-o) just in case rm failed
cp -a "viewer/Kintsugi 3D Viewer.app" "$K3D_STAGING_FOLDER/Kintsugi 3D Viewer.app" # Viewer: copy as archive (-a)
./macos-staging-folder.sh "$K3D_STAGING_FOLDER" # Create applications alias and layout window
hdiutil create -volname "$FRIENDLY_NAME" -srcfolder "$K3D_STAGING_FOLDER" -ov -format UDZO "$OUTPUT_DMG" # create DMG
