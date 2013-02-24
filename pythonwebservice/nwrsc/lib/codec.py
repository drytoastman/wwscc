
import struct
import datetime
import time

from sqlalchemy.databases.sqlite import SLDate, SLDateTime

class DataInput(object):
	def __init__(self, bytes):
		self.data = bytes
		self.index = 0

	def getCounter(self):
		return self.index

	def dataAvailable(self):
		return self.index < len(self.data)

	def readByte(self):
		val = struct.unpack(">b", self.data[self.index])[0]
		self.index += 1
		return val

	def readBoolean(self):
		val = struct.unpack(">b", self.data[self.index])[0]
		self.index += 1
		return val != 0

	def readShort(self):
		val = struct.unpack(">h", self.data[self.index:self.index+2])[0]
		self.index += 2
		return val

	def readInt(self):
		val = struct.unpack(">i", self.data[self.index:self.index+4])[0]
		self.index += 4
		return val

	def readLong(self):
		val = struct.unpack(">q", self.data[self.index:self.index+8])[0]
		self.index += 8
		return val

	def readDouble(self):
		val = struct.unpack(">d", self.data[self.index:self.index+8])[0]
		self.index += 8
		return val

	def readString(self, len):
		val = self.data[self.index:self.index+len]
		self.index += len
		return str(val)

	def readByteArray(self, len):
		val = self.data[self.index:self.index+len]
		self.index += len
		return val


class StringBuilder(object):

	def __init__(self):			self.p = list()
	def append(self, str):		self.p.append(str)
	def getString(self):		return ''.join(self.p)
	def writeByte(self, b):		self.p.append(struct.pack('>b', b))
	def writeBoolean(self, b):	self.p.append(struct.pack('>b', b))
	def writeShort(self, s):	self.p.append(struct.pack('>h', s))
	def writeInt(self, i):		self.p.append(struct.pack('>i', i))
	def writeLong(self, l):		self.p.append(struct.pack('>q', l))
	def writeDouble(self, d):	self.p.append(struct.pack('>d', d))
	def writeString(self, s):
		self.p.append(struct.pack('>h', len(s)))
		self.p.append(str(s))


class Codec(object):

	INTTYPE = 1
	LONGTYPE = 2
	DOUBLETYPE = 3
	STRINGTYPE = 4
	BOOLEANTYPE = 5
	BYTEARRAYTYPE = 6
	DATETYPE = 7
	DATETIMETYPE = 8

	SELECT = 20
	UPDATE = 21
	FUNCTION = 22
	LASTID = 23
	ERROR = 24
	RESULTS = 25

	sldate = SLDate()
	sldatetime = SLDateTime()

	@classmethod
	def decodeRequest(cls, data):
		type = data.readByte()
		key = data.readString(data.readShort())
		values = Codec.decodeValues(data)
		return (type, key, values)


	@classmethod
	def decodeValues(cls, data):
		valueslen = data.readShort()
		limit = valueslen + data.getCounter()
		vals = list()
		while data.getCounter() < limit:
			type = data.readByte()
			if type == Codec.INTTYPE: vals.append(data.readInt())
			elif type == Codec.LONGTYPE: vals.append(data.readLong())
			elif type == Codec.DOUBLETYPE: vals.append(data.readDouble())
			elif type == Codec.STRINGTYPE: vals.append(data.readString(data.readShort()))
			elif type == Codec.BOOLEANTYPE: vals.append(data.readBoolean())
			elif type == Codec.BYTEARRAYTYPE: vals.append(data.readByteArray(data.readShort()))
			elif type == Codec.DATETYPE: vals.append(datetime.date.fromtimestamp(data.readInt()))
			elif type == Codec.DATETIMETYPE: vals.append(datetime.datetime.fromtimestamp(data.readInt()))
			else: raise Exception("Invalid data type while decoding")

		return vals


	@classmethod
	def encodeError(cls, filename, lineno, msg):
		builder = StringBuilder()
		builder.writeByte(Codec.ERROR)
		builder.writeString(filename)
		builder.writeString(str(lineno))
		builder.writeString(msg)
		return builder.getString()

	@classmethod
	def encodeLastId(cls, id):
		if id is None:
			id = 0
		return struct.pack(">bl", Codec.LASTID, int(id))

	@classmethod
	def encodeResults(cls, rs):
		# RESULTS <int size> <header list> <object list> ...
		builder = StringBuilder()
		first = True
		for row in rs:
			if first: # Only encode header if there is actually results to give
				names = row.keys()
				h = Codec.encodeValues(['']*len(names), names)
				builder.writeShort(len(h))
				builder.append(h)
				first = False
			v = Codec.encodeValues(names, [row[k] for k in names])
			builder.writeShort(len(v))
			builder.append(v)

		data = builder.getString()
		return struct.pack(">bl", Codec.RESULTS, len(data)) + data

	@classmethod
	def encodeValues(cls, names, vals):
		stream = StringBuilder()
		for n, v in zip(names, vals):

			if n == 'date' and type(v) is str: # hack but useful
				v = Codec.sldate.result_processor(None)(v) 
			elif n == 'updated' and type(v) is str:
				v = Codec.sldatetime.result_processor(None)(v) 

			if type(v) is bool:
				stream.writeByte(Codec.BOOLEANTYPE)
				stream.writeBoolean(v)
			elif type(v) is datetime.date:
				stream.writeByte(Codec.DATETYPE)
				stream.writeInt(int(time.mktime(v.timetuple()))) 
			elif type(v) is datetime.datetime:
				stream.writeByte(Codec.DATETIMETYPE)
				stream.writeInt(int(time.mktime(v.timetuple()))) 
			elif type(v) is int:
				stream.writeByte(Codec.INTTYPE)
				stream.writeInt(v)
			elif type(v) is long:
				stream.writeByte(Codec.LONGTYPE)
				stream.writeLong(v)
			elif type(v) is float:
				stream.writeByte(Codec.DOUBLETYPE)
				stream.writeDouble(v)
			elif type(v) in (str,unicode):
				stream.writeByte(Codec.STRINGTYPE)
				stream.writeString(str(v))
			#elif type(v) is byte[]:
			elif v is None: # use blank string for null
				stream.writeByte(Codec.STRINGTYPE)
				stream.writeString('')
			else:
				raise Exception("unexpected param type: %s" % (type(v)));

		return stream.getString()

