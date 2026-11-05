$(document).ready(function () {
    const map = L.map("area-map").setView([45, 15], 5);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    const areaSelect = $("#area-select");
    const coordinateList = $("#coordinate-list");

    let areas = [];
    let selectedAreaId = null;
    let coordinates = [];

    const areaLayers = L.featureGroup().addTo(map);
    const selectedGeometry = L.featureGroup().addTo(map);
    const vertexMarkers = L.featureGroup().addTo(map);

    const zoneColors = {
        ZONE_1: "#dc3545",
        ZONE_2: "#fd7e14",
        ZONE_3: "#198754"
    };

    // Initial load
    loadAreas();

    // Select an existing area.
    areaSelect.on("change", function () {
        const id = $(this).val();

        if (!id) {
            resetForm();
            return;
        }

        const area = areas.find(a => String(a.id) === String(id));

        if (area) {
            editArea(area);
        }
    });

    // Start creating a new area.
    $("#new-area").on("click", function () {
        resetForm();
    });

    // Add a point by clicking the map.
    map.on("click", function (event) {
        coordinates.push({
            latitude: event.latlng.lat,
            longitude: event.latlng.lng
        });

        renderCoordinates();
        renderSelectedGeometry();
    });

    // Save area.
    $("#area-form").on("submit", function (event) {
        event.preventDefault();

        const name = $("#area-name").val().trim();
        const zone = $("#area-zone").val();

        if (!name || !zone) {
            showToast("warning", "Missing information",
                "Please enter an area name and select a zone.");
            return;
        }

        if (coordinates.length < 3) {
            showToast("warning", "Invalid polygon",
                "An area must contain at least three points.");
            return;
        }

        const dto = {
            id: selectedAreaId,
            name: name,
            zone: zone,
            coordinates: coordinates
        };

        const isUpdate = selectedAreaId !== null;

        $.ajax({
            url: "/rest/area",
            method: isUpdate ? "PUT" : "POST",
            contentType: "application/json",
            data: JSON.stringify(dto),

            success: function (response) {
                showToast(
                    "success",
                    "Area saved",
                    isUpdate
                        ? "Area updated successfully."
                        : "Area created successfully."
                );

                loadAreas();
            },

            error: function (xhr) {
                showToast(
                    "error",
                    "Save failed",
                    xhr.responseText || `HTTP ${xhr.status}`
                );
            }
        });
    });

    // Delete selected area.
    $("#delete-area").on("click", function () {
        if (selectedAreaId === null) {
            return;
        }

        const id = selectedAreaId;

        if (!confirm("Are you sure you want to delete this area?")) {
            return;
        }

        $.ajax({
            url: `/rest/area/${id}`,
            method: "DELETE",

            success: function () {
                showToast("success", "Area deleted",
                    "The area was deleted successfully.");

                loadAreas();
            },

            error: function (xhr) {
                showToast(
                    "error",
                    "Delete failed",
                    xhr.responseText || `HTTP ${xhr.status}`
                );
            }
        });
    });

    function loadAreas() {
        $("#map-status").text("Loading areas...");

        $.ajax({
            url: "/rest/area",
            method: "GET",
            dataType: "json",

            success: function (result) {
                areas = result || [];

                renderAreaSelect();
                renderAreaLayers();

                $("#map-status").text(
                    `${areas.length} area(s) loaded`
                );

                if (areas.length > 0) {
                    const bounds = areaLayers.getBounds();

                    if (bounds.isValid()) {
                        map.fitBounds(bounds, { padding: [30, 30] });
                    }
                }

                if (selectedAreaId !== null) {
                    const updatedArea = areas.find(
                        a => String(a.id) === String(selectedAreaId)
                    );

                    if (updatedArea) {
                        editArea(updatedArea);
                    } else {
                        resetForm();
                    }
                }
            },

            error: function (xhr) {
                $("#map-status").text("Failed to load areas.");

                showToast(
                    "error",
                    "Loading failed",
                    xhr.responseText || `HTTP ${xhr.status}`
                );
            }
        });
    }

    function renderAreaSelect() {
        areaSelect.empty().append(
            $("<option>").val("").text("Select an area...")
        );

        areas.forEach(function (area) {
            areaSelect.append(
                $("<option>")
                    .val(area.id)
                    .text(`${area.name} (${area.zone})`)
            );
        });

        areaSelect.val(selectedAreaId ?? "");
    }

    function renderAreaLayers() {
        areaLayers.clearLayers();

        areas.forEach(function (area) {
            const points = toLeafletCoordinates(area.coordinates);

            if (points.length < 3) {
                return;
            }

            const polygon = L.polygon(points, {
                color: zoneColors[area.zone] || "#3388ff",
                weight: 2,
                fillOpacity: 0.2
            });

            polygon.bindTooltip(
                `${area.name} (${area.zone})`
            );

            polygon.on("click", function () {
                editArea(area);
            });

            areaLayers.addLayer(polygon);
        });
    }

    function editArea(area) {
        selectedAreaId = area.id;

        $("#area-id").val(area.id);
        $("#area-name").val(area.name);
        $("#area-zone").val(area.zone);

        coordinates = (area.coordinates || []).map(point => ({
            latitude: point.latitude,
            longitude: point.longitude
        }));

        areaSelect.val(area.id);

        $("#save-area").text("Update area");
        $("#delete-area").prop("disabled", false);

        renderCoordinates();
        renderSelectedGeometry();
    }

    function resetForm() {
        selectedAreaId = null;
        coordinates = [];

        $("#area-form")[0].reset();
        $("#area-id").val("");

        areaSelect.val("");

        $("#save-area").text("Create area");
        $("#delete-area").prop("disabled", true);

        renderCoordinates();
        renderSelectedGeometry();
    }

    function renderSelectedGeometry() {
        selectedGeometry.clearLayers();
        vertexMarkers.clearLayers();

        const points = toLeafletCoordinates(coordinates);

        if (points.length >= 3) {
            selectedGeometry.addLayer(
                L.polygon(points, {
                    color: "#007bff",
                    weight: 3,
                    fillOpacity: 0.3
                })
            );
        } else if (points.length >= 2) {
            selectedGeometry.addLayer(
                L.polyline(points, {
                    color: "#007bff",
                    weight: 3
                })
            );
        }

        coordinates.forEach(function (point, index) {
            const marker = L.marker(
                [point.latitude, point.longitude],
                { draggable: true }
            );

            marker.bindTooltip(`Point ${index + 1}`);

            marker.on("dragend", function (event) {
                const position = event.target.getLatLng();

                coordinates[index] = {
                    latitude: position.lat,
                    longitude: position.lng
                };

                renderCoordinates();
                renderSelectedGeometry();
            });

            vertexMarkers.addLayer(marker);
        });
    }

    function renderCoordinates() {
        coordinateList.empty();

        coordinates.forEach(function (point, index) {
            const row = $("<div>")
                .addClass(
                    "list-group-item d-flex justify-content-between align-items-center"
                );

            const description = $("<span>").text(
                `${index + 1}. ${point.latitude.toFixed(6)}, ${point.longitude.toFixed(6)}`
            );

            const removeButton = $("<button>")
                .addClass("btn btn-sm btn-outline-danger")
                .attr("type", "button")
                .text("Remove")
                .on("click", function () {
                    coordinates.splice(index, 1);

                    renderCoordinates();
                    renderSelectedGeometry();
                });

            row.append(description, removeButton);
            coordinateList.append(row);
        });

        $("#coordinate-count").text(
            `${coordinates.length} point(s)`
        );
    }

    function toLeafletCoordinates(points) {
        return (points || []).map(point => [
            point.latitude,
            point.longitude
        ]);
    }
});