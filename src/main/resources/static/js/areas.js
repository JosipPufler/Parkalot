$(function () {
    const map = L.map("parking-map").setView([0, 0], 2);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    const areaLayer = L.featureGroup().addTo(map);
    let areas = [];

    const status = $("#map-status");
    const count = $("#area-count");

    function showAreaDetails(area, layer) {
        const popup = document.createElement("div");
        const title = document.createElement("strong");
        const id = document.createElement("div");
        id.style.textAlign = "center";

        title.textContent = area.name;
        id.textContent = `${area.zone}`;

        popup.append(title, id);

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

    $("#area-search").on("input", filterAreas);

    $("#show-areas").on("change", function () {
        if (this.checked) {
            map.addLayer(areaLayer);
        } else {
            map.removeLayer(areaLayer);
        }
    });

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
        startButton.prop("disabled", drawing);
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
        if (drawing) return;

        drawing = true;
        map.getContainer().style.cursor = "crosshair";
        updateDrawingControls();
    });

    map.on("click", function (event) {
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

    updateDrawingControls();
});