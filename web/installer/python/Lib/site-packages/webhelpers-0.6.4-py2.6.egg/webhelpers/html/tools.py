"""HTML helpers that are more than just simple tags.

There are no helpers to prettify HTML or canonicalize whitespace because
BeautifulSoup and HTMLTidy handle this well.
"""

import re
import urllib

from webhelpers.html import HTML, literal, lit_sub, escape
from webhelpers.html.tags import convert_boolean_attrs

__all__ = [
    'button_to', 
    'mail_to',
    'highlight', 
    'strip_links',
    'auto_link', 
    ]


AUTO_LINK_RE = re.compile(r"""
                        (                          # leading text
                          <\w+.*?>|                # leading HTML tag, or
                          [^=!:'"/]|               # leading punctuation, or 
                          ^                        # beginning of line
                        )
                        (
                          (?:https?://)|           # protocol spec, or
                          (?:www\.)                # www.*
                        ) 
                        (
                          [-\w]+                   # subdomain or domain
                          (?:\.[-\w]+)*            # remaining subdomains or domain
                          (?::\d+)?                # port
                          (?:/(?:(?:[~\w\+%-]|(?:[,.;:][^\s$]))+)?)* # path
                          (?:\?[\w\+%&=.;-]+)?     # query string
                          (?:\#[\w\-]*)?           # trailing anchor
                        )
                        ([\.,"'?!;:]|\s|<|$)       # trailing text
                           """, re.X)


def button_to(name, url='', **html_options):
    """Generate a form containing a sole button that submits to
    ``url``. 
    
    Use this method instead of ``link_to`` for actions that do not have
    the safe HTTP GET semantics implied by using a hypertext link.
    
    The parameters are the same as for ``link_to``.  Any 
    ``html_options`` that you pass will be applied to the inner
    ``input`` element. In particular, pass
    
        disabled = True/False
    
    as part of ``html_options`` to control whether the button is
    disabled.  The generated form element is given the class
    'button-to', to which you can attach CSS styles for display
    purposes.
    
    The submit button itself will be displayed as an image if you 
    provide both ``type`` and ``src`` as followed:

         type='image', src='icon_delete.gif'

    The ``src`` path should be the exact URL desired.  A previous version of
    this helper added magical prefixes but this is no longer the case.

    Example 1::
    
        # inside of controller for "feeds"
        >> button_to("Edit", url(action='edit', id=3))
        <form method="POST" action="/feeds/edit/3" class="button-to">
        <div><input value="Edit" type="submit" /></div>
        </form>
    
    Example 2::
    
        >> button_to("Destroy", url(action='destroy', id=3), 
        .. method='DELETE')
        <form method="POST" action="/feeds/destroy/3" 
         class="button-to">
        <div>
            <input type="hidden" name="_method" value="DELETE" />
            <input value="Destroy" type="submit" />
        </div>
        </form>

    Example 3::

        # Button as an image.
        >> button_to("Edit", url(action='edit', id=3), type='image', 
        .. src='icon_delete.gif')
        <form method="POST" action="/feeds/edit/3" class="button-to">
        <div><input alt="Edit" src="/images/icon_delete.gif"
         type="image" value="Edit" /></div>
        </form>
    
    .. note::
        This method generates HTML code that represents a form. Forms
        are "block" content, which means that you should not try to
        insert them into your HTML where only inline content is
        expected. For example, you can legally insert a form inside of
        a ``div`` or ``td`` element or in between ``p`` elements, but
        not in the middle of a run of text, nor can you place a form
        within another form.
        (Bottom line: Always validate your HTML before going public.)
    
    """
    if html_options:
        convert_boolean_attrs(html_options, ['disabled'])
    
    method_tag = ''
    method = html_options.pop('method', '')
    if method.upper() in ['PUT', 'DELETE']:
        method_tag = HTML.input(
            type='hidden', id='_method', name_='_method', value=method)
    
    form_method = (method.upper() == 'GET' and method) or 'POST'
    
    url, name = url, name or url
    
    submit_type = html_options.get('type')
    img_source = html_options.get('src')
    if submit_type == 'image' and img_source:
        html_options["value"] = name
        html_options.setdefault("alt", name)
    else:
        html_options["type"] = "submit"
        html_options["value"] = name
    
    return HTML.form(method=form_method, action=url, class_="button-to",
                     c=[HTML.div(method_tag, HTML.input(**html_options))])


def mail_to(email_address, name=None, cc=None, bcc=None, subject=None, 
    body=None, replace_at=None, replace_dot=None, encode=None, **html_options):
    """Create a link tag for starting an email to the specified 
    ``email_address``.
    
    This ``email_address`` is also used as the name of the link unless
    ``name`` is specified. Additional HTML options, such as class or
    id, can be passed in the ``html_options`` hash.
    
    You can also make it difficult for spiders to harvest email address
    by obfuscating them.
    
    Examples::
    
        >>> mail_to("me@domain.com", "My email", encode = "javascript")
        literal(u'<script type="text/javascript">\\n//<![CDATA[\\neval(unescape(\\'%64%6f%63%75%6d%65%6e%74%2e%77%72%69%74%65%28%27%3c%61%20%68%72%65%66%3d%22%6d%61%69%6c%74%6f%3a%6d%65%40%64%6f%6d%61%69%6e%2e%63%6f%6d%22%3e%4d%79%20%65%6d%61%69%6c%3c%2f%61%3e%27%29%3b\\'))\\n//]]>\\n</script>')
    
        >>> mail_to("me@domain.com", "My email", encode = "hex")
        literal(u'<a href="&#109;&#97;&#105;&#108;&#116;&#111;&#58;%6d%65@%64%6f%6d%61%69%6e.%63%6f%6d">My email</a>')
    
    You can also specify the cc address, bcc address, subject, and body
    parts of the message header to create a complex e-mail using the 
    corresponding ``cc``, ``bcc``, ``subject``, and ``body`` keyword 
    arguments. Each of these options are URI escaped and then appended
    to the ``email_address`` before being output. **Be aware that 
    javascript keywords will not be escaped and may break this feature 
    when encoding with javascript.**
    
    Examples::
    
        >>> mail_to("me@domain.com", "My email", cc="ccaddress@domain.com", bcc="bccaddress@domain.com", subject="This is an example email", body= "This is the body of the message.")
        literal(u'<a href="mailto:me@domain.com?cc=ccaddress%40domain.com&amp;bcc=bccaddress%40domain.com&amp;subject=This%20is%20an%20example%20email&amp;body=This%20is%20the%20body%20of%20the%20message.">My email</a>')
        
    """
    extras = []
    for item in ('cc', cc), ('bcc', bcc), ('subject', subject), ('body', body):
        option = item[1]
        if option:
            if not isinstance(option, literal):
                item = (item[0], escape(option))
            extras.append(item)
    options_query = urllib.urlencode(extras).replace("+", "%20")
    protocol = 'mailto:'

    email_address_obfuscated = email_address
    if replace_at:
        email_address_obfuscated = email_address_obfuscated.replace('@', 
            replace_at)
    if replace_dot:
        email_address_obfuscated = email_address_obfuscated.replace('.', 
            replace_dot)

    if encode == 'hex':
        email_address_obfuscated = HTML.literal(''.join(
            ['&#%d;' % ord(x) for x in email_address_obfuscated]))
        protocol = HTML.literal(''.join(['&#%d;' % ord(x) for x in protocol]))

        word_re = re.compile('\w')
        encoded_parts = []
        for x in email_address:
            if word_re.match(x):
                encoded_parts.append('%%%x' % ord(x))
            else:
                encoded_parts.append(x)
        email_address = HTML.literal(''.join(encoded_parts))

    url = HTML.literal(protocol + email_address)
    if options_query:
        url += HTML.literal('?') + options_query
    html_options['href'] = url

    tag = HTML.a(name or email_address_obfuscated, **html_options)

    if encode == 'javascript':
        tmp = "document.write('%s');" % tag
        string = ''.join(['%%%x' % ord(x) for x in tmp])
        return HTML.script(
            HTML.literal("\n//<![CDATA[\neval(unescape('%s'))\n//]]>\n" % string),
                         type="text/javascript")
    else:
        return tag


def highlight(text, phrase, 
              highlighter='<strong class="highlight">\\1</strong>'):
    """Highlight the ``phrase`` where it is found in the ``text``.
    
    The highlighted phrase will be surrounded by the highlighter, 
    by default::
    
        <strong class="highlight">I'm a highlight phrase</strong>
    
    ``highlighter``
        Defines the highlighting phrase. This argument should be a 
        single-quoted string with ``\\1`` where the phrase is supposed 
        to be inserted.
        
    Note: The ``phrase`` is sanitized to include only letters, digits, 
    and spaces before use.

    Example::

        >>> highlight('You searched for: Pylons', 'Pylons')
        'You searched for: <strong class="highlight">Pylons</strong>'
        
    """
    if not phrase or not text:
        return text
    highlight_re = re.compile('(%s)' % re.escape(phrase), re.I)
    if hasattr(text, '__html__'):
        return literal(highlight_re.sub(highlighter, text))
    else:
        return highlight_re.sub(highlighter, text)


def auto_link(text, link="all", **href_options):
    """
    Turn all urls and email addresses into clickable links.
    
    ``link``
        Used to determine what to link. Options are "all", 
        "email_addresses", or "urls"
    
    Example::
    
        >>> auto_link("Go to http://www.planetpython.com and say hello to guido@python.org")
        literal(u'Go to <a href="http://www.planetpython.com">http://www.planetpython.com</a> and say hello to <a href="mailto:guido@python.org">guido@python.org</a>')
        
    """
    if not text:
        return u""
    if link == "all":
        return _auto_link_urls(_auto_link_email_addresses(text), **href_options)
    elif link == "email_addresses":
        return _auto_link_email_addresses(text)
    else:
        return _auto_link_urls(text, **href_options)

def _auto_link_urls(text, **href_options):
    def handle_match(matchobj):
        all = matchobj.group()
        before, prefix, link, after = matchobj.group(1, 2, 3, 4)
        if re.match(r'<a\s', before, re.I):
            return all
        text = literal(prefix + link)
        if prefix == "www.":
            prefix = "http://www."
        a_options = dict(href_options)
        a_options['href'] = literal(prefix + link)
        return literal(before) + HTML.a(text, **a_options) + literal(after)
    return literal(re.sub(AUTO_LINK_RE, handle_match, text))

def _auto_link_email_addresses(text):
    return lit_sub(r'([\w\.!#\$%\-+.]+@[A-Za-z0-9\-]+(\.[A-Za-z0-9\-]+)+)',
                   r'<a href="mailto:\1">\1</a>', text)

def strip_links(text):
    """
    Strip link tags from ``text`` leaving just the link label.
    
    Example::
    
        >>> strip_links('<a href="something">else</a>')
        'else'
        
    """
    if isinstance(text, literal):
        lit = literal
    else:
        lit = lambda x: x
    strip_re = re.compile(r'<a\b.*?>(.*?)<\/a>', re.I | re.M)
    return lit(strip_re.sub(r'\1', text))
