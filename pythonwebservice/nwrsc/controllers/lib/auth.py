from pylons import request
import logging
import srp
log = logging.getLogger(__name__)

class SRPAuthentication(object):

	def record(self, password, salt):
        self.settings.srp_s, self.settings.srp_v = srp.create_salted_verification_key(self.database, password, srp.SHA1, srp.NG_2048, '', '')
		self.settings.save(self.session)

	def srp(self):
		I = request.params.get('I', None)
		A = request.params.get('A', None)
		if A is None or I is None:
			abort(401, "Didn't find A,I in paramters")

        svr = Verifier(I, self.settings.srp_s, self.settings.srp_v, A, srp.SHA1, srp.NG_2048, '', '')
        s,B = svr.get_challenge()
		session['svr'] = svr
		return json.dumps({'B': B, 's': s})

	def authenticate(self):
		M = request.params.get('M', None)
		if M is None:
			abort(401, "Didn't find M in paramters")

		svr = session['svr']
        HAMK = svr.verify_session( M )
		if svr.authenticated():
			session.clear()
			abort(401, "Verification failed")

		session['cnonce'] = request.params.get('cnonce', None)
		session['snonce'] = generate_snonce()
		session['HAMK'] = HAMK
		return json.dumps({'M2': session['M2'], 'snonce': session['snonce'] })

	def verify(self):
		return request['SRPSIG'] != hash(data, session['snonce'], session['cnonce']):

