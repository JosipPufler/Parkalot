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

    const saveButton = $("#save-user");
    const formStatus = $("#user-form-status");

    let users = [];

    loadUsers();

    // --------------------------------------------------
    // Load users
    // --------------------------------------------------

    function loadUsers() {
        usersStatus
            .removeClass("text-danger")
            .addClass("text-muted")
            .text("Loading users...");

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
                .text("Failed to load users.");

            console.error("Failed to load users:", xhr);
        });
    }

    // --------------------------------------------------
    // Render users
    // --------------------------------------------------

    function renderUsers() {
        usersTable.empty();

        if (!users.length) {
            usersStatus
                .removeClass("text-danger")
                .addClass("text-muted")
                .text("No users found.");

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

            roleBadge.text(user.role);
            row.append($("<td>").append(roleBadge));

            const actions = $("<td>").addClass("text-end text-nowrap");

            const editButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-primary me-2",
                title: "Edit user"
            }).html('<i class="bi bi-pencil"></i>');

            editButton.on("click", function () {
                openEditModal(user);
            });

            const deleteButton = $("<button>", {
                type: "button",
                class: "btn btn-sm btn-outline-danger",
                title: "Delete user"
            }).html('<i class="bi bi-trash"></i>');

            deleteButton.on("click", function () {
                deleteUser(user);
            });

            actions.append(editButton, deleteButton);
            row.append(actions);

            usersTable.append(row);
        });

        usersStatus.text(`${users.length} user(s) found.`);
    }

    // --------------------------------------------------
    // Edit modal
    // --------------------------------------------------

    function openEditModal(user) {
        userForm[0].reset();

        userId.val(user.id);
        firstNameInput.val(user.firstName);
        lastNameInput.val(user.lastName);
        emailInput.val(user.email);
        roleSelect.val(user.role);

        formStatus.empty();

        userModal.modal("show");
    }

    // --------------------------------------------------
    // Update user
    // --------------------------------------------------

    userForm.on("submit", function (event) {
        event.preventDefault();

        const user = {
            id: Number(userId.val()),
            firstName: firstNameInput.val().trim(),
            lastName: lastNameInput.val().trim(),
            email: emailInput.val().trim(),
            role: roleSelect.val()
        };

        if (!user.firstName || !user.lastName || !user.email) {
            showFormStatus("Please fill in all fields.", "danger");
            return;
        }

        saveButton.prop("disabled", true).text("Saving...");

        $.ajax({
            url: "/rest/users",
            method: "PUT",
            contentType: "application/json",
            data: JSON.stringify(user)
        }).done(function () {
            userModal.modal("hide");
            loadUsers();
        }).fail(function (xhr) {
            const message = xhr.responseJSON?.message
                || "Failed to update user.";

            showFormStatus(message, "danger");
        }).always(function () {
            saveButton.prop("disabled", false)
                .text("Save changes");
        });
    });

    // --------------------------------------------------
    // Delete user
    // --------------------------------------------------

    function deleteUser(user) {
        const confirmed = confirm(
            `Are you sure you want to delete ${user.firstName} ${user.lastName}?`
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

            alert(xhr.responseJSON?.message || "Failed to delete user.");
        });
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------

    function showFormStatus(message, type) {
        formStatus
            .removeClass("text-danger text-success")
            .addClass(type === "danger" ? "text-danger" : "text-success")
            .text(message);
    }
});