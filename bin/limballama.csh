#! /bin/csh -f

set mdl = llama4:scout
set host = localhost:11434
set ssh = ""

if ( "X$1" != "X" ) then
   set mdl = $1
   shift
endif
if ( "X$1" != "X" ) then
   set host = $1
   shift
endif
if ( "X$1" != "X" ) then
   set ssh = $1
   shift
endif


if ( "X$ssh" == "X" ) then
   setenv OLLAMA_HOST $host
   ollama run $mdl < /dev/null
else
   ssh $ssh "limballama.csh $mdl $host"
endif
