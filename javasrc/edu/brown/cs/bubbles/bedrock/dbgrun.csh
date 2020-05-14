#! /bin/csh -f

set port = `perl -e 'printf "%5.0f", (10000+(20000*rand))'`

/pro/wadi/eclipse34/eclipse/eclipse \
	-application edu.brown.cs.bubbles.bedrock.application \
	-nosplash -data /u/spr/Eclipse/sampleworkspace \
	-vmargs -Xdebug -Xrunjdwp:transport=dt_socket,address=$port,server=y,suspend=n


