
$(function () {
    const usersTable = $("#users-table-body");
    const usersStatus = $("#users-status");

    const userForm = $("#user-admin-form");
    const userModal = $("#user-modal");

    const userId = $("#user-id");
    const firstNameInput = $("#user-first-name");
    const lastNameInput = $("#user-last-name");
    const emailInput = $("#user-email");
    const roleSelect = $("#user-role");

    const streetInput = $("#user-street");
    const houseNumberInput = $("#user-house-number");
    const cityInput = $("#user-city");
    const postalCodeInput = $("#user-postal-code");
    const countryInput = $("#user-country");

    const saveButton = $("#save-user");
    const formStatus = $("#user-form-status");

    const t = window.i18n.users;

    let users = [];

    $("#user-modal [data-dismiss='modal']").on("click", function () {
        userModal.modal("hide");
    });

    function loadUsers() {
        usersStatus
            .removeClass("text-danger")
            .addClass("text-muted")
            .text(t.loadingUsers);

        usersTable.empty();

        $.ajax({
            url: "/rest/users",
            method: "GET",
            dataType: "json"
        }).done(function (data) {
            users = data;
            renderUsers();
        }).fail(function (xhr) {
            usersStatus
                .removeClass("text-muted")
                .addClass("text-danger")
                .text(t.loadError);

            console.error("Failed to load users:", xhr);
        });
    }

    function renderUsers() {
        usersTable.empty();

        if (!users.length) {
            usersStatus
                .removeClass("text-danger")
                .addClass("text-muted")
                .text(t.noUsers);

            return;
        }

        users.forEach(function (user) {
            const row = $("<tr>");

            row.append(
                $("<td>").text(`${user.firstName} ${user.lastName}`)
            );

            row.append($("<td>").text(user.email));

            const roleBadge = $("<span>", {
                class: "badge"
            });

            switch (user.role) {
                case "ADMIN":
                    roleBadge.addClass("badge-danger");
                    break;
                case "OFFICER":
                    roleBadge.addClass("badge-warning");
                    break;
                default:
                    roleBadge.addClass("badge-primary");
            }

            roleBadge.text(getRoleName(user.role));
            row.append($("<td>").append(roleBadge));

            const actions = $("<td>").addClass("text-end text-nowrap");

            const editButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-primary me-2",
                title: t.editUser
            }).html('<i class="bi bi-pencil"></i>');

            editButton.on("click", function () {
                openEditModal(user);
            });

            const deleteButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-danger",
                title: t.deleteUser
            }).html('<i class="bi bi-trash"></i>');

            deleteButton.on("click", function () {
                deleteUser(user);
            });

            actions.append(editButton, deleteButton);
            row.append(actions);

            usersTable.append(row);
        });

        usersStatus.text(
            translate(t.userCount, { count: users.length })
        );
    }

    function getRoleName(role) {
        const roles = {
            ADMIN: t.roleAdminLabel,
            OFFICER: t.roleOfficerLabel,
            USER: t.roleUserLabel
        };

        return roles[role] || role;
    }

    function openEditModal(user) {
        userForm[0].reset();

        const household = user.household;

        userId.val(user.id);
        firstNameInput.val(user.firstName);
        lastNameInput.val(user.lastName);
        emailInput.val(user.email);
        roleSelect.val(user.role);

        // Populate household fields.
        streetInput.val(household?.street ?? "");
        houseNumberInput.val(household?.houseNumber ?? "");
        cityInput.val(household?.city ?? "");
        postalCodeInput.val(household?.postalCode ?? "");
        countryInput.val(household?.country ?? "");

        formStatus.empty();

        saveButton.text(t.saveChanges);

        userModal.modal("show");
    }

    userForm.on("submit", function (event) {
        event.preventDefault();

        const id = Number(userId.val());

        const user = {
            id: id,
            firstName: firstNameInput.val().trim(),
            lastName: lastNameInput.val().trim(),
            email: emailInput.val().trim(),
            role: roleSelect.val(),

            household: {
                id: users.find(u => u.id === id)?.household?.id ?? null,
                street: streetInput.val().trim(),
                houseNumber: houseNumberInput.val().trim(),
                city: cityInput.val().trim(),
                postalCode: postalCodeInput.val().trim(),
                country: countryInput.val().trim()
            }
        };

        if (
            !user.firstName ||
            !user.lastName ||
            !user.email ||
            !user.household.street ||
            !user.household.houseNumber ||
            !user.household.city ||
            !user.household.postalCode ||
            !user.household.country
        ) {
            showFormStatus(t.fillAllFields, "danger");
            return;
        }

        saveButton.prop("disabled", true).text(t.saving);

        $.ajax({
            url: "/rest/users",
            method: "PUT",
            contentType: "application/json",
            data: JSON.stringify(user)
        }).done(function () {
            userModal.modal("hide");
            loadUsers();

            if (typeof showToast === "function") {
                showToast(t.updateSuccess, "success");
            }
        }).fail(function (xhr) {
            const message = xhr.responseJSON?.message
                || t.updateError;

            showFormStatus(message, "danger");
        }).always(function () {
            saveButton.prop("disabled", false)
                .text(t.saveChanges);
        });
    });

    function deleteUser(user) {
        const confirmed = confirm(
            translate(t.deleteConfirmation, {
                name: `${user.firstName} ${user.lastName}`
            })
        );

        if (!confirmed) {
            return;
        }

        $.ajax({
            url: `/rest/users/${user.id}`,
            method: "DELETE"
        }).done(function () {
            loadUsers();
        }).fail(function (xhr) {
            console.error("Failed to delete user:", xhr);

            alert(xhr.responseJSON?.message || t.deleteError);
        });
    }

    function showFormStatus(message, type) {
        formStatus
            .removeClass("text-danger text-success")
            .addClass(type === "danger" ? "text-danger" : "text-success")
            .text(message);
    }

    function translate(template, values = {}) {
        return template.replace(/\{(\w+)\}/g, function (match, key) {
            return values[key] ?? match;
        });
    }

    loadUsers();
});
