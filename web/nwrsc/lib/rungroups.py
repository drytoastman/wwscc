
class RunGroupList(object):
	def __init__(self, groupnum):
		self.groupnum = groupnum
		self.classes = list()

	def addClass(self, cls):
		self.classes.append(cls)


class Entrant(object):
	def __init__(self, dr, car):
		self.driver = dr
		self.car = car

class ClassOrder(object):

	def __init__(self, code):
		self.code = code
		self.first = list()
		self.second = list()

	def add(self, driver, car):
		if (car.number >= 100):
			self.second.append(Entrant(driver, car))
		else:
			self.first.append(Entrant(driver, car))

	def getFirstByNum(self):
		return sorted(self.first, key=attrgetter('car.number'))

	def getFirstByResult(self):
		return sorted(self.first, key=attrgetter('net'))

	def getSecondByNum(self):
		return sorted(self.second, key=attrgetter('car.number'))

	def getSecondByResult(self):
		return sorted(self.second, key=attrgetter('net'))


