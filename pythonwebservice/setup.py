try:
    from setuptools import setup, find_packages
except ImportError:
    from ez_setup import use_setuptools
    use_setuptools()
    from setuptools import setup, find_packages

x = setup(
    name='nwrsc',
    version='2.0',
    description='Scorekeeper Web Service',
    author='Brett Wilson',
    author_email='N/A',
    url='https://github.com/drytoastman/wwscc',

    install_requires=[
        "CherryPy",
        "Flask",
        "Flask-Assets",
        "Flask-Compress"
    ],

    extras_require = {
        'PDF':  ["ReportLab>=3.3.0"],
        'iCalendar': ["icalendar", "python-dateutil"]
    },

    packages=['nwrsc'],
    include_package_data=True,
    zip_safe=False,
)

