$(function () {
    const vehiclesTable = $("#vehicles-table-body");
    const vehicleForm = $("#vehicle-admin-form");
    const vehiclesStatus = $("#vehicles-status");
    const vehicleModal = $("#vehicle-modal");

    const saveButton = $("#save-vehicle");
    const formStatus = $("#vehicle-form-status");

    const vehicleId = $("#vehicle-id");
    const makeInput = $("#vehicle-make");
    const modelInput = $("#vehicle-model");
    const yearInput = $("#vehicle-year");
    const licencePlateInput = $("#vehicle-licence-plate");
    const ownersSelect = $("#vehicle-owners");

    let vehicles = [];
    let users = [];

    ownersSelect.select2({
        width: "100%",
        placeholder: "Select vehicle owners",
        allowClear: true,
        dropdownParent: $("#vehicle-modal")
    });

    function loadData() {
        vehiclesTable.html(`
            <tr>
                <td colspan="6" class="text-center text-muted">
                    Loading vehicles...
                </td>
            </tr>
        `);

        $.when(
            $.ajax({
                url: "/rest/vehicle",
                method: "GET",
                dataType: "json"
            }),
            $.ajax({
                url: "/rest/users",
                method: "GET",
                dataType: "json"
            })
        ).done(function (vehicleResponse, userResponse) {
            vehicles = vehicleResponse[0];
            users = userResponse[0];

            vehiclesStatus.empty();
            populateOwnersSelect();
            renderVehicles();
        }).fail(function (xhr) {
            vehiclesStatus
                .removeClass("text-muted")
                .addClass("text-danger")
                .text("Failed to load vehicles.");

            console.error("Failed to load admin vehicle data:", xhr);
        })
    }

    function populateOwnersSelect() {
        ownersSelect.empty();

        users.forEach(function (user) {
            const fullName = `${user.firstName} ${user.lastName}`;

            ownersSelect.append(
                $("<option>", {
                    value: user.id,
                    text: `${fullName} (${user.email})`
                })
            );
        });

        ownersSelect.trigger("change.select2");
    }

    function getOwnerNames(ownerIds) {
        if (!ownerIds || ownerIds.length === 0) {
            return "No owners assigned";
        }

        return ownerIds.map(function (id) {
            const user = users.find(u => u.id === id);

            return user
                ? `${user.firstName} ${user.lastName}`
                : `Unknown user (#${id})`;
        }).join(", ");
    }

    function renderVehicles() {
        vehiclesTable.empty();

        if (!vehicles.length) {
            vehiclesTable.html(`
                <tr>
                    <td colspan="6" class="text-center text-muted">
                        No vehicles found.
                    </td>
                </tr>
            `);
            return;
        }

        vehicles.forEach(function (vehicle) {
            const row = $("<tr>");

            row.append($("<td>").text(vehicle.licencePlate));
            row.append($("<td>").text(vehicle.make));
            row.append($("<td>").text(vehicle.model));
            row.append($("<td>").text(vehicle.year));
            row.append(
                $("<td>").text(getOwnerNames(vehicle.ownerIds))
            );

            const actions = $("<td>").addClass("text-end text-nowrap");

            const editButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-primary me-2",
                title: "Edit vehicle"
            }).html('<i class="bi bi-pencil"></i>');

            editButton.on("click", function () {
                openEditModal(vehicle);
            });

            const deleteButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-danger",
                title: "Delete vehicle"
            }).html('<i class="bi bi-trash"></i>');

            deleteButton.on("click", function () {
                deleteVehicle(vehicle);
            });

            actions.append(editButton, deleteButton);
            row.append(actions);

            vehiclesTable.append(row);
        });
    }

    $("#create-vehicle").on("click", function () {
        vehicleForm[0].reset();

        vehicleId.val("");
        ownersSelect.val([]).trigger("change");

        formStatus.empty();

        $("#vehicle-modal-title").text("Add Vehicle");
        saveButton.text("Create Vehicle");

        // Licence plates can be edited only when creating.
        licencePlateInput.prop("disabled", false);

        vehicleModal.modal("show");
    });

    function openEditModal(vehicle) {
        vehicleForm[0].reset();

        vehicleId.val(vehicle.id);
        makeInput.val(vehicle.make);
        modelInput.val(vehicle.model);
        yearInput.val(vehicle.year);
        licencePlateInput.val(vehicle.licencePlate);

        ownersSelect.val(
            (vehicle.ownerIds || []).map(String)
        ).trigger("change");

        formStatus.empty();

        $("#vehicle-modal-title").text("Edit Vehicle");
        saveButton.text("Save Changes");

        licencePlateInput.prop("disabled", false);

        vehicleModal.show();
    }

    vehicleForm.on("submit", function (event) {
        event.preventDefault();

        const id = vehicleId.val();

        const vehicle = {
            make: makeInput.val().trim(),
            model: modelInput.val().trim(),
            year: Number(yearInput.val()),
            licencePlate: licencePlateInput.val().trim().toUpperCase(),
            ownerIds: (ownersSelect.val() || []).map(Number)
        };

        if (!vehicle.make || !vehicle.model ||
            !vehicle.licencePlate || !vehicle.year) {
            showFormStatus("Please fill in all vehicle fields.", "danger");
            return;
        }

        const isUpdate = Boolean(id);

        if (isUpdate) {
            vehicle.id = Number(id);
        }

        saveButton.prop("disabled", true)
            .text(isUpdate ? "Saving..." : "Creating...");

        $.ajax({
            url: isUpdate ? "/rest/vehicle" : "/rest/vehicle/insert",
            method: isUpdate ? "PUT" : "POST",
            contentType: "application/json",
            data: JSON.stringify(vehicle)
        }).done(function () {
            vehicleModal.modal("hide");
            loadData();
        }).fail(function (xhr) {
            const message = xhr.responseJSON?.message
                || "Failed to save vehicle.";

            showFormStatus(message, "danger");
        }).always(function () {
            saveButton.prop("disabled", false)
                .text(isUpdate ? "Save Changes" : "Create Vehicle");
        });
    });

    function deleteVehicle(vehicle) {
        const confirmed = confirm(
            `Are you sure you want to delete vehicle ${vehicle.licencePlate}?`
        );

        if (!confirmed) {
            return;
        }

        $.ajax({
            url: `/rest/vehicle/${vehicle.id}`,
            method: "DELETE"
        }).done(function () {
            loadData();
        }).fail(function (xhr) {
            console.error("Failed to delete vehicle:", xhr);
            alert(xhr.responseJSON?.message || "Failed to delete vehicle.");
        });
    }

    function showFormStatus(message, type) {
        formStatus
            .removeClass("text-danger text-success")
            .addClass(type === "danger" ? "text-danger" : "text-success")
            .text(message);
    }

    loadData();
});