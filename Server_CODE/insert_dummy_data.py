# insert_dummy_data.py

from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from faker import Faker
from datetime import datetime, timedelta
import random
import os

# --- 1. SETUP AND CONFIGURATION ---

# IMPORTANT: Ensure these settings match your main.py
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
DATABASE_DIR = os.path.join(BASE_DIR, 'database')
DATABASE_FILE = os.path.join(DATABASE_DIR, 'database.db')
os.makedirs(DATABASE_DIR, exist_ok=True)  # Ensure the directory exists

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{DATABASE_FILE}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

database = SQLAlchemy(app)
fake = Faker()


# --- 2. MODEL DEFINITIONS (Copied from your structure) ---

class Region(database.Model):
    __tablename__ = 'Regions'
    RegionID = database.Column(database.Integer, primary_key=True)
    RegionName = database.Column(database.String(100), unique=True, nullable=False)
    RegionManager = database.Column(database.String(100))
    tickets = database.relationship('Ticket', backref='region', lazy=True)


class Ticket(database.Model):
    __tablename__ = 'Tickets'
    TicketID = database.Column(database.Integer, primary_key=True)
    Title = database.Column(database.String(255), nullable=False)
    Description = database.Column(database.Text)
    Status = database.Column(database.String(50), default='New')
    Priority = database.Column(database.String(50), default='Medium')
    ReportedAt = database.Column(database.String(50))
    EstRepairTime = database.Column(database.Float)
    ImageURL = database.Column(database.String(500))
    # NEW COLUMN ADDED
    Category = database.Column(database.String(2000))

    RegionID = database.Column(database.Integer, database.ForeignKey('Regions.RegionID'), nullable=False)

    assignment = database.relationship('TicketAssignments', backref='ticket', uselist=False, lazy=True)
    details = database.relationship('TicketDetails', backref='ticket', uselist=False, lazy=True)


class TicketAssignments(database.Model):
    __tablename__ = 'TicketAssignments'
    AssignmentID = database.Column(database.Integer, primary_key=True)
    TicketID = database.Column(database.Integer, database.ForeignKey('Tickets.TicketID'), unique=True, nullable=False)
    ReportedBy = database.Column(database.String(100))
    AssignedTo = database.Column(database.String(100))
    AssignmentNotes = database.Column(database.Text)


class TicketDetails(database.Model):
    __tablename__ = 'TicketDetails'
    DetailID = database.Column(database.Integer, primary_key=True)
    TicketID = database.Column(database.Integer, database.ForeignKey('Tickets.TicketID'), unique=True, nullable=False)
    Address = database.Column(database.Text)
    AmountOfReports = database.Column(database.Integer, default=1)
    InternalNotes = database.Column(database.Text)


# --- 3. DUMMY DATA GENERATION LOGIC ---

REGIONS_DATA = [
    {"name": "North Sector", "manager": "Alice Johnson"},
    {"name": "South Bay", "manager": "Bob Williams"},
    {"name": "East Coast Ops", "manager": "Charlie Brown"},
    {"name": "West Valley", "manager": "Diana Prince"},
]

TICKET_TITLES = [
    "Broken Server Rack Power Unit",
    "Facility Temperature Sensor Malfunction",
    "Network Switch Overheating",
    "HVAC Unit Making Loud Noise",
    "Security Camera Offline (East Wing)",
    "Emergency Generator Failure",
    "Water Leak in Server Room",
    "Access Card Reader Not Working",
    "Fire Alarm System False Alert",
    "Routine Maintenance Request (Monthly)",
]

TICKET_STATUSES = ['New', 'In Progress', 'Awaiting Parts', 'Closed']
TICKET_PRIORITIES = ['Low', 'Medium', 'High', 'Critical']
ASSIGNED_STAFF = ['Unassigned', 'Tech-101', 'Engineer-202', 'Support-303']

# NEW LIST OF CATEGORIES FOR DUMMY DATA
TICKET_CATEGORIES = [
    'IT Hardware',
    'Network/Connectivity',
    'HVAC',
    'Facility Repair',
    'Electrical',
    'Security',
    'Software Bug'
]

def create_dummy_data(num_tickets=50):
    """Generates and inserts dummy data into all tables."""
    print("--- Starting Database Population ---")

    # Clear existing data and create tables
    database.drop_all()
    database.create_all()
    print("Tables dropped and recreated successfully.")

    # 1. Insert Regions first
    regions_map = {}
    for data in REGIONS_DATA:
        region = Region(RegionName=data['name'], RegionManager=data['manager'])
        database.session.add(region)
        regions_map[data['name']] = region  # Store object for later access

    database.session.commit()
    print(f"Inserted {len(REGIONS_DATA)} regions.")

    # Get all RegionIDs for random assignment
    region_ids = [r.RegionID for r in Region.query.all()]

    # 2. Insert Tickets and their associated detail/assignment records
    for i in range(1, num_tickets + 1):
        # Base Ticket Data
        report_date = datetime.now() - timedelta(days=random.randint(0, 60), hours=random.randint(0, 23))

        ticket = Ticket(
            Title=random.choice(TICKET_TITLES),
            Description=fake.text(max_nb_chars=300),
            Status=random.choice(TICKET_STATUSES),
            Priority=random.choice(TICKET_PRIORITIES),
            ReportedAt=report_date.isoformat(),
            EstRepairTime=random.uniform(0.5, 20.0),
            ImageURL=None,  # Keeping it simple for dummy data
            RegionID=random.choice(region_ids),
            # ASSIGN CATEGORY DUMMY DATA
            Category=random.choice(TICKET_CATEGORIES)
        )
        database.session.add(ticket)
        database.session.flush()  # Flushes to get the TicketID

        # TicketAssignments Data
        assignment = TicketAssignments(
            TicketID=ticket.TicketID,
            ReportedBy=fake.name(),
            AssignedTo=random.choice(ASSIGNED_STAFF),
            AssignmentNotes=fake.sentence() if random.random() < 0.7 else None
        )
        database.session.add(assignment)

        # TicketDetails Data
        details = TicketDetails(
            TicketID=ticket.TicketID,
            Address=fake.address(),
            AmountOfReports=random.randint(1, 10),
            InternalNotes=fake.paragraph() if random.random() < 0.5 else None
        )
        database.session.add(details)

    database.session.commit()
    print(f"Successfully inserted {num_tickets} tickets with details and assignments.")
    print("--- Database Population Complete ---")


# --- 4. EXECUTION ---
if __name__ == '__main__':
    with app.app_context():
        create_dummy_data(num_tickets=50)