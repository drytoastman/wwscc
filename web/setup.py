try:
    from setuptools import setup, find_packages
except ImportError:
    from ez_setup import use_setuptools
    use_setuptools()
    from setuptools import setup, find_packages

x = setup(
    name='nwrsc',
    version='0.3',
    description='NWR Scorekeeper Web Portion',
    author='Brett Wilson',
    author_email='N/A',
    url='http://trac.bw.homdns.org/wwscc',
	# Make specific requests here so that everything is the same in each install (debugging, etc)
    install_requires=[
		"Beaker==1.4.2",
		"decorator==3.0.0",
		"FormEncode==1.2.1",
		"Mako==0.2.4",
		"nose==0.10.4",
		"Paste==1.7.2",
		"PasteDeploy==1.3.3",
		"PasteScript==1.7.3",
		"Pygments==1.0",
		"Pylons==0.10rc1",
		"Routes==1.10.3",
		"simplejson==2.0.8",
        "SQLAlchemy==0.5.8",
		"Tempita==0.2",
		"ToscaWidgets==0.9.9",
		"tw.dynforms==0.9.1",
		"tw.forms==0.9.9",
		"WebError==0.10.1",
		"WebHelpers==0.6.4",
		"WebOb==0.9.6.1",
		"WebTest==1.1"
    ],
    setup_requires=["PasteScript>=1.7.3"],
    packages=find_packages(exclude=['ez_setup']),
    include_package_data=True,
    test_suite='nose.collector',
    package_data={'nwrsc': ['i18n/*/LC_MESSAGES/*.mo']},
    zip_safe=False,
    entry_points="""
    [paste.app_factory]
    main = nwrsc.config.middleware:make_app

    [paste.app_install]
    main = pylons.util:PylonsInstaller
    """
)

import sys
if sys.argv[1] == 'sdist':
	import zipfile
	import os
	import subprocess
	
	zip = zipfile.ZipFile('PythonSetup-%s.zip' % x.get_version(), 'w')
	for f in os.listdir('installer'):
		if not os.path.isdir('installer/%s' % f):
			zip.write('installer/%s' % f, f)
	
	for f in os.listdir('installer/pysource'):
		if not os.path.isdir('installer/pysource/%s' % f):
			zip.write('installer/pysource/%s' % f, 'pysource/%s' % f)
	
	for f in os.listdir('dist'):
		if x.get_version() in f:
			distfile = "dist/%s" % f
			zip.write(distfile, 'pysource/%s' % f)
	zip.close()
	
	subprocess.call("scp PythonSetup-* %s brett_wilson@scorekeeper.wwscc.org:scorekeeper.wwscc.org/setup/" % distfile, shell=True)
	

