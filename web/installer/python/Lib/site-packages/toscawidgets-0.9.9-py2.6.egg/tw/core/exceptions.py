class WidgetException(RuntimeError):
    msg = "Widget error"
    def __init__(self, msg=None):
        self.msg = msg or self.msg

    def __str__(self):
        return self.msg

class WidgetUnlocked(WidgetException, AttributeError):
    msg = ("The widget is not locked. This method needs to wait until the "
           "widget is fully locked in order to function properly")

class WidgetLocked(WidgetException, AttributeError):
    msg = ("The widget is locked. It's unthread-safe to alter it's attributes "
           "after initialization.")

class WidgetInitialized(WidgetException, AttributeError):
    msg = ("The widget is already initialized, try doing it at the "
           "constructor.")

class WidgetUninitialized(WidgetException, AttributeError):
    msg = ("The widget is uninitialized.")
