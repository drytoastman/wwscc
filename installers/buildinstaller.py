#!/usr/bin/env python3

import distutils
import glob
import json
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

def printe(line):
    print(line, file=sys.stderr)
    sys.stderr.flush()

class BaseInstallCreator():
    
    def __init__(self):
        pass

    def execute(self):
        printe("Building jar file")
        subprocess.run([shutil.which("ant"), "-f", "../swingapps/buildjar.xml"])

        self.buildInstaller()

    def buildInstaller(self):
        raise NotImplementedError()


class WindowsInstallCreator(BaseInstallCreator):
    def buildInstaller(self):
        subprocess.run(["C:/Program Files (x86)/Inno Setup 5/iscc.exe", "windows.iss"])

class LinuxInstallCreator(BaseInstallCreator):
    pass

class DarwinInstallCreator(BaseInstallCreator):
    pass


if __name__ == "__main__":
    system = (platform.system(), sys.maxsize > 2**32 and 64 or 32)
    printe("System is {}/{}".format(*system))

    if system == ("Windows", 64): creator = WindowsInstallCreator()
    elif system == ("Linux", 64): creator = LinuxInstallCreator()
    elif system[0] == "darwin":   creator = DarwinInstallCreator()
    else: printe("Unable to work with system ({})".format(system)); sys.exit(-1)

    creator.execute()

