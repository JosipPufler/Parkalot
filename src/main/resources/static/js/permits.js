$(document).ready(function () {
    const container = $("#permits-container");
    const status = $("#permits-status");

    $.ajax({
        url: "/rest/permits",
        method: "GET",
        dataType: "json",
        success: function (permit) {
            renderPermits(permit);
        },
        error: function (xhr) {
            status
                .removeClass("text-muted")
                .addClass("text-danger")
                .text(xhr.status === 401
                    ? "Please log in to view your permits."
                    : "Failed to load permits.");
        }
    });

    $(document).on("click", "[data-collapse-target]", function () {
        const target = $(this).attr("data-collapse-target");
        $(target).collapse("toggle");
    });

    function renderPermits(permits) {
        console.log(permits)
        container.empty();

        if (!permits || permits.length === 0) {
            status.text("You have no permits.");
            return;
        }

        // Group permits by licence plate.
        const groups = {};

        permits.forEach(function (permit) {
            const plate = permit.vehicle.licencePlate || "Unknown vehicle";

            if (!groups[plate]) {
                groups[plate] = [];
            }

            groups[plate].push(permit);
        });

        status.text(
            `${permits.length} permits(s) across ${Object.keys(groups).length} vehicle(s).`
        );

        Object.entries(groups).forEach(function ([plate, items], index) {
            const collapseId = `vehicle-permits-${index}`;

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
                .text(`${items.length} permits(s)`);

            header.append(toggle, count);

            // Collapsible group content.
            const body = $("<div>")
                .addClass("collapse")
                .attr("id", collapseId);

            const bodyInner = $("<div>").addClass("card-body");

            items.forEach(function (permit) {
                bodyInner.append(createPermitCard(permit));
            });

            body.append(bodyInner);
            card.append(header, body);
            container.append(card);
        });
    }

    function createPermitCard(permit) {
        const card = $("<div>").addClass("card mb-3");
        const body = $("<div>").addClass("card-body");

        const heading = $("<div>")
            .addClass("d-flex justify-content-between align-items-start mb-2");

        const message = $("<h5>")
            .addClass("card-title mb-0")
            .text(permit.message || "Parking permit");

        const active = permit.active === true;

        const badge = $("<span>")
            .addClass(`badge ${active ? "badge-danger" : "badge-secondary"}`)
            .text(active ? "Active" : "Inactive");

        heading.append(message, badge);

        const details = $("<div>").addClass("text-muted small");

        addDetail(details, "Area", permit.areaDto.name);
        addDetail(details, "Start date", formatDate(permit.startDate));
        addDetail(details, "Expiration date", formatDate(permit.endDate));
        addDetail(details, "Duration", permit.permitDuration);
        addDetail(details, "Status", permit.active ? "Active" : permit.expired ? "Expired" : "Inactive");
        addDetail(details, "Cost", `${permit.price}€`);

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