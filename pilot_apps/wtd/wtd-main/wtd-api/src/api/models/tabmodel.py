
"""This module holds tab data."""

from marshmallow import fields, Schema
import datetime
from . import db
from .tilemodel import BlogpostSchema


class Tab(db.Model):  # pylint: disable=too-many-instance-attributes
    """This class manages all of the tab data.

    """


    __versioned__ = {}
    __tablename__ = 'Tab'

    title = db.Column('Tab', db.String(50), nullable=False) # index=True)
    tabroles = db.Column('TabRoles', db.Text, nullable=True)
    taborder = db.Column('TabOrder', db.Integer, nullable=True)

    # parent keys

    # relationships
    tab = db.relationship("Tab", uselist=False, back_populates="Tab")

    def save(self):
        """Save the object to the database immediately. Only used for unit testing."""
        db.session.add(self)
        db.session.commit()

    @classmethod
    def find_by_id(cls, id: int):  # -> tab:
        """Return the tab matching the id."""
        tab = None
        if id:
            tab = cls.query.get(id)
        return tab


    @property
    def json(self):
        """Return a dict of this object, with keys in JSON format."""

        tab = {
            'title': self.title,
            'tabroles': self.tabroles,
            'taborder': self.taborder
        }

        return tab

    @staticmethod
    def create_from_dict(new_info: dict):
        """Create an tab object from dict/json."""
        tab = Tab()

        tab.title = new_info.get('title')
        tab.tabroles = new_info.get('tabroles')
        tab.taborder = new_info.get('taborder')

        return tab

    @staticmethod
    def create_from_json(json_data):
        """Create an tab object from a json tab schema object: map json to db."""
        tab = Tab()

        tab.title = json_data['tab']
        tab.tabroles = json_data['tabroles']
        tab.taborder = json_data['taborder']

        return tab


    def update(self, data):
        for key, item in data.items():
            setattr(self, key, item)
            db.session.commit()

    def delete(self):
        db.session.delete(self)
        db.session.commit()

    @staticmethod
    def get_all_users():
        return Tab.query.all()

    @staticmethod
    def get_one_tab(id):
        return Tab.query.get(id)
