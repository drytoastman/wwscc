
class AttrBase(object):

    def __init__(self, **kwargs):
        self.merge(**kwargs)

    def merge(self, **kwargs):
        for k, v in kwargs.items():
            if k == 'attr':
                if not hasattr(self, 'attr'):
                    self.attr = dict()
                self.attr.update(v)
            else:
                setattr(self, k, v)

    def getPublicFeed(self):
        d = dict()
        for k,v in self.__dict__.iteritems():
            if v is None:
                continue
            if isinstance(v, float):
                if v != 0:
                    d[k] = "%0.3f" % (v)
            else:
                d[k] = v
        return d

