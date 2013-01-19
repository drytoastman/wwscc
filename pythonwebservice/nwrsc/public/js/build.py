#!/usr/bin/env python

import subprocess
import sys
import shutil

def compile(files, output):
	prog = ["java", "-jar", "compiler.jar"]

	if len(sys.argv) > 1:
		if sys.argv[1] == "s":  
			concat(files, output)
			return
		if sys.argv[1] == "a":
			prog.extend(["--compilation_level", "ADVANCED_OPTIMIZATIONS"])
	
	for f in files:
		prog.extend(["--js", f])
	prog.extend(["--js_output_file", output])
	print ' '.join(prog)
	subprocess.call(prog)


def concat(files, output):
	dest = open(output, 'w')
	for f in files:
	    shutil.copyfileobj(open(f, 'r'), dest)
	dest.close()


compile([
		'external/jquery-1.9.0.js',
		'external/jquery-ui-1.10.0.custom.js',
		'external/jquery.validate.min.js',
		'internal/careditor.js',
		'internal/drivereditor.js',
		'internal/register.js'
		], 'register.js')

