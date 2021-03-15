
"""This module holds tile data."""

from marshmallow import fields, Schema
import datetime
from . import db


class Tile(db.Model):  # pylint: disable=too-many-instance-attributes
    """This class manages all of the tile data.

    """


    __versioned__ = {}
    __tablename__ = 'Tile'

    title = db.Column('Title', db.String(50), nullable=False) # index=True)
    text = db.Column('Text', db.String(200), nullable=True)
    tiletype = db.Column('TileType', db.String(50), nullable=False)
    tilelink = db.Column('TileURL', db.String(200), nullable=True)
    tabid = db.Column('TabId', db.Integer, nullable=True)
    tileroles = db.Column('TileRoles', db.Text, nullable=True)
    tileorder = db.Column('TileOrder', db.Integer, nullable=True)

    # parent keys

    # relationships
    tile = db.relationship("Tile", uselist=False, back_populates="tile")

    def save(self):
        """Save the object to the database immediately. Only used for unit testing."""
        db.session.add(self)
        db.session.commit()

    @classmethod
    def find_by_id(cls, id: int):  # -> tile:
        """Return the tile matching the id."""
        tile = None
        if id:
            tile = cls.query.get(id)
        return tile


    @property
    def json(self):
        """Return a dict of this object, with keys in JSON format."""

        tile = {
            'title': self.title,
            'text': self.text,
            'tiletype': self.tiletype,
            'tileurl': self.tileurl,
            'tabid': self.tabid,
            'tileroles': self.tileroles,
            'tileorder': self.tileorder
        }

        return tile

    @staticmethod
    def create_from_dict(new_info: dict):
        """Create an tile object from dict/json."""
        tile = Tile()

        tile.title = new_info.get('title')
        tile.text = new_info.get('text')
        tile.tiletype = new_info.get('tiletype')
        tile.tileurl = new_info.get('tileurl')
        tile.tabid = new_info.get('tabid')
        tile.tileroles = new_info.get('tileroles')
        tile.tileorder = new_info.get('tileorder')

        return tile

    @staticmethod
    def create_from_json(json_data):
        """Create an tile object from a json tile schema object: map json to db."""
        tile = Tile()

        tile.title = json_data['title']
        tile.text = json_data['text']
        tile.tiletype = json_data['tiletype']
        tile.tileurl = json_data['tileurl']
        tile.tabid = json_data['tabid']
        tile.tileroles = json_data['tileroles']
        tile.tileorder = json_data['tileorder']

        return tile
