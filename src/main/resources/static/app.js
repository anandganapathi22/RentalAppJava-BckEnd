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

  async function postJson(url, payload) {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });
    const text = await response.text();
    if (!response.ok) {
      throw new Error(text || `Request failed with status ${response.status}`);
    }
    return text ? JSON.parse(text) : {};
  }

  function App() {
    const [locations, setLocations] = useState([]);
    const [selectedLocation, setSelectedLocation] = useState("LOC001");
    const [customers, setCustomers] = useState([]);
    const [loadingLocations, setLoadingLocations] = useState(true);
    const [loadingCustomers, setLoadingCustomers] = useState(false);
    const [error, setError] = useState("");
    const [chatMessages, setChatMessages] = useState([
      {
        role: "assistant",
        content: "Ask a question about the selected location's rental records.",
        recordsUsed: 0
      }
    ]);
    const [chatQuestion, setChatQuestion] = useState("");
    const [chatLoading, setChatLoading] = useState(false);
    const [chatError, setChatError] = useState("");

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

    useEffect(() => {
      setChatMessages([
        {
          role: "assistant",
          content: "Ask a question about the selected location's rental records.",
          recordsUsed: 0
        }
      ]);
      setChatQuestion("");
      setChatError("");
    }, [selectedLocation]);

    const selectedLocationDetails = useMemo(
      () => locations.find((location) => location.hertzLocationCode === selectedLocation),
      [locations, selectedLocation]
    );

    const sortedCustomers = useMemo(
      () => [...customers].sort((left, right) => (left.customerName || "").localeCompare(right.customerName || "")),
      [customers]
    );

    async function submitChatQuestion(event) {
      event.preventDefault();
      const question = chatQuestion.trim();
      if (!question || chatLoading) {
        return;
      }

      setChatQuestion("");
      setChatError("");
      setChatLoading(true);
      setChatMessages((messages) => [...messages, { role: "user", content: question }]);

      try {
        const response = await postJson("/admin/ai/customer-query", {
          locationId: selectedLocation,
          question
        });
        setChatMessages((messages) => [
          ...messages,
          {
            role: "assistant",
            content: response.answer || "No answer returned.",
            recordsUsed: response.recordsUsed
          }
        ]);
      } catch (chatRequestError) {
        setChatError(chatRequestError.message);
      } finally {
        setChatLoading(false);
      }
    }

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
        { className: "workspace-band" },
        React.createElement(
          "div",
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
        ),
        React.createElement(ChatPanel, {
          selectedLocation,
          chatMessages,
          chatQuestion,
          chatLoading,
          chatError,
          onQuestionChange: setChatQuestion,
          onSubmit: submitChatQuestion
        })
      )
    );
  }

  function ChatPanel({
    selectedLocation,
    chatMessages,
    chatQuestion,
    chatLoading,
    chatError,
    onQuestionChange,
    onSubmit
  }) {
    return React.createElement(
      "aside",
      { className: "ai-panel" },
      React.createElement(
        "div",
        { className: "ai-panel-header" },
        React.createElement(
          "div",
          null,
          React.createElement("h2", null, "Rental AI"),
          React.createElement("p", null, selectedLocation || "No location selected")
        ),
        React.createElement("span", { className: "ai-model-pill" }, "Ollama")
      ),
      React.createElement(
        "div",
        { className: "chat-log" },
        chatMessages.map((message, index) =>
          React.createElement(
            "div",
            { key: `${message.role}-${index}`, className: `chat-message ${message.role}` },
            React.createElement("div", { className: "chat-bubble" }, message.content),
            message.role === "assistant" && message.recordsUsed > 0
              ? React.createElement("span", { className: "chat-meta" }, `${message.recordsUsed} records used`)
              : null
          )
        ),
        chatLoading
          ? React.createElement(
              "div",
              { className: "chat-message assistant" },
              React.createElement("div", { className: "chat-bubble loading-bubble" }, "Thinking...")
            )
          : null
      ),
      chatError ? React.createElement("div", { className: "notice error chat-error" }, chatError) : null,
      React.createElement(
        "form",
        { className: "chat-form", onSubmit },
        React.createElement("textarea", {
          value: chatQuestion,
          onChange: (event) => onQuestionChange(event.target.value),
          disabled: chatLoading,
          rows: 3,
          placeholder: "Who is assigned to stall A12?"
        }),
        React.createElement(
          "button",
          { type: "submit", disabled: chatLoading || !chatQuestion.trim() },
          chatLoading ? "Sending" : "Ask"
        )
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
