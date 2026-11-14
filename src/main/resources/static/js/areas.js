
$(document).ready(function () {
    const map = L.map("area-map").setView([45, 15], 5);

    const t = window.i18n.areas;
    const eligibilityT = window.i18n?.map?.admin?.area || {};

    const locale = document.documentElement.lang === "hr"
        ? "hr-HR"
        : "en-GB";

    const pluralRules = new Intl.PluralRules(locale);

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

    function translate(template, values = {}) {
        return template.replace(/\{(\w+)\}/g, function (match, key) {
            return values[key] ?? match;
        });
    }

    function getCountText(count, type) {
        const category = pluralRules.select(count);
        const key = `${type}Count${category.charAt(0).toUpperCase()}${category.slice(1)}`;

        return translate(t[key] || t[`${type}CountOther`], { count });
    }

    function formatZone(zone) {
        const zones = {
            ZONE_1: t.zone1,
            ZONE_2: t.zone2,
            ZONE_3: t.zone3
        };

        return zones[zone] || zone || t.notAvailable;
    }

    // Add eligibility controls to the form.
    // This keeps the existing HTML template compatible.
    function createEligibilityControls() {
        if ($("#requires-resident-address").length) {
            return;
        }

        const section = $("<div>")
            .addClass("border rounded p-3 mb-3");

        $("<h6>")
            .addClass("mb-3")
            .text(eligibilityT.eligibility || "Permit eligibility requirements")
            .appendTo(section);

        const addressGroup = $("<div>")
            .addClass("form-check mb-3");

        $("<input>")
            .attr({
                type: "checkbox",
                id: "requires-resident-address"
            })
            .addClass("form-check-input")
            .appendTo(addressGroup);

        $("<label>")
            .attr("for", "requires-resident-address")
            .addClass("form-check-label")
            .text(
                eligibilityT.requiresResidentAddress ||
                "Requires resident address"
            )
            .appendTo(addressGroup);

        section.append(addressGroup);

        const vehiclesGroup = $("<div>")
            .addClass("form-group");

        $("<label>")
            .attr("for", "max-vehicles-per-household")
            .text(
                eligibilityT.maxVehicles ||
                "Maximum vehicles per household"
            )
            .appendTo(vehiclesGroup);

        $("<input>")
            .attr({
                type: "number",
                id: "max-vehicles-per-household",
                min: 1,
                step: 1
            })
            .addClass("form-control")
            .appendTo(vehiclesGroup);

        $("<small>")
            .addClass("form-text text-muted")
            .text(
                eligibilityT.maxVehiclesInfo ||
                "Leave empty for no limit."
            )
            .appendTo(vehiclesGroup);

        section.append(vehiclesGroup);

        const emissionGroup = $("<div>")
            .addClass("form-group mb-0");

        $("<label>")
            .attr("for", "minimum-emission-class")
            .text(
                eligibilityT.minimumEmissionClass ||
                "Minimum emission class"
            )
            .appendTo(emissionGroup);

        const emissionSelect = $("<select>")
            .attr("id", "minimum-emission-class")
            .addClass("form-control");

        $("<option>")
            .val("")
            .text("—")
            .appendTo(emissionSelect);

        for (let i = 1; i <= 7; i++) {
            $("<option>")
                .val(`EURO_${i}`)
                .text(`Euro ${i}`)
                .appendTo(emissionSelect);
        }

        emissionGroup.append(emissionSelect);
        section.append(emissionGroup);

        $("#area-zone").closest(".form-group").after(section);
    }

    createEligibilityControls();

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

        const maxVehiclesInput = $("#max-vehicles-per-household");
        const maxVehiclesValue = maxVehiclesInput.val();

        if (!name || !zone) {
            showToast(
                "warning",
                t.missingInformation,
                t.enterNameAndZone
            );
            return;
        }

        if (coordinates.length < 3) {
            showToast(
                "warning",
                t.invalidPolygon,
                t.minimumThreePoints
            );
            return;
        }

        if (maxVehiclesValue !== "" && !maxVehiclesInput[0].checkValidity()) {
            maxVehiclesInput[0].reportValidity();
            return;
        }

        const rule = {
            requiresResidentAddress: $("#requires-resident-address").is(":checked"),
            maxVehiclesPerHousehold: maxVehiclesValue === ""
                ? null
                : Number(maxVehiclesValue),
            emissionClass: $("#minimum-emission-class").val() || null
        };

        const dto = {
            id: selectedAreaId,
            name: name,
            zone: zone,
            coordinates: coordinates,
            rule: rule
        };

        const isUpdate = selectedAreaId !== null;
        const saveButton = $("#save-area");

        saveButton.prop("disabled", true);

        $.ajax({
            url: "/rest/area",
            method: isUpdate ? "PUT" : "POST",
            contentType: "application/json",
            data: JSON.stringify(dto),

            success: function () {
                showToast(
                    "success",
                    t.areaSaved,
                    isUpdate
                        ? t.areaUpdated
                        : t.areaCreated
                );

                loadAreas();
            },

            error: function (xhr) {
                showToast(
                    "error",
                    t.saveFailed,
                    xhr.responseText || `HTTP ${xhr.status}`
                );
            },

            complete: function () {
                saveButton.prop("disabled", false);
            }
        });
    });

    // Delete selected area.
    $("#delete-area").on("click", function () {
        if (selectedAreaId === null) {
            return;
        }

        const id = selectedAreaId;

        if (!confirm(translate(t.deleteConfirmation, { id }))) {
            return;
        }

        $.ajax({
            url: `/rest/area/${id}`,
            method: "DELETE",

            success: function () {
                showToast(
                    "success",
                    t.areaDeleted,
                    t.areaDeleteSuccess
                );

                loadAreas();
            },

            error: function (xhr) {
                showToast(
                    "error",
                    t.deleteFailed,
                    xhr.responseText || `HTTP ${xhr.status}`
                );
            }
        });
    });

    function loadAreas() {
        $("#map-status").text(t.loadingAreas);

        $.ajax({
            url: "/rest/area",
            method: "GET",
            dataType: "json",

            success: function (result) {
                areas = result || [];

                renderAreaSelect();
                renderAreaLayers();

                $("#map-status").text(
                    getCountText(areas.length, "area")
                );

                if (areas.length > 0) {
                    const bounds = areaLayers.getBounds();

                    if (bounds.isValid()) {
                        map.fitBounds(bounds, {
                            padding: [30, 30]
                        });
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
                $("#map-status").text(t.loadFailed);

                showToast(
                    "error",
                    t.loadingFailed,
                    xhr.responseText || `HTTP ${xhr.status}`
                );
            }
        });
    }

    function renderAreaSelect() {
        areaSelect.empty().append(
            $("<option>").val("").text(t.selectArea)
        );

        areas.forEach(function (area) {
            areaSelect.append(
                $("<option>")
                    .val(area.id)
                    .text(`${area.name} (${formatZone(area.zone)})`)
            );
        });

        areaSelect
            .val(selectedAreaId ?? "")
            .trigger("change.select2");
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
                `${area.name} (${formatZone(area.zone)})`
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
        $("#area-zone").val(area.zone).trigger("change.select2");

        const rule = area.rule || {};

        $("#requires-resident-address").prop(
            "checked",
            rule.requiresResidentAddress === true
        );

        $("#max-vehicles-per-household").val(
            rule.maxVehiclesPerHousehold ?? ""
        );

        $("#minimum-emission-class").val(
            rule.emissionClass ?? ""
        );

        coordinates = (area.coordinates || []).map(point => ({
            latitude: point.latitude,
            longitude: point.longitude
        }));

        areaSelect.val(area.id);

        $("#save-area").text(t.updateArea);
        $("#delete-area").prop("disabled", false);

        renderCoordinates();
        renderSelectedGeometry();
    }

    function resetForm() {
        selectedAreaId = null;
        coordinates = [];

        $("#area-form")[0].reset();
        $("#area-id").val("");

        $("#requires-resident-address").prop("checked", false);
        $("#max-vehicles-per-household").val("");
        $("#minimum-emission-class").val("");

        areaSelect.val("");

        $("#area-zone").trigger("change.select2");

        $("#save-area").text(t.createArea);
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
                {
                    draggable: true,
                    icon: L.divIcon({
                        className: "custom-marker",
                        html: '<div class="marker-dot"></div>',
                        iconSize: [16, 16],
                        iconAnchor: [8, 8]
                    })
                }
            );

            marker.bindTooltip(
                translate(t.pointNumber, { number: index + 1 })
            );

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
                .text(t.remove)
                .on("click", function () {
                    coordinates.splice(index, 1);

                    renderCoordinates();
                    renderSelectedGeometry();
                });

            row.append(description, removeButton);
            coordinateList.append(row);
        });

        $("#coordinate-count").text(
            getCountText(coordinates.length, "point")
        );
    }

    function toLeafletCoordinates(points) {
        return (points || []).map(point => [
            point.latitude,
            point.longitude
        ]);
    }
});
