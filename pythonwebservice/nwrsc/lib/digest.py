
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
	pass


def DigestAuthentication(store, request, realm, password):
	"""
		Attempt to verify the request using a digest header, if things don't check out, send a 401 with a digest request
		store -  subsection of storage on server  { realm: (nonce, nc), ... }
		request - the incoming request to verify
		realm - the realm to verify against
		password - the password we should use
	"""
	try:
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
			raise AuthException("No previous info for realm %s." % auth.realm)
		if auth.nonce != info.nonce:
			raise AuthException("Nonce value not the same as what we provided (%s vs %s)" % (auth.nonce, info.none))
		if auth.nc < info.nc:
			raise AuthException("NC value has not incremented (provided %s vs recorded %s)" % (auth.nc, info.nc))

		# finally check if they actually got the right value
		digest = getattr(hashlib, auth.algorithm.lower())

		ha1 = digest("%s:%s:%s" % (auth.username, auth.realm, password)).hexdigest()
		method = request.environ['REQUEST_METHOD']
		path = request.environ['PATH_INFO']
		body = request.body
		if auth.qop == 'auth-int':
			ha2 = digest('%s:%s:%s' % (method, path, digest(body).hexdigest())).hexdigest()
		else:
			ha2 = digest('%s:%s' % (method, path)).hexdigest()
		if auth.response != digest("%s:%s:%s:%s:%s:%s" % (ha1, auth.nonce, auth.nc, auth.cnonce, auth.qop, ha2)).hexdigest():
			raise AuthException("Digest response invalid")

		info.nc = auth.nc
		return True

	except AuthException, e: 
		log.info("Authorization failed: %s", e)
		info = RealmInfo()
		store[realm] = info
		abort(401, headers = {"WWW-Authenticate": str('Digest realm="%s", qop="auth,auth-int", nonce="%s"' % (realm, info.nonce))} )

