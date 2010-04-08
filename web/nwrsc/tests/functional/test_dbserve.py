from nwrsc.tests import *

class TestDbserveController(TestController):

    def test_index(self):
        response = self.app.get(url(controller='dbserve', action='index'))
        # Test response...
