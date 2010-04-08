
import gzip
import cStringIO

class GzipMiddleware(object):
	def __init__(self, app, compresslevel=9):
		self.app = app
		self.compresslevel = compresslevel

	def __call__(self, environ, start_response):
		# Browser doesn't understand gzip, just call subapp
		if 'gzip' not in environ.get('HTTP_ACCEPT_ENCODING', ''):
			return self.app(environ, start_response)

		subbuffer = cStringIO.StringIO()
		subargs = []

		def dummy_start_response(status, headers, exc_info=None):
			subargs.append(status)
			subargs.append(headers)
			subargs.append(exc_info)
			return subbuffer.write

		# Call the sub app
		app_iter = self.app(environ, dummy_start_response)


		# See if it returned something we wish to compress, if not wrap and return
		dontcompress = True
		for name, value in subargs[1]:
			if name.lower() == 'content-type':
				if value.startswith('text/html') or value.startswith('application'):
					dontcompress = False
					break

		if dontcompress:
			writable = start_response(subargs[0], subargs[1], subargs[1])
			writable(subbuffer.getvalue())
			subbuffer.close()
			return app_iter
	
		# Compress both the data written through startresponse callable and the iterator returned
		gzipbuffer = cStringIO.StringIO()
		compressor = gzip.GzipFile(mode='wb', compresslevel=self.compresslevel, fileobj=gzipbuffer)
		compressor.write(subbuffer.getvalue())
		for line in app_iter:
			compressor.write(line)
		if hasattr(app_iter, 'close'):
			app_iter.close()
		compressor.close()
		result = gzipbuffer.getvalue()

		# Form our new Content-Length and add Content-Encoding
		headers = []
		for name, value in subargs[1]:
			if name.lower() != 'content-length':
				headers.append((name, value))
		headers.append(('Content-Length', str(len(result))))
		headers.append(('Content-Encoding', 'gzip'))

		# Call start response and return our compressed data
		start_response(subargs[0], headers, subargs[2])
		subbuffer.close()
		gzipbuffer.close()
		return [result]

