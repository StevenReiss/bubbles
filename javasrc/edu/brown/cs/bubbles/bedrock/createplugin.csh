#! /bin/csh -f


set cwd = `pwd`



set RBD = `pwd`
set RSD = /pro/bubbles/bedrock/src


cd /pro/ivy/java
find . -name '*.class' -print > $RBD/jf1
jar cf $RBD/j1.jar `cat $RBD/jf1`

cd /pro/bubbles/java
find . -name '*.class' -print > $RBD/jf2
jar cf $RBD/j2.jar `cat $RBD/jf2`

cd $RBD
rm -rf classes
mkdir classes
cd classes
jar xf ../j1.jar
jar xf ../j2.jar

cd $RBD

jar cfm $RBD/j1.jar $RSD/manifest.mf -C classes . -C $RSD plugin.xml

rm -rf jf1 jf2 j2.jar classes

mv j1.jar /pro/wadi/camel/src/plugins/edu.brown.cs.bubbles.bedrock_1.0.0.jar
