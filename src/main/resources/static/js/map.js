$(function () {
    const map = L.map("parking-map").setView([0, 0], 2);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    const areaLayer = L.featureGroup().addTo(map);
    let areas = [];
    const areaLayers = new Map();
    const activePermits = new Map();

    const status = $("#map-status");
    const count = $("#area-count");
    let selectedArea;

    $("#area-search").on("input", filterAreas);

    $("#show-areas").on("change", function () {
        if (this.checked) {
            map.addLayer(areaLayer);
        } else {
            map.removeLayer(areaLayer);
        }
    });

    $(document).on("vehicle:selected", function (event, vehicle) {
        loadVehiclePermits(vehicle);
    });

    window.areas = {
        getSelectedArea: function () {
            return selectedArea;
        }
    };

    function showAreaDetails(area, layer) {
        selectedArea = area;

        const popup = document.createElement("div");
        popup.style.textAlign = "center";

        const title = document.createElement("strong");
        title.textContent = area.name;

        const zone = document.createElement("div");
        zone.textContent = area.zone;

        const vehicleInfo = document.createElement("div");
        vehicleInfo.className = "mt-2";

        const permitInfo = document.createElement("div");
        permitInfo.className = "mt-2";

        const buyButton = document.createElement("button");
        buyButton.type = "button";
        buyButton.className = "btn btn-primary btn-sm mt-2";
        buyButton.textContent = "Buy permit";

        function updateVehicleInfo() {
            const vehicle = window.vehicles.getSelectedVehicle();

            vehicleInfo.textContent = vehicle
                ? `Vehicle: ${vehicle.licencePlate}`
                : "Select a vehicle first.";

            buyButton.disabled = !vehicle;

            const permit = activePermits.get(area.id);
            console.log(permit);
            if (permit) {
                permitInfo.textContent =
                    `Permit active until: ${permit.endDate}`;
                permitInfo.className = "mt-2 text-success";
            } else {
                permitInfo.textContent = "";
                permitInfo.className = "mt-2";
            }
        }

        updateVehicleInfo();

        buyButton.addEventListener("click", function () {
            const vehicle = window.vehicles.getSelectedVehicle();

            if (!vehicle) {
                return;
            }

            $("#permit-area-name").text(area.name);
            $("#permit-vehicle").text(vehicle.licencePlate);

            $("#permit-modal")
                .data("area-id", area.id)
                .data("vehicle-id", vehicle.id);
            $("#permit-message").empty();

            $("#permit-start-date")
                .val(getLocalDateString())
                .attr("min", getLocalDateString());

            $("#permit-duration").val("DAY").trigger("change");

            $("#permit-modal").modal("show");
        });

        $(document).on("vehicle:selected", updateVehicleInfo);

        popup.append(title, zone, vehicleInfo, permitInfo, buyButton);

        layer.bindPopup(popup).openPopup();
    }

    function drawAreas(data) {
        console.log(data);
        areaLayer.clearLayers();
        areas = data;

        data.forEach(area => {
            if (!area.coordinates?.length) {
                return;
            }

            // Leaflet expects [latitude, longitude].
            const coordinates = area.coordinates.map(point => [
                point.latitude,
                point.longitude
            ]);

            const polygon = L.polygon(coordinates, {
                color: "#0d6efd",
                weight: 2,
                fillColor: "#0d6efd",
                fillOpacity: 0.2
            });

            polygon.bindTooltip(area.name);
            areaLayers.set(area.id, polygon);
            polygon.on("click", () => showAreaDetails(area, polygon));

            // Keep the DTO associated with its layer for searching.
            polygon.areaData = area;
            areaLayer.addLayer(polygon);
        });

        count.text(`${data.length} areas`);
        status.text(
            data.length
                ? "Select a zone to view its details."
                : "No parking areas found."
        );

        if (areaLayer.getLayers().length) {
            map.fitBounds(areaLayer.getBounds(), {
                padding: [30, 30],
                maxZoom: 16
            });
        }
    }

    function filterAreas() {
        const query = $("#area-search").val().trim().toLowerCase();

        areaLayer.eachLayer(layer => {
            const matches = layer.areaData.name
                .toLowerCase()
                .includes(query);

            if (matches) {
                layer.addTo(map);
            } else {
                map.removeLayer(layer);
            }
        });
    }

    $.ajax({
        url: "/rest/area",
        method: "GET",
        dataType: "json"
    })
        .done(drawAreas)
        .fail(() => {
            count.text("Unavailable");
            status.text("Could not load parking areas. Please try again.");
        });


    // Admin area drawing
    const drawingLayer = L.featureGroup().addTo(map);

    let drawing = false;
    let vertices = [];
    let previewPolygon = null;

    const drawingStatus = $("#drawing-status");
    const startButton = $("#start-drawing");
    const undoButton = $("#undo-vertex");
    const clearButton = $("#clear-drawing");
    const saveButton = $("#save-area");
    let selectingPlateLocation = false;
    let selectedPlateLocation = null;

    function updateDrawingControls() {
        drawingStatus.text(
            vertices.length === 0
                ? "No vertices selected."
                : `${vertices.length} vertices selected. ${
                    drawing ? "Click the map to add more." : ""
                }`
        );

        undoButton.prop("disabled", vertices.length === 0);
        clearButton.prop("disabled", vertices.length === 0);
        saveButton.prop("disabled", vertices.length < 3);

        startButton
            .toggleClass("btn-outline-primary", !drawing)
            .toggleClass("btn-primary", drawing)
            .attr("aria-pressed", String(drawing))
            .text(drawing ? "Stop drawing" : "Start drawing");
    }

    function renderDrawing() {
        drawingLayer.clearLayers();

        vertices.forEach((point, index) => {
            L.circleMarker(point, {
                radius: 5,
                color: "#dc3545",
                fillColor: "#dc3545",
                fillOpacity: 1
            })
                .bindTooltip(`Vertex ${index + 1}`)
                .addTo(drawingLayer);
        });

        if (vertices.length >= 2) {
            previewPolygon = L.polygon(vertices, {
                color: "#dc3545",
                weight: 2,
                dashArray: "6 4",
                fillColor: "#dc3545",
                fillOpacity: 0.15
            }).addTo(drawingLayer);
        } else {
            previewPolygon = null;
        }

        updateDrawingControls();
    }

    function clearDrawing() {
        drawing = false;
        vertices = [];
        previewPolygon = null;
        drawingLayer.clearLayers();
        map.getContainer().style.cursor = "";
        updateDrawingControls();
    }

    startButton.on("click", function () {
        if (drawing) {
            drawing = false;
            map.getContainer().style.cursor = "";
        } else {
            drawing = true;
            map.getContainer().style.cursor = "crosshair";
        }

        updateDrawingControls();
    });

    $("#start-plate-scan").on("click", function () {
        if (drawing) {
            // Use your existing function to exit area drawing mode.
            stopDrawing();
        }

        selectingPlateLocation = true;
        selectedPlateLocation = null;

        areaLayers.forEach(polygon => {
            polygon.getElement()?.style.setProperty("pointer-events", "none");
        });
        map.getContainer().style.cursor = "crosshair";
        showNotification("Click the vehicle's location on the map.", "info");
    });

    $(document).on("keydown", function (event) {
        if (event.key === "Escape" && drawing) {
            drawing = false;
            map.getContainer().style.cursor = "";
            updateDrawingControls();
        }
    });

    map.on("click", function (event) {
        if (selectingPlateLocation) {
            selectingPlateLocation = false;
            areaLayers.forEach(polygon => {
                polygon.getElement()?.style.removeProperty("pointer-events");
            });
            map.getContainer().style.cursor = "";

            selectedPlateLocation = event.latlng;

            $("#plate-scan-coordinates").text(
                `${event.latlng.lat.toFixed(6)}, ${event.latlng.lng.toFixed(6)}`
            );

            $("#plate-scan-image").val("");
            $("#plate-scan-result").empty();

            $("#plate-scan-modal").modal("show");

            return;
        }

        if (!drawing) return;

        vertices.push([
            event.latlng.lat,
            event.latlng.lng
        ]);

        renderDrawing();
    });

    undoButton.on("click", function () {
        vertices.pop();
        renderDrawing();
    });

    clearButton.on("click", clearDrawing);

    saveButton.on("click", function () {
        const name = $("#new-area-name").val().trim();
        const zone = $("#new-area-zone").val();

        if (!name || !zone) {
            drawingStatus.text("Enter an area name and select a zone.");
            return;
        }

        if (vertices.length < 3) {
            drawingStatus.text("A polygon needs at least 3 vertices.");
            return;
        }

        // Close the drawing mode before saving.
        drawing = false;
        map.getContainer().style.cursor = "";
        updateDrawingControls();

        const areaDto = {
            name: name,
            zone: zone,
            coordinates: vertices.map(([latitude, longitude]) => ({
                latitude,
                longitude
            }))
        };
        console.log(areaDto)
        saveButton.prop("disabled", true).text("Saving...");

        $.ajax({
            url: "/rest/area",
            method: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify(areaDto)
        })
            .done(function () {
                clearDrawing();
                $("#new-area-name").val("");
                $("#new-area-zone").val("");
                drawingStatus.text("Area created successfully.");

                // Refresh the map from the server.
                $.ajax({
                    url: "/rest/area",
                    method: "GET",
                    dataType: "json"
                })
                    .done(drawAreas)
                    .fail(function () {
                        drawingStatus.text(
                            "Area saved, but refreshing the map failed."
                        );
                    });
            })
            .fail(function (xhr) {
                drawing = false;
                drawingStatus.text(
                    xhr.responseJSON?.message ||
                    `Could not save area (HTTP ${xhr.status}).`
                );
            })
            .always(function () {
                saveButton.text("Save area");
                updateDrawingControls();
            });
    });

    function getLocalDateString() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, "0");
        const day = String(now.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function calculateEndDate(startDate, duration) {
        const date = new Date(startDate + "T12:00:00");
        const originalDay = date.getDate();

        switch (duration) {
            case "DAY":
                date.setDate(date.getDate() + 1);
                break;
            case "WEEK":
                date.setDate(date.getDate() + 7);
                break;
            case "MONTH":
                date.setDate(1);
                date.setMonth(date.getMonth() + 1);
                date.setDate(Math.min(
                    originalDay,
                    new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
                ));
                break;
            case "YEAR":
                date.setDate(1);
                date.setFullYear(date.getFullYear() + 1);
                date.setDate(Math.min(
                    originalDay,
                    new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
                ));
                break;
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function updatePermitEndDate() {
        const startDate = $("#permit-start-date").val();
        const duration = $("#permit-duration").val();

        $("#permit-end-date").text(
            startDate ? calculateEndDate(startDate, duration) : "—"
        );
    }

    $("#permit-start-date, #permit-duration").on("change", updatePermitEndDate);
    $("#permit-modal [data-dismiss='modal']").on("click", function () {
        $("#permit-modal").modal("hide");
    });

    function loadVehiclePermits(vehicle) {
        // Reset the previous vehicle's permit information.
        activePermits.clear();

        areaLayers.forEach(polygon => {
            polygon.setStyle({
                color: "#3388ff",
                fillColor: "#3388ff",
                fillOpacity: 0.2
            });
        });

        if (!vehicle) {
            return;
        }

        $.ajax({
            url: "/rest/permits/" + encodeURIComponent(vehicle.licencePlate),
            method: "GET",
            dataType: "json",

            success: function (permits) {
                permits
                    .filter(permit => permit.active)
                    .forEach(permit => {
                        const areaId = permit.areaDto.id;

                        activePermits.set(areaId, permit);

                        const polygon = areaLayers.get(areaId);

                        if (polygon) {
                            polygon.setStyle({
                                color: "#198754",
                                fillColor: "#28a745",
                                fillOpacity: 0.5
                            });
                        }
                    });

                if (selectedArea) {
                    const polygon = areaLayers.get(selectedArea.id);

                    if (polygon && polygon.isPopupOpen()) {
                        showAreaDetails(selectedArea, polygon);
                    }
                }
            },

            error: function (xhr) {
                console.error(
                    "Failed to load vehicle permits:",
                    xhr.status
                );
            }
        });
    }


    function showNotification(message, type = "success") {
        const alert = $("<div>")
            .addClass(`alert alert-${type} alert-dismissible fade show shadow`)
            .attr("role", "alert")
            .text(message);

        const closeButton = $("<button>")
            .attr({
                type: "button",
                "data-dismiss": "alert",
                "aria-label": "Close"
            })
            .addClass("close")
            .append($("<span>").attr("aria-hidden", "true").html("&times;"));

        alert.append(closeButton);
        $("#notification-container").append(alert);

        setTimeout(function () {
            alert.alert("close");
        }, 5000);
    }
// PLATE

    $("#plate-scan-modal [data-dismiss='modal']").on("click", function () {
        $("#plate-scan-modal").modal("hide");
    });

    $("#plate-scan-form").on("submit", function (e) {
        e.preventDefault();

        if (!selectedPlateLocation) {
            showNotification("Select a location on the map first.", "warning");
            return;
        }

        const fileInput = $("#plate-scan-image")[0];

        if (!fileInput.files.length) {
            showNotification("Please select an image.", "warning");
            return;
        }

        const formData = new FormData();
        formData.append("image", fileInput.files[0]);
        formData.append("latitude", selectedPlateLocation.lat);
        formData.append("longitude", selectedPlateLocation.lng);

        const button = $("#submit-plate-scan");

        button.prop("disabled", true).text("Processing...");
        $("#plate-scan-result").empty();

        $.ajax({
            url: "/rest/officer/scan",
            method: "POST",
            data: formData,
            processData: false,
            contentType: false,

            success: function (result) {
                $("#plate-scan-result").empty().append(
                    $("<div>")
                        .addClass("alert alert-success")
                        .text("Recognized licence plate: " +
                            (result.violationDto.licencePlate || "No plate recognized"))
                );
            },

            error: function (xhr) {
                const message = xhr.responseJSON?.message
                    || xhr.responseJSON?.error
                    || "Failed to process the image.";

                $("#plate-scan-result").empty().append(
                    $("<div>").addClass("alert alert-danger").text(message)
                );
            },

            complete: function () {
                button.prop("disabled", false).text("Upload and scan");
            }
        });
    });

    updateDrawingControls();
});