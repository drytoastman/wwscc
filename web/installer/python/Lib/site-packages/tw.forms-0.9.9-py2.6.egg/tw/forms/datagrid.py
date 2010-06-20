"""Generic widget to present and manipulate data in a grid (tabular) form.

Adapted from turbogears.widgets.datagrid

"""

from tw.api import Widget, CSSLink

NoDefault = object()

__all__ = ["DataGrid"]


class attrwrapper(object):
    """Helper class that returns an object's attribute when called.

    This allows to access 'dynamic' attributes (properties) as well as
    simple static ones, and also allows nested access.

    """

    def __init__(self, name):
        assert isinstance(name, basestring)
        self.name = name

    def __call__(self, obj):
        for name in self.name.split('.'):
            obj = getattr(obj, name)
        return obj


class Column(object):
    """Simple struct that describes a single DataGrid column.

    Column has:
      - a name, which allows to uniquely identify a column in a DataGrid
      - a getter, which is used to extract the field's value
      - a title, which is displayed in the table's header
      - options, which is a way to carry arbitrary user-defined data

    """

    def __init__(self, name, getter=None, title=None, options=None):
        if not name:
            raise ValueError, 'name is required'
        if getter:
            if callable(getter):
                self.getter = getter
            else: # assume it's an attribute name
                self.getter = attrwrapper(getter)
        else:
            self.getter = attrwrapper(name)
        self.name = name
        self.title = title is None and name.capitalize() or title
        self.options = options or {}

    def get_option(self, name, default=NoDefault):
        if name in self.options:
            return self.options[name]
        if default is NoDefault: # no such key and no default is given
            raise KeyError(name)
        return default

    def get_field(self, row, displays_on=None):
        if isinstance(self.getter, Widget):
            return self.getter(row, displays_on=displays_on)
        return self.getter(row)

    def __str__(self):
        return "<Column %s>" % self.name


class DataGrid(Widget):
    """Generic widget to present and manipulate data in a grid (tabular) form.

    The columns to build the grid from are specified with fields constructor
    argument which is a list. An element can be a Column, an accessor
    (attribute name or function), a tuple (title, accessor) or a tuple
    (title, accessor, options).

    You can specify columns' data statically, via fields constructor parameter,
    or dynamically, via 'fields' key.

    """
    css_class = "grid"
    css=[CSSLink(modname='tw.forms', filename='static/grid.css')]
    template = "tw.forms.templates.datagrid"
    engine_name = "genshi"
    fields = []
    params = ["fields"]

    def get_column(self, name):
        """Return Column with specified name.

        Raises KeyError if no such column exists.

        """
        for col in self.columns:
            if col.name == name:
                return col
        raise KeyError(name)

    def __getitem__(self, name):
        """Shortcut to get_column."""
        return self.get_column(name)

    @staticmethod
    def get_field_getter(columns):
        """Return a function to access the fields of table by row, col."""
        idx = {} # index columns by name
        for col in columns:
            idx[col.name] = col
        def _get_field(row, col):
            return idx[col].get_field(row)
        return _get_field

    def update_params(self, d):
        super(DataGrid, self).update_params(d)
        if d.get('fields'):
            fields = d.pop('fields')
            columns = self._parse(fields)
        else:
            columns = self.columns[:]
        d['columns'] = columns
        d['get_field'] = self.get_field_getter(columns)
        # this is for backward compatibility
        d['headers'] = [col.title for col in columns]
        d['collist'] = [col.name for col in columns]

    def _parse(self, fields):
        """Parse field specifications into a list of Columns.

        A specification can be a Column,
        an accessor (attribute name or function), a tuple (title, accessor)
        or a tuple (title, accessor, options).

        """
        columns = []
        names = {} # keep track of names to ensure there are no dups
        for n, col in enumerate(fields):
            if not isinstance(col, Column):
                if isinstance(col, str) or callable(col):
                    name_or_f = col
                    title = options = None
                else:
                    title, name_or_f = col[:2]
                    try:
                        options = col[2]
                    except IndexError:
                        options = None
                # construct name using column index
                name = 'column-' + str(n)
                col = Column(name, name_or_f, title, options)
            if col.name in names:
                raise ValueError('Duplicate column name: %s' % col.name)
            columns.append(col)
            names[col.name] = 1
        return columns
