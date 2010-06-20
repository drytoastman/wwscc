"""MIMEType helpers

These helpers depend on the WebOb package.
"""
import mimetypes

class MIMETypes(object):
    """MIMETypes registration mapping
    
    The MIMETypes object class provides a single point to hold onto all
    the registered mimetypes, and their association extensions. It's
    used by the mimetypes function to determine the appropriate content
    type to return to a client.
    
    """
    aliases = {}
    
    def init(cls):
        """Loads a default mapping of extensions and mimetypes
        
        These are suitable for most web applications by default. 
        Additional types can be added with the using the mimetypes
        module.
        
        """
        mimetypes.init()
    init = classmethod(init)
    
    def add_alias(cls, alias, mimetype):
        """Creates a MIMEType alias to a full mimetype
        
        These aliases may not include /'s. Examples include 
        html->text/html, xml->application/xml."""
        if '/' in alias:
            raise ValueError("MIMEType aliases may not contain '/'")
        cls.aliases[alias] = mimetype
    add_alias = classmethod(add_alias)
    
    def __init__(self, environ):
        self.env = environ
    
    def _set_responce_conetent_type(self, mimetype):
        if 'pylons.pylons' in self.env:
            self.env['pylons.pylons'].response.content_type = mimetype
        return mimetype
        
    def mimetype(self, content_type):
        """Check the PATH_INFO of the current request and clients HTTP Accept 
        to attempt to use the appropriate mime-type
    
        If a content-type is matched, the appropriate response content
        type is set as well.
                
        This works best with URLs that end in extensions that differentiate
        content-type. Examples: http://example.com/example, 
        http://example.com/example.xml, http://example.com/example.csv
                
        Since browsers generally allow for any content-type, but should be
        sent HTML when possible, the html mimetype check should always come
        first, as shown in the example below.
        
        Example::
        
            # some code likely in environment.py
            MIMETypes.init()
            MIMETypes.add_alias('html', 'text/html')
            MIMETypes.add_alias('xml', 'application/xml')
            MIMETypes.add_alias('csv', 'text/csv')
            
            # code in a controller
            def somaction(self):
                # prepare a bunch of data
                # ......
                
                # prepare MIMETypes object
                m = MIMETypes(request.environ)
                
                if m.mimetype('html'):
                    return render('/some/template.html')
                elif m.mimetype('atom'):
                    return render('/some/xml_template.xml')
                elif m.mimetype('csv'):
                    # write the data to a csv file
                    return csvfile
                else:
                    abort(404)
        
        """
        import webob

        if content_type in MIMETypes.aliases:
            content_type = MIMETypes.aliases[content_type]
        path = self.env['PATH_INFO']
        guess_from_url = mimetypes.guess_type(path)[0]
        possible_from_accept_header = None
        has_extension = False
        if len(path.split('/')) > 1:
            last_part = path.split('/')[-1]
            if '.' in last_part:
                has_extension = True
        if 'HTTP_ACCEPT' in self.env:
            possible_from_accept_header = webob.acceptparse.MIMEAccept('ACCEPT', 
                self.env['HTTP_ACCEPT'])
        if has_extension == False:
            if possible_from_accept_header is None:
                return self._set_responce_conetent_type(content_type)
            elif content_type in possible_from_accept_header:
                return self._set_responce_conetent_type(content_type)
            else:
                return False
        if content_type == guess_from_url:
            # Guessed same mimetype
            return self._set_responce_conetent_type(content_type)
        elif guess_from_url is None and content_type in possible_from_accept_header:
            return self._set_responce_conetent_type(content_type)
        else:
            return False
