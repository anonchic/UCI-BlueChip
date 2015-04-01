#!/usr/bin/python

import os
import subprocess
import sys
import time

TARGET_MACHINE = "root@imp0.cs.uiuc.edu"
NUM_ITER = 3

def gen_cmd(cmd):
    return "ssh " + TARGET_MACHINE + " \"" + cmd + "\""

def system(cmd):
    if cmd == None:
        return 0.0

    cmd = gen_cmd(cmd)
    startTime = time.time()
    p = subprocess.Popen(cmd, shell=True);
    os.waitpid(p.pid,0)
    return time.time() - startTime

def run_test(preCmd, cmd, postCmd):
    res = []
    for i in range(NUM_ITER):
        system(preCmd)
        res.append(system(cmd))
        system(postCmd)

    return res


res = run_test("cd ntp-4.2.4p7/ntpdate; make clean", "cd ntp-4.2.4p7/ntpdate; make -j2", None)
print res
res = run_test(None,"wget http://www.cs.uiuc.edu/homes/kingst/benchmark.jpg", "rm benchmark.jpg")
print res
res = run_test("wget http://www.cs.uiuc.edu/homes/kingst/benchmark.jpg", 
               "./jpeg-7/djpeg < benchmark.jpg > /dev/null",
               "rm benchmark.jpg")
print res

