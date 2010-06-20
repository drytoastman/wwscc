"""Functions that convert from text markup languages to HTML.

"""
import re

from webhelpers.html import HTML, escape, literal, lit_sub
import webhelpers.textile as textile
import webhelpers.markdown as _markdown

__all__ = [
    "markdown", 
    "textilize",
    "nl2br",
    "format_paragraphs",
    ]

_universal_newline_rx = re.compile(R"\r\n|\n|\r")  # All types of newline.
_paragraph_rx = re.compile(R"\n{2,}")  # Paragraph break: 2 or more newlines.
br = HTML.br() + "\n"

def markdown(text, **kwargs):
    """Format the text to HTML with MarkDown formatting.
    
    This function uses the `Python MarkDown library 
    <http://www.freewisdom.org/projects/python-markdown/>`_
    which is included with WebHelpers.  It does not include extensions
    due to circular import issues.  If you need the footnotes or RSS
    extensions, use the full Markdown package instead.  
    
    IMPORTANT:
    If your source text is untrusted and may contain malicious HTML markup,
    pass ``safe_mode="escape"`` to escape it, ``safe_mode="replace"`` to
    replace it with a scolding message, or ``safe_mode="remove"`` to strip it.

    There is at least one other Markdown package for Python.  python-markdown2
    (http://code.google.com/p/python-markdown2/), which claims to be faster and
    to handle edge cases better.  WebHelpers is sticking to the original Python
    Markdown for now for backward compatibility and because nobody has
    complained about the speed.
    """
    return literal(_markdown.markdown(text, **kwargs))

def textilize(text, sanitize=False):
    """Format the text to HTML with Textile formatting.
    
    This function uses the `PyTextile library <http://dealmeida.net/>`_ 
    which is included with WebHelpers.
    
    Additionally, the output can be sanitized which will fix tags like 
    <img />,  <br /> and <hr /> for proper XHTML output.
    
    """
    texer = textile.Textiler(text)
    return literal(texer.process(sanitize=sanitize))

def nl2br(text):
    """Insert a <br /> before each newline.
    """
    if text is None:
        return literal("")
    text = lit_sub(_universal_newline_rx, "\n", text)
    text = HTML(text).replace("\n", br)
    return text

def format_paragraphs(text, preserve_lines=False):
    """Convert text to HTML paragraphs.

    ``text``:
        the text to convert.  Split into paragraphs at blank lines (i.e.,
        wherever two or more consecutive newlines appear), and wrap each
        paragraph in a <p>.

    ``preserve_lines``:
        If true, add <br />  before each single line break
    """
    if text is None:
        return literal("")
    text = lit_sub(_universal_newline_rx, "\n", text)
    paragraphs = _paragraph_rx.split(text)
    for i, para in enumerate(paragraphs):
        if preserve_lines:
            para = HTML(para)
            para = para.replace("\n", br)
        paragraphs[i] = HTML.p(para)
    return "\n\n".join(paragraphs)

