const cloud = require("wx-server-sdk");

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

const db = cloud.database();
const _ = db.command;
const DRUGS_COLLECTION = "drugs";
const USERS_COLLECTION = "users";
const MAX_PAGE_SIZE = 50;
const CATEGORY_VALUES = ["antipyretic", "antibiotic", "digestive", "vitamin", "external", "other"];
const OPEN_STATE_VALUES = ["unopened", "opened", "independent"];
const DOSAGE_FORM_VALUES = ["bottle", "blister", "pouch", "tube", "other"];
const SOURCE_VALUES = ["manual", "vision", "voice", "migration", "unknown"];

exports.main = async (event = {}) => {
  try {
    const wxContext = cloud.getWXContext();
    const openid = wxContext.OPENID;

    switch (event.action) {
      case "listDrugs":
        return ok(await listDrugs(event, openid));
      case "getDrug":
        return ok(await getDrug(event, openid));
      case "createDrug":
        return ok(await createDrug(event, openid));
      case "batchCreateDrugs":
        return ok(await batchCreateDrugs(event, openid));
      case "updateDrug":
        return ok(await updateDrug(event, openid));
      case "deleteDrug":
        return ok(await deleteDrug(event, openid));
      case "getFileTempUrls":
        return ok(await getFileTempUrls(event, openid));
      case "deleteCloudFiles":
        return ok(await deleteCloudFiles(event, openid));
      case "refreshExpirySnapshots":
        return ok(await refreshExpirySnapshots(event, openid));
      case "getUserProfile":
        return ok(await getUserProfile(openid));
      case "updateUserProfile":
        return ok(await updateUserProfile(event, openid));
      default:
        return fail("UNKNOWN_ACTION", "Unknown database action.");
    }
  } catch (err) {
    console.error("database function error:", sanitizeError(err));
    return fail(err.code || "DATABASE_ERROR", err.publicMessage || "Database service is temporarily unavailable.");
  }
};

async function listDrugs(event, openid) {
  const page = Math.max(1, Number(event.page || 1));
  const pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, Number(event.pageSize || 20)));
  const filter = normalizeListFilter(event.filter);
  const sort = normalizeSort(event.sort);
  const where = buildDrugWhere(filter, openid);

  let query = db.collection(DRUGS_COLLECTION)
    .where(where)
    .orderBy(sort.field, sort.direction);

  if (sort.field !== "createdAtTs") {
    query = query.orderBy("createdAtTs", "desc");
  }

  query = query
    .skip((page - 1) * pageSize)
    .limit(pageSize);

  const [listRes, countRes] = await Promise.all([
    query.get(),
    db.collection(DRUGS_COLLECTION).where(where).count()
  ]);

  return {
    page,
    pageSize,
    total: countRes.total || 0,
    items: (listRes.data || []).map(toClientDrug)
  };
}

async function getUserProfile(openid) {
  const res = await db.collection(USERS_COLLECTION)
    .where({ _openid: openid })
    .limit(1)
    .get();
  const profile = res.data && res.data[0];

  if (profile) return toClientProfile(profile);

  const now = new Date();
  const defaultProfile = {
    _openid: openid,
    nickName: "家庭药箱用户",
    avatarUrl: "/images/avatar.png",
    description: "药品效期云端管理已开启",
    createdAt: now,
    createdAtTs: now.getTime(),
    updatedAt: now,
    updatedAtTs: now.getTime()
  };
  const addRes = await db.collection(USERS_COLLECTION).add({ data: defaultProfile });
  return toClientProfile({ ...defaultProfile, _id: addRes._id });
}

async function updateUserProfile(event, openid) {
  const profile = normalizeProfilePayload(event.profile || event);
  const now = new Date();
  const res = await db.collection(USERS_COLLECTION)
    .where({ _openid: openid })
    .limit(1)
    .get();
  const existing = res.data && res.data[0];

  if (existing) {
    await db.collection(USERS_COLLECTION).doc(existing._id).update({
      data: {
        ...profile,
        updatedAt: now,
        updatedAtTs: now.getTime()
      }
    });
    return toClientProfile({ ...existing, ...profile, updatedAt: now, updatedAtTs: now.getTime() });
  }

  const created = {
    _openid: openid,
    ...profile,
    createdAt: now,
    createdAtTs: now.getTime(),
    updatedAt: now,
    updatedAtTs: now.getTime()
  };
  const addRes = await db.collection(USERS_COLLECTION).add({ data: created });
  return toClientProfile({ ...created, _id: addRes._id });
}

async function getDrug(event, openid) {
  const id = requiredString(event.id, "id");
  const res = await db.collection(DRUGS_COLLECTION)
    .where({ _id: id, _openid: openid, isDeleted: _.neq(true) })
    .limit(1)
    .get();
  const drug = res.data && res.data[0];
  if (!drug) throw publicError("NOT_FOUND", "Drug record was not found.");
  return toClientDrug(drug);
}

async function createDrug(event, openid) {
  const now = new Date();
  const drug = normalizeDrugPayload(event.drug || event, {
    openid,
    now,
    mode: "create"
  });

  const addRes = await db.collection(DRUGS_COLLECTION).add({ data: drug });
  return {
    id: addRes._id,
    drug: toClientDrug({ ...drug, _id: addRes._id })
  };
}

async function batchCreateDrugs(event, openid) {
  const sourceItems = Array.isArray(event.drugs) ? event.drugs : Array.isArray(event.items) ? event.items : [];
  if (!sourceItems.length) throw publicError("INVALID_ARGUMENT", "drugs must contain at least one item.");
  if (sourceItems.length > 20) throw publicError("INVALID_ARGUMENT", "drugs cannot contain more than 20 items.");

  const now = new Date();
  const created = [];
  for (const item of sourceItems) {
    const drug = normalizeDrugPayload(item, { openid, now, mode: "create" });
    const addRes = await db.collection(DRUGS_COLLECTION).add({ data: drug });
    created.push(toClientDrug({ ...drug, _id: addRes._id }));
  }

  return {
    count: created.length,
    items: created
  };
}

async function updateDrug(event, openid) {
  const id = requiredString(event.id, "id");
  const now = new Date();
  const existing = await getOwnedDrug(id, openid);
  const updateData = normalizeDrugPayload(
    { ...existing, ...(event.drug || {}) },
    { openid, now, mode: "update" }
  );

  delete updateData._openid;
  delete updateData.createdAt;
  delete updateData.createdAtTs;

  await db.collection(DRUGS_COLLECTION).doc(id).update({ data: updateData });
  return toClientDrug({ ...existing, ...updateData, _id: id });
}

async function deleteDrug(event, openid) {
  const id = requiredString(event.id, "id");
  const existing = await getOwnedDrug(id, openid);
  const now = new Date();

  await db.collection(DRUGS_COLLECTION).doc(id).update({
    data: {
      isDeleted: true,
      deletedAt: now,
      deletedAtTs: now.getTime(),
      updatedAt: now,
      updatedAtTs: now.getTime()
    }
  });

  const shouldDeleteFiles = event.deleteFiles === true;
  let fileResult = { deleted: 0, failed: [] };
  if (shouldDeleteFiles) {
    fileResult = await deleteFiles(collectFileIds(existing.attachments));
  }

  return {
    id,
    deleted: true,
    files: fileResult
  };
}

async function getFileTempUrls(event, openid) {
  const fileIDs = normalizeFileIds(event.fileIDs || event.fileIds);
  if (!fileIDs.length) return { fileList: [] };
  await assertFilesBelongToUser(fileIDs, openid);

  const res = await cloud.getTempFileURL({ fileList: fileIDs });
  return {
    fileList: (res.fileList || []).map(file => ({
      fileID: file.fileID,
      tempFileURL: file.tempFileURL || "",
      status: file.status,
      maxAge: file.maxAge
    }))
  };
}

async function deleteCloudFiles(event, openid) {
  const fileIDs = normalizeFileIds(event.fileIDs || event.fileIds);
  if (!fileIDs.length) return { deleted: 0, failed: [] };
  await assertFilesBelongToUser(fileIDs, openid);
  return await deleteFiles(fileIDs);
}

async function refreshExpirySnapshots(event, openid) {
  const limit = Math.min(MAX_PAGE_SIZE, Math.max(1, Number(event.limit || 50)));
  const res = await db.collection(DRUGS_COLLECTION)
    .where({ _openid: openid, isDeleted: _.neq(true) })
    .orderBy("updatedAtTs", "asc")
    .limit(limit)
    .get();

  const today = getTodayStart();
  let updated = 0;
  for (const drug of res.data || []) {
    const snapshot = buildExpirySnapshot(drug.expireDateText, today);
    await db.collection(DRUGS_COLLECTION).doc(drug._id).update({
      data: {
        ...snapshot,
        updatedAt: new Date(),
        updatedAtTs: Date.now()
      }
    });
    updated += 1;
  }

  return { scanned: (res.data || []).length, updated };
}

async function getOwnedDrug(id, openid) {
  const res = await db.collection(DRUGS_COLLECTION)
    .where({ _id: id, _openid: openid, isDeleted: _.neq(true) })
    .limit(1)
    .get();
  const drug = res.data && res.data[0];
  if (!drug) throw publicError("NOT_FOUND", "Drug record was not found.");
  return drug;
}

function buildDrugWhere(filter, openid) {
  const where = {
    _openid: openid,
    isDeleted: _.neq(true)
  };

  if (filter.category) where.category = filter.category;
  if (filter.openState) where.openState = filter.openState;
  if (filter.expiryStatus) where.expiryStatus = filter.expiryStatus;
  if (filter.location) where.location = filter.location;
  if (filter.keyword) {
    where.searchText = db.RegExp({
      regexp: escapeRegExp(filter.keyword.toLowerCase()),
      options: "i"
    });
  }

  return where;
}

function normalizeListFilter(filter = {}) {
  return {
    category: CATEGORY_VALUES.includes(filter.category) ? filter.category : "",
    openState: OPEN_STATE_VALUES.includes(filter.openState) ? filter.openState : "",
    expiryStatus: ["expired", "expiring", "normal", "unknown"].includes(filter.expiryStatus) ? filter.expiryStatus : "",
    location: safeString(filter.location, 40),
    keyword: safeString(filter.keyword, 40)
  };
}

function normalizeSort(sort = {}) {
  const key = typeof sort === "string" ? sort : sort.key || sort.field;
  const direction = sort.direction === "desc" ? "desc" : "asc";
  const map = {
    expiryCountdown: "expireAtTs",
    expireAt: "expireAtTs",
    createdAt: "createdAtTs",
    updatedAt: "updatedAtTs",
    name: "nameSortKey"
  };

  if (!key) return { field: "expireAtTs", direction: "asc" };
  return {
    field: map[key] || "expireAtTs",
    direction: key === "createdAt" || key === "updatedAt" ? (sort.direction || "desc") : direction
  };
}

function normalizeDrugPayload(input, options) {
  const now = options.now || new Date();
  const name = requiredString(input.name, "name").slice(0, 80);
  const qty = requiredString(input.qty || input.quantity, "qty").slice(0, 40);
  const expireDateText = normalizeDateText(input.expireDateText || input.expireDate || input.date);
  const snapshot = buildExpirySnapshot(expireDateText, getTodayStart(now));
  const attachments = normalizeAttachments(input.attachments || input.files);
  const source = SOURCE_VALUES.includes(input.source) ? input.source : "unknown";

  const drug = {
    name,
    nameSortKey: name.toLowerCase(),
    qty,
    category: CATEGORY_VALUES.includes(input.category) ? input.category : "other",
    dosageForm: DOSAGE_FORM_VALUES.includes(input.dosageForm) ? input.dosageForm : "other",
    openState: normalizeOpenState(input.openState),
    location: safeString(input.location, 40) || "medicine_box",
    expireDateText,
    expireDate: snapshot.expireDate,
    expireAtTs: snapshot.expireAtTs,
    daysToExpire: snapshot.daysToExpire,
    expiryStatus: snapshot.expiryStatus,
    source,
    ai: normalizeAi(input.ai || input),
    attachments,
    searchText: buildSearchText(name, qty, input.location, input.category),
    isDeleted: false,
    updatedAt: now,
    updatedAtTs: now.getTime()
  };

  if (options.mode === "create") {
    drug._openid = options.openid;
    drug.createdAt = now;
    drug.createdAtTs = now.getTime();
  }

  return drug;
}

function normalizeAi(input = {}) {
  return {
    confidence: clamp(Number(input.confidence || 0)),
    note: safeString(input.note, 200),
    rawText: safeString(input.rawText || input.recognizedText, 1000),
    model: safeString(input.model, 80)
  };
}

function normalizeAttachments(input = {}) {
  return {
    images: normalizeFileIds(input.images || input.imageFileIDs || input.imageFileIds).slice(0, 8),
    audios: normalizeFileIds(input.audios || input.audioFileIDs || input.audioFileIds).slice(0, 4)
  };
}

function normalizeFileIds(value) {
  if (!Array.isArray(value)) return [];
  const seen = new Set();
  return value
    .map(item => typeof item === "string" ? item : item && item.fileID)
    .map(item => String(item || "").trim())
    .filter(item => item && item.startsWith("cloud://"))
    .filter(item => {
      if (seen.has(item)) return false;
      seen.add(item);
      return true;
    });
}

function collectFileIds(attachments = {}) {
  return [
    ...normalizeFileIds(attachments.images),
    ...normalizeFileIds(attachments.audios)
  ];
}

async function assertFilesBelongToUser(fileIDs, openid) {
  const prefix = `/${openid}/`;
  const invalid = fileIDs.filter(fileID => !fileID.includes(prefix) && !fileID.includes("/ai-inputs/"));
  if (invalid.length) {
    throw publicError("FORBIDDEN_FILE", "Some files are not in the current user's storage scope.");
  }
}

async function deleteFiles(fileIDs) {
  if (!fileIDs.length) return { deleted: 0, failed: [] };
  const res = await cloud.deleteFile({ fileList: fileIDs });
  const fileList = res.fileList || [];
  return {
    deleted: fileList.filter(item => item.status === 0).length,
    failed: fileList
      .filter(item => item.status !== 0)
      .map(item => ({ fileID: item.fileID, status: item.status, errMsg: item.errMsg || "" }))
  };
}

function normalizeOpenState(value) {
  if (OPEN_STATE_VALUES.includes(value)) return value;
  const text = safeString(value, 40);
  if (/opened|已开封/.test(text)) return "opened";
  if (/independent|独立|板装|袋装/.test(text)) return "independent";
  return "unopened";
}

function buildExpirySnapshot(dateText, today) {
  if (!dateText) {
    return {
      expireDate: null,
      expireAtTs: Number.MAX_SAFE_INTEGER,
      daysToExpire: null,
      expiryStatus: "unknown"
    };
  }

  const expireDate = parseLocalDate(dateText);
  const daysToExpire = Math.floor((expireDate.getTime() - today.getTime()) / 86400000);
  return {
    expireDate,
    expireAtTs: expireDate.getTime(),
    daysToExpire,
    expiryStatus: daysToExpire < 0 ? "expired" : daysToExpire <= 3 ? "expiring" : "normal"
  };
}

function toClientDrug(drug) {
  return {
    id: drug._id,
    name: drug.name,
    qty: drug.qty,
    category: drug.category,
    dosageForm: drug.dosageForm,
    openState: drug.openState,
    location: drug.location,
    expireDate: drug.expireDateText,
    date: drug.expireDateText,
    days: drug.daysToExpire,
    status: drug.expiryStatus,
    source: drug.source,
    ai: drug.ai || {},
    attachments: drug.attachments || { images: [], audios: [] },
    createdAt: drug.createdAtTs,
    updatedAt: drug.updatedAtTs
  };
}

function normalizeProfilePayload(input = {}) {
  const nickName = safeString(input.nickName, 24) || "家庭药箱用户";
  const avatarUrl = safeString(input.avatarUrl, 300) || "/images/avatar.png";
  const description = safeString(input.description, 60) || "药品效期云端管理已开启";
  return { nickName, avatarUrl, description };
}

function toClientProfile(profile) {
  return {
    id: profile._id,
    nickName: profile.nickName || "家庭药箱用户",
    avatarUrl: profile.avatarUrl || "/images/avatar.png",
    description: profile.description || "药品效期云端管理已开启",
    updatedAt: profile.updatedAtTs || Date.now()
  };
}

function normalizeDateText(value) {
  const text = safeString(value, 20);
  if (!text) return "";
  if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    throw publicError("INVALID_DATE", "Date must use YYYY-MM-DD.");
  }
  parseLocalDate(text);
  return text;
}

function parseLocalDate(dateText) {
  const [year, month, day] = dateText.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    throw publicError("INVALID_DATE", "Date is not valid.");
  }
  date.setHours(0, 0, 0, 0);
  return date;
}

function getTodayStart(input = new Date()) {
  const date = new Date(input);
  date.setHours(0, 0, 0, 0);
  return date;
}

function buildSearchText(...parts) {
  return parts
    .map(part => safeString(part, 120).toLowerCase())
    .filter(Boolean)
    .join(" ");
}

function requiredString(value, field) {
  const text = safeString(value, 200);
  if (!text) throw publicError("INVALID_ARGUMENT", `${field} is required.`);
  return text;
}

function safeString(value, maxLength = 80) {
  return String(value || "").trim().slice(0, maxLength);
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function clamp(value) {
  if (!Number.isFinite(value)) return 0;
  return Math.max(0, Math.min(1, value));
}

function publicError(code, message) {
  const err = new Error(message);
  err.code = code;
  err.publicMessage = message;
  return err;
}

function ok(data) {
  return { success: true, data };
}

function fail(code, message) {
  return { success: false, code, message };
}

function sanitizeError(err) {
  return {
    code: err && err.code,
    message: err && err.message,
    stack: err && err.stack ? err.stack.split("\n").slice(0, 2).join("\n") : ""
  };
}
