document.addEventListener("DOMContentLoaded", () => {
  const navItems = document.querySelectorAll(".navbar ul li");
  const sections = document.querySelectorAll(".nav-item");

  navItems.forEach(item => {
    item.addEventListener("click", () => {
      // Remove "show" from all sections
      sections.forEach(section => section.classList.remove("show"));
        console.log(sections)
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

  let assignButtons = document.querySelectorAll(".assign-team");

  assignButtons.forEach(button => {
    button.addEventListener("click", () => {
      const newText = prompt("Enter the assigned team:");
     
      if (newText) {
        // find the closest ticket-info section
        const ticket = button.closest(".container-big");

        // find the paragraph we want to update inside this ticket
        const output = ticket.querySelector(".ticket-info .assigned");

        
        output.textContent = `Assigned to: ${newText}`;
        
      }
    });
  });

  assignButtons = document.querySelectorAll(".add-notes");

  assignButtons.forEach(button => {
    button.addEventListener("click", () => {
      const newText = prompt("Enter the notes to be added:");
      
      if (newText) {
        // find the closest ticket-info section
        const ticket = button.closest(".container-big");

        // find the paragraph we want to update inside this ticket
        const output = ticket.querySelector(".ticket-info .notes");

        
        output.textContent = `Notes: ${newText}`;
      }
    });
  });

  assignButtons = document.querySelectorAll(".notify");

  assignButtons.forEach(button => {
    button.addEventListener("click", () => {
      alert("Message Sent to all in the Area!!");
    });
  });

  assignButtons = document.querySelectorAll(".resolved");

  assignButtons.forEach(button => {
    button.addEventListener("click", () => {
      alert("The ticket has been resolved!");
    });
  })
});