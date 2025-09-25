from .models import Ticket, Region, TicketAssignments, TicketDetails
from datetime import datetime
import os


def create_new_ticket(database, ticket_data, image_url):
    """
        Handles the multi-table insertion logic for a new ticket.

        Args:
            database: The SQLAlchemy object (to access models and session).
            data (dict): The parsed JSON data from the client.
            image_url (str, optional): The URL/path to the saved image.
            file_path (str, optional): The physical path to the saved file (for cleanup).

        Returns:
            int: The primary key (TicketID) of the newly created ticket.

        Raises:
            ValueError: If required data is missing.
        """

    # Basic validation

    try:
        # --- 1. Handle Region (Parent of Ticket) ---
        region_name = ticket_data['region_name']
        region = database.session.execute(database.select(Region).filter_by(RegionName=region_name)).scalar_one_or_none()

        if not region:
            region = Region(RegionName=region_name, RegionManager=data.get('region_manager', 'N/A'))
            database.session.add(region)

        # --- 2. Create the main Ticket record ---
        new_ticket = Ticket(
            Title=ticket_data['title'],
            Description=ticket_data.get('description', ''),
            Status=ticket_data.get('status', 'New'),
            Priority=ticket_data.get('priority', 'Medium'),
            ReportedAt=datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            EstRepairTime=ticket_data.get('est_repair_time'),
            ImageURL=image_url,
            region=region  # SQLAlchemy handles the RegionID FK
        )
        database.session.add(new_ticket)

        # Flush to get the TicketID without committing the full transaction
        database.session.flush()
        new_ticket_id = new_ticket.TicketID

        # --- 3. Create Child Records using new_ticket_id (FK) ---

        # A. TicketAssignments
        assignment = TicketAssignments(
            TicketID=new_ticket_id,
            ReportedBy=ticket_data.get('reported_by'),
            AssignedTo=ticket_data.get('assigned_to'),
            AssignmentNotes=ticket_data.get('assignment_notes')
        )
        database.session.add(assignment)

        # B. TicketDetails
        details = TicketDetails(
            TicketID=new_ticket_id,
            Address=ticket_data.get('address'),
            AmountOfReports=ticket_data.get('amount_of_reports', 1),
            InternalNotes=ticket_data.get('internal_notes')
        )
        database.session.add(details)

        # --- 4. Final Commit ---
        database.session.commit()
        return new_ticket_id

    except Exception as e:
        # Rollback all database changes if any part of the transaction failed
        database.session.rollback()

        # Re-raise the exception to be caught in main.py
        raise Exception(f"Database insertion failed: {e}")
