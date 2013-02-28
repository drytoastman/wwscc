from pylons import request
import logging
import srp
log = logging.getLogger(__name__)

class SRPAuthentication(object):

	HASH = srp.SHA1
	NSIZE = srp.NG_2048

	def record(self, password, salt):
		self.settings.srp_s, self.settings.srp_v = srp.create_salted_verification_key(self.database, password, HASH, NSIZE, '', '')
		self.settings.save(self.session)

	def srp(self):
		I = request.params.get('I', None)
		A = request.params.get('A', None)
		if A is None or I is None:
			abort(401, "Didn't find A,I in paramters")

		svr = Verifier(I, self.settings.srp_s, self.settings.srp_v, A, HASH, NSIZE, '', '')
		s,B = svr.get_challenge()
		self.srpsession['svr'] = svr
		return json.dumps({'B': B, 's': s})

	def authenticate(self):
		M = request.params.get('M', None)
		cnonce = request.params.get('cnonce', None)

		if M is None:
			abort(401, "Didn't find M in paramters")

		svr = session['svr']
		HAMK = svr.verify_session( M )
		if svr.authenticated():
			session.clear()
			abort(401, "Verification failed")

		self.srpsession['cnonce'] = cnonce
		self.srpsession['snonce'] = generate_snonce()
		self.srpsession['HAMK'] = HAMK
		return json.dumps({'M2': self.srpsession['M2'], 'snonce': self.srpsession['snonce'] })

	def verify(self, data):
		return False
		#return request['SRPSIG'] != hash(data, self.srpsession['snonce'], self.srpsession['cnonce'])

