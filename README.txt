Welcome to Code Bubbles.

Building Bubbles can be a bit tricky.

First you will probably want to have and build IVY.  The script
        setup.csh
should do this, pulling IVY from its github repo and actually
building it.

Second, there are problems that people run into:

1)  Some of the scripts are csh-based (and you need a relatively
        modern version of csh, i.e. tcsh) to run them.  (Most systems
        have csh as a link to tcsh (or they are identical).

2)  IVY uses make (actualy gnumake, but again they are the same on
        most systems).

3)  You might want to add source <../ivy/bin/ivysetupenv> to your
        startup scripts if you are going to work on ivy.  This should
        make it so you can do a make in any ivy subdirectory.

4)  Bubbles wants java 7 or 8.  You probably want to work with at
        least java 8 (we will be moving over to J8 completely by the
        end of the summer).

Note that if ivy doesn't build successfully, we have not included
build.xml files that should build the java portion of ivy (all that is
needed for bubbles) using ant.  Just do ant at the top level.
