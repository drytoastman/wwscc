#!/usr/bin/python

import subprocess
import time

while True:
	subprocess.call(["wget", "-O", "/dev/null", "http://192.168.2.124/results/pro2010/1/all"]);
	time.sleep(1)

