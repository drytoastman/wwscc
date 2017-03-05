
class AttrBase(object):

    def __init__(self, **kwargs):
        self.merge(**kwargs)

    def merge(self, **kwargs):
        """ Merge these values into this object, attr* gets merged with the attr dict """
        for k, v in kwargs.items():
            if k.startswith('attr'):
                if self.__dict__.get('attr', None) is None:
                    self.attr = dict()
                self.attr.update(v)
            else:
                setattr(self, k, v)

    def attrToUpper(self):
        """ Bring attr dict up to main level dict for output """
        for k, v in self.attr.items():
            setattr(self, k, v)

    def cleanAttr(self):
        if hasattr(self, 'attr'):
            for k, v in self.attr.items():
                if v is None or \
                  type(v) is str and v.strip() == "" or \
                  type(v) is int and v == 0 or \
                  type(v) is float and v <= 0.0 or \
                  type(v) is bool and not v:
                    del self.attr[k]

    def feedFilter(self, key, value):
        return value

    def getPublicFeed(self):
        d = dict()
        for k,v in self.__dict__.items():
            if k[0] == '_' or k == 'attr':
                continue
            v = self.feedFilter(k, v)
            if v is None:
                continue
            if isinstance(v, float):
                if v != 0:
                    d[k] = "%0.3f" % (v)
            else:
                d[k] = v
        return d

