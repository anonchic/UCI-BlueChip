mkdir temp1
mkdir temp2

del leon3mp.ngc
del leon3mp.ngd
del leon3mp.bit
del synlog.log
del leon3mp.map
del leon3mp.ncd
del leon3mp.par
del timing.twr

xst -ifn compile.xst -ofn synlog.log
xst -ifn leon3mp.xst -ofn synlog.log
ngdbuild -p xc2vp30-7-ff896 -uc leon3mp.ucf -aul leon3mp.ngc
map -timing -cm speed -logic_opt on -ol high -xe n -p xc2vp30-7-ff896 -register_duplication -t 7 leon3mp.ngd
par -ol high -w leon3mp leon3mp.ncd
rem trce -v 25 leon3mp.ncd leon3mp.pcf 
bitgen leon3mp -w -d -f ../../boards/digilent-xup-xc2vp/default.ut

cleanup.bat