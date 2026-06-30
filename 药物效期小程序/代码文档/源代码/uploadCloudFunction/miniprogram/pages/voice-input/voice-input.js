const drugService = require("../../utils/drugService");

const CATEGORY_OPTIONS = [
  { value: "antipyretic", name: "感冒退热" },
  { value: "antibiotic", name: "抗感染" },
  { value: "digestive", name: "肠胃用药" },
  { value: "vitamin", name: "维生素/保健品" },
  { value: "external", name: "外用药" },
  { value: "other", name: "其他" }
];

const LOCATION_OPTIONS = ["药箱", "抽屉", "随身包", "冰箱", "其他"];
const OPEN_STATE_OPTIONS = ["未开封", "已开封", "独立包装"];
const recorderManager = wx.getRecorderManager();

Page({
  data: {
    isRecording: false,
    isRecognizing: false,
    isSaving: false,
    micClass: "",
    micHint: "点击开始录音",
    micDesc: "再次点击结束录音，也可以输入文字后解析。",
    recognizedText: "",
    audioFileID: "",
    drug: {},
    warnings: [],
    confidenceText: "低",
    confidenceClass: "confidence-low",
    categoryOptions: CATEGORY_OPTIONS,
    locationOptions: LOCATION_OPTIONS,
    openStateOptions: OPEN_STATE_OPTIONS,
    categoryIndex: 5,
    locationIndex: 0,
    openStateIndex: 0,
    dateText: "请选择日期"
  },

  onLoad() {
    this.initRecorder();
    this.setDrug(this.normalizeDrug({ date: this.getDateFromOffset(7) }));
  },

  initRecorder() {
    recorderManager.onStart(() => {
      wx.showToast({ title: "录音中，再次点击结束", icon: "none", duration: 30000 });
    });
    recorderManager.onError(err => {
      console.error("录音失败", err);
      wx.hideToast();
      this.setData({
        isRecording: false,
        micClass: "",
        micHint: "点击开始录音",
        micDesc: "录音失败，也可以输入文字后解析。"
      });
      wx.showToast({ title: "录音失败", icon: "none" });
    });
    recorderManager.onStop(res => {
      wx.hideToast();
      this.setData({
        isRecording: false,
        micClass: "",
        micHint: "点击开始录音",
        micDesc: "也可以输入文字后解析。"
      });
      if (res.tempFilePath) {
        this.recognizeVoice(res.tempFilePath);
      } else {
        wx.showToast({ title: "未获取到录音文件", icon: "none" });
      }
    });
  },

  toggleRecording() {
    if (this.data.isRecognizing) return;
    if (this.data.isRecording) {
      this.stopRecording();
      return;
    }
    this.startRecording();
  },

  startRecording() {
    if (this.data.isRecording || this.data.isRecognizing) return;
    this.setData({
      isRecording: true,
      micClass: "recording",
      micHint: "点击结束录音",
      micDesc: "请说出药品名称、数量和到期时间。"
    });

    wx.getSetting({
      success: res => {
        if (res.authSetting["scope.record"]) {
          this.beginRecord();
          return;
        }
        wx.authorize({
          scope: "scope.record",
          success: () => this.beginRecord(),
          fail: () => {
            this.setData({
              isRecording: false,
              micClass: "",
              micHint: "长按录音"
            });
            wx.showModal({
              title: "需要录音权限",
              content: "开启录音权限后才能使用语音录入药品。",
              confirmText: "去设置",
              success: modalRes => {
                if (modalRes.confirm) wx.openSetting();
              }
            });
          }
        });
      }
    });
  },

  beginRecord() {
    recorderManager.start({
      duration: 30000,
      sampleRate: 16000,
      numberOfChannels: 1,
      encodeBitRate: 48000,
      format: "mp3"
    });
  },

  stopRecording() {
    if (this.data.isRecording) recorderManager.stop();
  },

  async recognizeVoice(filePath) {
    this.setData({
      isRecognizing: true,
      micDesc: "正在识别语音..."
    });
    wx.showLoading({ title: "识别中...", mask: true });
    try {
      const fileID = await this.uploadAudio(filePath);
      const res = await wx.cloud.callFunction({
        name: "aiAssistant",
        data: { action: "voiceRecognize", fileID }
      });
      const result = res.result || {};
      if (!result.success) throw new Error(result.message || "识别失败");
      const data = result.data || {};
      const item = Array.isArray(data.items) && data.items.length ? data.items[0] : {};
      this.setData({
        recognizedText: data.recognizedText || "",
        audioFileID: fileID
      });
      this.setDrug(this.normalizeDrug(item));
      wx.showToast({ title: "识别成功", icon: "success" });
    } catch (err) {
      console.error("语音识别失败", err);
      wx.showToast({ title: "识别失败，可手动填写", icon: "none" });
    } finally {
      wx.hideLoading();
      this.setData({
        isRecognizing: false,
        micDesc: "也可以输入文字后解析。"
      });
    }
  },

  async parseTypedText() {
    const text = String(this.data.recognizedText || "").trim();
    if (!text) return wx.showToast({ title: "请先输入文字", icon: "none" });
    this.setData({
      isRecognizing: true,
      micDesc: "正在解析文字..."
    });
    wx.showLoading({ title: "解析中...", mask: true });
    try {
      const res = await wx.cloud.callFunction({
        name: "aiAssistant",
        data: { action: "parseDrugText", text }
      });
      const result = res.result || {};
      if (!result.success) throw new Error(result.message || "解析失败");
      const items = result.data && result.data.items;
      this.setDrug(this.normalizeDrug(items && items[0]));
      wx.showToast({ title: "解析成功", icon: "success" });
    } catch (err) {
      console.error("文字解析失败", err);
      wx.showToast({ title: "解析失败", icon: "none" });
    } finally {
      wx.hideLoading();
      this.setData({
        isRecognizing: false,
        micDesc: "也可以输入文字后解析。"
      });
    }
  },

  uploadAudio(filePath) {
    return wx.cloud.uploadFile({
      cloudPath: `ai-inputs/audio/${Date.now()}-${Math.random().toString(16).slice(2)}.mp3`,
      filePath
    }).then(res => res.fileID);
  },

  normalizeDrug(item = {}) {
    return this.addPickerIndexes({
      name: item.name || "",
      qty: item.qty || "",
      category: item.category || "other",
      location: item.location || "药箱",
      openState: this.normalizeOpenState(item.openState),
      date: item.date || this.getDateFromOffset(7),
      confidence: Number(item.confidence || 0),
      note: item.note || ""
    });
  },

  setDrug(drug) {
    const normalized = this.addPickerIndexes(drug);
    const warnings = this.getDrugWarnings(normalized);
    const confidence = Number(normalized.confidence || 0);
    let confidenceText = "高";
    let confidenceClass = "confidence-high";

    if (confidence < 0.45) {
      confidenceText = "低";
      confidenceClass = "confidence-low";
    } else if (confidence < 0.75) {
      confidenceText = "中";
      confidenceClass = "confidence-mid";
    }

    if (confidence < 0.75) warnings.unshift("AI 识别可信度偏低，请核对药名、数量和日期。");
    this.setData({
      drug: normalized,
      warnings,
      confidenceText,
      confidenceClass,
      categoryIndex: normalized.categoryIndex,
      locationIndex: normalized.locationIndex,
      openStateIndex: normalized.openStateIndex,
      dateText: normalized.date || "请选择日期"
    });
  },

  getDrugWarnings(drug) {
    const warnings = [];
    if (!String(drug.name || "").trim()) warnings.push("药品名称为空。");
    if (!String(drug.qty || "").trim()) warnings.push("数量为空。");
    if (!drug.date) warnings.push("到期日期缺失。");

    if (drug.date) {
      const days = drugService.getDaysRemaining(drug.date);
      if (days < -365 || days > 3650) warnings.push("到期日期看起来异常，请确认年份。");
      if (drug.openState === "已开封" && days > 180) warnings.push("已开封药品剩余时间较长，请核对开封后有效期。");
    }

    if (drug.category === "antibiotic") warnings.push("抗感染类药品请遵医嘱使用。");
    return warnings;
  },

  onRecognizedTextInput(e) {
    this.setData({ recognizedText: e.detail.value || "" });
  },

  onNameInput(e) {
    this.setDrug({ ...this.data.drug, name: e.detail.value || "" });
  },

  onQtyInput(e) {
    this.setDrug({ ...this.data.drug, qty: e.detail.value || "" });
  },

  onCategoryChange(e) {
    const option = CATEGORY_OPTIONS[Number(e.detail.value)] || CATEGORY_OPTIONS[5];
    this.setDrug({ ...this.data.drug, category: option.value });
  },

  onLocationChange(e) {
    this.setDrug({ ...this.data.drug, location: LOCATION_OPTIONS[Number(e.detail.value)] || "药箱" });
  },

  onOpenStateChange(e) {
    this.setDrug({ ...this.data.drug, openState: OPEN_STATE_OPTIONS[Number(e.detail.value)] || "未开封" });
  },

  onDateChange(e) {
    this.setDrug({ ...this.data.drug, date: e.detail.value || "" });
  },

  saveDrug() {
    const drug = this.data.drug;
    if (!String(drug.name || "").trim()) return wx.showToast({ title: "请填写药品名称", icon: "none" });
    if (!String(drug.qty || "").trim()) return wx.showToast({ title: "请填写数量", icon: "none" });
    if (!drug.date) return wx.showToast({ title: "请选择日期", icon: "none" });

    if (this.data.warnings.length) {
      wx.showModal({
        title: "请核对识别结果",
        content: this.data.warnings.join("\n").slice(0, 500),
        confirmText: "继续保存",
        success: res => {
          if (res.confirm) this.persistDrug();
        }
      });
      return;
    }
    this.persistDrug();
  },

  async persistDrug() {
    if (this.data.isSaving) return;
    const drug = this.data.drug;
    this.setData({ isSaving: true });
    wx.showLoading({ title: "保存中...", mask: true });
    try {
      await drugService.createDrug({
        ...drug,
        source: "voice",
        recognizedText: this.data.recognizedText,
        attachments: this.data.audioFileID ? { images: [], audios: [this.data.audioFileID] } : { images: [], audios: [] }
      });
      wx.showToast({
        title: "保存成功",
        icon: "success",
        success: () => setTimeout(() => wx.switchTab({ url: "/pages/drug-list/drug-list" }), 900)
      });
    } catch (err) {
      console.error("保存语音药品失败", err);
      wx.showToast({ title: "保存失败，请检查云数据库", icon: "none" });
    } finally {
      wx.hideLoading();
      this.setData({ isSaving: false });
    }
  },

  addPickerIndexes(drug) {
    const categoryIndex = Math.max(0, CATEGORY_OPTIONS.findIndex(option => option.value === drug.category));
    const locationIndex = Math.max(0, LOCATION_OPTIONS.findIndex(option => option === drug.location));
    const openStateIndex = Math.max(0, OPEN_STATE_OPTIONS.findIndex(option => option === drug.openState));
    return {
      ...drug,
      categoryIndex,
      locationIndex,
      openStateIndex,
      categoryText: (CATEGORY_OPTIONS[categoryIndex] || CATEGORY_OPTIONS[5]).name,
      locationText: drug.location || "药箱",
      openStateText: drug.openState || "未开封"
    };
  },

  normalizeOpenState(value) {
    if (value === "已开封" || value === "Opened" || value === "opened") return "已开封";
    if (value === "独立包装" || value === "Independent" || value === "independent") return "独立包装";
    return "未开封";
  },

  getDateFromOffset(days) {
    const date = new Date();
    date.setDate(date.getDate() + days);
    return this.formatDate(date);
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  },

  goBack() {
    wx.navigateBack();
  },

  goToList() {
    wx.switchTab({ url: "/pages/drug-list/drug-list" });
  },

  onUnload() {
    if (this.data.isRecording) recorderManager.stop();
  }
});
