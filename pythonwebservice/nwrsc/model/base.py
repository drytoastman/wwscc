import json

class BaseEncoder(json.JSONEncoder):
    """ Helper that calls getPublicFeed if available for getting json encoding """
    def default(self, o):
        if hasattr(o, 'getPublicFeed'):
            return o.getPublicFeed()
        else:
            return str(o)


class AttrBase(object):

    def __init__(self, **kwargs):
        self.attr = dict()
        self.merge(**kwargs)

    def merge(self, **kwargs):
        """ Merge these values into this object, attr gets merged with the attr dict """
        for k, v in kwargs.items():
            if k == 'attr':
                self.attr.update(v)
            else:
                setattr(self, k, v)

    def cleanAttr(self):
        if hasattr(self, 'attr'):
            for k in list(self.attr.keys()):
                v = self.attr[k]
                if v is None or \
                  type(v) is str and v.strip() == "" or \
                  type(v) is int and v == 0 or \
                  type(v) is float and v <= 0.0 or \
                  type(v) is bool and not v:
                    del self.attr[k]

    def feedFilter(self, key, value):
        """ Override this function to filter our things that shouldn't end up in the public json/xml feed """
        return value

    def getPublicFeed(self):
        """ Return a single level dict of the attributes and values to create a feed for this object """
        d = dict()
        self.cleanAttr()
        for k,v in {**self.__dict__, **self.attr}.items():
            if k[0] == '_' or k == 'attr':
                continue
            v = self.feedFilter(k, v)
            if v is None:
                continue
            d[k] = v
        return d

    def __repr__(self):
        return "<{}>".format(self.__class__.__name__)


class Entrant(AttrBase):
    """ Generic holder for some subset of driver and car entry data """
    def __repr__(self):
        return "Entrant ({} {})".format(getattr(self, 'firstname', 'Missing'), getattr(self, 'lastname', 'Missing'))

