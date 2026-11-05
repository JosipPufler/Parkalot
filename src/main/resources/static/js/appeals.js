$(document).ready(function () {
    const container = $("#appeals-container");
    const status = $("#appeals-status");
    const appealsContainer = document.getElementById("appeals-container");
    const userRole = appealsContainer.dataset.userRole;
    let allAppeals = [];
    let currentFilter = "all";
    let pendingAppealId = null;
    let pendingApprove = null;

    const canResolveAppeals = userRole === "OFFICER" || userRole === "ADMIN";

    $.ajax({
        url: "/rest/violations/appeal",
        method: "GET",
        dataType: "json",
        success: function (appeal) {
            rerenderAppeals(appeal);
        },
        error: function (xhr) {
            status
                .removeClass("text-muted")
                .addClass("text-danger")
                .text(xhr.status === 401
                    ? "Please log in to view your appeals."
                    : "Failed to load appeals.");
        }
    });

    $(document).on("click", "[data-collapse-target]", function () {
        const target = $($(this).attr("data-collapse-target"));
        target.collapse("toggle");
    });

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

    $(document).on("shown.bs.collapse", ".collapse", function () {
        $(this).find("[data-evidence-loaded]").each(function () {
            loadAppealEvidence($(this));
        });
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

    $("#confirmResolveAppeal").on("click", function () {
        if (pendingAppealId === null || pendingApprove === null) {
            return;
        }

        resolveAppeal(pendingAppealId, pendingApprove);
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
                    ? "You have no appeals."
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
        const card = $("<div>")
            .addClass("card mb-3");

        const body = $("<div>")
            .addClass("card-body");

        body.append(
            $("<h5>")
                .addClass("card-title")
                .text(appeal.title)
        );

        body.append(
            $("<p>")
                .addClass("card-text")
                .text(appeal.description)
        );

        body.append(
            $("<p>")
                .addClass("text-muted")
                .text(`Violation ID: ${appeal.violationId}`)
        );

        const details = $("<div>").addClass("mb-4");

        addDetail(details, "Appeal ID", appeal.appealId);
        addDetail(details, "Description", appeal.description);
        addDetail(details, "Violation ID", appeal.violationId);
        addDetail(details, "Issued at", formatDate(appeal.issuedAt));
        addDetail(details, "Deadline", formatDate(appeal.deadline));
        addDetail(details, "Status", appeal.rejected ? "Rejected" : appeal.resolved ? "Approved" : "Not resolved");

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
        body.append(details, evidenceSection);

        if (canResolveAppeals) {
            const actions = $("<div>")
                .addClass("d-flex mt-3");

            const approveButton = $("<button>")
                .addClass("btn btn-success mr-2")
                .attr("type", "button")
                .attr("data-resolve-appeal", "approve")
                .attr("data-appeal-id", appeal.appealId)
                .prop("disabled", appeal.resolved)
                .text("Approve");

            const rejectButton = $("<button>")
                .addClass("btn btn-danger")
                .attr("type", "button")
                .attr("data-resolve-appeal", "reject")
                .attr("data-appeal-id", appeal.appealId)
                .prop("disabled", appeal.resolved)
                .text("Reject");

            if (appeal.resolved) {
                approveButton.attr("title", "This appeal has already been resolved.");
                rejectButton.attr("title", "This appeal has already been resolved.");
            }

            actions.append(approveButton, rejectButton);
            body.append(actions);
        }

        card.append(body);

        return card;
    }


    function addDetail(parent, label, value) {
        const row = $("<p>").addClass("mb-1");
        $("<strong>").text(`${label}: `).appendTo(row);
        row.append(document.createTextNode(value ?? "N/A"));
        parent.append(row);
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
            method: "PUT",

            success: function () {
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
                    `Appeal #${appealId} was ${approve ? "approved" : "rejected"} successfully.`
                );
            },

            error: function (xhr) {
                $("#resolveAppealModal").modal("hide");

                showToast(
                    "danger",
                    "Resolution failed",
                    xhr.responseText || `Could not ${action} appeal. HTTP ${xhr.status}.`
                );
            },

            complete: function () {
                confirmButton.prop("disabled", false);
                pendingAppealId = null;
                pendingApprove = null;
                confirmButton.text("Confirm");
            }
        });
    }

    function loadAppealEvidence(evidenceList) {
        const appealId = evidenceList.attr("data-appeal-id");

        if (evidenceList.attr("data-evidence-loaded") === "true") {
            return;
        }

        evidenceList
            .empty()
            .append(
                $("<p>")
                    .addClass("text-muted mb-0")
                    .text("Loading evidence...")
            );

        $.ajax({
            url: `/rest/violations/appeal/${appealId}/evidence`,
            method: "GET",
            dataType: "json",

            success: function (evidence) {
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
                            .attr("href", `/rest/violations/appeal/${encodeURIComponent(file.id)}`)
                            .attr("download", file.filename || "")
                            .text("Download");

                        item.append(fileInfo, downloadLink);
                        evidenceList.append(item);
                    });
                }

                evidenceList.attr("data-evidence-loaded", "true");
            },

            error: function () {
                evidenceList.empty().append(
                    $("<p>")
                        .addClass("text-danger mb-0")
                        .text("Failed to load evidence. Expand again to retry.")
                );
            }
        });
    }
});