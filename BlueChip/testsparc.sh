#!/bin/bash
make clean
make
cd sparc_tests
make clean
make
cd ..
for TEST in `ls sparc_tests/*img`
do 
    echo "${TEST}: "
    ./simulator ${TEST}
done