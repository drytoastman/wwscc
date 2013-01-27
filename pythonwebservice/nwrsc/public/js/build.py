#!/usr/bin/env python

import subprocess
import sys
import shutil
import os


ROOT = os.path.dirname(os.path.abspath(__file__))
def fullpath(inpath):
	return os.path.join(ROOT, inpath)
	
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
	dest = open(fullpath(output), 'w')
	for f in files:
	    shutil.copyfileobj(open(fullpath(f), 'r'), dest)
	dest.close()



compile([
		'external/jquery-1.9.0.js',
		'external/jquery-ui-1.10.0.custom.js',
		'external/jquery.validate.1.10.js',
		'internal/nwr.js',
		'internal/careditor.js',
		'internal/drivereditor.js',
		'internal/regeditor.js',
		'internal/register.js'
		], 'register.js')

compile([
		'external/jquery-1.9.0.js',
		'external/jquery-ui-1.10.0.custom.js',
		'external/jquery.validate.1.10.js',
		'external/superfish-1.5.0.js',
		'external/migrate.js',
		'external/anytime.js',
		'external/jquery.dataTables.min.js',
		'internal/nwr.js',
		'internal/careditor.js',
		'internal/drivereditor.js',
		'internal/regeditor.js',
		'internal/admin.js'
		], 'admin.js')

compile([
		'external/jquery-1.9.0.js',
		'external/jquery-ui-1.10.0.custom.js',
		'internal/nwr.js',
		'internal/announcer.js',
		], 'announcer.js')

