
$(document).ready(function () {
    const container = $("#permits-container");
    const status = $("#permits-status");
    const isAdmin = $("#admin-flag").length > 0;

    const t = window.i18n.permits;
    const locale = document.documentElement.lang === "hr"
        ? "hr-HR"
        : "en-GB";

    const pluralRules = new Intl.PluralRules(locale);

    const modal = $("#update-permit-modal");
    const permitForm = $("#update-permit-form");
    const modalTitle = $("#update-permit-title");
    const submitButton = $("#permit-submit-button");

    const permitIdInput = $("#update-permit-id");
    const startDateInput = $("#update-start-date");
    const durationSelect = $("#update-duration");
    const areaSelect = $("#update-area");
    const vehicleSelect = $("#update-vehicle");

    let currentMode = "create";

    function translate(template, values = {}) {
        return template.replace(/\{(\w+)\}/g, function (match, key) {
            return values[key] ?? match;
        });
    }

    function getText(key, fallback) {
        return t[key] || fallback;
    }

    function getCountText(count, type) {
        const category = pluralRules.select(count);
        const key = `${type}Count${category.charAt(0).toUpperCase()}${category.slice(1)}`;

        return translate(t[key] || t[`${type}CountOther`], { count });
    }

    function formatDate(dateString) {
        if (!dateString) {
            return t.notAvailable;
        }

        let date;

        // Parse date-only values as local dates to avoid timezone shifts.
        if (/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
            const [year, month, day] = dateString.split("-").map(Number);
            date = new Date(year, month - 1, day);
        } else {
            date = new Date(dateString);
        }

        if (Number.isNaN(date.getTime())) {
            return dateString;
        }

        return new Intl.DateTimeFormat(locale).format(date);
    }

    function getToday() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, "0");
        const day = String(now.getDate()).padStart(2, "0");

        return `${year}-${month}-${day}`;
    }

    function formatDuration(duration) {
        const durationLabels = {
            DAY: t.durationDay,
            WEEK: t.durationWeek,
            MONTH: t.durationMonth,
            YEAR: t.durationYear
        };

        return durationLabels[duration] || duration || t.notAvailable;
    }

    function initializeModalSelects() {
        [durationSelect, areaSelect, vehicleSelect].forEach(function (select) {
            if (select.hasClass("select2-hidden-accessible")) {
                select.select2("destroy");
            }

            select.select2({
                width: "100%",
                dropdownParent: modal,
                placeholder: getText("selectOption", "Select an option"),
                allowClear: true
            });
        });
    }

    $(document).on("click", "[data-collapse-target]", function () {
        const target = $(this).attr("data-collapse-target");
        $(target).collapse("toggle");
    });

    $("#create-permit").on("click", function () {
        openPermitModal();
    });

    // Handle modal close buttons.
    modal.find("[data-dismiss='modal']").on("click", function () {
        modal.modal("hide");
    });

    permitForm.on("submit", function (event) {
        event.preventDefault();

        if (!isAdmin) {
            return;
        }

        const id = permitIdInput.val();
        const isUpdate = Boolean(id);

        const permit = {
            startDate: startDateInput.val(),
            permitDuration: durationSelect.val(),
            areaId: Number(areaSelect.val()),
            vehicleId: Number(vehicleSelect.val())
        };

        if (isUpdate) {
            permit.id = Number(id);
        }

        const url = isUpdate
            ? "/rest/permits"
            : "/rest/permits/admin";

        const method = isUpdate ? "PUT" : "POST";

        submitButton.prop("disabled", true)
            .text(isUpdate
                ? getText("saving", "Saving...")
                : getText("creating", "Creating..."));

        $.ajax({
            url: url,
            method: method,
            contentType: "application/json",
            data: JSON.stringify(permit)
        }).done(function () {
            modal.modal("hide");
            loadPermits();
        }).always(function () {
            submitButton.prop("disabled", false)
                .text(isUpdate
                    ? getText("save", "Save changes")
                    : getText("create", "Create permit"));
        });
    });

    function loadPermits() {
        $.ajax({
            url: "/rest/permits",
            method: "GET",
            dataType: "json"
        }).done(function (permits) {
            renderPermits(permits);
        }).fail(function (xhr) {
            status
                .removeClass("text-muted")
                .addClass("text-danger")
                .text(
                    xhr.status === 401
                        ? t.loginRequired
                        : t.loadError
                );
        });
    }

    function renderPermits(permits) {
        container.empty();
        status.removeClass("text-danger").addClass("text-muted");

        if (!permits || permits.length === 0) {
            status.text(t.noPermits);
            return;
        }

        const groups = {};

        permits.forEach(function (permit) {
            const plate = permit.vehicleDto?.licencePlate || t.unknownVehicle;

            if (!groups[plate]) {
                groups[plate] = [];
            }

            groups[plate].push(permit);
        });

        const vehicleCount = Object.keys(groups).length;

        status.text(
            translate(t.permitSummary, {
                permits: getCountText(permits.length, "permit"),
                vehicles: getCountText(vehicleCount, "vehicle")
            })
        );

        Object.entries(groups).forEach(function ([plate, items], index) {
            const collapseId = `vehicle-permits-${index}`;

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
                .text(getCountText(items.length, "permit"));

            header.append(toggle, count);

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
            .text(translate(t.permitNumber, { id: permit.id }));

        const active = permit.active === true;
        const expired = permit.expired === true;

        const permitStatus = active
            ? t.active
            : expired
                ? t.expired
                : t.inactive;

        const badge = $("<span>")
            .addClass(
                `badge ${active ? "badge-success" : "badge-secondary"}`
            )
            .text(permitStatus);

        heading.append(message, badge);

        const details = $("<div>").addClass("text-muted small");

        addDetail(details, t.area, permit.areaDto?.name);
        addDetail(details, t.startDate, formatDate(permit.startDate));
        addDetail(details, t.expirationDate, formatDate(permit.endDate));
        addDetail(details, t.duration, formatDuration(permit.permitDuration));
        addDetail(details, t.status, permitStatus);
        addDetail(details, t.cost, `${permit.price}€`);

        if (isAdmin) {
            addDetail(details, t.buyer, permit.email);
        }

        body.append(heading, details);

        if (isAdmin) {
            const actions = $("<div>")
                .addClass("mt-3 d-flex justify-content-end");

            const updateButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-primary mr-2"
            })
                .text(t.update)
                .on("click", function () {
                    openPermitModal(permit);
                });

            const deleteButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-danger"
            })
                .text(t.delete)
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

        $("<strong>")
            .text(`${label}: `)
            .appendTo(row);

        row.append(document.createTextNode(value ?? t.notAvailable));
        parent.append(row);
    }

    function deletePermit(id) {
        if (!confirm(translate(t.deleteConfirmation, { id }))) {
            return;
        }

        $.ajax({
            url: `/rest/permits/${id}`,
            method: "DELETE"
        }).done(function () {
            loadPermits();
        });
    }

    function openPermitModal(permit = null) {
        if (!isAdmin) {
            return;
        }

        currentMode = permit ? "update" : "create";

        permitForm[0].reset();

        permitIdInput.val(permit?.id || "");
        startDateInput.val(permit?.startDate || getToday());

        modalTitle.text(
            permit
                ? getText("update", "Update permit")
                : getText("create", "Create permit")
        );

        submitButton.text(
            permit
                ? getText("save", "Save changes")
                : getText("create", "Create permit")
        );

        $.when(
            $.getJSON("/rest/area"),
            $.getJSON("/rest/vehicle")
        ).done(function (areasResponse, vehiclesResponse) {
            const areas = areasResponse[0];
            const vehicles = vehiclesResponse[0];

            areaSelect.empty();
            vehicleSelect.empty();

            $("<option>")
                .val("")
                .text(getText("selectArea", "Select an area"))
                .appendTo(areaSelect);

            $("<option>")
                .val("")
                .text(getText("selectVehicle", "Select a vehicle"))
                .appendTo(vehicleSelect);

            areas.forEach(function (area) {
                $("<option>")
                    .val(area.id)
                    .text(`${area.name} (${area.zoneName})`)
                    .appendTo(areaSelect);
            });

            vehicles.forEach(function (vehicle) {
                $("<option>")
                    .val(vehicle.id)
                    .text(
                        `${vehicle.licencePlate} - ${vehicle.make} ${vehicle.model}`
                    )
                    .appendTo(vehicleSelect);
            });

            initializeModalSelects();

            if (permit) {
                areaSelect.val(String(permit.areaDto.id));
                vehicleSelect.val(String(permit.vehicleDto.id));
                durationSelect.val(permit.permitDuration);
            } else {
                areaSelect.val("");
                vehicleSelect.val("");
                durationSelect.val("DAY");
            }

            areaSelect.trigger("change");
            vehicleSelect.trigger("change");
            durationSelect.trigger("change");

            modal.modal("show");
        });
    }

    loadPermits();
});
