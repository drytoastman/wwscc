
"""
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
		self.ones = set()
		self.first = list()
		self.second = list()

	def add(self, driver, car, ckey):
		if car.number+100 not in self.ones and car.number-100 not in self.ones:
			self.first.append(Entrant(driver, car))
			self.first.sort(key=lambda e: getattr(e.car, ckey))
			self.ones.add(car.number)
		else:
			self.second.append(Entrant(driver, car))
			self.second.sort(key=lambda e: getattr(e.car, ckey))
"""

