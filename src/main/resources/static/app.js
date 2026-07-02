(function () {
  const { useEffect, useMemo, useState } = React;

  const fallbackLocations = Array.from({ length: 200 }, (_, index) => {
    const code = `LOC${String(index + 1).padStart(3, "0")}`;
    return {
      hertzLocationCode: code,
      displayName: `RentalApps Location ${String(index + 1).padStart(3, "0")}`,
      timeZone: ""
    };
  });

  function normalizeCustomerPayload(payload) {
    if (Array.isArray(payload)) {
      return payload;
    }
    if (Array.isArray(payload?.value)) {
      return payload.value;
    }
    return [];
  }

  async function fetchJson(url) {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    if (response.status === 204 || response.status === 404) {
      return [];
    }
    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`);
    }
    return response.json();
  }

  function App() {
    const [locations, setLocations] = useState([]);
    const [selectedLocation, setSelectedLocation] = useState("LOC001");
    const [customers, setCustomers] = useState([]);
    const [loadingLocations, setLoadingLocations] = useState(true);
    const [loadingCustomers, setLoadingCustomers] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
      let cancelled = false;

      async function loadLocations() {
        setLoadingLocations(true);
        try {
          const payload = await fetchJson("/admin/locations");
          const data = Array.isArray(payload?.data) && payload.data.length > 0
            ? payload.data
            : fallbackLocations;
          if (!cancelled) {
            setLocations(data);
            setSelectedLocation(data[0]?.hertzLocationCode || "LOC001");
          }
        } catch (loadError) {
          if (!cancelled) {
            setLocations(fallbackLocations);
            setSelectedLocation("LOC001");
          }
        } finally {
          if (!cancelled) {
            setLoadingLocations(false);
          }
        }
      }

      loadLocations();
      return () => {
        cancelled = true;
      };
    }, []);

    useEffect(() => {
      if (!selectedLocation) {
        return;
      }

      let cancelled = false;

      async function loadCustomers() {
        setLoadingCustomers(true);
        setError("");
        try {
          const payload = await fetchJson(`/StallDetails2?locationId=${encodeURIComponent(selectedLocation)}`);
          if (!cancelled) {
            setCustomers(normalizeCustomerPayload(payload));
          }
        } catch (loadError) {
          if (!cancelled) {
            setCustomers([]);
            setError("Customer details are unavailable for the selected location.");
          }
        } finally {
          if (!cancelled) {
            setLoadingCustomers(false);
          }
        }
      }

      loadCustomers();
      return () => {
        cancelled = true;
      };
    }, [selectedLocation]);

    const selectedLocationDetails = useMemo(
      () => locations.find((location) => location.hertzLocationCode === selectedLocation),
      [locations, selectedLocation]
    );

    const sortedCustomers = useMemo(
      () => [...customers].sort((left, right) => (left.customerName || "").localeCompare(right.customerName || "")),
      [customers]
    );

    return React.createElement(
      "main",
      { className: "app-shell" },
      React.createElement(
        "section",
        { className: "masthead" },
        React.createElement(
          "div",
          { className: "masthead-copy" },
          React.createElement("p", { className: "eyebrow" }, "Rental Applications"),
          React.createElement("h1", null, "Customer Stall Board"),
          React.createElement("p", { className: "summary" }, "Live location view for rental customer assignments.")
        ),
        React.createElement(
          "div",
          { className: "location-panel" },
          React.createElement("label", { htmlFor: "location-select" }, "Location"),
          React.createElement(
            "select",
            {
              id: "location-select",
              value: selectedLocation,
              disabled: loadingLocations,
              onChange: (event) => setSelectedLocation(event.target.value)
            },
            locations.map((location) =>
              React.createElement(
                "option",
                { key: location.hertzLocationCode, value: location.hertzLocationCode },
                `${location.hertzLocationCode} - ${location.displayName || location.hertzLocationCode}`
              )
            )
          ),
          React.createElement(
            "div",
            { className: "location-meta" },
            React.createElement("span", null, selectedLocationDetails?.timeZone || "Local database"),
            React.createElement("strong", null, `${sortedCustomers.length} records`)
          )
        )
      ),
      React.createElement(
        "section",
        { className: "content-band" },
        React.createElement(
          "div",
          { className: "toolbar" },
          React.createElement(
            "div",
            null,
            React.createElement("h2", null, selectedLocation || "Location"),
            React.createElement("p", null, selectedLocationDetails?.displayName || "")
          ),
          React.createElement("div", { className: "status-pill" }, loadingCustomers ? "Loading" : "Current")
        ),
        error && React.createElement("div", { className: "notice error" }, error),
        !error && !loadingCustomers && sortedCustomers.length === 0
          ? React.createElement("div", { className: "notice" }, "No customer records found for this location.")
          : React.createElement(CustomerTable, { customers: sortedCustomers, loading: loadingCustomers })
      )
    );
  }

  function CustomerTable({ customers, loading }) {
    const skeletonRows = Array.from({ length: 5 }, (_, index) => index);

    return React.createElement(
      "div",
      { className: "table-wrap" },
      React.createElement(
        "table",
        null,
        React.createElement(
          "thead",
          null,
          React.createElement(
            "tr",
            null,
            ["Customer", "Stall", "One Club", "RA", "Arrival", "Updated"].map((heading) =>
              React.createElement("th", { key: heading }, heading)
            )
          )
        ),
        React.createElement(
          "tbody",
          null,
          loading
            ? skeletonRows.map((row) =>
                React.createElement(
                  "tr",
                  { key: row },
                  Array.from({ length: 6 }, (_, cell) =>
                    React.createElement(
                      "td",
                      { key: cell },
                      React.createElement("span", { className: "skeleton" })
                    )
                  )
                )
              )
            : customers.map((customer) =>
                React.createElement(
                  "tr",
                  { key: customer.id || `${customer.locationCode}-${customer.customerName}-${customer.ra}` },
                  React.createElement("td", { className: "customer-name" }, customer.customerName || "-"),
                  React.createElement("td", null, React.createElement("span", { className: "stall-badge" }, customer.stall || "-")),
                  React.createElement("td", null, customer.oneClub || "-"),
                  React.createElement("td", null, customer.ra || "-"),
                  React.createElement("td", null, [customer.arrivalDate, customer.arrivalTime].filter(Boolean).join(" ") || "-"),
                  React.createElement("td", null, customer.updatedDateTime || customer.createdDateTime || "-")
                )
              )
        )
      )
    );
  }

  ReactDOM.createRoot(document.getElementById("root")).render(React.createElement(App));
})();
