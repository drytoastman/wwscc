#!/usr/bin/env python

""" provide the python side values for testing java srp """

import srp._pysrp as srp
import base64

username = "ww2013:series"
password = "mypassword"

N = "EEAF0AB9ADB38DD69C33F80AFA8FC5E86072618775FF3C0B9EA2314C9C256576D674DF7496EA81D3383B4813D692C6E0E0D5D8E250B98BE48E495C1D6089DAD15DC7D7B46154D6B6CE8EF4AD69B15D4982559B297BCF1885C529F566660E57EC68EDBC3C05726CC02FD4CBF4976EAA9AFD5138FE8376435B9FC61D2FC0EB06E3"
g = "2"


#Server

#Client
usr = srp.User(username, password, hash_alg=srp.SHA1, ng_type=srp.NG_CUSTOM, n_hex=N, g_hex=g)
uname, A = usr.start_authentication()

#Server
(salt, ver)	= srp.create_salted_verification_key(username, password, hash_alg=srp.SHA1, ng_type=srp.NG_CUSTOM, n_hex=N, g_hex=g)
svr	= srp.Verifier( username, salt, ver, A, hash_alg=srp.SHA1, ng_type=srp.NG_CUSTOM, n_hex=N, g_hex=g)
(s,B) = svr.get_challenge()

#Client
M = usr.process_challenge( s, B )

#Server
HAMK = svr.verify_session( M )

# Server => Client: HAMK
usr.verify_session( HAMK )

def printVar(name, _bytes):
	print 'BigInteger %s = new BigInteger(1, Base64.decode("%s"));' % (name, base64.b64encode(_bytes))


printVar('N', srp.long_to_bytes(usr.N))
printVar('g', srp.long_to_bytes(usr.g))
printVar('k', srp.long_to_bytes(usr.k))

printVar('a', srp.long_to_bytes(usr.a))
printVar('A', A)

printVar('salt', salt)
printVar('B', B)
printVar('M1', M)
printVar('M2', HAMK)
printVar('K', usr.K)

