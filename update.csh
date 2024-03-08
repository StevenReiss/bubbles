#! /bin/csh

pushd ivy
git pull
make
make bubbles
popd

pushd seedebb
git pull
popd
pushd seede
git pull
ant
popd

pushd fredit
git pull
popd
pushd faitbb
git pull
popd
pushd karma
git pull
popd
pushd fait
git pull
ant
popd

pushd bubblesuml
git pull
ant
popd

pushd rose
git pull
ant
popd

pushd lspbase
git pull
cd src
ant
popd

pushd bubbles
git pull
ant
popd

























































































																		      :A
