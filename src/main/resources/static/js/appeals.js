$(document).ready(function () {
    const container = $("#appeals-container");
    const status = $("#appeals-status");

    const userRole = container.data("user-role");
    const isAdmin = userRole === "ADMIN";
    const canResolveAppeals = userRole === "OFFICER" || isAdmin;

    const appealModal = $("#appeal-modal");
    const appealForm = $("#appeal-admin-form");
    const saveButton = $("#save-appeal");
    const formStatus = $("#appeal-form-status");

    const violationSelect = $("#appeal-violation");
    const ownerSelect = $("#appeal-owner");

    let allAppeals = [];
    let violations = [];
    let vehicles = [];
    let users = [];

    let currentFilter = "all";
    let pendingAppealId = null;
    let pendingApprove = null;
    let pendingDeleteAppealId = null;

    $(document).on("click", "[data-appeal-filter]", function () {
        currentFilter = $(this).attr("data-appeal-filter");

        $("[data-appeal-filter]")
            .removeClass("btn-primary active")
            .addClass("btn-outline-primary");

        $(this)
            .removeClass("btn-outline-primary")
            .addClass("btn-primary active");

        rerenderAppeals(allAppeals);
    });

    $(document).on("click", "[data-resolve-appeal]", function () {
        pendingAppealId = Number($(this).attr("data-appeal-id"));
        pendingApprove = $(this).attr("data-resolve-appeal") === "approve";

        const action = pendingApprove ? "approve" : "reject";

        $("#resolveAppealModalTitle").text(
            `${action.charAt(0).toUpperCase() + action.slice(1)} appeal`
        );

        $("#resolveAppealModalBody").text(
            `Are you sure you want to ${action} appeal #${pendingAppealId}?`
        );

        $("#confirmResolveAppeal")
            .removeClass("btn-success btn-danger")
            .addClass(pendingApprove ? "btn-success" : "btn-danger")
            .text(pendingApprove ? "Approve" : "Reject");

        $("#resolveAppealModal").modal("show");
    });

    $(document).on("click", "[data-collapse-target]", function () {
        const target = $($(this).attr("data-collapse-target"));
        target.collapse("toggle");
    });

    $(document).on("shown.bs.collapse", ".collapse", function () {
        $(this).find("[data-evidence-loaded]").each(function () {
            loadAppealEvidence($(this));
        });
    });

    $("#create-appeal").on("click", function () {
        appealForm[0].reset();

        $("#appeal-id").val("");
        $("#appeal-modal-title").text("Create Appeal");
        $("#appeal-edit-fields").hide();
        $("#appeal-evidence-group").show();
        $("#appeal-owner-group").show();

        populateViolationSelect();
        populateOwnersSelect(null);

        formStatus.empty();
        saveButton.text("Create Appeal");

        appealModal.modal("show");
    });

    $("#confirm-delete-appeal").on("click", function () {
        if (pendingDeleteAppealId === null) {
            return;
        }

        const appealId = pendingDeleteAppealId;
        const button = $(this);

        button.prop("disabled", true).text("Deleting...");

        $.ajax({
            url: `/rest/violations/appeal/${appealId}`,
            method: "DELETE"
        })
            .done(function () {
                $("#delete-appeal-modal").modal("hide");
                loadData();

                showToast(
                    "success",
                    "Appeal deleted",
                    `Appeal #${appealId} was deleted successfully.`
                );
            })
            .fail(function (xhr) {
                $("#delete-appeal-modal").modal("hide");

                showToast(
                    "danger",
                    "Delete failed",
                    xhr.responseText || `Could not delete appeal #${appealId}.`
                );
            })
            .always(function () {
                button.prop("disabled", false).text("Delete");
                pendingDeleteAppealId = null;
            });
    });

    $("#confirmResolveAppeal").on("click", function () {
        if (pendingAppealId === null || pendingApprove === null) {
            return;
        }

        resolveAppeal(pendingAppealId, pendingApprove);
    });

    function loadData() {
        status.removeClass("text-danger").addClass("text-muted")
            .text("Loading appeals...");

        const requests = [
            $.ajax({
                url: "/rest/violations/appeal",
                method: "GET",
                dataType: "json"
            })
        ];

        if (isAdmin) {
            requests.push(
                $.ajax({
                    url: "/rest/violations",
                    method: "GET",
                    dataType: "json"
                }),
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
            );
        }

        $.when(...requests)
            .done(function (...responses) {
                allAppeals = responses[0][0] || [];

                if (isAdmin) {
                    violations = responses[1][0] || [];
                    vehicles = responses[2][0] || [];
                    users = responses[3][0] || [];

                    populateViolationSelect();
                }

                rerenderAppeals(allAppeals);
            })
            .fail(function (xhr) {
                status.removeClass("text-muted")
                    .addClass("text-danger")
                    .text(xhr.status === 401
                        ? "Please log in to view appeals."
                        : "Failed to load appeals.");
            });
    }

    function getVehicleForViolation(violationId) {
        const violation = violations.find(
            item => Number(violation.id) === Number(violationId)
        );

        if (!violation) {
            return null;
        }

        return vehicles.find(
            vehicle => vehicle.licencePlate === violation.id
        ) || null;
    }

    function getUserName(user) {
        return `${user.firstName} ${user.lastName} (${user.email})`;
    }

    function formatDate(value) {
        if (!value) {
            return "N/A";
        }

        return new Date(value).toLocaleString();
    }

    function toDateTimeLocal(value) {
        return value ? value.substring(0, 16) : "";
    }

    function populateViolationSelect() {
        violationSelect.empty()
            .append(new Option("Select a violation", ""));

        violations.forEach(function (violation) {
            const id = violation.id;
            const plate = violation.licencePlate || "Unknown vehicle";

            violationSelect.append(
                new Option(`Violation #${id} - ${plate}`, id)
            );
        });
    }

    function populateOwnersSelect(violationId) {
        ownerSelect.empty();

        if (!violationId) {
            ownerSelect.append(
                new Option("Select a violation first", "")
            );

            ownerSelect.prop("disabled", true);
            return;
        }

        const vehicle = getVehicleForViolation(violationId);

        if (!vehicle || !vehicle.ownerIds || vehicle.ownerIds.length === 0) {
            ownerSelect.append(
                new Option("No owners found for this vehicle", "")
            );

            ownerSelect.prop("disabled", true);
            return;
        }

        const ownerIds = vehicle.ownerIds.map(Number);

        const vehicleOwners = users.filter(
            user => ownerIds.includes(Number(user.id))
        );

        ownerSelect.append(
            new Option("Select an owner", "")
        );

        vehicleOwners.forEach(function (user) {
            ownerSelect.append(
                new Option(getUserName(user), user.id)
            );
        });

        ownerSelect.prop("disabled", vehicleOwners.length === 0);
    }

    violationSelect.on("change", function () {
        populateOwnersSelect($(this).val());
    });

    function rerenderAppeals(appeals) {
        allAppeals = appeals || [];
        container.empty();

        const filteredAppeals = allAppeals.filter(function (appeal) {
            if (currentFilter === "active") {
                return !appeal.resolved;
            }

            if (currentFilter === "inactive") {
                return appeal.resolved;
            }

            return true;
        });

        if (filteredAppeals.length === 0) {
            status.text(
                allAppeals.length === 0
                    ? "There are no appeals."
                    : "No appeals match the selected filter."
            );
            return;
        }

        const groups = {};

        filteredAppeals.forEach(function (appeal) {
            const plate = appeal.licencePlate || "Unknown vehicle";

            if (!groups[plate]) {
                groups[plate] = [];
            }

            groups[plate].push(appeal);
        });

        status.text(
            `${filteredAppeals.length} appeal(s) across ${Object.keys(groups).length} vehicle(s).`
        );

        Object.entries(groups).forEach(function ([plate, items], index) {
            const collapseId = `vehicle-appeals-${index}`;

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
                .text(`${items.length} appeal(s)`);

            header.append(toggle, count);

            const body = $("<div>")
                .addClass("collapse")
                .attr("id", collapseId);

            const bodyInner = $("<div>").addClass("card-body");

            items.forEach(function (appeal) {
                bodyInner.append(createAppealCard(appeal));
            });

            body.append(bodyInner);
            card.append(header, body);
            container.append(card);
        });
    }

    function createAppealCard(appeal) {
        const card = $("<div>").addClass("card mb-3");
        const body = $("<div>").addClass("card-body");

        body.append(
            $("<h5>")
                .addClass("card-title")
                .text(appeal.title)
        );

        const details = $("<div>").addClass("mb-3");

        addDetail(details, "Appeal ID", appeal.appealId);
        addDetail(details, "Description", appeal.description);
        addDetail(details, "Violation ID", appeal.violationId);
        addDetail(details, "Issued at", formatDate(appeal.issuedAt));
        addDetail(details, "Deadline", formatDate(appeal.deadline));

        const appealStatus = appeal.rejected
            ? "Rejected"
            : appeal.resolved
                ? "Approved"
                : "Not resolved";

        addDetail(details, "Status", appealStatus);

        body.append(details);

        // Evidence
        const evidenceSection = $("<div>")
            .addClass("border-top pt-3 mt-3");

        $("<h5>")
            .addClass("mb-3")
            .text("Evidence")
            .appendTo(evidenceSection);

        const evidenceList = $("<div>")
            .addClass("list-group")
            .attr("data-evidence-loaded", "false")
            .attr("data-appeal-id", appeal.appealId);

        $("<p>")
            .addClass("text-muted mb-0")
            .text("Expand to load evidence.")
            .appendTo(evidenceList);

        evidenceSection.append(evidenceList);
        body.append(evidenceSection);

        // Admin CRUD buttons
        if (isAdmin) {
            const adminActions = $("<div>")
                .addClass("d-flex flex-wrap mt-3");

            const editButton = $("<button>")
                .addClass("btn btn-primary btn-sm mr-2 mb-2")
                .attr("type", "button")
                .text("Edit")
                .on("click", function () {
                    openEditModal(appeal);
                });

            const deleteButton = $("<button>")
                .addClass("btn btn-danger btn-sm mb-2")
                .attr("type", "button")
                .text("Delete")
                .on("click", function () {
                    openDeleteModal(appeal);
                });

            adminActions.append(editButton, deleteButton);
            body.append(adminActions);
        }

        // Officer/Admin resolution buttons
        if (canResolveAppeals) {
            const actions = $("<div>").addClass("d-flex mt-2");

            const approveButton = $("<button>")
                .addClass("btn btn-success btn-sm mr-2")
                .attr("type", "button")
                .attr("data-resolve-appeal", "approve")
                .attr("data-appeal-id", appeal.appealId)
                .prop("disabled", appeal.resolved)
                .text("Approve");

            const rejectButton = $("<button>")
                .addClass("btn btn-danger btn-sm")
                .attr("type", "button")
                .attr("data-resolve-appeal", "reject")
                .attr("data-appeal-id", appeal.appealId)
                .prop("disabled", appeal.resolved)
                .text("Reject");

            if (appeal.resolved) {
                approveButton.attr(
                    "title",
                    "This appeal has already been resolved."
                );

                rejectButton.attr(
                    "title",
                    "This appeal has already been resolved."
                );
            }

            actions.append(approveButton, rejectButton);
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

        row.append(document.createTextNode(value ?? "N/A"));
        parent.append(row);
    }

    function openEditModal(appeal) {
        appealForm[0].reset();

        $("#appeal-id").val(appeal.appealId);
        $("#appeal-title").val(appeal.title);
        $("#appeal-description").val(appeal.description);

        violationSelect.val(String(appeal.violationId));

        $("#appeal-deadline").val(appeal.deadline || "");
        $("#appeal-issued-at").val(toDateTimeLocal(appeal.issuedAt));
        $("#appeal-resolved").prop("checked", appeal.resolved);

        $("#appeal-modal-title").text(`Edit Appeal #${appeal.appealId}`);
        $("#appeal-edit-fields").show();

        // These fields are only used when creating an appeal.
        $("#appeal-evidence-group").hide();
        $("#appeal-owner-group").hide();

        formStatus.empty();
        saveButton.text("Save Changes");

        appealModal.modal("show");
    }

    appealForm.on("submit", function (event) {
        event.preventDefault();

        if (!isAdmin) {
            return;
        }

        const appealId = $("#appeal-id").val();

        saveButton.prop("disabled", true).text("Saving...");
        formStatus.empty();

        if (appealId) {
            updateAppeal(appealId);
        } else {
            createAppeal();
        }
    });

    function createAppeal() {
        const violationId = violationSelect.val();
        const ownerId = ownerSelect.val();

        if (!violationId || !ownerId) {
            formStatus
                .addClass("text-danger")
                .text("Select a violation and an owner.");

            saveButton.prop("disabled", false).text("Create Appeal");
            return;
        }

        const formData = new FormData();

        formData.append("violationId", violationId);
        formData.append("ownerId", ownerId);
        formData.append("title", $("#appeal-title").val());
        formData.append("message", $("#appeal-description").val());

        const evidenceFiles = $("#appeal-evidence")[0].files;

        for (let i = 0; i < evidenceFiles.length; i++) {
            formData.append("evidence", evidenceFiles[i]);
        }

        $.ajax({
            url: "/rest/violations/appeal/insert",
            method: "POST",
            data: formData,
            processData: false,
            contentType: false
        })
            .done(function () {
                appealModal.modal("hide");
                loadData();

                showToast(
                    "success",
                    "Appeal created",
                    "The appeal was created successfully."
                );
            })
            .fail(function (xhr) {
                formStatus
                    .removeClass("text-success")
                    .addClass("text-danger")
                    .text(xhr.responseText || "Failed to create appeal.");
            })
            .always(function () {
                saveButton.prop("disabled", false).text("Create Appeal");
            });
    }

    function updateAppeal(appealId) {
        const appealData = {
            id: Number(appealId),
            title: $("#appeal-title").val(),
            description: $("#appeal-description").val(),
            violationId: Number(violationSelect.val()),
            deadline: $("#appeal-deadline").val() || null,
            issuedAt: $("#appeal-issued-at").val() || null,
            resolved: $("#appeal-resolved").is(":checked")
        };

        $.ajax({
            url: "/rest/violations/appeal",
            method: "PUT",
            contentType: "application/json",
            data: JSON.stringify(appealData)
        })
            .done(function () {
                appealModal.modal("hide");
                loadData();

                showToast(
                    "success",
                    "Appeal updated",
                    `Appeal #${appealId} was updated successfully.`
                );
            })
            .fail(function (xhr) {
                formStatus
                    .removeClass("text-success")
                    .addClass("text-danger")
                    .text(xhr.responseText || "Failed to update appeal.");
            })
            .always(function () {
                saveButton.prop("disabled", false).text("Save Changes");
            });
    }

    function openDeleteModal(appeal) {
        pendingDeleteAppealId = appeal.appealId;

        $("#delete-appeal-message").text(
            `Are you sure you want to delete appeal #${appeal.appealId}?`
        );

        $("#delete-appeal-modal").modal("show");
    }

    function resolveAppeal(appealId, approve) {
        const action = approve ? "approve" : "reject";

        const url = approve
            ? `/rest/violations/appeal/resolve/${appealId}`
            : `/rest/violations/appeal/reject/${appealId}`;

        const confirmButton = $("#confirmResolveAppeal");

        confirmButton.prop("disabled", true).text("Processing...");

        $.ajax({
            url: url,
            method: "PUT"
        })
            .done(function () {
                const appeal = allAppeals.find(
                    item => Number(item.appealId) === Number(appealId)
                );

                if (appeal) {
                    appeal.resolved = true;
                    appeal.rejected = !approve;
                }

                $("#resolveAppealModal").modal("hide");
                rerenderAppeals(allAppeals);

                showToast(
                    "success",
                    "Appeal resolved",
                    `Appeal #${appealId} was ${action === "approve" ? "approved" : "rejected"} successfully.`
                );
            })
            .fail(function (xhr) {
                $("#resolveAppealModal").modal("hide");

                showToast(
                    "danger",
                    "Resolution failed",
                    xhr.responseText || `Could not ${action} appeal.`
                );
            })
            .always(function () {
                confirmButton.prop("disabled", false).text("Confirm");
                pendingAppealId = null;
                pendingApprove = null;
            });
    }

    function loadAppealEvidence(evidenceList) {
        const appealId = evidenceList.attr("data-appeal-id");

        if (evidenceList.attr("data-evidence-loaded") === "true") {
            return;
        }

        evidenceList.empty().append(
            $("<p>")
                .addClass("text-muted mb-0")
                .text("Loading evidence...")
        );

        $.ajax({
            url: `/rest/violations/appeal/${appealId}/evidence`,
            method: "GET",
            dataType: "json"
        })
            .done(function (evidence) {
                evidenceList.empty();

                if (!evidence || evidence.length === 0) {
                    $("<p>")
                        .addClass("text-muted mb-0")
                        .text("No evidence attached.")
                        .appendTo(evidenceList);
                } else {
                    evidence.forEach(function (file) {
                        const item = $("<div>")
                            .addClass("list-group-item d-flex justify-content-between align-items-center flex-wrap");

                        const fileInfo = $("<div>")
                            .addClass("d-flex align-items-center mr-3")
                            .css("min-width", "0");

                        $("<span>")
                            .addClass("mr-2")
                            .text("📎")
                            .appendTo(fileInfo);

                        $("<span>")
                            .addClass("text-break")
                            .text(file.filename || `Evidence ${file.id}`)
                            .appendTo(fileInfo);

                        const downloadLink = $("<a>")
                            .addClass("btn btn-outline-primary btn-sm mt-1 mb-1")
                            .attr(
                                "href",
                                `/rest/violations/appeal/${encodeURIComponent(file.id)}`
                            )
                            .attr("download", file.filename || "")
                            .text("Download");

                        item.append(fileInfo, downloadLink);
                        evidenceList.append(item);
                    });
                }

                evidenceList.attr("data-evidence-loaded", "true");
            })
            .fail(function () {
                evidenceList.empty().append(
                    $("<p>")
                        .addClass("text-danger mb-0")
                        .text("Failed to load evidence. Expand again to retry.")
                );
            });
    }

    loadData();
});