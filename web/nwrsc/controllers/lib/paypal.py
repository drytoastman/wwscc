from pylons import request
from nwrsc.model import *

import logging
log = logging.getLogger(__name__)

class PayPalIPN(object):

	def ipn(self):
		data = dict(request.POST.items())
		args = { 'cmd': '_notify-validate' }
		args.update(dict(request.POST.items()))
		log.debug("Paypal IPN sends: %s" % args)

		result = urllib.urlopen("https://www.paypal.com/cgi-bin/webscr", urllib.urlencode(args)).read().strip()
		if result == 'VERIFIED':
			## txid is a unique key, if a previous exists, this one will overwrite
			## the only time this would concievably happen is when an echeck clears
			log.info("Paypal IPN: %s" % result)
			tx = self.session.query(Payment).get(data.get('txn_id'))
			if tx is None:
				tx = Payment()
				self.session.add(tx)
				tx.txid = data.get('txn_id')
			tx.type = data.get('payment_type')
			tx.date = datetime.datetime.now()
			tx.status = data.get('payment_status')
			tx.amount = data.get('mc_gross')
			parts = map(int, data.get('custom').split('.'))
			if len(parts) != 2:
				log.error("Paypal IPN: invalid custom: %s" % data.get('custom'))
			else:
				tx.eventid = parts[0]
				tx.driverid = parts[1]
				self.session.commit()
		else:
			log.error("Paypal result not verified: '%s'", result)

