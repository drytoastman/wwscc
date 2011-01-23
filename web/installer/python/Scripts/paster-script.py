#!C:\cygwin\home\bwilson\wwscc\web\installer\python\python.exe
# EASY-INSTALL-ENTRY-SCRIPT: 'pastescript==1.7.3','console_scripts','paster'
__requires__ = 'pastescript==1.7.3'
import sys
from pkg_resources import load_entry_point

sys.exit(
   load_entry_point('pastescript==1.7.3', 'console_scripts', 'paster')()
)
