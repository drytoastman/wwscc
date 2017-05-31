#!/usr/bin/env python3

import distutils
import glob
import os
import pip
import platform
import subprocess
import shutil
import sys
import tarfile
import tempfile
import urllib
import wheel
import zipfile


STAGE  = "stage"
WHEELS = os.path.join(STAGE, "wheels")
NWRWHL = "nwrsc-2.0-py3-none-any.whl"
PYTHON = "../pythonwebservice"
PDIST  = os.path.join(PYTHON, "dist", NWRWHL)
PREQ   = os.path.join(PYTHON, "versionedrequirements.txt")
JAVA   = "../swingapps"

class BaseInstallCreator():
    
    def __init__(self):
        pass

    def execute(self):
        """
        print("Building jar file")
        subprocess.run([shutil.which("ant"), "-f", "../swingapps/buildjar.xml"])

        print("Checking for cached postgresql archive")
        pg = self.extractPostgresql()

        print("Creating postgresql layout")
        for file in pg.namelist():
            if file.startswith(('pgsql/bin/', 'pgsql/lib', 'pgsql/share')) and not file.endswith((".lib", ".mo", ".a")):
                pg.extract(file, STAGE)
           
        print("Downloading any necessary python wheels")
        pip.main(["wheel", "-r", PREQ, "-f", "file:"+WHEELS, "-q", "-w", WHEELS])

        print("Building webservice wheel")
        save = os.getcwd()
        os.chdir(PYTHON)
        distutils.core.run_setup("setup.py", ['bdist_wheel', '-q'])
        os.chdir(save)
        shutil.copyfile(PDIST, os.path.join(WHEELS, NWRWHL))
        """

        self.buildInstaller()


    def ensurearchive(self, name):
        path = os.path.join(STAGE, name)
        if not os.path.exists(path):
            print("Downloading {} ... ".format(name))
            urllib.request.urlretrieve("https://get.enterprisedb.com/postgresql/"+name, path)
        return path

    def extractPostgresql(self):
        raise NotImplementedError()

    def buildInstaller(self):
        raise NotImplementedError()


class WindowsInstallCreator(BaseInstallCreator):
    def extractPostgresql(self):
        return zipfile.ZipFile(self.ensurearchive("postgresql-9.6.3-1-windows-x64-binaries.zip"))

    def buildInstaller(self):
        subprocess.run(["C:/Program Files (x86)/Inno Setup 5/iscc.exe", "windows.iss"])

class LinuxInstallCreator(BaseInstallCreator):
    def extractPostgresql(self):
        return zipfile.ZipFile(self.ensurearchive("postgresql-9.6.3-1-linux-x64-binaries.tar.gz"))

class DarwinInstallCreator(BaseInstallCreator):
    def extractPostgresql(self):
        return zipfile.ZipFile(self.ensurearchive("postgresql-9.6.3-1-windows-x64-binaries.zip"))


if __name__ == "__main__":
    system = (platform.system(), sys.maxsize > 2**32 and 64 or 32)
    print("System is {}/{}".format(*system))

    if system == ("Windows", 64): creator = WindowsInstallCreator()
    elif system == ("Linux", 64): creator = LinuxInstallCreator()
    elif system[0] == "darwin":   creator = DarwinInstallCreator()
    else: print("Unable to work with system ({})".format(system)); sys.exit(-1)

    creator.execute()

