#!C:\cygwin\home\bwilson\wwscc\web\installer\python\python.exe
# EASY-INSTALL-ENTRY-SCRIPT: 'pygments==1.0','console_scripts','pygmentize'
__requires__ = 'pygments==1.0'
import sys
from pkg_resources import load_entry_point

sys.exit(
   load_entry_point('pygments==1.0', 'console_scripts', 'pygmentize')()
)
