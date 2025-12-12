#! /bin/csh

pushd ivy
git pull
if ($status > 0) exit;
make
if ($status > 0) exit;
make bubbles
if ($status > 0) exit;
popd

pushd seedebb
git pull
if ($status > 0) exit;
popd
pushd seede
git pull
if ($status > 0) exit;
ant
if ($status > 0) exit;
popd

pushd fredit
git pull
if ($status > 0) exit;
popd
pushd faitbb
git pull
if ($status > 0) exit;
popd
pushd karma
git pull
if ($status > 0) exit;
popd
pushd fait
git pull
if ($status > 0) exit;
ant
if ($status > 0) exit;
popd

pushd bubblesuml
git pull
if ($status > 0) exit;
ant
if ($status > 0) exit;
popd

pushd rose
git pull
if ($status > 0) exit;
ant
if ($status > 0) exit;
popd

pushd lspbase
git pull
if ($status > 0) exit;
cd src
ant
if ($status > 0) exit;
popd

pushd limbabb
git pull
if ($status > 0) exit;
ant
popd

pushd limba
git pull
if ($status > 0) exit;
bubbles.csh
if ($status > 0) exit;
popd

if ( -e ../diad ) then
   # do nothing
else
   pushd $PRO
   git clone https://github.com/StevenReiss/diad
   git clone https://github.com/StevenReiss/diadbb
endif

pushd diadbb
git pull
if ($status > 0) exit;
ant
popd

pushd diad
git pull
if ($status > 0) exit;
# bubbles.csh
if ($status > 0) exit;
popd

pushd bubbles
git pull
if ($status > 0) exit;
ant
if ($status > 0) exit;
popd
