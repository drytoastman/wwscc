import errno
import re
import operator
import shutil
import sys
import os
import tempfile
import subprocess
import md5
import mimetypes

try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

import pkg_resources
from setuptools import Command
from distutils import log

from tw.core.resources import registry, merge_resources, _JavascriptFileIter

from tw.core.util import OrderedSet

class archive_tw_resources(Command):
    """
    Setuptools command to copy and optionally compress all static resources
    from a series of distributions and their dependencies into a directory
    where they can be served by a fast web server.

    To enable compression of CSS and JS files you will need to have installed a
    Java Runtime Environment and YUICompressor
    (http://www.julienlecomte.net/yuicompressor)

    In order for resources from widget eggs to be properly collected these
    need to have a 'toscawidgets.widgets' 'widgets' entry-point which points
    to a module which, when imported, instantiates all needed JS and CSS Links.

    The result is laid out in the output directory in such a way that when
    a a web server such as Apache or Nginx is configured to map URLS that
    begin with /toscawidgets to that directory static files will be served
    from there bypassing python completely.


    To integrate this command into your build process you can add these lines
    to ``setup.cfg``::

        [archive_tw_resources]
        output = /home/someuser/public_html/toscawidgets/
        compresslevel = 2
        distributions = MyProject
        yuicompressor = /home/someuser/bin/yuicompressor.jar
        onepass = true

        [aliases]
        deploy = archive_tw_resources --force install

    This way you can run::

        $ python setup.py deploy

    To install a new version of your app and copy/compress resources.
    """
    description = "Copies ToscaWidgets static resources into a directory where"\
                  " a fast web-server can serve them."
    user_options = [
        ("output=", "o",
         "Output directory. If it doesn't exist it will be created."),
        ("force", "f", "If output dir exists, it will be ovewritten"),
        ("onepass", None, "If given, yuicompressor will only be called once "\
                          "for each kind of file with a all files "\
                          "together and then separated back into smaller "\
                          "files"),
        ("compresslevel=", "c",
         "Compression level: 0) for no compression (default). "\
                            "1) for js-minification. "\
                            "2) for js & css compression"),
        ("yuicompressor=", None, "Name of the yuicompressor jar."),
        ("distributions=", "d",
         "List of widget dists. to include resources from "
         "(dependencies will be handled recursively). Note that "
         "these distributions need to define a 'toscawidgets.widgets' "
         "'widgets' entrypoint pointing to a a module where "
         "resources are located."),
        ("requireonce", "r",
         "Surround the gathered Javascript with a require_once-guard."
         )
        ]


    IGNORED_NAMES = [".svn",]
    """
    A list of names to ignore, used to prevent collecting
    subversion control data.
    """

    NO_VARIANT = object()

    def initialize_options(self):
        self.output = ''
        self.force = False
        self.onepass = False
        self.compresslevel = 0
        self.distributions = []
        self.yuicompressor = 'yuicompressor.jar'
        self.requireonce = False


    def finalize_options(self):
        self.ensure_string("output")
        self.ensure_string("yuicompressor")
        self.ensure_string_list("distributions")
        self.compresslevel = int(self.compresslevel)
        self.yuicompressor = os.path.abspath(self.yuicompressor)

    def run(self):
        if not self.output:
            print >> sys.stderr, "Need to specify an output directory"
            return
        if not self.distributions:
            print >> sys.stderr, "Need to specify at least one distribution"
            return
        if os.path.exists(self.output) and not self.force:
            print >> sys.stderr, ("Destination dir %s exists. " % self.output)+\
                                  "Use -f to ovewrite"
            return
        if self.compresslevel > 0 and not os.path.exists(self.yuicompressor):
            print >> sys.stderr, "Could not find YUICompressor at " + \
                                 self.yuicompressor
            return

        self.tempdir = tempdir = tempfile.mktemp()
        self.execute(os.makedirs, (tempdir,), "Creating temp dir %s" % tempdir)

        if self.compresslevel > 0:
            if self.onepass:
                self.writer = OnePassCompressingWriter(self, tempdir)
            else:
                self.writer = CompressingWriter(self, tempdir)
        else:
            self.writer = FileWriter(self, tempdir)

        self.execute(self._copy_resources, tuple(), "Extracting resources")
        self.writer.finalize()
        if os.path.exists(self.output):
            self.execute(shutil.rmtree, (self.output,),
                         "Deleting old output dir %s" % self.output)
        self.execute(os.makedirs, (self.output,), "Creating output dir")
        final_dest = os.path.join(self.output, registry.prefix.strip('/'))
        self.execute(shutil.move, (tempdir, final_dest),
                     "Moving build to %s" % final_dest)

    def _load_widgets(self, distribution):
        try:
            requires = [r.project_name for r in
                        pkg_resources.get_distribution(distribution).requires()]
            map(self._load_widgets, requires)
            mod = pkg_resources.load_entry_point(distribution,
                                                 'toscawidgets.widgets',
                                                 'widgets')
            self.announce("Loaded %s" % mod.__name__)
        except ImportError, e:
            self.announce("%s has no widgets entrypoint" % distribution)

    def _copy_resources(self):
        map(self._load_widgets, self.distributions)
        for webdir, dirname in registry:
            parts = filter(None, webdir.split('/'))
            modname = parts[0]
            fname = '/'.join(parts[1:])
            self.execute(self._copy_resource_tree, (modname, fname),
                         "Copying %s recursively into %s" %
                         (dirname, self.writer.base))


    def _copy_resource_tree(self, modname, fname):
        try:
            for name in pkg_resources.resource_listdir(modname, fname):
                if name in self.IGNORED_NAMES:
                    continue
                name = '/'.join((fname, name))
                rel_name = '/'.join((modname, name))
                if pkg_resources.resource_isdir(modname, name):
                    self.execute(self._copy_resource_tree, (modname, name),
                                 "Recursing into " + rel_name)
                else:
                    full_name = pkg_resources.resource_filename(modname, name)
                    ct, _  = mimetypes.guess_type(full_name)
                    require_once = None
                    if self.requireonce and ct == "application/javascript":
                        require_once = _JavascriptFileIter._marker_name(modname, name)
                    stream = pkg_resources.resource_stream(modname, name)
                    filename = '/'.join((modname, name))
                    self.execute(self.writer.write_file, (stream, filename),
                                 "Processing " + filename)
                    if require_once is not None:
                        filename = os.path.join(self.tempdir, filename)
                        inf = open(filename)
                        outname = tempfile.mktemp()
                        outf = open(outname, "w")
                        outf.write(_JavascriptFileIter.START_TEMPLATE % require_once)
                        outf.write(inf.read())
                        outf.write(_JavascriptFileIter.END_TEMPLATE % require_once)
                        outf.close()
                        os.rename(outname, filename)
                    stream.close()
        except OSError, e:
            if e.errno == errno.ENOENT:
                self.warn("Could not copy %s" % repr((modname, fname, e)))


class ResourceAggregator(object):
    """
    The aggregation of files is delegated to instances of this class.

    That allows for pre/postprocessing.
    """

    def __new__(cls, command, filename):
        kind = command.kind
        if kind == "js":
            return object.__new__(JSResourceAggregator, filename)
        elif kind == "css":
            return object.__new__(CSSResourceAggregator, filename)
        raise Exception("Unknown resource kind, must be 'js' or 'css'.")


    def __init__(self, command, filename, immediate_write=True, add_separator_comments=True):
        self.command = command
        self.out_name = filename
        self.added_files = []
        if immediate_write:
            self.outf = open(self.out_name, "w")
        self.immediate_write = immediate_write
        self.add_separator_comments = add_separator_comments


    def add_file(self, modname, filename):
        inf_name = self.resolve_filename(modname, filename)
        if not os.path.exists(inf_name):
            self.command.announce("WARNING: missing resource: %s.%s" % (modname, filename))
            return

        self.added_files.append((modname, filename))

        if self.immediate_write:
            # we need universal line ending support here
            # to not mix file line endings.
            stream = open(inf_name, "U")
            tempname = tempfile.mktemp()
            self.command.writer.write_file(stream, tempname)
            if self.add_separator_comments:
                self.outf.write("\n// -- %s %s --\n" % (modname, filename))
            self.outf.write(open(tempname).read())


    def __nonzero__(self):
        return bool(self.added_files)


    def flush(self):
        if self.immediate_write:
            self.outf.close()


    def write_mapfile(self, outf):
        for entry in self.added_files:
            outf.write("%s|%s\n" % entry)


    def resolve_filename(self, modname, filename):
        inf_name = pkg_resources.resource_filename(modname, filename)
        return inf_name


    def post_hook(self, out_filename):
        pass


class JSResourceAggregator(ResourceAggregator):
    pass



class CSSResourceAggregator(ResourceAggregator):
    """
    This class allows to splice in the abl.cssprocessor.CSSRewriter
    so that image-references are re-written.
    """

    def __init__(self, command, filename):
        immediate_write = True
        self.use_css_rewriter = False

        if command.rewrite:
            immediate_write = False
            self.use_css_rewriter = True

        super(CSSResourceAggregator, self).__init__(command, filename, immediate_write)
        if self.use_css_rewriter:
            from abl.vpath import URI
            from abl.cssprocessor.rewriter import CSSRewriter
            output = URI(filename)
            self.rewriter = CSSRewriter(output)


    def add_file(self, modname, filename):
        super(CSSResourceAggregator, self).add_file(modname, filename)
        if self.use_css_rewriter:
            from abl.vpath import URI
            css_file = URI(self.resolve_filename(modname, filename))
            self.rewriter.add_css(css_file)


    def flush(self):
        super(CSSResourceAggregator, self).flush()
        if self.use_css_rewriter:
            self.rewriter.rewrite()


    def post_hook(self, out_filename):
        if self.use_css_rewriter:
            from abl.vpath import URI
            # move the images to the destination
            out_dir = URI(out_filename).dirname()
            output = self.rewriter.output
            (output.dirname() / "images").copy(out_dir, "r")


class aggregate_tw_resources(Command):
    """
    Setuptools commmand to aggregate Javascript- or CSS-files to one large
    file, possibly compressed through the use of YUICompressor.

    To enable compression of CSS and JS files you will need to have installed a
    Java Runtime Environment and YUICompressor
    (http://www.julienlecomte.net/yuicompressor)

    In order for resources from widget eggs to be properly collected these
    need to have a 'toscawidgets.widgets' 'widgets' entry-point which points
    to a module which, when imported, instantiates all needed JS and CSS Links.

    The aggregated resources are served via the :class:`tw.mods.base.HostFramework` and
    thus must be placed inside a python package to be served as normal resources.

    An example commandline invocation would look like this::

      python2.5 setup.py aggregate_tw_resources -o myproject/public/javascript/aggregated/ -d MyProject --package=myproject


    """
    description = "Aggregates ToscaWidgets static resources into a single file."

    user_options = [
        ("output=", "o",
         "Output directory. If it doesn't exist it will be created."),
        ("onepass", None, "If given, yuicompressor will only be called once "\
                          "for each kind of file with a all files "\
                          "together and then separated back into smaller "\
                          "files"),
        ("compresslevel=", "c",
         "Compression level: 0) for no compression (default). "\
                            "1) for js-minification. "\
                            "2) for js & css compression"),
        ("yuicompressor=", None, "Name of the yuicompressor jar."),
        ("distributions=", "d",
         "List of widget dists. to include resources from "
         "(dependencies will be handled recursively). Note that "
         "these distributions need to define a 'toscawidgets.widgets' "
         "'widgets' entrypoint pointing to a a module where "
         "resources are located."),
        ("kind=", "k",
         "The kind, either js or css"
         ),
        ("variant=", "p",
         "The registry variant to fetch the data from. If this option is used,"
         "only the selected kind of resources is gathered. The resulting "
         "fileset is processed and concatenated to a large file."
         ),
        ("package=", "m",
         "If gathering a variant, this option can be used to filter the output "
         "for a given package. The name of the package can also contain path-elements."
         "So you can group together e.g. 'foo.bar/public/javascript/subdir'."
         ),
        ("rewrite", "r",
         "Use the CSS-rewriter to rewrite relative url/image-references before producing "
         "the output. To make this actually work, make user you have the AbletonCSSProcessor "
         "installed."
         ),
        ]


    IGNORED_NAMES = [".svn",]
    """
    A list of names to ignore, used to prevent collecting
    subversion control data.
    """

    NO_VARIANT = object()

    def initialize_options(self):
        self.output = ''
        self.force = False
        self.onepass = False
        self.compresslevel = 0
        self.rewrite = False
        self.kind = "js"
        self.distributions = []
        self.variant = registry.DEFAULT_VARIANT
        self.package = None
        self.yuicompressor = 'yuicompressor.jar'

    def finalize_options(self):
        self.ensure_string("output")
        self.ensure_string("yuicompressor")
        self.ensure_string_list("distributions")
        self.compresslevel = int(self.compresslevel)
        self.yuicompressor = os.path.abspath(self.yuicompressor)

    def run(self):
        if not self.output:
            print >> sys.stderr, "Need to specify an output directory"
            return
        if not self.distributions:
            print >> sys.stderr, "Need to specify at least one distribution"
            return
        if not os.path.exists(self.output):
            self.announce("Creating output dir %s" % self.output)
            os.mkdir(self.output)
        if self.compresslevel > 0 and not os.path.exists(self.yuicompressor):
            print >> sys.stderr, "Could not find YUICompressor at " + \
                                 self.yuicompressor
            return

        tempdir = tempfile.mktemp()
        self.execute(os.makedirs, (tempdir,), "Creating temp dir %s" % tempdir)

        if self.compresslevel > 0:
            if self.onepass:
                self.writer = OnePassCompressingWriter(self, tempdir)
            else:
                self.writer = CompressingWriter(self, tempdir)
        else:
            self.writer = FileWriter(self, tempdir)

        def resource_filter(resource):
            return True

        # try & locate a resource-filter for
        # the listed distributions.
        # all the found filters are combined,
        # and for a resource to end up in the
        # aggregation, they all must return True
        for distribution_name in self.distributions:
            ep = pkg_resources.get_entry_info(distribution_name, "toscawidgets.widgets", "resource_aggregation_filter")
            if ep is not None:
                def combine_predicates(a, b):
                    def resource_filter(resource):
                        return a(resource) and b(resource)
                    return resource_filter
                resource_filter = combine_predicates(resource_filter, ep.load())

        self.execute(self._collect_variant_resources, (resource_filter,), "Collection resources for variant %s" % self.variant)


    def _load_widgets(self, distribution):
        try:
            requires = [r.project_name for r in
                        pkg_resources.get_distribution(distribution).requires()]
            map(self._load_widgets, requires)
            mod = pkg_resources.load_entry_point(distribution,
                                                 'toscawidgets.widgets',
                                                 'widgets')
            self.announce("Loaded %s" % mod.__name__)
        except ImportError, e:
            self.announce("%s has no widgets entrypoint" % distribution)


    def _collect_variant_resources(self, resource_filter):
        # yes, this is sick, but it happens
        # that some imports can have that as
        # a side-effect
        cwd = os.getcwd()
        map(self._load_widgets, self.distributions)
        os.chdir(cwd)
        package = self.package
        filebase = None
        if package is not None and "/" in package:
            package, filebase = package.split("/", 1)

        def in_package(widget, filename):
            if package is None:
                return True
            if widget.modname.startswith(package):
                if filebase is None:
                    return True
                return filename.startswith(filebase)
            return False

        variant_filename = tempfile.mktemp()
        resource_aggregator = ResourceAggregator(self, variant_filename)

        def widget_name(widget):
            filename = widget.filename
            if isinstance(filename, dict):
                filename = filename[registry.DEFAULT_VARIANT]
            return widget.modname, filename

        widgets = sorted(registry._widgets, key=widget_name)

        dependency_ordered_widgets = []
        for widget in widgets:
            for resource in widget.retrieve_resources()["head"]:
                if resource_filter(resource) and resource not in dependency_ordered_widgets:
                    dependency_ordered_widgets.append(resource)

        from tw.api import (AggregatedJSLink, AggregatedCSSLink)

        for widget in dependency_ordered_widgets:
            # don't collect other aggregates.
            if isinstance(widget, (AggregatedJSLink, AggregatedCSSLink)):
                continue
            variant_mapping = widget.filename
            # this can happen for e.g. the ThemedCSSLinks from
            # abl.jquery.ui
            if variant_mapping is None:
                continue
            if isinstance(variant_mapping, basestring):
                variant_mapping = {registry.DEFAULT_VARIANT : variant_mapping}
            if self.variant in variant_mapping:
                filename = variant_mapping[self.variant]
            else:
                filename = variant_mapping[registry.DEFAULT_VARIANT]

            _, ext = os.path.splitext(filename)
            if ext.lower()[1:] != self.kind:
                continue
            if not in_package(widget, filename):
                continue

            modname = widget.modname
            self.announce("Processing %s %s" % (modname, filename))
            resource_aggregator.add_file(modname, filename)

        if resource_aggregator:
            # make the aggregator do it's acutal work.
            # this must result in an existing variant_filename
            resource_aggregator.flush()
            inf = open(variant_filename)
            hash = md5.new()
            while True:
                block = inf.read(4096)
                if not block:
                    break
                hash.update(block)
            hex = hash.hexdigest()[1:-1]
            dest_name = "%s-%s.%s" % (hex, self.variant, self.kind)
            dest_name = os.path.join(self.output, dest_name)
            os.rename(variant_filename, dest_name)
            self.announce("Created concatenatenated file: %s" % dest_name, log.INFO)
            dest_name = "%s-%s.%s.map" % (hex, self.variant, self.kind)
            dest_name = os.path.join(self.output, dest_name)
            outf = open(dest_name, "w")
            resource_aggregator.write_mapfile(outf)
            outf.close()
            resource_aggregator.post_hook(dest_name)
            self.announce("Created mapping file: %s" % dest_name, log.INFO)


class FileWriter(object):
    def __init__(self, cmd, base):
        self.base = base
        self.cmd = cmd

    def finalize(self):
        pass

    def write_file(self, stream, path):
        final = os.path.join(self.base, path)
        if not os.path.exists(os.path.dirname(final)):
            os.makedirs(os.path.dirname(final))
        dest = open(final, 'wb')
        self.announce("Writing %s" % path)
        shutil.copyfileobj(stream, dest)
        dest.close()


    # Delegate methods to Command
    for name in "warn announce error execute".split():
        exec """\
def %(name)s(self, *args, **kw):
    return self.cmd.%(name)s(*args, **kw)
""" % locals()

class CompressingWriter(FileWriter):

    def __init__(self, *args, **kw):
        super(CompressingWriter, self).__init__(*args, **kw)
        self.counters = 0, 0

    def finalize(self):
        try:
            avg =  reduce(operator.truediv, self.counters) * 100
            msg = "Total JS&CSS compressed size is %.2f%% of original" % avg
            self.announce(msg)
        except ZeroDivisionError:
            # No files were compressed
            pass

    def compress(self, stream, path):
        typ = path.split('.')[-1]
        if typ not in ('css', 'js'):
            return stream
        args = ['java', '-jar', self.cmd.yuicompressor, '--type', typ]
        if self.cmd.compresslevel < 2:
            args.append('--nomunge')
        args.append('--charset=utf8')
        p = subprocess.Popen(args, stdout=subprocess.PIPE,
                             stdin=subprocess.PIPE,
                             stderr=subprocess.PIPE)
        self.announce("Compressing %s" % path)
        buffer = StringIO()
        shutil.copyfileobj(stream, buffer)
        data = buffer.getvalue()
        if not data:
            return buffer
        stdout, stderr = p.communicate(data)
        if p.returncode != 0:
            self.warn("Failed to compress %s: %d" % (path, p.returncode))
            self.warn("File will be copied untouched")
            sys.stderr.write(stderr)
            sys.stderr.write(stdout)
            stream.seek(0)
        else:
            count = len(stdout), len(data)
            ratio = reduce(operator.truediv, count)
            self.counters = map(sum, zip(self.counters, count))
            msg = "Compressed %s (New size: %.2f%%)" % (path, ratio*100)
            self.announce(msg)
            stream = StringIO(stdout)
        return stream

    def write_file(self, stream, path):
        stream = self.compress(stream, path)
        return super(CompressingWriter, self).write_file(stream, path)

class OnePassCompressingWriter(CompressingWriter):
    def __init__(self, *args, **kw):
        super(OnePassCompressingWriter, self).__init__(*args, **kw)
        #XXX This comment trick only works with JS as of YUICompressor 2.3.5
        self._caches = {'js': StringIO()}
        self._marker = "/*! MARKER #### %(path)s #### MARKER */"
        regexp = r"^\/\* MARKER #### (?P<path>.*?) #### MARKER \*\/$"
        self._re = re.compile(regexp)

    def _demultiplex(self, stream):
        cur_file = None
        buffer = StringIO()
        stream.seek(0)
        for line in stream:
            m = self._re.match(line)
            if m:
                if cur_file:
                    buffer.seek(0)
                    FileWriter.write_file(self, buffer, cur_file)
                    buffer.truncate(0)
                cur_file = m.group('path')
            else:
                buffer.write(line)

    def finalize(self):
        self.announce("Compressing all defered files")
        for typ, cache in self._caches.iteritems():
            cache.seek(0)
            # self.compress only wants to know the file extension to see
            # what kind of file it is, we pass a dummy one
            compressed = self.compress(cache, '__defered__.'+typ)
            self._demultiplex(compressed)
        super(OnePassCompressingWriter, self).finalize()

    def write_file(self, stream, path):
        typ = path.split('.')[-1]
        cache = self._caches.get(typ)
        if not cache:
            self.announce("Will not consider %s for onepass" % path)
            return CompressingWriter.write_file(self, stream, path, require_once)
        print >> cache, self._marker % locals()
        self.announce("Defering %s for compression in one pass" % path)
        shutil.copyfileobj(stream, cache)
