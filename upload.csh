#! /bin/csh

pushd ivy
git commit -a
git push
set x = `pwd`
bvcrupdate -p ivy $x
popd

pushd seede
git commit -a
git push
popd

pushd seedebb
git commit -a
git push
popd

pushd fredit
git commit -a
git push
popd
pushd faitbb
git commit -a
git push
popd
pushd karma
git commit -a
git push
popd
pushd fait
git commit -a
git push
popd

pushd bubblesuml
git commit -a
git push
popd

pushd rose
git commit 0a
git push
popd

pushd lspbase
git commit -a
git push
popd

pushd bubbles
git commit -a
git push
set x = `pwd`
bvcrupdate -p bubbles $x
popd
