#!/bin/sh

rm -f *.class
javac *.java
jar -cmf manifest.mf Stome.jar *.class stome.jpg org net
