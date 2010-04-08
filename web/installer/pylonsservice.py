import win32serviceutil
from paste.script.serve import ServeCommand
import os, sys
import ConfigParser

import win32service
import win32event

class PasterService(win32serviceutil.ServiceFramework):
	"""NT Service."""
	
	iniFile = "c:/timing/nwrsc.ini"
	c = ConfigParser.SafeConfigParser()
	c.read(iniFile)
	_svc_name_ = c.get('winservice', 'name')
	_svc_display_name_ = c.get('winservice', 'display')
	_svc_description_ = c.get('winservice', 'descrip')

	def __init__(self, args):
		win32serviceutil.ServiceFramework.__init__(self, args)
		# create an event that SvcDoRun can wait on and SvcStop can set.
		self.stop_event = win32event.CreateEvent(None, 0, 0, None)

	def SvcDoRun(self):
		os.chdir(os.path.dirname(__file__))
		ServeCommand(None).run([self.iniFile])
		win32event.WaitForSingleObject(self.stop_event, win32event.INFINITE)
	
	def SvcStop(self):
		self.ReportServiceStatus(win32service.SERVICE_STOP_PENDING)
		self.ReportServiceStatus(win32service.SERVICE_STOPPED)
		sys.exit()

if __name__ == '__main__':
	win32serviceutil.HandleCommandLine(PasterService)

