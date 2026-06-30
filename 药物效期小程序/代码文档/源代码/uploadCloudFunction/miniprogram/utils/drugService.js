const PAGE_SIZE = 100;

const OPEN_STATE_LABELS = {
  unopened: "未开封",
  opened: "已开封",
  independent: "独立包装"
};

function listDrugs(options = {}) {
  return callDatabase({
    action: "listDrugs",
    page: options.page || 1,
    pageSize: options.pageSize || PAGE_SIZE,
    filter: options.filter || {},
    sort: options.sort || "expiryCountdown"
  }).then(data => normalizeCloudItems(data.items || []));
}

function getDrug(id) {
  return callDatabase({
    action: "getDrug",
    id: String(id)
  }).then(data => normalizeCloudItem(data));
}

function createDrug(drug) {
  return callDatabase({
    action: "createDrug",
    drug: toCloudDrug(drug, drug.source || "manual")
  }).then(data => normalizeCloudItem(data.drug));
}

function batchCreateDrugs(drugs, options = {}) {
  const source = options.source || "unknown";
  return callDatabase({
    action: "batchCreateDrugs",
    drugs: drugs.map(item => toCloudDrug(item, source))
  }).then(data => normalizeCloudItems(data.items || []));
}

function updateDrug(id, drug) {
  return callDatabase({
    action: "updateDrug",
    id: String(id),
    drug: toCloudDrug(drug, drug.source || "manual")
  }).then(data => normalizeCloudItem(data));
}

function deleteDrug(id) {
  return callDatabase({
    action: "deleteDrug",
    id: String(id)
  });
}

function callDatabase(data) {
  return wx.cloud.callFunction({
    name: "database",
    data
  }).then(res => {
    const result = res.result || {};
    if (!result.success) {
      throw new Error(result.message || result.code || "database call failed");
    }
    return result.data;
  });
}

function toCloudDrug(drug, source) {
  return {
    name: String(drug.name || "").trim(),
    qty: String(drug.qty || "").trim(),
    dosageForm: drug.dosageForm || "other",
    category: drug.category || "other",
    openState: toCloudOpenState(drug.openState),
    location: drug.location || "medicine_box",
    expireDateText: drug.expireDate || drug.date || dateFromDays(drug.days),
    source,
    confidence: Number(drug.confidence || 0),
    note: drug.note || "",
    rawText: drug.rawText || drug.recognizedText || "",
    attachments: drug.attachments || { images: [], audios: [] }
  };
}

function normalizeCloudItems(items) {
  return (items || []).map(normalizeCloudItem).filter(Boolean);
}

function normalizeCloudItem(item) {
  if (!item) return null;
  const date = item.date || item.expireDate || item.expireDateText || "";
  const days = item.days === null || item.days === undefined ? getDaysRemaining(date) : Number(item.days);
  const status = item.status || item.expiryStatus || getStatus(date);

  return {
    id: item.id || item._id,
    name: item.name || "",
    qty: item.qty || "",
    dosageForm: item.dosageForm || "other",
    category: item.category || "other",
    openState: normalizeOpenStateLabel(item.openState),
    location: item.location || "medicine_box",
    date,
    days,
    status,
    confidence: item.ai && item.ai.confidence !== undefined ? item.ai.confidence : item.confidence,
    note: item.ai && item.ai.note ? item.ai.note : item.note,
    attachments: item.attachments || { images: [], audios: [] },
    createdAt: item.createdAt || Date.now(),
    updatedAt: item.updatedAt || Date.now(),
    source: item.source || "unknown"
  };
}

function normalizeOpenStateLabel(value) {
  if (OPEN_STATE_LABELS[value]) return OPEN_STATE_LABELS[value];
  if (value === "已开封" || value === "Opened" || value === "opened") return "已开封";
  if (value === "独立包装" || value === "Independent" || value === "independent") return "独立包装";
  return "未开封";
}

function toCloudOpenState(value) {
  if (value === "已开封" || value === "Opened" || value === "opened") return "opened";
  if (value === "独立包装" || value === "Independent" || value === "independent") return "independent";
  return "unopened";
}

function dateFromDays(days) {
  if (days === null || days === undefined || Number.isNaN(Number(days))) return "";
  const date = new Date();
  date.setDate(date.getDate() + Number(days));
  return formatDate(date);
}

function getDaysRemaining(dateText) {
  if (!dateText) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expireDate = new Date(dateText);
  expireDate.setHours(0, 0, 0, 0);
  return Math.floor((expireDate - today) / 86400000);
}

function getStatus(dateText) {
  const days = getDaysRemaining(dateText);
  if (days === null) return "unknown";
  return days < 0 ? "expired" : days <= 3 ? "expiring" : "normal";
}

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

module.exports = {
  listDrugs,
  getDrug,
  createDrug,
  batchCreateDrugs,
  updateDrug,
  deleteDrug,
  getDaysRemaining,
  getStatus
};
