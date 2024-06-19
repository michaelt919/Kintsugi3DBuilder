#!/bin/sh

#  kintsugi3d.sh
#  
#
#  Created by Tetzlaff, Michael on 1/16/24.
#

cd "$(dirname "$0")/../Resources"
jre/Home/bin/java -Xdock:icon=Kintsugi3D.icns -jar Kintsugi3DBuilder-shaded.jar
