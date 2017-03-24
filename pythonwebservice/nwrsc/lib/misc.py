
def csvlist(inputstr, converter=None):
    arr = inputstr.strip().split(',')
    arr = [x for x in arr if x.strip() != '']
    if converter is not None:
        ret = []
        for x in arr:
            try:
                ret.append(converter(x))
            except:
                pass
        return ret
    else:
        return arr

