"""Multimedia helpers for images, etc."""

import logging
import os
import struct
import sys

__all__ = ["choose_height", "get_dimensions_pil", "get_dimensions"]

def choose_height(new_width, width, height):
    """Return the height corresponding to ``new_width`` that's proportional
       to the original size (``width`` x ``height``).
    """
    proportion = float(height) / float(width)
    return int(new_width * proportion)

def get_dimensions_pil(path, default=(None, None)):
    """Get an image's size using the Python Imaging Library (PIL)

    Returns ``(width, height)`` as two integers, or ``default`` if the size
    could not be ascertained.  Failuer usually means the file does not exist
    or is not in a format recognized by PIL.

    Depends on the Python Imaging Library (http://pypi.python.org/pypi/PIL).
    See ``get_dimensions()`` if your application is not otherwise using PIL.
    """
    import Image
    try:
        im = Image(path)
    except Exception:
        return default
    return im.size

def get_dimensions(path, default=(None, None)):
    """Get an image's size using only the Python standard library

    Returns ``(width, height)`` as two integers, or ``default`` if the size
    could not be ascertained.  Failure usually means the file does not exist
    or is not in a recognized format.  Only JPG/PNG/GIF/BMP are supported at
    this time.

    The algorithms are based on a PyCode recipe by Perenzo/Welch/Ray.
    (http://www.pycode.com/modules/?id=32&tab=download)

    This helper recognizes fewer image formats and is potentially less
    accurate than ``get_dimensions_pil()``.
    """
    apath = os.path.abspath(path)
    try:
        f = open(path, "rb")
    except IOError:
        return default
    try:
        header = f.read(1024)
        # JPG
        if header.startswith("\xFF\xD8"):
            width = height = None
            f.seek(2)
            while True:
                length = 4
                buf = f.read(length)
                try:
                    marker, code, length = struct.unpack("!ccH", buf)
                except Exception:
                    break
                if marker != "\xff":
                    break
                if 0xc0 <= ord(code) <= 0xc3:
                    length = 5
                    buf = f.read(length)
                    height, width = struct.unpack("!xHH", buf)
                else:
                    f.read(length-2)
            return width, height
        # PNG
        elif header.startswith("\x89PNG\x0d\x0a\x1a\x0a") or \
            header.startswith("\x8aMNG\x0d\x0a\x1a\x0a"):
            f.seek(12)
            control = f.read(4)
            if control in ["IHDR", "MHDR"]:
                buf = f.read(8)
                width, height = struct.unpack("!II", buf)
                return width, height
        # GIF
        elif header.startswith("GIF87a") or header.startswith("GIF89a"):
            f.seek(6)
            buf = f.read(7)
            width, height, flags, bci, par = struct.unpack("<HHBBB", buf)
            return width, height
        # BMP
        elif header.startswith("BM"):
            f.seek(18)
            buf = f.read(8)
            width, height = struct.unpack("<II", buf)
            return width, height
        # Unknown
        return default
    finally:
        f.close()

def test_get_dimensions():
    files = sys.argv[1:]
    if not files:
        sys.exit("usage: %s FILES ...\nPrints dimensions of each image")
    for file in files:
        apath = os.path.abspath(file)
        print "%s:" % apath,
        if not os.path.isfile(file):
            print "does not exist or is not a plain file"
            continue
        width, height = get_dimensions(file)
        if width is None and height is None:
            print "could not get dimensions"
        else:
            if width is None:
                width = "UNKNOWN"
            if height is None:
                height = "UNKNOWN"
            print "%s x %s" % (width, height)
            
        

if __name__ == "__main__":  test_get_dimensions()
