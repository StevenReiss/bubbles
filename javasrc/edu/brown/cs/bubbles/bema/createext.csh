#! /bin/csh

rm -rf external
mkdir external
sed -e 's^spr/bubbles^spr/bubbles.ext^' < /pro/bubbles/lib/version.xml > external/version.xml
cp bubbles.jar external
pushd external
jar uf bubbles.jar version.xml
cp bubbles.jar version.xml /pro/web/web/people/spr/bubbles.ext
pushd /pro/web/web/people/spr/bubbles.ext
webupdate
popd
popd

echo done setting up external
./sendupdateext.csh
