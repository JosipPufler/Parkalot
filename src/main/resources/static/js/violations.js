$(document).ready(function () {
    const container = $("#violations-container");
    const status = $("#violations-status");

    $.ajax({
        url: "/rest/violations",
        method: "GET",
        dataType: "json",
        success: function (violations) {
            renderViolations(violations);
        },
        error: function (xhr) {
            status
                .removeClass("text-muted")
                .addClass("text-danger")
                .text(xhr.status === 401
                    ? "Please log in to view your violations."
                    : "Failed to load violations.");
        }
    });

    $(document).on("click", "[data-toggle='collapse']", function () {
        const target = $(this).attr("data-target");
        $(target).collapse("toggle");
    });

    function renderViolations(violations) {
        console.log(violations)
        container.empty();

        if (!violations || violations.length === 0) {
            status.text("You have no violations.");
            return;
        }

        // Group violations by licence plate.
        const groups = {};

        violations.forEach(function (violation) {
            const plate = violation.licencePlate || "Unknown vehicle";

            if (!groups[plate]) {
                groups[plate] = [];
            }

            groups[plate].push(violation);
        });

        status.text(
            `${violations.length} violation(s) across ${Object.keys(groups).length} vehicle(s).`
        );

        Object.entries(groups).forEach(function ([plate, items], index) {
            const collapseId = `vehicle-violations-${index}`;

            // Vehicle group heading.
            const card = $("<div>").addClass("card mb-3");

            const header = $("<div>")
                .addClass("card-header d-flex justify-content-between align-items-center");

            const toggle = $("<button>")
                .addClass("btn btn-link text-left p-0 font-weight-bold")
                .attr("type", "button")
                .attr("data-collapse-target", `#${collapseId}`)
                .text(plate);

            const count = $("<span>")
                .addClass("badge badge-secondary")
                .text(`${items.length} violation(s)`);

            header.append(toggle, count);

            // Collapsible group content.
            const body = $("<div>")
                .addClass("collapse")
                .attr("id", collapseId);

            const bodyInner = $("<div>").addClass("card-body");

            items.forEach(function (violation) {
                bodyInner.append(createViolationCard(violation));
            });

            body.append(bodyInner);
            card.append(header, body);
            container.append(card);
        });
    }

    function createViolationCard(violation) {
        const card = $("<div>").addClass("card mb-3");
        const body = $("<div>").addClass("card-body");

        const heading = $("<div>")
            .addClass("d-flex justify-content-between align-items-start mb-2");

        const message = $("<h5>")
            .addClass("card-title mb-0")
            .text(violation.message || "Parking violation");

        const active = violation.active === true;

        const badge = $("<span>")
            .addClass(`badge ${active ? "badge-danger" : "badge-secondary"}`)
            .text(active ? "Active" : "Inactive");

        heading.append(message, badge);

        const details = $("<div>").addClass("text-muted small");

        addDetail(details, "Area", violation.area);
        addDetail(details, "Issued", formatDate(violation.issuedAt));
        addDetail(details, "Officer", violation.officer);

        if (violation.price != null) {
            addDetail(details, "Fine", `${violation.price.toFixed(2)}`);
        }

        body.append(heading, details);
        card.append(body);

        return card;
    }

    function addDetail(parent, label, value) {
        const row = $("<p>").addClass("mb-1");
        $("<strong>").text(`${label}: `).appendTo(row);
        row.append(document.createTextNode(value ?? "N/A"));
        parent.append(row);
    }

    function formatDate(value) {
        if (!value) return "N/A";

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) return value;

        return date.toLocaleString();
    }
});