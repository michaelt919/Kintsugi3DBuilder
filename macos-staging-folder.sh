#!/bin/bash
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

set -e

STAGING_DIR="$1"

echo "$STAGING_DIR"
cd "$STAGING_DIR"

echo "→ Creating Applications alias..."

osascript <<EOF
tell application "Finder"
    set aliasFile to make alias file to POSIX file "/Applications" at POSIX file "$PWD"
    set name of aliasFile to "temp"
end tell
EOF

mv temp Applications || true

echo "→ Writing Finder layout script..."

# IDK, ChatGPT wrote this.
osascript <<EOF
tell application "Finder"
    set targetFolder to POSIX file "$PWD" as alias
    tell folder targetFolder
        open
        set current view of container window to icon view
        set toolbar visible of container window to false
        set statusbar visible of container window to false
        set the bounds of container window to {100, 100, 800, 450}

        set viewOptions to the icon view options of container window
        set arrangement of viewOptions to not arranged
        set icon size of viewOptions to 72

        -- Position icons
        set position of item "Kintsugi 3D Builder.app" of container window to {200, 100}
        set position of item "Kintsugi 3D Viewer.app" of container window to {200, 250}
        set position of item "Applications" of container window to {650, 150}

        close
        open
        update without registering applications
        delay 2
        close
    end tell
end tell
EOF

echo "→ Done. Ready for hdiutil packaging."