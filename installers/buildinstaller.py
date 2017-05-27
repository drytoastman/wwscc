#!/usr/bin/env python3

import distutils
import os
import pip
import platform
import sys
import tarfile
import tempfile
import urllib
import wheel
import zipfile


def ensurearchive(name):
    path = os.path.join("downloads", name)
    if not os.path.exists(path):
        print("Downloading {} ... ".format(name))
        urllib.request.urlretrieve("https://get.enterprisedb.com/postgresql/"+name, path)
    return path

if __name__ == "__main__":
    system = (platform.system(), sys.maxsize > 2**32 and 64 or 32)
    os.makedirs("downloads", exist_ok=True)
    print("System is {}/{}".format(*system))

    print("Checking for cached postgresql archive")
    if system == ("Windows", 64):
        pg = zipfile.ZipFile(ensurearchive("postgresql-9.6.3-1-windows-x64-binaries.zip"))
    elif system == ("Linux", 64):
        pg = tarfile.TarFile(ensurearchive("postgresql-9.6.3-1-linux-x64-binaries.tar.gz"))
    elif system[0] == "darwin":
        pg = zipfile.ZipFile(ensurearchive("postgresql-9.6.3-1-osx-binaries.zip"))
    else:
        print("Unable to work with system ({})".format(system))
        sys.exit(-1)
       
    print("Caching any necessary python wheels")
    pip.main(["wheel", "-r", "pythonwebservice/versionedrequirements.txt", "-f", "file:downloads/wheels", "-q", "-w", "downloads/wheels"])

    destdir = tempfile.mkdtemp()
    print("Creating install layout in {}".format(destdir))
    pip.main(["wheel", "-r", "pythonwebservice/versionedrequirements.txt", "-f", "file:downloads/wheels", "-q", "--no-index", "-w", os.path.join(destdir, "wheels")])
    os.chdir("pythonwebservice")
    distutils.core.run_setup("setup.py", ['bdist_wheel', '-d', os.path.join(destdir, 'wheels')])
    os.chdir("..")
    for file in pg.namelist():
        if file.startswith(('pgsql/bin/', 'pgsql/lib', 'pgsql/share')) and not file.endswith((".lib", ".mo", ".a")):
            pg.extract(file, destdir)

