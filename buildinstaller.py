#!/usr/bin/env python3

import platform
import tempfile
import tarfile
import zipfile
import sys
import pip
import wheel
import os

system  = (platform.system(), sys.maxsize > 2**32 and 64 or 32)
destdir = tempfile.mkdtemp()

if system == ("Windows", 64):
    pg = zipfile.ZipFile("downloads/postgresql-9.6.3-1-windows-x64-binaries.zip")
elif system == ("Linux", 64):
    pg = tarfile.TarFile("downloads/postgresql-9.6.3-1-linux-x64-binaries.tar.gz")
elif system[0] == "darwin":
    pg = zipfile.ZipFile("downloads/postgresql-9.6.3-1-osx-binaries.zip")
else:
    print("Unable to work with system ({})".format(system))
    sys.exit(-1)
   
print(system, destdir)
for file in pg.namelist():
    if file.startswith(('pgsql/bin/', 'pgsql/lib', 'pgsql/share')) and not file.endswith((".lib", ".mo", ".a")):
        pg.extract(file, destdir)

pip.main(["wheel", "-r", "pythonwebservice/versionedrequirements.txt", "-w", os.path.join(destdir, "wheels")])

