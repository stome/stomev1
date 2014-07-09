#!/bin/sh

rm -f stome/*.class
javac stome/*.java
jar -cmf manifest.mf Stome.jar stome/*.class stome.jpg org net
