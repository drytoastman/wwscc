
Development
======================

1. Run "python setup.py develop" to install all the dependencies and add a pointer to the nwrsc code

2. Change to the rundir directory, acts like a regular sitedir

3. Run "paster serve --reload development.ini" to run the web server


Installing
======================

1. Run "python setup.py bdist" to create an egg for installing in dist/

2. At the other location run "easy_install <eggname>" to install on another machine


