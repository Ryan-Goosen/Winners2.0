from .database_initialize import db as database

class Region(database.Model):
    __tablename__ = 'Regions'
    RegionID = database.Column(database.Integer, primary_key=True)
    RegionName = database.Column(database.String(100), unique=True, nullable=False)
    RegionManager = database.Column(database.String(100))

    # Relationship to access tickets easily
    tickets = database.relationship('Ticket', backref='region', lazy=True)


class Ticket(database.Model):
    __tablename__ = 'Tickets'
    TicketID = database.Column(database.Integer, primary_key=True)
    Title = database.Column(database.String(255), nullable=False)
    Description = database.Column(database.Text)
    Status = database.Column(database.String(50), default='New')
    Priority = database.Column(database.String(50), default='Medium')
    ReportedAt = database.Column(database.String(50))  # Store as ISO string
    EstRepairTime = database.Column(database.Float)
    Category = database.Column(database.String(2000))
    ImageURL = database.Column(database.String(500))  # Path to the stored file

    # Foreign Key
    RegionID = database.Column(database.Integer, database.ForeignKey('Regions.RegionID'), nullable=False)

    # Relationships to child tables (for easy joining)
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