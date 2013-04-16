# 8inch = 576points, half = 288
# 5inch = 360points, half = 180
MIDDLE = 288

barChar = {}
barChar['0'] = 'nnnwwnwnn'
barChar['1'] = 'wnnwnnnnw'
barChar['2'] = 'nnwwnnnnw'
barChar['3'] = 'wnwwnnnnn'
barChar['4'] = 'nnnwwnnnw'
barChar['5'] = 'wnnwwnnnn'
barChar['6'] = 'nnwwwnnnn'
barChar['7'] = 'nnnwnnwnw'
barChar['8'] = 'wnnwnnwnn'
barChar['9'] = 'nnwwnnwnn'
barChar['*'] = 'nwnnwnwnn'
barChar['-'] = 'nwnnnnwnw'

months = ["", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]

def code39Right(c, x, y, code, height=20, baseline=3.0):
	wide = baseline
	narrow = baseline / 2.5
	gap = narrow

	codestr = '*%s*' % code.upper()
	xpos = x
	for char in codestr[::-1]:
		seq = barChar.get(char, None)
		if seq is None:
			continue

		for bar in range(8, -1, -1):
			if seq[bar] == 'n':
				lineWidth = narrow
			else:
				lineWidth = wide

			xpos -= lineWidth/2.0
			c.setLineWidth(lineWidth)
			if (bar % 2) == 0:
				c.setStrokeColorRGB(0,0,0)
				c.line(xpos, y, xpos, y+height)
			xpos -= lineWidth/2.0

		xpos -= gap

	c.setFont('Courier', 8)
	c.drawRightString(x, y-8, code)


def timerow(c, y, height):
	c.rect(MIDDLE-270,y, 220, height)
	c.rect(MIDDLE-50, y, 50,  height)
	c.rect(MIDDLE,    y, 50,  height)
	c.rect(MIDDLE+50, y, 220, height)

def row22(c, y, height):
	c.rect(MIDDLE-270, y, 270, height)
	c.rect(MIDDLE,     y, 270, height)

def row21(c, y, height):
	c.rect(MIDDLE-270, y, 270, height)
	c.rect(MIDDLE,     y, 135, height)

def row211(c, y, height):
	c.rect(MIDDLE-270, y, 270, height)
	c.rect(MIDDLE,     y, 135, height)
	c.rect(MIDDLE+135, y, 135, height)

def drawCard(c, event, driver, car, image, **kwargs):

	# Draw all the boxes
	c.setLineWidth(0.5)

	y = 20
	timerow(c, y, 23); y += 23
	timerow(c, y, 23); y += 23
	timerow(c, y, 23); y += 23
	timerow(c, y, 23); y += 23
	timerow(c, y, 23); y += 23
	timerow(c, y, 23); y += 23

	timerow(c, y, 15); y += 21

	row22(c, y, 21); y+= 21
	row22(c, y, 21); y+= 21
	row22(c, y, 21); y+= 21
	row22(c, y, 21); y+= 21
	row211(c, y, 21); y+= 21
	row211(c, y, 21); y+= 21

	# Draw Labels
	c.setFont('Helvetica-Bold', 10)
	x = 67
	y = 187
	c.drawRightString(x, y, "Sponsor:"); y += 21
	c.drawRightString(x, y, "Email:"); y += 21
	c.drawRightString(x, y, "Phone:"); y += 21
	c.drawRightString(x, y, "CSZ:"); y += 21
	c.drawRightString(x, y, "Address:"); y += 21
	c.drawRightString(x, y, "Name:"); y += 21

	x = 325
	y = 187
	c.drawRightString(x, y, "Brag:"); y += 21
	c.drawRightString(x, y, "Memb:"); y += 21
	c.drawRightString(x, y, "Color:"); y += 21
	c.drawRightString(x, y, "Model:"); y += 21
	c.drawRightString(x, y, "Make:"); y += 21
	c.drawRightString(x, y, "Year:"); y += 21

	x = 477
	y = 187 + (4*21)
	c.drawRightString(x, y, "Number:"); y += 21
	c.drawRightString(x, y, "Class(Idx):"); y += 21

	c.setFont('Helvetica-Bold', 13)
	c.drawCentredString(MIDDLE, 337, "%s" % (event.name))
	c.setFont('Helvetica-Bold', 11)
	c.drawCentredString(MIDDLE, 325, "%s %d,%d" % (months[event.date.month], event.date.day, event.date.year))
	c.setFont('Helvetica-Bold', 12)
	c.drawAlignedString(MIDDLE, 313, "Sponsored by: %s" % (event.sponsor), ':')
	if driver is None or car is None:
		return

	c.setFont('Courier', 10)
	x = 70
	y = 187
	c.drawString(x, y, driver.sponsor or ""); y += 21
	c.drawString(x, y, driver.email or ""); y += 21
	c.drawString(x, y, driver.phone or ""); y += 21
	c.drawString(x, y, "%s %s %s" % (driver.city, driver.state, driver.zip)); y += 21
	c.drawString(x, y, driver.address or ""); y += 21
	c.drawString(x, y, "%s %s" % (driver.firstname, driver.lastname)); y += 21

	x = 330
	y = 187
	c.drawString(x, y, driver.brag or ""); y += 21
	c.drawString(x, y, driver.membership or ""); y += 21
	c.drawString(x, y, car.color or ""); y += 21
	c.drawString(x, y, car.model or ""); y += 21
	c.drawString(x, y, car.make or ""); y += 21
	c.drawString(x, y, car.year or ""); y += 21

	x = 480
	y = 187 + (4*21)
	c.drawString(x, y, str(car.number)); y += 21
	if car.indexcode:
		c.drawString(x, y, "%s (%s)" % (car.classcode, car.indexcode)); y += 21
	else:
		c.drawString(x, y, car.classcode); y += 21

	code39Right(c, 558, 314, str(car.id))


