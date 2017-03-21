from setuptools import setup, find_packages

x = setup(
    name='nwrsc',
    version='2.0',
    description='Scorekeeper Web Service',
    author='Brett Wilson',
    author_email='N/A',
    url='https://github.com/drytoastman/wwscc',

    install_requires=[
        "psycopg2",
        "CherryPy",
        "Flask",
        "Flask-Assets",
        "Flask-Compress",
        "formencode",
        "ReportLab",
        "icalendar",
        "python-dateutil",
    ],

    packages=find_packages(),
    scripts=['bin/webserver'],
    include_package_data=True,
    zip_safe=False,
)

