document.addEventListener("DOMContentLoaded", () => {
  /* ---------------------------
     NAVIGATION HANDLING
  --------------------------- */
  const navItems = document.querySelectorAll(".navbar ul li");
  const sections = document.querySelectorAll(".nav-item");

  navItems.forEach(item => {
    item.addEventListener("click", () => {
      // Remove "show" from all sections
      sections.forEach(section => section.classList.remove("show"));

      // Remove highlight from all nav items
      navItems.forEach(nav => nav.classList.remove("nav-open"));

      // Add highlight to clicked nav item
      item.classList.add("nav-open");

      // Show the matching section
      const targetClass = item.getAttribute("data-target");
      const targetSection = document.querySelector(`.${targetClass}`);
      if (targetSection) {
        targetSection.classList.add("show");
      }
    });
  });

  /* ---------------------------
     HELPER FUNCTIONS
  --------------------------- */
  const formatReportedDate = (isoString) => {
    try {
      const date = new Date(isoString);
      const options = { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true };
      return date.toLocaleDateString('en-US', options);
    } catch (e) {
      return 'N/A';
    }
  };

  const bindTicketEvents = (ticketElement) => {
    const assignBtn = ticketElement.querySelector(".assign-team");
    const notesBtn = ticketElement.querySelector(".add-notes");
    const notifyBtn = ticketElement.querySelector(".notify");
    const resolveBtn = ticketElement.querySelector(".resolved");

    if (assignBtn) {
      assignBtn.addEventListener("click", () => {
        const newText = prompt("Enter the assigned team:");
        if (newText) {
          const output = ticketElement.querySelector(".ticket-info .assigned");
          if (output) output.textContent = `Assigned to: ${newText}`;
        }
      });
    }

    if (notesBtn) {
      notesBtn.addEventListener("click", () => {
        const newText = prompt("Enter the notes to be added:");
        if (newText) {
          const output = ticketElement.querySelector(".ticket-info .notes");
          if (output) output.textContent = `Notes: ${newText}`;
        }
      });
    }

    if (notifyBtn) {
      notifyBtn.addEventListener("click", () => {
        alert("Message Sent to all in the Area!!");
      });
    }

    if (resolveBtn) {
      resolveBtn.addEventListener("click", () => {
        alert("The ticket has been resolved!");
      });
    }
  };

  /* ---------------------------
     RENDER TICKETS (API FETCH)
  --------------------------- */
  const renderTickets = async () => {
    const template = document.getElementById('ticket-template');
    const container = document.querySelector('.ticket-list');

    if (!template || !container) {
      console.error("Template or container element not found.");
      return;
    }

    try {
      const response = await fetch('http://127.0.0.1:5000/api/tickets');
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const tickets = await response.json();

      // Clear existing content
      container.innerHTML = '';

      // Compile handlebars template
      const templateSource = template.innerHTML;
      const compiledTemplate = Handlebars.compile(templateSource);

      // Render each ticket
      tickets.forEach(ticket => {
        const html = compiledTemplate({
          title: ticket.title,
          description: ticket.description,
          location: ticket.address || 'Address not available',
          date: formatReportedDate(ticket.reported_at),
          reports: ticket.amount_of_reports || 1,
          assigned: ticket.assigned_to,
          notes: ticket.assignment_notes
        });

        // Append to container
        const wrapper = document.createElement('div');
        wrapper.innerHTML = html;
        const ticketElement = wrapper.firstElementChild;

        container.appendChild(ticketElement);

        // Bind interactive events
        bindTicketEvents(ticketElement);
      });

    } catch (error) {
      console.error('Failed to fetch and render tickets:', error);
      container.innerHTML = '<p class="error-message">Failed to load tickets. Check the CORS settings or server status.</p>';
    }
  };

  // Call renderTickets on page load
  renderTickets();
});
