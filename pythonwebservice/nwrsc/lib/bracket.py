#!/usr/bin/python

from pylons import config
import os, cStringIO, logging
log = logging.getLogger(__name__)

class Bracket(object):

	RANK1 =  [ 1 ]
	RANK2 =  [ 1, 2 ]
	RANK4 =  [ 1, 4, 2, 3 ]
	RANK8 =  [ 1, 8, 4, 5, 2, 7, 3, 6 ]
	RANK16 = [ 1, 16, 8, 9, 4, 13, 5, 12, 2, 15, 7, 10, 3, 14, 6, 11 ]
	RANK32 = [ 1, 32, 16, 17, 8, 25, 9, 24, 4, 29, 13, 20, 5, 28, 12, 21, 2, 31, 15, 18, 7, 26, 10, 23, 3, 30, 14, 19, 6, 27, 11, 22 ]
	RANKS = RANK32 + RANK16 + RANK8 + RANK4 + RANK2 + RANK1 + [0]
	RANKS.reverse()

	def __init__(self, depth, rounds = None):
		self.roundwidth = 145
		self.depth = depth
		self.rounds = rounds
		self.coords = list()

		self.baserounds = 2**(depth-1)
		imagesize, self.initialspacing = {
				2: ((470, 270), 44), 
				4: ((615, 400), 44), 
				8: ((760, 530), 33), 
				16: ((905,710), 22), 
			}.get(self.baserounds, None)
	
		if imagesize is None:
			log.error("drawBracket with invalid sizing %d" % (depth))
			return
	
		try:
			from PIL import Image, ImageFont, ImageDraw
		except:
			import Image, ImageFont, ImageDraw
		
		self.image = Image.new("L", imagesize, "White")
		self.draw = ImageDraw.Draw(self.image)
		self.font = ImageFont.truetype(os.path.join(os.path.dirname(__file__), 'universal.ttf'), 11)
		self.fill = 'black'

	def getCoords(self):
		return self.coords

	def doEntrant(self, x, y, first, last, time):
		w2, h2 = self.font.getsize(time)
		name = first + " " + last
		w1, h1 = self.font.getsize(name)
		while w1 > 100:
			name = name[:-1]
			w1, h1 = self.font.getsize(name)
		

		self.draw.text((x+4, y-h1), name, font=self.font, fill=self.fill)
		self.draw.text((x+self.roundwidth-w2-4, y-h2), time, font=self.font, fill=self.fill)

	def upperText(self, x, y, rnd):
		if self.draw is None or self.rounds is None: return
		try:
			dr = self.rounds[rnd].car1.driver
			self.doEntrant(x, y, dr.firstname, dr.lastname, "%0.3lf" % self.rounds[rnd].car1dial)
		except Exception, e: log.debug(e)

	def lowerText(self, x, y, rnd):
		if self.draw is None or self.rounds is None: return
		try: 
			dr = self.rounds[rnd].car2.driver
			self.doEntrant(x, y, dr.firstname, dr.lastname, "%0.3lf" % self.rounds[rnd].car2dial)
		except Exception, e: log.debug(e)

	def line(self, x1, y1, x2, y2):
		if self.draw is None: return
		self.draw.line(((x1,y1), (x2,y2)), fill='black')

	def drawBracket(self, x, y, spacing, rnd):
		self.coords.append((rnd, "%d,%d,%d,%d" % (x, y-10, x+self.roundwidth, y+spacing)))
		if self.draw is None: return

		self.upperText(x, y, rnd)
		self.line(x, y, x+self.roundwidth, y)
		y += spacing
		self.lowerText(x, y, rnd)
		self.line(x, y, x+self.roundwidth, y)
		x += self.roundwidth
		self.line(x, y-spacing, x, y)

	def getImage(self):
		# This is where we actually draw the bracket, converting from Java code
		self.coords = list()
		startx = 13
		starty = 20
		spacing = self.initialspacing
		x = 0
		y = 0
		
		# Draw each round of brackets 
		ii = self.baserounds
		rnd = (self.baserounds*2)-1
		while ii > 0:
			# Line ourselves up 
			x = startx
			y = starty
			
			# Draw one vertical line of brackets 
			for jj in range(0, ii):
				# Draw first horizontal, second horizontal and then right hand vertical 
				if self.draw is not None and ii == self.baserounds:
					w2, h2 = self.font.getsize("32")
					self.draw.text((x-12, y-h2), str(self.RANKS[rnd*2+1]), font=self.font, fill=self.fill)
					self.draw.text((x-12, y+spacing-h2), str(self.RANKS[rnd*2]), font=self.font, fill=self.fill)
				self.drawBracket(x, y, spacing, rnd)
				y += (2*spacing)
				rnd -= 1
			
			# Adjust our starting position and spacing for the next column 
			startx += self.roundwidth
			starty += spacing/2
			spacing *= 2
			ii /= 2
	
		# Draw the third place bracket and 3rd place winner line 
		x += 20
		y = y - (spacing/2) + (2*self.initialspacing)
		self.drawBracket(x, y, self.initialspacing, 99)

		# Draw the 3rd and 1st place winners (round 0)
		x += self.roundwidth;
		y += (self.initialspacing/2);
		self.lowerText(x, y, 0)
		self.line(x, y, x+self.roundwidth-5, y)
		self.upperText(startx, starty, 0)
		self.line(startx, starty, startx+self.roundwidth-5, starty)
	
		if self.draw is None:
			return None

		f = cStringIO.StringIO()
		self.image.save(f, "PNG")
		return f.getvalue()
			
