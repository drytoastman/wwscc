from setuptools import setup, find_packages

x = setup(
    name='nwrsc',
    version='2.0',
    description='Scorekeeper Web Service',
    author='Brett Wilson',
    author_email='N/A',
    url='https://github.com/drytoastman/wwscc',

    install_requires=[
        "Flask",
        "Flask-Assets",
        "Flask-Bcrypt",
        "Flask-Compress",
        "Flask-Mail",
        "Flask-WTF",
        "cheroot",
        "cssmin",
        "icalendar",
        "libsass",
        "ReportLab",
        "psycopg2",
        "python-dateutil",
        "zeroconf"
    ],

    packages=find_packages(),
    scripts=['bin/webserver.py', 'bin/dbcreate.py', 'bin/olddbimport.py'],
    include_package_data=True,
    zip_safe=False,
)

