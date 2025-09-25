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
});