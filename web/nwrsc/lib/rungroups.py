
from nwrsc.model import RunGroup, Driver, Car, Registration
from sqlalchemy.sql import func

class ClassList(object):

	def __init__(self, code):
		self.code = code
		self.first = list()
		self.second = list()

	def add(self, e):
		if (e.car.number >= 100):
			self.second.append(e)
		else:
			self.first.append(e)

	def addGroups(self, f, s):
		f.append(Group(self.code, False, len(self.first)))
		if len(self.second) > 0:
			s.append(Group(self.code, True, len(self.second)))


class Group(object):

	def __init__(self, name, second, size):
		self.name = name
		self.second = second
		self.size = size


def allPerms(input):
    if len(input) <=1:
        yield input
    else:
        for perm in allPerms(input[1:]):
            for i in range(len(perm)+1):
                yield perm[:i] + input[0:1] + perm[i:]


class GridOrder(object):


	def __init__(self, session, event):
		#g1classes = [x.classcode for x in session.query(RunGroup).filter(RunGroup.eventid==event.id).filter(RunGroup.rungroup==1)]
		g2classes = [x.classcode for x in session.query(RunGroup).filter(RunGroup.eventid==event.id).filter(RunGroup.rungroup==2)]
		group1A = dict()
		group1B = dict()
		group2A = dict()
		group2B = dict()
		for c in session.query(Car).join("registration").filter(Registration.eventid==event.id):
			if c.classcode in g2classes:
				if c.number < 100: g = group1A
				else: g = group1B
			else:
				if c.number < 100: g = group2A
				else: g = group2B

			g.setdefault(c.classcode, []).append(c.classcode)
			

		self.process(group1A, group1B)
		self.process(group2A, group2B)

		"""
		group1A = group1A.values()
		group1B = group1B.values()
		group2A = group2A.values()
		group2B = group2B.values()

		for d in (group1A, group1B, group2A, group2B):
			print "group"
			lastclass = ""
			for x in sorted(d, key=len, reverse=True):
				for pair in self.pair(x):
					if pair[0] != lastclass:
						print "-----"
					print pair
					lastclass = pair[0]
		"""


	def pair(self, l):
		if len(l) % 2 != 0:
			last = l.pop(-1)
			return zip(l, l) + [(last,)]
		return zip(l, l)


	def process(self, firsts, seconds):
		onlyfirst = list()
		grouping1 = list()
		grouping2 = list()

		for x in firsts:
			if x not in seconds:
				onlyfirst.append(x)
		for k, v in firsts.iteritems():
			if len(v) == 1:
				grouping1.append(k)
		for k, v in seconds.iteritems():
			if len(v) == 1:
				grouping2.append(k)

		print "first %s" % onlyfirst
		print "g1 %s" % grouping1
		print "g2 %s" % grouping2

		list1 = firsts.values()

	def printPairs(self, list):
		
		#entrants = Database.d.getRegisteredEntrants();
		entrants = list()
		clmap = dict()
		groupA1 = list()
		groupA2 = list()
		groupB1 = list()
		groupB2 = list()

		#for code in g1classes + g2classes:
		#	clmap[code] = ClassList(code)

		#for e in entrants:
		#	clmap[e.classcode].add(e)

		#for c in clmap.itervalues():
		#	if c.code in g2classes:
		#		c.addGroups(groupB1, groupB2)
		#	else:
		#		c.addGroups(groupA1, groupA2)

#		processGroup(groupA1, groupA2);
#		System.out.println("----------");
#		processGroup(groupB1, groupB2);

"""

	def score(self, first, second):
		pass
		#for (int ii = 0; ii < first.length; ii++)

	
	def processGroup(first, second):
		PermutationGenerator gen;
		//for (Group g : first)
		//{
		
		gen = new PermutationGenerator(first.size());
		while (gen.hasMore())
		{
			Group test[] = getOrderedGroup(first, gen.getNext());
		}
		//}
		for (Group g : second)
		{
			System.out.println(g.name + "(" + g.second + "): " + g.size);
		}

"""
