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
    const emissionClassSelect = $("#vehicle-emission-class");

    const t = window.i18n.vehicles;

    let vehicles = [];
    let users = [];

    ownersSelect.select2({
        width: "100%",
        placeholder: t.selectOwners,
        allowClear: true,
        dropdownParent: vehicleModal
    });

    $("#vehicle-modal [data-dismiss='modal']").on("click", function () {
        vehicleModal.modal("hide");
    });

    function loadData() {
        vehiclesTable.html(`
            <tr>
                <td colspan="6" class="text-center text-muted">
                    ${t.loadingVehicles}
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
                .text(t.loadError);

            console.error("Failed to load admin vehicle data:", xhr);
        });
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
            return t.noOwners;
        }

        return ownerIds.map(function (id) {
            const user = users.find(u => u.id === id);

            return user
                ? `${user.firstName} ${user.lastName}`
                : translate(t.unknownUser, { id });
        }).join(", ");
    }

    function renderVehicles() {
        vehiclesTable.empty();

        if (!vehicles.length) {
            vehiclesTable.html(`
                <tr>
                    <td colspan="6" class="text-center text-muted">
                        ${t.noVehicles}
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
                title: t.editVehicle
            }).html('<i class="bi bi-pencil"></i>');

            editButton.on("click", function () {
                openEditModal(vehicle);
            });

            const deleteButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-danger",
                title: t.deleteVehicle
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
        emissionClassSelect.val("").trigger("change");

        formStatus.empty();

        $("#vehicle-modal-title").text(t.addVehicle);
        saveButton.text(t.createVehicle);

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

        emissionClassSelect
            .val(vehicle.emissionClass || "")
            .trigger("change");

        formStatus.empty();

        $("#vehicle-modal-title").text(t.editVehicle);
        saveButton.text(t.saveChanges);

        licencePlateInput.prop("disabled", false);

        vehicleModal.modal("show");
    }

    vehicleForm.on("submit", function (event) {
        event.preventDefault();

        const id = vehicleId.val();

        const vehicle = {
            make: makeInput.val().trim(),
            model: modelInput.val().trim(),
            year: Number(yearInput.val()),
            licencePlate: licencePlateInput.val().trim().toUpperCase(),
            ownerIds: (ownersSelect.val() || []).map(Number),
            emissionClass: emissionClassSelect.val() || null
        };

        if (!vehicle.make || !vehicle.model ||
            !vehicle.licencePlate || !vehicle.year) {
            showFormStatus(t.fillAllFields, "danger");
            return;
        }

        const isUpdate = Boolean(id);

        if (isUpdate) {
            vehicle.id = Number(id);
        }

        saveButton.prop("disabled", true)
            .text(isUpdate ? t.saving : t.creating);

        $.ajax({
            url: isUpdate ? "/rest/vehicle" : "/rest/vehicle/insert",
            method: isUpdate ? "PUT" : "POST",
            contentType: "application/json",
            data: JSON.stringify(vehicle)
        }).done(function () {
            vehicleModal.modal("hide");
            loadData();
        }).always(function () {
            saveButton.prop("disabled", false)
                .text(isUpdate ? t.saveChanges : t.createVehicle);
        });
    });

    function deleteVehicle(vehicle) {
        const confirmed = confirm(
            translate(t.deleteConfirmation, {
                plate: vehicle.licencePlate
            })
        );

        if (!confirmed) {
            return;
        }

        $.ajax({
            url: `/rest/vehicle/${vehicle.id}`,
            method: "DELETE"
        }).done(function () {
            loadData();
        });
    }

    function showFormStatus(message) {
        formStatus
            .removeClass("text-danger text-success")
            .addClass("text-success")
            .text(message);
    }

    function translate(template, values = {}) {
        return template.replace(/\{(\w+)\}/g, function (match, key) {
            return values[key] ?? match;
        });
    }

    loadData();
});
