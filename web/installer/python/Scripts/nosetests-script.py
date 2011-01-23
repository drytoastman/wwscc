#!C:\cygwin\home\bwilson\wwscc\web\installer\python\python.exe
# EASY-INSTALL-ENTRY-SCRIPT: 'nose==0.10.4','console_scripts','nosetests'
__requires__ = 'nose==0.10.4'
import sys
from pkg_resources import load_entry_point

sys.exit(
   load_entry_point('nose==0.10.4', 'console_scripts', 'nosetests')()
)
