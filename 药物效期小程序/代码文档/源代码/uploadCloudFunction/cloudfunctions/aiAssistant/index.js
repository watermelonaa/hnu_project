const cloud = require("wx-server-sdk");
const https = require("https");

cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV });

let privateConfig = {};
try {
  privateConfig = require("./config.private");
} catch (err) {
  privateConfig = require("./config.example");
}

const cfg = {
  qwenApiKey: privateConfig.qwenApiKey,
  zhipuApiKey: privateConfig.zhipuApiKey,
  qwenTextModel: privateConfig.qwenTextModel || "qwen-plus",
  qwenVisionModel: privateConfig.qwenVisionModel || "qwen-vl-plus",
  zhipuTextModel: privateConfig.zhipuTextModel || "glm-4.7",
  asrModel: privateConfig.asrModel || "paraformer-v2"
};

const CATEGORY_VALUES = ["antipyretic", "antibiotic", "digestive", "vitamin", "external", "other"];

exports.main = async (event = {}) => {
  try {
    switch (event.action) {
      case "chat":
        return ok(await handleChat(event));
      case "visionRecognize":
        return ok(await handleVisionRecognize(event));
      case "voiceRecognize":
        return ok(await handleVoiceRecognize(event));
      case "parseDrugText":
        return ok(await handleParseDrugText(event));
      default:
        return fail("未知的 AI 操作类型。");
    }
  } catch (err) {
    console.error("aiAssistant error:", sanitizeError(err));
    return fail("AI 服务暂不可用，请稍后重试。");
  }
};

async function handleChat(event) {
  const question = String(event.question || "").trim();
  if (!question) throw new Error("empty question");

  const drugs = normalizeDrugs(event.drugs);
  const recentMessages = normalizeMessages(event.messages).slice(-8);
  const system = [
    "你是“药物效期管家”的 AI 助手，只提供家庭药箱管理、药品效期、保存方式、提醒设置和一般用药安全建议。",
    "不得替代医生或药师诊断，不给出处方、剂量调整或治疗方案。",
    "遇到处方药、儿童/孕妇/老人用药、药物相互作用、严重不适、副作用等问题，必须建议咨询医生或药师。",
    "回答要简洁、中文、适合小程序气泡展示。必须结合用户当前药箱数据。",
    "生成风险报告或提醒计划时，要按优先级分组，并明确哪些内容只是管理建议。"
  ].join("\n");

  const messages = [
    { role: "system", content: system },
    { role: "user", content: `当前药箱数据：${JSON.stringify(summarizeDrugs(drugs))}` },
    ...recentMessages,
    { role: "user", content: question }
  ];

  const reply = await callTextWithFallback(messages);
  return { reply: ensureSafetyNote(reply), provider: "qwen/zhipu" };
}

async function handleVisionRecognize(event) {
  const fileID = String(event.fileID || "").trim();
  if (!fileID) throw new Error("missing image fileID");

  const image = await downloadCloudFile(fileID);
  const mime = detectMime(image.fileContent) || "image/jpeg";
  const dataUrl = `data:${mime};base64,${image.fileContent.toString("base64")}`;
  const prompt = [
    "识别图片中的药品包装或药品标签，提取可用于家庭药箱录入的信息。",
    "只输出 JSON，不要 Markdown。格式：",
    "{\"items\":[{\"name\":\"药品名\",\"qty\":\"数量\",\"category\":\"antipyretic|antibiotic|digestive|vitamin|external|other\",\"location\":\"药箱\",\"openState\":\"未开封|已开封|独立包装\",\"date\":\"YYYY-MM-DD或空字符串\",\"confidence\":0.0,\"note\":\"简短说明\"}]}",
    "如果无法确定字段，使用空字符串或 other，confidence 使用 0 到 1。不要编造明确到期日期。"
  ].join("\n");

  let parsed;
  try {
    const content = await callQwenVision(prompt, dataUrl);
    parsed = parseJsonObject(content);
  } catch (err) {
    console.error("vision failed:", sanitizeError(err));
    parsed = { items: [] };
  }

  return {
    items: normalizeRecognizedItems(parsed.items),
    rawText: parsed.rawText || ""
  };
}

async function handleVoiceRecognize(event) {
  const fileID = String(event.fileID || "").trim();
  if (!fileID) throw new Error("missing audio fileID");

  const tempUrl = await getTempFileUrl(fileID);
  let transcript = "";
  try {
    transcript = await transcribeAudio(tempUrl);
  } catch (err) {
    console.error("asr failed:", sanitizeError(err));
  }

  if (!transcript) {
    transcript = String(event.fallbackText || "").trim();
  }
  if (!transcript) throw new Error("empty transcript");

  const parsed = await parseDrugTextWithModel(transcript);
  return {
    recognizedText: transcript,
    items: normalizeRecognizedItems(parsed.items, transcript)
  };
}

async function handleParseDrugText(event) {
  const text = String(event.text || "").trim();
  if (!text) throw new Error("empty text");
  const parsed = await parseDrugTextWithModel(text);
  return {
    recognizedText: text,
    items: normalizeRecognizedItems(parsed.items, text)
  };
}

async function parseDrugTextWithModel(text) {
  const today = getChinaTodayString();
  const messages = [
    {
      role: "system",
      content: [
        "你负责把用户自然语言中的药品信息解析成结构化药箱录入数据。",
        `今天日期是 ${today}，时区为 Asia/Shanghai。`,
        "只输出 JSON，不要 Markdown。格式：",
        "{\"items\":[{\"name\":\"药品名\",\"qty\":\"数量\",\"category\":\"antipyretic|antibiotic|digestive|vitamin|external|other\",\"location\":\"药箱\",\"openState\":\"未开封|已开封|独立包装\",\"date\":\"YYYY-MM-DD或空字符串\",\"confidence\":0.0,\"note\":\"简短说明\"}]}",
        "如果用户只说“两天后过期”等相对日期，请以今天为基准换算成 YYYY-MM-DD。无法确定则留空。"
      ].join("\n")
    },
    { role: "user", content: text }
  ];
  const content = await callTextWithFallback(messages);
  return parseJsonObject(content);
}

async function callTextWithFallback(messages) {
  try {
    return await callOpenAICompatible({
      url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
      apiKey: cfg.qwenApiKey,
      model: cfg.qwenTextModel,
      messages,
      temperature: 0.3
    });
  } catch (err) {
    console.error("qwen text failed:", sanitizeError(err));
    return await callOpenAICompatible({
      url: "https://open.bigmodel.cn/api/paas/v4/chat/completions",
      apiKey: cfg.zhipuApiKey,
      model: cfg.zhipuTextModel,
      messages,
      temperature: 0.3
    });
  }
}

async function callQwenVision(prompt, imageDataUrl) {
  return await callOpenAICompatible({
    url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
    apiKey: cfg.qwenApiKey,
    model: cfg.qwenVisionModel,
    messages: [
      {
        role: "user",
        content: [
          { type: "text", text: prompt },
          { type: "image_url", image_url: { url: imageDataUrl } }
        ]
      }
    ],
    temperature: 0.1
  });
}

async function callOpenAICompatible({ url, apiKey, model, messages, temperature }) {
  if (!apiKey || apiKey.startsWith("your-")) throw new Error("missing api key");
  const res = await requestJson(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: {
      model,
      messages,
      temperature
    },
    timeout: 30000
  });
  const content = res && res.choices && res.choices[0] && res.choices[0].message && res.choices[0].message.content;
  if (!content) throw new Error("empty model response");
  return Array.isArray(content) ? content.map(part => part.text || "").join("") : String(content);
}

async function transcribeAudio(fileUrl) {
  const submit = await requestJson("https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${cfg.qwenApiKey}`,
      "Content-Type": "application/json",
      "X-DashScope-Async": "enable"
    },
    body: {
      model: cfg.asrModel,
      input: { file_urls: [fileUrl] },
      parameters: { language_hints: ["zh", "en"] }
    },
    timeout: 20000
  });
  const taskId = submit && submit.output && submit.output.task_id;
  if (!taskId) throw new Error("missing asr task id");

  for (let i = 0; i < 30; i++) {
    await sleep(1000);
    const status = await requestJson(`https://dashscope.aliyuncs.com/api/v1/tasks/${taskId}`, {
      method: "GET",
      headers: { Authorization: `Bearer ${cfg.qwenApiKey}` },
      timeout: 10000
    });
    const taskStatus = status && status.output && status.output.task_status;
    if (taskStatus === "SUCCEEDED") {
      const results = status.output.results || [];
      const texts = [];
      for (const item of results) {
        if (item.transcription_url) {
          const detail = await requestJson(item.transcription_url, { method: "GET", timeout: 10000 });
          texts.push(extractTranscript(detail));
        }
      }
      return texts.filter(Boolean).join("\n").trim();
    }
    if (taskStatus && taskStatus !== "PENDING" && taskStatus !== "RUNNING") {
      throw new Error(`asr task ${taskStatus}`);
    }
  }
  throw new Error("asr timeout");
}

function requestJson(url, options) {
  return new Promise((resolve, reject) => {
    const payload = options.body ? JSON.stringify(options.body) : undefined;
    const req = https.request(url, {
      method: options.method || "GET",
      headers: {
        ...(options.headers || {}),
        ...(payload ? { "Content-Length": Buffer.byteLength(payload) } : {})
      },
      timeout: options.timeout || 30000
    }, res => {
      const chunks = [];
      res.on("data", chunk => chunks.push(chunk));
      res.on("end", () => {
        const text = Buffer.concat(chunks).toString("utf8");
        let json = null;
        try {
          json = text ? JSON.parse(text) : {};
        } catch (err) {
          return reject(new Error(`invalid json response ${res.statusCode}`));
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`http ${res.statusCode}: ${json.message || json.error || "request failed"}`));
        }
        resolve(json);
      });
    });
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("request timeout")));
    if (payload) req.write(payload);
    req.end();
  });
}

async function downloadCloudFile(fileID) {
  return await cloud.downloadFile({ fileID });
}

async function getTempFileUrl(fileID) {
  const res = await cloud.getTempFileURL({ fileList: [fileID] });
  const file = res.fileList && res.fileList[0];
  if (!file || !file.tempFileURL) throw new Error("missing temp file url");
  return file.tempFileURL;
}

function normalizeMessages(messages) {
  if (!Array.isArray(messages)) return [];
  return messages
    .filter(m => m && (m.role === "user" || m.role === "ai" || m.role === "assistant"))
    .map(m => ({
      role: m.role === "ai" ? "assistant" : m.role,
      content: String(m.content || "").slice(0, 500)
    }));
}

function normalizeDrugs(drugs) {
  if (!Array.isArray(drugs)) return [];
  return drugs.slice(0, 80).map(d => ({
    name: d.name || "",
    qty: d.qty || "",
    status: d.status || "",
    days: Number.isFinite(Number(d.days)) ? Number(d.days) : null,
    openState: d.openState || "",
    category: d.category || "",
    location: d.location || ""
  }));
}

function summarizeDrugs(drugs) {
  return {
    total: drugs.length,
    expired: drugs.filter(d => d.status === "expired"),
    expiring: drugs.filter(d => d.status === "expiring"),
    normalCount: drugs.filter(d => d.status === "normal").length
  };
}

function normalizeRecognizedItems(items, sourceText = "") {
  const source = Array.isArray(items) && items.length ? items : [{}];
  const relativeDate = inferRelativeDate(sourceText);
  return source.slice(0, 8).map(item => ({
    name: safeString(item.name),
    qty: safeString(item.qty),
    category: CATEGORY_VALUES.includes(item.category) ? item.category : "other",
    location: safeString(item.location) || "药箱",
    openState: normalizeOpenState(item.openState),
    date: relativeDate || normalizeDate(item.date),
    confidence: clamp(Number(item.confidence) || 0),
    note: safeString(item.note)
  }));
}

function parseJsonObject(text) {
  if (!text) return {};
  const cleaned = String(text).replace(/```json|```/g, "").trim();
  try {
    return JSON.parse(cleaned);
  } catch (err) {
    const start = cleaned.indexOf("{");
    const end = cleaned.lastIndexOf("}");
    if (start >= 0 && end > start) {
      return JSON.parse(cleaned.slice(start, end + 1));
    }
    throw err;
  }
}

function extractTranscript(detail) {
  const fragments = [];
  const walk = value => {
    if (!value) return;
    if (typeof value === "string") return;
    if (Array.isArray(value)) return value.forEach(walk);
    if (typeof value === "object") {
      if (typeof value.text === "string") fragments.push(value.text);
      if (typeof value.sentence === "string") fragments.push(value.sentence);
      Object.keys(value).forEach(key => {
        if (key !== "text" && key !== "sentence") walk(value[key]);
      });
    }
  };
  walk(detail);
  return dedupeTranscript(fragments);
}

function dedupeTranscript(fragments) {
  const cleaned = fragments
    .map(text => String(text || "").replace(/\s+/g, "").trim())
    .filter(Boolean);
  if (!cleaned.length) return "";

  const longest = cleaned.reduce((best, text) => text.length > best.length ? text : best, "");
  const unique = [];
  cleaned
    .sort((a, b) => b.length - a.length)
    .forEach(text => {
      if (unique.some(item => item.includes(text) || text.includes(item))) return;
      unique.push(text);
    });

  if (unique.length === 1) return unique[0];
  if (unique.some(text => text !== longest && longest.includes(text))) return longest;
  return unique.reverse().join("。").replace(/。+/g, "。").replace(/。$/, "");
}

function ensureSafetyNote(reply) {
  const text = String(reply || "").trim();
  if (/医生|药师|不替代|遵医嘱/.test(text)) return text;
  return `${text}\n\n提示：以上仅用于药箱管理参考，不能替代医生或药师建议；处方药、相互作用或不适症状请咨询专业人士。`;
}

function detectMime(buffer) {
  if (!buffer || buffer.length < 4) return "";
  if (buffer[0] === 0xff && buffer[1] === 0xd8) return "image/jpeg";
  if (buffer[0] === 0x89 && buffer[1] === 0x50) return "image/png";
  if (buffer.slice(0, 4).toString("ascii") === "RIFF") return "image/webp";
  return "";
}

function normalizeOpenState(value) {
  const text = safeString(value);
  if (text.includes("已")) return "已开封";
  if (text.includes("独立")) return "独立包装";
  return "未开封";
}

function normalizeDate(value) {
  const text = safeString(value);
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text : "";
}

function inferRelativeDate(text) {
  const value = safeString(text);
  if (!value) return "";

  let offset = null;
  if (/大后天/.test(value)) offset = 3;
  else if (/后天/.test(value)) offset = 2;
  else if (/明天|明日/.test(value)) offset = 1;
  else if (/今天|今日|当天/.test(value)) offset = 0;
  else {
    const match = value.match(/([0-9一二两三四五六七八九十]+)\s*天后/);
    if (match) offset = parseChineseNumber(match[1]);
  }

  if (offset === null || Number.isNaN(offset)) return "";
  const date = getChinaTodayDate();
  date.setUTCDate(date.getUTCDate() + offset);
  return formatDateUTC(date);
}

function parseChineseNumber(value) {
  if (/^\d+$/.test(value)) return Number(value);
  const map = { 一: 1, 二: 2, 两: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9 };
  if (value === "十") return 10;
  if (value.startsWith("十")) return 10 + (map[value.slice(1)] || 0);
  if (value.includes("十")) {
    const [tens, ones] = value.split("十");
    return (map[tens] || 1) * 10 + (map[ones] || 0);
  }
  return map[value];
}

function getChinaTodayString() {
  return formatDateUTC(getChinaTodayDate());
}

function getChinaTodayDate() {
  const now = new Date();
  const chinaTime = new Date(now.getTime() + 8 * 60 * 60 * 1000);
  return new Date(Date.UTC(chinaTime.getUTCFullYear(), chinaTime.getUTCMonth(), chinaTime.getUTCDate()));
}

function formatDateUTC(date) {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function safeString(value) {
  return String(value || "").trim().slice(0, 80);
}

function clamp(value) {
  return Math.max(0, Math.min(1, value));
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function ok(data) {
  return { success: true, data };
}

function fail(message) {
  return { success: false, message };
}

function sanitizeError(err) {
  return {
    message: err && err.message,
    stack: err && err.stack ? err.stack.split("\n").slice(0, 2).join("\n") : ""
  };
}
