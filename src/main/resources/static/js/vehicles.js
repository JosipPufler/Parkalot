$(function () {
    const vehicleSidebar = $("#vehicle-sidebar");
    const vehicleToggle = $("#vehicle-sidebar-toggle");
    const vehicleList = $("#vehicle-list");
    const vehicleForm = $("#register-vehicle-form");
    const vehicleFormStatus = $("#vehicle-form-status");
    const saveVehicleButton = $("#save-vehicle");
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
            $("<p>").addClass("text-muted small").text("Loading vehicles...")
        );

        $.ajax({
            url: "/rest/vehicle",
            method: "GET",
            dataType: "json"
        })
        .done(function (vehicles) {
            vehicleList.empty();

            if (!vehicles.length) {
                vehicleList.append(
                    $("<p>").addClass("text-muted small")
                        .text("You haven't registered any vehicles yet.")
                );
                return;
            }

            vehicles.forEach(function (vehicle) {
                const card = $("<button>", {
                    type: "button",
                    class: "card vehicle-card text-start w-100",
                    "aria-pressed": "false"
                });

                const body = $("<div>").addClass("card-body");

                body.append(
                    $("<h3>").addClass("h6 card-title")
                        .text(vehicle.make + " " + vehicle.model),
                    $("<p>").addClass("mb-1")
                        .text("Licence plate: " + vehicle.licencePlate),
                    $("<p>").addClass("text-muted small mb-0")
                        .text("Year: " + vehicle.year)
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
            });
        })
        .fail(function (xhr) {
            vehicleList.empty().append(
                $("<p>").addClass("text-danger small").text(
                    xhr.status === 401
                        ? "Please log in to view your vehicles."
                        : ""
                )
            );
        });
    }

    vehicleForm.on("submit", function (event) {
        event.preventDefault();

        const vehicle = {
            make: $("#vehicle-make").val().trim(),
            model: $("#vehicle-model").val().trim(),
            year: Number($("#vehicle-year").val()),
            licencePlate: $("#vehicle-licence-plate").val().trim()
        };

        if (!vehicle.make || !vehicle.model ||
            !vehicle.licencePlate || !vehicle.year) {
            vehicleFormStatus
                .removeClass("text-success")
                .addClass("text-danger")
                .text("Please fill in all fields.");
            return;
        }

        saveVehicleButton.prop("disabled", true).text("Registering...");
        vehicleFormStatus.removeClass("text-danger text-success").text("");

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
                || `Could not register vehicle (HTTP ${xhr.status}).`;

            vehicleFormStatus
                .removeClass("text-success")
                .addClass("text-danger")
                .text(message);
        })
        .always(function () {
            saveVehicleButton.prop("disabled", false)
                .text("Register vehicle");
        });
    });

    loadVehicles();
});