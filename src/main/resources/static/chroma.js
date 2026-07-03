const state = {
  tenant: "default_tenant",
  database: "default_database",
  collections: [],
  selected: null
};

const elements = {
  status: document.getElementById("status"),
  refreshButton: document.getElementById("refreshButton"),
  tenantInput: document.getElementById("tenantInput"),
  databaseInput: document.getElementById("databaseInput"),
  createCollectionForm: document.getElementById("createCollectionForm"),
  collectionNameInput: document.getElementById("collectionNameInput"),
  collectionCount: document.getElementById("collectionCount"),
  collectionsList: document.getElementById("collectionsList"),
  selectedTitle: document.getElementById("selectedTitle"),
  recordCount: document.getElementById("recordCount"),
  loadRecordsButton: document.getElementById("loadRecordsButton"),
  recordsOutput: document.getElementById("recordsOutput")
};

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(text || `${response.status} ${response.statusText}`);
  }
  return text ? JSON.parse(text) : null;
}

function syncInputs() {
  state.tenant = elements.tenantInput.value.trim() || "default_tenant";
  state.database = elements.databaseInput.value.trim() || "default_database";
}

function collectionBaseUrl() {
  const tenant = encodeURIComponent(state.tenant);
  const database = encodeURIComponent(state.database);
  return `/api/chroma/tenants/${tenant}/databases/${database}/collections`;
}

async function loadCollections() {
  syncInputs();
  elements.status.textContent = "Connecting...";
  const identity = await requestJson("/api/chroma/identity");
  const data = await requestJson(collectionBaseUrl());
  const collections = Array.isArray(data) ? data : data.value || [];
  state.collections = collections;
  state.selected = collections.find((item) => state.selected && item.id === state.selected.id) || null;
  elements.status.textContent = `Connected as ${identity.tenant || state.tenant}`;
  renderCollections();
  renderSelected();
}

function renderCollections() {
  elements.collectionCount.textContent = String(state.collections.length);
  elements.collectionsList.innerHTML = "";

  if (state.collections.length === 0) {
    const empty = document.createElement("div");
    empty.className = "collectionItem";
    empty.textContent = "No collections yet.";
    elements.collectionsList.appendChild(empty);
    return;
  }

  state.collections.forEach((collection) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `collectionItem${state.selected && state.selected.id === collection.id ? " active" : ""}`;
    button.innerHTML = `
      <span class="collectionName">${escapeHtml(collection.name)}</span>
      <span class="collectionMeta">${escapeHtml(collection.id || "")}</span>
    `;
    button.addEventListener("click", () => selectCollection(collection));
    elements.collectionsList.appendChild(button);
  });
}

async function selectCollection(collection) {
  state.selected = collection;
  renderCollections();
  renderSelected();
  await loadCount();
}

function renderSelected() {
  if (!state.selected) {
    elements.selectedTitle.textContent = "Select a collection";
    elements.recordCount.textContent = "";
    elements.loadRecordsButton.disabled = true;
    elements.recordsOutput.textContent = "No collection selected.";
    return;
  }
  elements.selectedTitle.textContent = state.selected.name;
  elements.recordCount.textContent = state.selected.dimension ? `dimension ${state.selected.dimension}` : "";
  elements.loadRecordsButton.disabled = false;
  elements.recordsOutput.textContent = JSON.stringify(state.selected, null, 2);
}

async function loadCount() {
  if (!state.selected) {
    return;
  }
  const count = await requestJson(`${collectionBaseUrl()}/${encodeURIComponent(state.selected.id)}/count`);
  elements.recordCount.textContent = `${count} records`;
}

async function loadRecords() {
  if (!state.selected) {
    return;
  }
  elements.recordsOutput.textContent = "Loading records...";
  const records = await requestJson(`${collectionBaseUrl()}/${encodeURIComponent(state.selected.id)}/get`, {
    method: "POST",
    body: JSON.stringify({
      limit: 25,
      offset: 0,
      include: ["documents", "metadatas"]
    })
  });
  elements.recordsOutput.textContent = JSON.stringify(records, null, 2);
}

async function createCollection(event) {
  event.preventDefault();
  syncInputs();
  const name = elements.collectionNameInput.value.trim();
  if (!name) {
    return;
  }
  await requestJson(collectionBaseUrl(), {
    method: "POST",
    body: JSON.stringify({
      name,
      get_or_create: true,
      metadata: {
        created_by: "rental-applications-chroma-admin"
      }
    })
  });
  elements.collectionNameInput.value = "";
  await loadCollections();
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function run(action) {
  try {
    await action();
  } catch (error) {
    elements.status.textContent = "Chroma request failed";
    elements.recordsOutput.textContent = error.message;
  }
}

elements.refreshButton.addEventListener("click", () => run(loadCollections));
elements.loadRecordsButton.addEventListener("click", () => run(loadRecords));
elements.createCollectionForm.addEventListener("submit", (event) => run(() => createCollection(event)));
elements.tenantInput.addEventListener("change", () => run(loadCollections));
elements.databaseInput.addEventListener("change", () => run(loadCollections));

run(loadCollections);
