#
# Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
# Copyright (c) 2019 The Regents of the University of Minnesota
#
# Licensed under GPLv3
# ( http://www.gnu.org/licenses/gpl-3.0.html )
#
# This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
# This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
#

# Download script developed with assistance of ChatGPT :)

set -euo pipefail

# clear out old python directory if it exists
# try twice since sometimes there are weird file lock issues
rm -rf python || sleep 1 && rm -rf python

# get the URL of the latest release of Python 3.13 (MacOS x86, install only stripped)
URL=$(curl -s https://api.github.com/repos/astral-sh/python-build-standalone/releases/latest |
  jq -r '.assets[].browser_download_url' |
  grep '3\.13\.[0-9].*x86_64-apple-darwin-install_only_stripped\.tar\.gz$' |
  head -1)

# Check if URL was actually found
if [ -z "$URL" ]; then
  echo "No URL found"
  exit 1
fi

echo "$URL"

# Download Python
curl -L "$URL" -o python.tar.gz

# Unzip and delete the zipped download when done
# Put the files in a directory called "python" without duplicating the "python" folder in the path
mkdir -p python
tar -xzf python.tar.gz --strip-components=1 -C python
rm -f python.tar.gz

# Install required python packages
python/bin/python3 -m ensurepip --upgrade
python/bin/python3 -m pip install --upgrade pip setuptools wheel
python/bin/python3 -m pip install bpy
python/bin/python3 -m pip install usd-core
