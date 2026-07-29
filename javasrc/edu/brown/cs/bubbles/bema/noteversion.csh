#! /bin/csh -f

pushd /pro/bubbles

set v = `cat lib/version.txt | awk '{ print $2 }'`
set vnm = V_$v

git tag $vnm
git push origin $vnm

git commit -a -m "new version"
git push

popd




















































