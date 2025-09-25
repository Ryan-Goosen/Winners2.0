
class Region(db.Model):
    __tablename__ = 'Regions'
    RegionID = db.Column(db.Integer, primary_key=True, autoincrement=True)
    RegionName = db.Column(db.String(100), unique=True, nullable=False)

class Ticket(db.Model):
    __tablename__ = 'Tickets'
    TicketID = db.Column(db.Integer, primary_key=True, autoincrement=True)
    Title = db.Column(db.String(255), nullable=False)
    # ... other columns
    RegionID = db.Column(db.Integer, db.ForeignKey('Regions.RegionID'), nullable=False)

class TicketDetails(db.Model):
    __tablename__ = 'TicketDetails'
    DetailID = db.Column(db.Integer, primary_key=True)
    TicketID = db.Column(db.Integer, db.ForeignKey('Tickets.TicketID'), unique=True, nullable=False)
    Address = db.Column(db.Text)
    # ... other columns