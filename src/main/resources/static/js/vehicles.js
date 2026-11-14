
$(function () {
    const vehicleSidebar = $("#vehicle-sidebar");
    const vehicleToggle = $("#vehicle-sidebar-toggle");
    const vehicleList = $("#vehicle-list");
    const vehicleForm = $("#register-vehicle-form");
    const vehicleFormStatus = $("#vehicle-form-status");
    const saveVehicleButton = $("#save-vehicle");

    const t = window.i18n;

    let selectedVehicle = null;

    window.vehicles = {
        getSelectedVehicle: function () {
            return selectedVehicle;
        }
    };

    function setVehicleSidebarOpen(open) {
        vehicleSidebar.toggleClass("open", open);
        vehicleSidebar.attr("aria-hidden", String(!open));
        vehicleToggle.attr("aria-expanded", String(open));
        $("#vehicle-toggle-icon").text(open ? "‹" : "›");
    }

    vehicleToggle.on("click", function () {
        setVehicleSidebarOpen(!vehicleSidebar.hasClass("open"));
    });

    $("#close-vehicle-sidebar").on("click", function () {
        setVehicleSidebarOpen(false);
    });

    function loadVehicles() {
        vehicleList.empty().append(
            $("<p>")
                .addClass("text-muted small")
                .text(t.vehicle.loading)
        );

        $.ajax({
            url: "/rest/vehicle",
            method: "GET",
            dataType: "json"
        })
            .done(function (vehicles) {
                vehicleList.empty();

                if (!vehicles.length) {
                    selectedVehicle = null;

                    vehicleList.append(
                        $("<p>")
                            .addClass("text-muted small")
                            .text(t.vehicle.empty)
                    );

                    $(document).trigger("vehicle:selected", [null]);
                    return;
                }

                // Preserve the previously selected vehicle after reloading.
                const previousId = selectedVehicle?.id;

                vehicles.forEach(function (vehicle) {
                    const card = $("<button>", {
                        type: "button",
                        class: "card vehicle-card text-start w-100",
                        "aria-pressed": "false"
                    });

                    const body = $("<div>").addClass("card-body");

                    const emissionLabel =
                        t.emissionClasses[vehicle.emissionClass]
                        || vehicle.emissionClass;

                    body.append(
                        $("<h3>")
                            .addClass("h6 card-title")
                            .text(vehicle.make + " " + vehicle.model),

                        $("<p>")
                            .addClass("mb-1")
                            .text(t.vehicle.licencePlate + ": " + vehicle.licencePlate),

                        $("<p>")
                            .addClass("text-muted small mb-1")
                            .text(t.vehicle.year + ": " + vehicle.year),

                        $("<p>")
                            .addClass("text-muted small mb-0")
                            .text(t.vehicle.emissionClass + ": " + emissionLabel)
                    );

                    card.append(body);

                    card.on("click", function () {
                        selectedVehicle = vehicle;

                        vehicleList.find(".vehicle-card")
                            .removeClass("border-primary bg-light")
                            .attr("aria-pressed", "false");

                        card.addClass("border-primary bg-light")
                            .attr("aria-pressed", "true");

                        $(document).trigger("vehicle:selected", [vehicle]);
                    });

                    vehicleList.append(card);

                    if (vehicle.id === previousId) {
                        card.trigger("click");
                    }
                });
            })
            .fail(function (xhr) {
                vehicleList.empty();

                const message = xhr.status === 401
                    ? t.vehicle.loginRequired
                    : t.vehicle.loadError;

                vehicleList.append(
                    $("<p>")
                        .addClass("text-danger small")
                        .text(message)
                );
            });
    }

    vehicleForm.on("submit", function (event) {
        event.preventDefault();

        const vehicle = {
            make: $("#vehicle-make").val().trim(),
            model: $("#vehicle-model").val().trim(),
            year: Number($("#vehicle-year").val()),
            licencePlate: $("#vehicle-licence-plate").val().trim(),
            emissionClass: $("#vehicle-emission-class").val()
        };

        if (
            !vehicle.make ||
            !vehicle.model ||
            !vehicle.licencePlate ||
            !vehicle.year ||
            !vehicle.emissionClass
        ) {
            vehicleFormStatus
                .removeClass("text-success")
                .addClass("text-danger")
                .text(t.vehicle.fillFields);

            return;
        }

        saveVehicleButton
            .prop("disabled", true)
            .text(t.vehicle.registering);

        vehicleFormStatus
            .removeClass("text-danger text-success")
            .text("");

        $.ajax({
            url: "/rest/vehicle",
            method: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify(vehicle)
        })
            .done(function () {
                vehicleForm[0].reset();

                bootstrap.Modal.getOrCreateInstance(
                    document.getElementById("register-vehicle-modal")
                ).hide();

                loadVehicles();
            })
            .fail(function (xhr) {
                const message = xhr.responseJSON?.message
                    || `${t.vehicle.registerError} (HTTP ${xhr.status}).`;

                vehicleFormStatus
                    .removeClass("text-success")
                    .addClass("text-danger")
                    .text(message);
            })
            .always(function () {
                saveVehicleButton
                    .prop("disabled", false)
                    .text(t.vehicle.register);
            });
    });

    loadVehicles();
});
