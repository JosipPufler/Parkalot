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

    $(document).on("click", "[data-collapse-target]", function () {
        const target = $(this).attr("data-collapse-target");
        $(target).collapse("toggle");
    });

    $(document).on("click", "[data-appeal-violation]", function () {
        const violationId = $(this).attr("data-appeal-violation");

        $("#appeal-form")[0].reset();
        $("#appeal-violation-id").val(violationId);
        $("#appeal-error").text("");
        $("#appeal-modal").modal("show");
    });

    // Submit the appeal as multipart/form-data.
    $("#appeal-form").on("submit", function (event) {
        event.preventDefault();

        const formData = new FormData();
        formData.append("violationId", $("#appeal-violation-id").val());
        formData.append("title", $("#appeal-title").val().trim());
        formData.append("message", $("#appeal-message").val().trim());

        const files = $("#appeal-evidence")[0].files;

        for (const file of files) {
            formData.append("evidence", file);
        }

        const submitButton = $("#appeal-submit");
        submitButton.prop("disabled", true).text("Submitting...");
        $("#appeal-error").text("");

        $.ajax({
            url: "/rest/violations/appeal",
            method: "POST",
            data: formData,
            processData: false,
            contentType: false,
            success: function () {
                $("#appeal-modal").modal("hide");
                $("#appeal-form")[0].reset();

                // Replace with your existing notification function if available.
                status.removeClass("text-muted text-danger")
                    .addClass("text-success")
                    .text("Appeal submitted successfully.");
            },
            error: function (xhr) {
                $("#appeal-error").text(
                    xhr.responseJSON?.message ||
                    "Failed to submit the appeal. Please try again."
                );
            },
            complete: function () {
                submitButton.prop("disabled", false)
                    .text("Submit Appeal");
            }
        });
    });

    // Payment initiation: replace this URL with your actual payment endpoint.
    $(document).on("click", "[data-pay-violation]", function () {
        const violationId = $(this).attr("data-pay-violation");

        if (!confirm("Proceed to payment for this violation?")) {
            return;
        }

        // Example contract only; implement this endpoint on your backend.
        $.ajax({
            url: `/rest/payments/violations/${encodeURIComponent(violationId)}`,
            method: "POST",
            success: function (response) {
                // If your payment provider returns a checkout URL:
                if (response.checkoutUrl) {
                    window.location.assign(response.checkoutUrl);
                } else {
                    status.removeClass("text-muted text-danger")
                        .addClass("text-success")
                        .text("Payment request initiated.");
                }
            },
            error: function (xhr) {
                alert(
                    xhr.responseJSON?.message ||
                    "Could not initiate payment. Please try again."
                );
            }
        });
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

        const actions = $("<div>").addClass("mt-3 d-flex flex-wrap");

        const payButton = $("<button>")
            .attr("type", "button")
            .addClass("btn btn-success btn-sm mr-2")
            .text("Pay")
            .attr("data-pay-violation", violation.id)
            .prop("disabled", !active || violation.id == null);

        const appealButton = $("<button>")
            .attr("type", "button")
            .addClass("btn btn-outline-primary btn-sm")
            .text("Raise Appeal")
            .attr("data-appeal-violation", violation.id)
            .prop("disabled", !active || violation.id == null);

        actions.append(payButton, appealButton);
        body.append(actions);

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