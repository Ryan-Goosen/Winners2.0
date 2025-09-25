from .models import Ticket, Region, TicketAssignments, TicketDetails
from datetime import datetime
from __main__ import gemi_app_calls
import os

def get_unique_categories(database):
    """
    Queries the database and returns a list of all unique categories
    from the 'Category' column of the 'Tickets' table.

    Args:
        database: The SQLAlchemy object (to access models and session).

    Returns:
        List[str]: A list containing all unique category strings.
    """
    try:
        # Use select() with distinct() to get only unique values from the Category column
        unique_categories = database.session.execute(
            database.select(Ticket.Category).distinct()
        ).scalars().all()

        # The result is a list of strings, which is what we want
        return unique_categories

    except Exception as e:
        # Log the error and return an empty list
        print(f"Error fetching unique categories: {e}")
        return []


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

        categories = get_unique_categories(database)
        new_category = gemi_app_calls(ticket_data.get('description', ''), categories)
        # --- 2. Create the main Ticket record ---
        new_ticket = Ticket(
            Title=ticket_data['title'],
            Description=ticket_data.get('description', ''),
            Status=ticket_data.get('status', 'New'),
            Priority=ticket_data.get('priority', 'Medium'),
            ReportedAt=datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            EstRepairTime=ticket_data.get('est_repair_time'),
            Category= new_category,
            ImageURL=image_url,
            region=region
        )
        database.session.add(new_ticket)

        # Flush to get the TicketID without committing the full transaction
        database.session.flush()
        new_ticket_id = new_ticket.TicketID

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



def get_all_tickets(database):
    """
    Returns all the current tickets in the database.


    Args:
        database: The SQLAlchemy object (to access models and session).

    Returns:
        List: A list containing all the tickets that are in the database.
    """
    try:
        tickets_with_data = database.session.execute(
            database.select(Ticket)
            .join(Region)
            .outerjoin(TicketAssignments)
            .outerjoin(TicketDetails)
        ).scalars().all()

        # Format the results into a clean list of dictionaries
        result_list = []
        for ticket in tickets_with_data:
            # Safely access related data (which might be None if using outerjoin)
            assignment = ticket.assignment
            details = ticket.details

            ticket_data = {
                # --- Core Ticket Data ---
                "ticket_id": ticket.TicketID,
                "title": ticket.Title,
                "description": ticket.Description,
                "status": ticket.Status,
                "priority": ticket.Priority,
                "reported_at": ticket.ReportedAt,
                "est_repair_time": ticket.EstRepairTime,
                "image_url": ticket.ImageURL,

                # --- Region Data ---
                "region_id": ticket.RegionID,
                "region_name": ticket.region.RegionName,  # Access via relationship backref
                "region_manager": ticket.region.RegionManager,

                # --- Assignment Data ---
                "reported_by": assignment.ReportedBy if assignment else None,
                "assigned_to": assignment.AssignedTo if assignment else None,
                "assignment_notes": assignment.AssignmentNotes if assignment else None,

                # --- Detail Data ---
                "address": details.Address if details else None,
                "amount_of_reports": details.AmountOfReports if details else 1,  # Default to 1 if missing
                "internal_notes": details.InternalNotes if details else None,
            }
            result_list.append(ticket_data)

        return result_list

    except Exception as e:
        print(f"Error fetching all tickets: {e}")
        return []


def create_tables_if_not_exist(database):
    """
    Creates all defined database tables using SQLAlchemy's declarative base
    if they do not already exist in the database file.

    Args:
        database: The SQLAlchemy object initialized in main.py.

    Returns:
        None
    """
    try:
        # SQLAlchemy's create_all() is idempotent: it only creates tables
        # that don't already exist. It needs to be run within the
        # Flask application context.
        database.create_all()
        print("✅ Database tables checked and created successfully (if they didn't exist).")
    except Exception as e:
        print(f"❌ Error during database table creation: {e}")
        # In a real-world app, you might want to exit the application here
        # if the database is critical.
