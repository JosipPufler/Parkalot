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
        url: "/rest/public/area",
        method: "GET",
        dataType: "json"
    })
        .done(drawAreas)
        .fail(() => {
            count.text("Unavailable");
            status.text("Could not load parking areas. Please try again.");
        });
});