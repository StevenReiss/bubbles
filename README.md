Welcome to Code Bubbles.

Building Bubbles can be a bit tricky.

First, it is recommended that you put bubbles, ivy and any other related software
(e.g. fait, seede, rose, bubblesuml) under the same directory.	Say the current
directory is /vol/projects.  Then you should have /vol/projects/ivy,
/vol/projects/bubbles, etc.  The environment variable PRO (BROWN_IVY_ROOT),
refers to this common directory.
						
Second you will need to have and build IVY.  The script
	setup.csh
should do this, pulling IVY from its github repo and actually
building it.  You might want to do this manually as the setupupenv
script should be run before IVY is built or used.  Do this before
attempting any other builds.

Third, you can build bubbles
	cd <this directory>
	ant
Note that ant can be used in any of the source subdirectories to recompile
just that package.

Once bubbles has successfully compiled, you can use the codebb script in the bin
subdirectory to run it.  Note you might want to edit the setting of PRO in this
script and it assumes that ivy/bin/ivysetupenv has been sourced.  You might also
want to change the default java location.   This is a csh (tcsh) script.

Note that you need to have a writable Eclipse installation available before
you run bubbles for Java.  Bubbles needs to install a jar file into the
Eclipse dropins (or plugins) directory.



These are some of the problems that people run into:

1)  Ensure that IVY builds correctly

2)  Bubbles wants at least java 10 (>= 15 preferred, and has been
       tested with versions up to 18).

3)  The first time that bubbles (ivy really) is run, it attempts to setup the
	~/.ivy directory
    This should create the file Props in that directory looking something like:

	<?xml version="1.0" encoding="UTF-8" standalone="no"?>
	<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
	<properties>
	<comment>SETUP on Thu Feb 24 19:39:39 EST 2011 by BOARD</comment>
	<entry key="edu.brown.cs.ivy.IVY">/vol/projects/ivy</entry>
	<entry key="edu.brown.cs.ivy.mint.registryhost">xxxx.cs.brown.edu</entry>
	<entry key="BROWN_IVY_IVY">/vol/projects/ivy</entry>
	</properties>

    If this fails, you can create this file manually.

4)  If you want to use a real database system, you shhould edit the Database.props file in
    $(PRO)/ivy/lib to indicate what type of database (POSTGRESQL and MYSQL are supported)
    what the host machine is, a user id and a password.  The default is to use DERBY
    which has its limitations.	If you don't want this public, you can put the edited
    Database.props file in ~/.ivy with permisssions so that only you can read it.


5)  If you plan to make a bubbles distribution and you edit ivy, you will need to do
	make bubbles
    in the IVY directory.  Before doing this, you should edit the Makefile so that the
    paths are correct for this.

6)  Sometimes the sources reference things that aren't compiled yet.  (I try to keep this
    from happening, but it keeps recurring.)  If this happens, try to run ant in the
    individual source directory that is being referred to and then running the top
    level ant again.  You should report this so it can be fixed.

7)  Bubbles wants a relatively recent version of Eclipse (> 2020 probably).  The eclipse
    jar files it compiles against reflect this.


8)  To enable indexing for search over the repository (used by ROSE among other things),
    you should create a copy of Database.props in ~/.ivy named Cocker.Database.props
    that uses either POSTGRESQL or MYSQL.



Note that Bubbles is under continual development, with updates posted fairly frequently.
The Java version for Eclipse is quite stable.  The JavaScript version (for node.js),
somewhat less so.  The other versions (Java for Intellij, python) or quite experimental.
