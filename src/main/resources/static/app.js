const apiStatus = document.querySelector('#apiStatus');
const usCount = document.querySelector('#usCount');
const euCount = document.querySelector('#euCount');
const totalCount = document.querySelector('#totalCount');
const topicBand = document.querySelector('#topicBand');
const recentEvents = document.querySelector('#recentEvents');
const displayRows = document.querySelector('#displayRows');
const auditRows = document.querySelector('#auditRows');
const publishResult = document.querySelector('#publishResult');

async function fetchJson(url, options = {}) {
  const response = await fetch(url, {
    headers: {'Content-Type': 'application/json'},
    ...options
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || response.statusText);
  }
  return response.json();
}

function setStatus(ok) {
  apiStatus.classList.toggle('ok', ok);
}

function renderTopics(topics) {
  topicBand.innerHTML = topics.map(topic => `
    <article class="topic-card">
      <b>${topic.name}</b>
      <span>${topic.description}</span>
      <strong>${topic.eventCount}</strong>
    </article>
  `).join('');
}

function renderRecent(events) {
  recentEvents.innerHTML = events.length ? events.map(event => `
    <article class="event-item">
      <b>${event.topic}</b> <span>${event.eventType || ''}</span><br>
      ${event.locationCode || '-'} ${event.customerName || ''}<br>
      <span>${event.source} / ${event.region} / ${new Date(event.timestamp).toLocaleString()}</span>
    </article>
  `).join('') : '<p>No local events published yet.</p>';
}

function renderDisplay(rows) {
  displayRows.innerHTML = rows.length ? rows.slice(0, 80).map(row => `
    <tr>
      <td>${row.locationCode || ''}</td>
      <td>${row.customerName || ''}</td>
      <td>${row.stall || ''}</td>
      <td>${row.ra || ''}</td>
    </tr>
  `).join('') : '<tr><td colspan="4">No records found.</td></tr>';
}

function renderAudit(rows) {
  auditRows.innerHTML = rows.length ? rows.map(row => `
    <article class="audit-item">
      <b>${row.operation || 'event'}</b> ${row.customerName || ''}<br>
      ${row.locationCode || '-'} / ${row.stall || '-'} / ${row.sourceSystem || '-'}<br>
      <span>${row.operationTime || ''}</span>
    </article>
  `).join('') : '<p>No audit events found.</p>';
}

async function loadOverview() {
  try {
    const data = await fetchJson('/api/architecture/overview');
    usCount.textContent = data.usCustomerCount;
    euCount.textContent = data.euCustomerCount;
    totalCount.textContent = data.totalCustomerCount;
    renderTopics(data.topics || []);
    renderRecent(data.recentEvents || []);
    setStatus(true);
  } catch (error) {
    setStatus(false);
    recentEvents.innerHTML = `<p>${error.message}</p>`;
  }
}

async function loadDisplay() {
  const region = document.querySelector('#displayRegion').value;
  const locationId = document.querySelector('#displayLocation').value.trim();
  const query = locationId ? `?locationId=${encodeURIComponent(locationId)}` : '';
  const rows = await fetchJson(`/api/architecture/display/${region}${query}`);
  renderDisplay(rows);
}

async function loadAudit() {
  const rows = await fetchJson('/api/architecture/audit?limit=25');
  renderAudit(rows);
}

document.querySelector('#refreshBtn').addEventListener('click', () => {
  loadOverview();
  loadDisplay();
  loadAudit();
});

document.querySelector('#loadDisplayBtn').addEventListener('click', loadDisplay);
document.querySelector('#loadAuditBtn').addEventListener('click', loadAudit);

document.querySelector('#eventForm').addEventListener('submit', async event => {
  event.preventDefault();
  publishResult.textContent = '';
  const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
  try {
    const result = await fetchJson('/api/architecture/events', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    publishResult.textContent = `${result.inboundTopic} -> ${result.displayTopic}`;
    await loadOverview();
    await loadDisplay();
    await loadAudit();
  } catch (error) {
    publishResult.textContent = error.message;
  }
});

loadOverview();
loadDisplay();
loadAudit();
