"""
  This is the code for the merger processing.
"""

from flask import Blueprint, request, abort, g
from nwrsc.model import *
from nwrsc.lib.encoding import json_encode

Merge = Blueprint("Merge", __name__)

@Merge.route("/hashes")
def hash():
    return json_encode(loadHashes())

@Merge.route("/pk/<table>")
def pk(table):
    return json_encode(loadPk(table))

@Merge.route("/action", methods=['POST'])
def action():
    json = request.get_json()
    print(json)
    return json_encode(json)
