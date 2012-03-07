LastName,FirstName,Email,Address,City,State,Zip,Phone,Sponsor,Brag,Year,Make,Model,Color,Number,Class,Index,Carid
<%
for (dr,car,reg) in c.registered:
	line = []
	for s in (dr.lastname, dr.firstname, dr.email, dr.address, dr.city, dr.state, dr.zip, dr.phone, dr.sponsor, dr.brag,
			car.year, car.make, car.model, car.color, car.number, car.classcode, car.indexcode, car.id):
		if s is None:
			line.append("\"\"")
		elif type(s) is int:
			line.append("%d" % s)
		else:
			line.append("\"%s\""%s.replace('\n', ' ').replace('"', '""'))
	context.write(','.join(line) + '\n')
%>
