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

pushd bubbles
git pull
if ($status > 0) exit;
ant
if ($status > 0) exit;
popd
