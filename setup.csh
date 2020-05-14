#! /bin/csh -f

if (! (-e ../ivy)) then

   pushd ..
   git clone https://github.com/StevenReiss/ivy.git
   cd ivy
   unsetenv BROWN_IVY_IVY
   unsetenv BROWN_IVY_ROOT
   unsetenv BROWN_IVY_ARCH
   unsetenv BROWN_JAVA_HOME
   unsetenv BROWN_JAVA_ARCH
   unsetenv BRONN_JAVA_THREADS
   unsetenv BRWON_JAVA_OS
   unsetenv BROWN_IVY_DBMS
   set path = (/usr/bin /bin /usr/local/bin )
   pushd bin
   source ivysetupenv
   popd
   make newmachine
   popd

endif

ant setup
