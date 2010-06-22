
import socket
import ConfigParser
import sys
import glob
import os

config = ConfigParser.ConfigParser()
config.read(sys.argv[1])
seriesdir = config.get('app:main', 'seriesdir', 'asdf') % { 'here': os.path.dirname(os.path.abspath(sys.argv[1])) } 

MDNSAddr = "228.98.99.100";
MDNSPort = 6789;

def databaseAnnouncer():
	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	try:
		sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
		sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
	except:
		pass
	sock.setsockopt(socket.SOL_IP, socket.IP_MULTICAST_TTL, 5)
	sock.setsockopt(socket.SOL_IP, socket.IP_MULTICAST_LOOP, 1)
	try:
		sock.bind(('', MDNSPort))
	except:
		pass
	intf = socket.gethostbyname(socket.gethostname())
	sock.setsockopt(socket.SOL_IP, socket.IP_MULTICAST_IF, socket.inet_aton(intf) + socket.inet_aton('0.0.0.0'))
	sock.setsockopt(socket.SOL_IP, socket.IP_ADD_MEMBERSHIP, socket.inet_aton(MDNSAddr) + socket.inet_aton('0.0.0.0'))


	done = False
	while not done:
		data, (addr, port) = sock.recvfrom(256)
		request = data.split()
		if request[0] == 'FIND' and request[1] == 'RemoteDatabase':
			dblist = [os.path.basename(db)[:-3] for db in glob.glob(seriesdir+"/*.db")]
			sock.sendto("SERVICE RemoteDatabase 80 " + ' '.join(dblist), 0, (MDNSAddr, MDNSPort))


if __name__ == '__main__':	
	try:
		databaseAnnouncer()
	except Exception, e:
		print e
