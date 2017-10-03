@echo off

echo Building data

cp -rf tools/ic_icon.png mobile/src/main/res/drawable
cp -rf tools/Application.mk libcore/jni

cd temp/koishi_ocgupd/ygopro-7210srv
git checkout master
git pull origin master

cd ../../ygopro-222DIY

git checkout master
git pull origin master
cp -rf ocgcore ../../Classes
cp -rf lflist.conf ../../mobile/assets/data/conf
rm -rf ../../mobile/assets/data/conf/strings.conf
cat ../koishi_ocgupd/ygopro-7210srv/strings.conf expansions/strings.conf > ../../mobile/assets/data/conf/strings.conf

cls
echo Copying files

git checkout data
git pull origin data

cd ..

rm -rf pics
mkdir pics
rm -rf script
mkdir script
cp -rf ygopro-222DIY/expansions/script/*.lua script
cp -rf ygopro-scripts/*.lua script
cp -rf ygopro-222DIY/expansions/pics/* pics

cp -rf koishi_ocgupd/ygopro-7210srv/expansions/script/*.lua script
cp -rf koishi_ocgupd/data/pics/* pics

cd koishi_ocgupd/ygopro-7210srv
git checkout prepics
git pull origin prepics
cd ../..

cp -rf koishi_ocgupd/ygopro-7210srv/expansions/pics/* pics

REM rm -rf ../mobile/assets/data/pics
rm -rf ../mobile/assets/data/script
REM cp -rf pics ../mobile/assets/data
cp -rf script ../mobile/assets/data


cls
echo Merging databases
rm -rf ../mobile/assets/data/cards.cdb
"../tools/sqlite3.exe" koishi_ocgupd/ygopro-7210srv/cards.cdb < ../tools/mgcmd
"../tools/sqlite3.exe" ../mobile/assets/data/cards.cdb < merge.mg
"../tools/sqlite3.exe" koishi_ocgupd/ygopro-7210srv/expansions/pre-release.cdb < ../tools/mgcmd
"../tools/sqlite3.exe" ../mobile/assets/data/cards.cdb < merge.mg
"../tools/sqlite3.exe" ygopro-222DIY/expansions/222DIY.cdb < ../tools/mgcmd
"../tools/sqlite3.exe" ../mobile/assets/data/cards.cdb < merge.mg

cls
echo Zipping data

rm -rf pics/*.db
rm -rf ../../mobilepics.7z
"../tools/7za.exe" a ../../mobilepics.7z pics -xr!.git* -mx=9

cls
echo Compiling core

cd ../libcore
"../ndk/ndk-build.cmd" -j4 > ../Compile.log

echo Make complete
pause