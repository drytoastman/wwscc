import cStringIO

from sqlalchemy.sql import func
from pylons import request, response, tmpl_context as c
from pylons.templating import render_mako

from nwrsc.model import *



class Registered(object):
	pass

class CardPrinting(object):

	def loadPythonFunc(self, func, text):
		# Create place to load stuff defined in loaded code, provide limited builtins
		loadenv = dict()
		sand = dict()
		for k in ['str', 'range']:
			sand[k] = __builtins__[k]
		loadenv['__builtins__'] = sand

		# Some flailing attempt at stopping bad behaviour
		if 'import' in text:
			raise Exception("python code to load contains import, not loading")
		
		text = str(text)
		text = text.replace('\r', '')
		exec text in loadenv
		return loadenv[func]


	def csv(self, filename, titles, objects):
		# CSV data, just use a template and return
		response.headers['Content-type'] = "application/octet-stream"
		response.headers['Content-Disposition'] = 'attachment;filename=%s.csv' % filename
		response.charset = 'utf8'

		def wrapper():
			# output title line
			yield ','.join(titles)
			yield '\n'
	
			for obj in objects:
				line = []
				for ii, t in enumerate(titles):
					try:
						s = getattr(obj, t) 
					except:
						s = obj[ii]

					if s is None:
						line.append("\"\"")
					elif hasattr(s, 'replace'):
						line.append("\"%s\""%s.replace('\n', ' ').replace('"', '""'))
					else:
						line.append("%s"%s)
	
				yield(','.join(line))
				yield('\n')

		return wrapper()


	def printcards(self):

		drawCard = self.loadPythonFunc('drawCard', self.session.query(Data).get('card.py').data)

		page = request.GET.get('page', 'card')
		type = request.GET.get('type', 'blank')

		query = self.session.query(Driver,Car,Registration).join('cars', 'registration').filter(Registration.eventid==self.eventid)
		if type == 'blank':
			registered = [(None,None,None)]
		elif type == 'lastname':
			registered = query.order_by(func.lower(Driver.lastname), func.lower(Driver.firstname)).all()
		elif type == 'classnumber':
			registered = query.order_by(Car.classcode, Car.number).all()
		
		if page == 'csv':
			# CSV data, just use a template and return
			objects = list()
			for (dr, car, reg) in registered:
				o = Registered()
				o.__dict__.update(dr.__dict__)
				o.__dict__.update(reg.__dict__)
				o.__dict__.update(car.__dict__)  # car is last so id = car.id
				objects.append(o)

			titles = ['lastname', 'firstname', 'email', 'address', 'city', 'state', 'zip', 'phone', 'sponsor', 'brag',
						'id', 'year', 'make', 'model', 'color', 'number', 'classcode', 'indexcode']
			return self.csv("cards", titles, objects)


		# Otherwise we are are PDF
		try:
			from reportlab.pdfgen import canvas
			from reportlab.lib.units import inch
			from reportlab.lib.utils import ImageReader
		except:
			c.text = "<h4>PDFGen not installed, can't create timing card PDF files from this system</h4>"
			return render_mako("/admin/simple.mako")

		try:
			from PIL import Image
		except ImportError:
			try:
				import Image
			except:
				c.text = "<h4>Python Image not installed, can't create timing card PDF files from this system</h4>"
				return render_mako("/admin/simple.mako")

		if page == 'letter': # Letter has an additional 72 points Y to space out
			size = (8*inch, 11*inch)
		else:
			size = (8*inch, 5*inch)

		if page == 'letter' and len(registered)%2 != 0:
			registered.append((None,None,None)) # Pages are always two cards per so make it divisible by 2

		buffer = cStringIO.StringIO()
		canvas = canvas.Canvas(buffer, pagesize=size, pageCompression=1)
		carddata = self.session.query(Data).get('cardimage')
		if carddata is None:
			cardimage = Image.new('RGB', (1,1))
		else:
			cardimage = Image.open(cStringIO.StringIO(carddata.data))

		cardimage = ImageReader(cardimage)

		while len(registered) > 0:
			if page == 'letter':
				canvas.translate(0, 18)  # 72/4, bottom margin for letter page
				(driver, car, reg) = registered.pop(0)
				drawCard(canvas, c.event, driver, car, cardimage)
				canvas.translate(0, 396)  # 360+72/2 card size plus 2 middle margins
				(driver, car, reg) = registered.pop(0)
				drawCard(canvas, c.event, driver, car, cardimage)
			else:
				(driver, car, reg) = registered.pop(0)
				drawCard(canvas, c.event, driver, car, cardimage)
			canvas.showPage()
		canvas.save()

		response.headers['Content-type'] = "application/octet-stream"
		response.headers['Content-Disposition'] = 'attachment;filename=cards.pdf'
		return buffer.getvalue()


