"""Functions that output text (not HTML).

Helpers for filtering, formatting, and transforming strings.
"""

import re
import textwrap

__all__ = [
    "truncate", 
    "excerpt",
    "plural",
    "chop_at",
    "lchop",
    "rchop",
    "series",
    "strip_leading_whitespace",
    "wrap_paragraphs",
    ]

def truncate(text, length=30, indicator='...', whole_word=False):
    """Truncate ``text`` with replacement characters.
    
    ``length``
        The maximum length of ``text`` before replacement
    ``indicator``
        If ``text`` exceeds the ``length``, this string will replace
        the end of the string
    ``whole_word``
        If true, shorten the string further to avoid breaking a word in the
        middle.  A word is defined as any string not containing whitespace.
        If the entire text before the break is a single word, it will have to
        be broken.

    Example::

        >>> truncate('Once upon a time in a world far far away', 14)
        'Once upon a...'
        
    """
    if not text: 
        return ""
    if len(text) <= length:
        return text
    short_length = length - len(indicator)
    if not whole_word:
        return text[:short_length] + indicator
    # Go back to end of previous word.
    i = short_length
    while i >= 0 and not text[i].isspace():
        i -= 1
    while i >= 0 and text[i].isspace():
        i -= 1
    #if i < short_length:
    #    i += 1   # Set to one after the last char we want to keep.
    if i <= 0:
        # Entire text before break is one word, or we miscalculated.
        return text[:short_length] + indicator
    return text[:i+1] + indicator


def excerpt(text, phrase, radius=100, excerpt_string="..."):
    """Extract an excerpt from the ``text``, or '' if the phrase isn't
    found.

    ``phrase``
        Phrase to excerpt from ``text``
    ``radius``
        How many surrounding characters to include
    ``excerpt_string``
        Characters surrounding entire excerpt
    
    Example::
    
        >>> excerpt("hello my world", "my", 3)
        '...lo my wo...'

    """
    if not text or not phrase:
        return text

    pat = re.compile('(.{0,%s}%s.{0,%s})' % (radius, re.escape(phrase), 
                                             radius), re.I)
    match = pat.search(text)
    if not match:
        return ""
    excerpt = match.expand(r'\1')
    if match.start(1) > 0:
        excerpt = excerpt_string + excerpt
    if match.end(1) < len(text):
        excerpt = excerpt + excerpt_string
    if hasattr(text, '__html__'):
        return literal(excertp)
    else:
        return excerpt


def plural(n, singular, plural, with_number=True):
    """Return the singular or plural form of a word, according to the number.

    Usage:
    >>> plural(2, "ox", "oxen")
    '2 oxen'
    >>> plural(2, "ox", "oxen", False)
    'oxen'
    """
    if n == 1:
        form = singular
    else:
        form = plural
    if with_number:
        return "%s %s" % (n, form)
    else:
        return form

def chop_at(s, sub, inclusive=False):
    """Truncate string ``s`` at the first occurence of ``sub``.

    If ``inclusive`` is true, truncate just after ``sub`` rather than at it.

    >>> chop_at("plutocratic brats", "rat")
    'plutoc'
    >>> chop_at("plutocratic brats", "rat", True)
    'plutocrat'
    """
    pos = s.find(sub)
    if pos == -1:
        return s
    if inclusive:
        return s[:pos+len(sub)]
    return s[:pos]

def lchop(s, sub):
    """Chop ``sub`` off the front of ``s`` if present.
    
    >>> lchop("##This is a comment.##", "##")
    'This is a comment.##'
    """
    if s.startswith(sub):
        s = s[len(sub):]
    return s
    
def rchop(s, sub):
    """Chop ``sub`` off the end of ``s`` if present.
    
    >>> rchop("##This is a comment.##", "##")
    '##This is a comment.'
    """
    if s.endswith(sub):
        s = s[:-len(sub)]
    return s

def strip_leading_whitespace(s):
    """Strip the leading whitespace in all lines in ``s``.
    
    This deletes *all* leading whitespace.  ``textwrap.dedent`` deletes only
    the whitespace common to all lines.
    """
    ret = [x.lstrip() for x in s.splitlines(True)]
    return "".join(ret)

def wrap_paragraphs(text, width=72):
    """Wrap all paragraphs in a text string to the specified width.

    ``width`` may also be a ``textwrap.TextWrapper`` instance, in which case it
    will be used to do the wrapping.  This provides a way to set other options
    besides the width, and is more efficient when wrapping many texts.
    """
    if isinstance(width, textwrap.TextWrapper):
        wrapper = width
    else:
        wrapper = textwrap.TextWrapper(width=width)
    result = []
    lines = text.splitlines(True)
    lines_len = len(lines)
    start = 0
    end = None
    while start < lines_len:
        # Leave short lines as-is.
        if len(lines[start]) <= width:
            result.append(lines[start])
            start += 1
            continue
        # Found a long line, peek forward to end of paragraph.
        end = start + 1
        while end < lines_len and not lines[end].isspace():
            end += 1
        # 'end' is one higher than last long lone.
        paragraph = ''.join(lines[start:end])
        paragraph = wrapper.fill(paragraph) + "\n"
        result.append(paragraph)
        start = end
        end = None
    return "".join(result)

def series(items, conjunction="and", strict_commas=True):
    """Format a series for use in English text.

    Examples:

    >>> series(["A", "B", "C"])
    'A, B, and C'
    >>> series(["A", "B", "C"], "or")
    'A, B, or C'
    >>> series(["A", "B", "C"], strict_commas=False)
    'A, B and C'
    >>> series(["A", "B"])
    'A and B'
    >>> series(["A"])
    'A'
    >>> series([])
    ''
    """
    items = list(items)
    length = len(items)
    if length == 0:
        return ""
    if length == 1:
        return items[0]
    if length == 2:
        strict_commas = False
    nonlast = ", ".join(items[:-1])
    last = items[-1]
    comma = strict_commas and "," or ""
    return "%s%s %s %s" % (nonlast, comma, conjunction, last)
