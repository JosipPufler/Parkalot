$(document).ready(function () {
    const container = $("#permits-container");
    const status = $("#permits-status");
    const isAdmin = $("#admin-flag").length > 0;

    $(document).on("click", "[data-collapse-target]", function () {
        const target = $(this).attr("data-collapse-target");
        $(target).collapse("toggle");
    });

    $("#update-permit-form").on("submit", function (event) {
        event.preventDefault();

        const permit = {
            id: Number($("#update-permit-id").val()),
            startDate: $("#update-start-date").val(),
            permitDuration: $("#update-duration").val(),
            areaId: Number($("#update-area").val()),
            vehicleId: Number($("#update-vehicle").val())
        };

        $.ajax({
            url: "/rest/permits",
            method: "PUT",
            contentType: "application/json",
            data: JSON.stringify(permit),

            success: function () {
                $("#update-permit-modal").modal("hide");
                loadPermits();
            },

            error: function (xhr) {
                alert(xhr.responseText || "Failed to update permit.");
            }
        });
    });

    function loadPermits() {
        $.ajax({
            url: "/rest/permits",
            method: "GET",
            dataType: "json",
            success: function (permits) {
                renderPermits(permits);
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
    }

    function renderPermits(permits) {
        console.log(permits)
        container.empty();

        if (!permits || permits.length === 0) {
            status.text("You have no permits.");
            return;
        }

        const groups = {};

        permits.forEach(function (permit) {
            const plate = permit.vehicleDto.licencePlate || "Unknown vehicle";

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
            .text(`Permit #${permit.id}`);

        const active = permit.active === true;

        const badge = $("<span>")
            .addClass(`badge ${active ? "badge-success" : "badge-secondary"}`)
            .text(active ? "Active" : permit.expired ? "Expired" : "Inactive");

        heading.append(message, badge);

        const details = $("<div>").addClass("text-muted small");

        addDetail(details, "Area", permit.areaDto.name);
        addDetail(details, "Start date", formatDate(permit.startDate));
        addDetail(details, "Expiration date", formatDate(permit.endDate));
        addDetail(details, "Duration", permit.permitDuration);
        addDetail(details, "Status", active ? "Active" : permit.expired ? "Expired" : "Inactive");
        addDetail(details, "Cost", `${permit.price}€`);

        if (isAdmin) {
            addDetail(details, "Buyer", permit.email);
        }

        body.append(heading, details);

        if (isAdmin) {
            const actions = $("<div>").addClass("mt-3 d-flex justify-content-end");

            const updateButton = $("<button>")
                .addClass("btn btn-sm btn-primary mr-2")
                .text("Update")
                .attr("type", "button")
                .on("click", function () {
                    openUpdateModal(permit);
                });

            const deleteButton = $("<button>")
                .addClass("btn btn-sm btn-danger")
                .text("Delete")
                .attr("type", "button")
                .on("click", function () {
                    deletePermit(permit.id);
                });

            actions.append(updateButton, deleteButton);
            body.append(actions);
        }

        card.append(body);
        return card;
    }

    function addDetail(parent, label, value) {
        const row = $("<p>").addClass("mb-1");
        $("<strong>").text(`${label}: `).appendTo(row);
        row.append(document.createTextNode(value ?? "N/A"));
        parent.append(row);
    }

    function deletePermit(id) {
        if (!confirm(`Are you sure you want to delete permit #${id}?`)) {
            return;
        }

        $.ajax({
            url: `/rest/permits/${id}`,
            method: "DELETE",
            success: function () {
                loadPermits();
            },
            error: function (xhr) {
                alert(xhr.responseText || "Failed to delete permit.");
            }
        });
    }

    function openUpdateModal(permit) {
        $("#update-permit-id").val(permit.id);
        $("#update-start-date").val(permit.startDate);
        $("#update-duration").val(permit.permitDuration);

        $.when(
            $.getJSON("/rest/area"),
            $.getJSON("/rest/vehicle")
        ).done(function (areasResponse, vehiclesResponse) {

            const areas = areasResponse[0];
            const vehicles = vehiclesResponse[0];

            const areaSelect = $("#update-area");
            const vehicleSelect = $("#update-vehicle");

            areaSelect.empty();
            vehicleSelect.empty();

            areas.forEach(function (area) {
                $("<option>")
                    .val(area.id)
                    .text(`${area.name} (${area.zoneName})`)
                    .appendTo(areaSelect);
            });

            vehicles.forEach(function (vehicle) {
                $("<option>")
                    .val(vehicle.id)
                    .text(`${vehicle.licencePlate} - ${vehicle.make} ${vehicle.model}`)
                    .appendTo(vehicleSelect);
            });

            areaSelect.val(permit.areaDto.id);
            vehicleSelect.val(permit.vehicleDto.id);

            $("#update-permit-modal").modal("show");

        }).fail(function () {
            alert("Failed to load areas or vehicles.");
        });
    }

    loadPermits();
});