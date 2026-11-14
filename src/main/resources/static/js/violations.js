
$(document).ready(function () {
    const container = $("#violations-container");
    const status = $("#violations-status");
    const t = window.i18n.violations;
    const locale = document.documentElement.lang || "en";

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
                    ? t.loginRequired
                    : t.loadError);
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

    // Submit appeal.
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

        submitButton.prop("disabled", true).text(t.submitting);
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

                status
                    .removeClass("text-muted text-danger")
                    .addClass("text-success")
                    .text(t.appealSuccess);
            },

            error: function (xhr) {
                $("#appeal-error").text(
                    xhr.responseJSON?.message || t.appealError
                );
            },

            complete: function () {
                submitButton
                    .prop("disabled", false)
                    .text(t.submitAppeal);
            }
        });
    });

    // Payment initiation.
    $(document).on("click", "[data-pay-violation]", function () {
        const violationId = $(this).attr("data-pay-violation");

        if (!confirm(t.paymentConfirm)) {
            return;
        }

        $.ajax({
            url: `/rest/payments/violations/${encodeURIComponent(violationId)}`,
            method: "POST",

            success: function (response) {
                if (response.checkoutUrl) {
                    window.location.assign(response.checkoutUrl);
                } else {
                    status
                        .removeClass("text-muted text-danger")
                        .addClass("text-success")
                        .text(t.paymentInitiated);
                }
            },

            error: function (xhr) {
                alert(
                    xhr.responseJSON?.message || t.paymentError
                );
            }
        });
    });

    function renderViolations(violations) {
        container.empty();

        if (!violations || violations.length === 0) {
            status.text(t.empty);
            return;
        }

        const groups = {};

        violations.forEach(function (violation) {
            const plate = violation.licencePlate || t.unknownVehicle;

            if (!groups[plate]) {
                groups[plate] = [];
            }

            groups[plate].push(violation);
        });

        status.text(
            t.summary
                .replace("{violations}", violations.length)
                .replace("{vehicles}", Object.keys(groups).length)
        );

        Object.entries(groups).forEach(function ([plate, items], index) {
            const collapseId = `vehicle-violations-${index}`;

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
                .text(
                    t.violationCount.replace("{count}", items.length)
                );

            header.append(toggle, count);

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
            .text(violation.message || t.parkingViolation);

        const active = violation.active === true;

        const badge = $("<span>")
            .addClass(`badge ${active ? "badge-danger" : "badge-secondary"}`)
            .text(active ? t.active : t.inactive);

        heading.append(message, badge);

        const details = $("<div>").addClass("text-muted small");

        addDetail(details, t.area, violation.area);
        addDetail(details, t.issued, formatDate(violation.issuedAt));
        addDetail(details, t.officer, violation.officer);

        if (violation.price != null) {
            addDetail(details, t.fine, Number(violation.price).toFixed(2));
        }

        body.append(heading, details);

        const actions = $("<div>").addClass("mt-3 d-flex flex-wrap");

        const payButton = $("<button>")
            .attr("type", "button")
            .addClass("btn btn-success btn-sm mr-2")
            .text(t.pay)
            .attr("data-pay-violation", violation.id)
            .prop("disabled", !active || violation.id == null);

        const appealButton = $("<button>")
            .attr("type", "button")
            .addClass("btn btn-outline-primary btn-sm")
            .text(t.raiseAppeal)
            .attr("data-appeal-violation", violation.id)
            .prop("disabled", !active || violation.id == null);

        actions.append(payButton, appealButton);
        body.append(actions);
        card.append(body);

        return card;
    }

    function addDetail(parent, label, value) {
        const row = $("<p>").addClass("mb-1");

        $("<strong>")
            .text(`${label}: `)
            .appendTo(row);

        row.append(
            document.createTextNode(value ?? "N/A")
        );

        parent.append(row);
    }

    function formatDate(value) {
        if (!value) return "N/A";

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) return value;

        return date.toLocaleString(
            locale === "hr" ? "hr-HR" : "en-GB"
        );
    }
});
