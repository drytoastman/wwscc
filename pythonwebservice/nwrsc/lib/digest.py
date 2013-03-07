
import time, random, hashlib, logging
from pylons.controllers.util import abort

log = logging.getLogger(__name__)
	
class RealmInfo(object):
	def __init__(self, nonce = None, nc = None):
		self.nonce = nonce
		self.nc = nc
		if self.nonce is None or self.nc is None:
			self.nonce = hashlib.md5("%s:%s" % (time.time(), random.random())).hexdigest()
			self.nc = '00000000'


class AuthHeader(object):
	def __init__(self):
		self.algorithm = 'md5'


class AuthException(Exception):
	def __init__(self, msg, stale=False):
		self.args = msg,
		self.stale = stale

def authCheck(store, realm, passwords, request):
	try:
		return digestAuthentication(store, realm, passwords, request)
	except AuthException, e: 
		log.info("Authorization failed: %s", e)
		abortChallenge(store, realm, e.stale)
		

def digestAuthentication(store, realm, passwords, request):
	"""
		Attempt to verify the request using a digest header, if things don't check out, send a 401 with a digest request
		store -  subsection of storage on server  { realm: (nonce, nc), ... }
		realm - the realm we are authenticating on
		passwords - dictionary of username to password mapping that can be used
		request - the incoming request to verify
	"""
	if 'HTTP_AUTHORIZATION' not in request.environ:
		raise AuthException("No authorization header")

	(authtype, param) = request.environ['HTTP_AUTHORIZATION'].split(" ", 1)
	if authtype.strip().lower() != 'digest':
		raise AuthException("Not digest authorization")

	auth = AuthHeader()
	for itm in param.split(","):
		(k,v) = [s.strip() for s in itm.strip().split("=",1)]
		setattr(auth, k, v.replace('"', ''))

	# sanity check parameters
	if auth.qop not in ('auth', 'auth-int'):
		raise AuthException("Invalid qop value: %s" % auth.qop)
	if auth.algorithm.lower() not in ('md5', 'sha1', 'sha224', 'sha256'):
		raise AuthException("Invalid algorithm value: %s" % auth.algoritm)
	if auth.realm != realm:
		raise AuthException("Authorization for %s, we want %s" % (auth.realm, realm))

	info = store.get(auth.realm, None)

	# verify we understand the nonce and nc
	if info is None:
		raise AuthException("No previous info for realm %s." % auth.realm, stale=True)
	if auth.nonce != info.nonce:
		raise AuthException("Nonce value not the same as what we provided (%s vs %s)" % (auth.nonce, info.none), stale=True)
	if auth.nc < info.nc:
		raise AuthException("NC value has not incremented (provided %s vs recorded %s)" % (auth.nc, info.nc), stale=True)

	if auth.username not in passwords:
		raise AuthException("Not a username we recognize: %s" % (auth.username))

	# finally check if they actually got the right value
	digest = getattr(hashlib, auth.algorithm.lower())
	ha1 = digest("%s:%s:%s" % (auth.username, auth.realm, passwords[auth.username])).hexdigest()

	method = request.environ['REQUEST_METHOD']
	path = request.environ['REQUEST_URI']
	body = request.environ['wsgi.input']
	if auth.qop == 'auth-int':
		ha2 = digest('%s:%s:%s' % (method, path, digest(body).hexdigest())).hexdigest()
	else:
		ha2 = digest('%s:%s' % (method, path)).hexdigest()

	if auth.response != digest("%s:%s:%s:%s:%s:%s" % (ha1, auth.nonce, auth.nc, auth.cnonce, auth.qop, ha2)).hexdigest():
		raise AuthException("Digest response invalid")

	info.nc = auth.nc
	return auth.username



def abortChallenge(store, realm, stale=False):
	info = RealmInfo()
	store[realm] = info
	authstring = str('Digest realm="%s", qop="auth,auth-int", nonce="%s"' % (realm, info.nonce))
	if stale:
		authstring += ', stale=TRUE'

	abort(401, headers = {"WWW-Authenticate": authstring })

