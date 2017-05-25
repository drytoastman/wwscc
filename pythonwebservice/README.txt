** ALL DEVELOPMENT/DEPLOYMENT IS BASED ON PYTHON 3 **

Development
======================

1. Create a new python virtual environment to work in:
   $PYTHONBINDIR/python -m venv $DESTDIR

2. We use a predetermined set of versions for consistency.  Install the versioned requirements:
   (On a Windows machine, the zeroconf/netifaces depdendency will require the free VS 2015 C++ tools)
   $DESTDIR/Scripts/pip install -r versionedrequirements.txt

3. Install nwrsc as a development pointer (mods to git directory are seen by python venv)
   $DESTDIR/Scripts/pip install -e .

Running
======================

1. To run the webserver use:
   $DESTDIR/Scripts/python $DESTDIR/Scripts/webserver.py

ADD postgresql instructions here

