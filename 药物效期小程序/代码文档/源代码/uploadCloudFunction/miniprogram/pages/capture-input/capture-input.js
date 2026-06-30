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

Page({
  data: {
    entrySheetClass: "",
    isRecognizing: false,
    recognizingClass: "",
    shutterText: "点击拍照识别",
    cameraTip: "请将药品包装或标签放入取景框",
    isSaving: false,
    selectedImage: "",
    selectedImageFileID: "",
    detectedItems: [],
    categoryOptions: CATEGORY_OPTIONS,
    locationOptions: LOCATION_OPTIONS,
    openStateOptions: OPEN_STATE_OPTIONS
  },

  onLoad() {
    this.cameraContext = wx.createCameraContext();
    this.resetDetectedItems();
  },

  noop() {},

  resetDetectedItems() {
    this.setData({
      detectedItems: [this.withAiChecks(this.createEmptyItem("请确认药品信息。", 0))]
    });
  },

  createEmptyItem(note = "", confidence = 0) {
    return this.addPickerIndexes({
      name: "",
      qty: "",
      category: "other",
      location: "药箱",
      openState: "未开封",
      date: "",
      confidence,
      note,
      confidenceText: "低",
      confidenceClass: "confidence-low",
      warnings: []
    });
  },

  takePhoto() {
    if (this.data.isRecognizing) return;

    if (!this.cameraContext) {
      this.cameraContext = wx.createCameraContext();
    }

    this.cameraContext.takePhoto({
      quality: "high",
      success: res => {
        this.recognizeImage(res.tempImagePath);
      },
      fail: err => {
        console.error("拍照失败", err);
        wx.showToast({ title: "无法打开摄像头，请检查授权", icon: "none" });
      }
    });
  },

  chooseAlbumImage() {
    if (this.data.isRecognizing) return;

    this.chooseImageFile("album")
      .then(filePath => {
        if (filePath) this.recognizeImage(filePath);
      })
      .catch(err => {
        console.error("选择相册图片失败", err);
        wx.showToast({ title: "选择图片失败", icon: "none" });
      });
  },

  async recognizeImage(filePath) {
    if (!filePath || this.data.isRecognizing) return;

    this.setData({
      selectedImage: filePath,
      selectedImageFileID: "",
      isRecognizing: true,
      recognizingClass: "loading",
      shutterText: "识别中...",
      cameraTip: "正在识别图片内容"
    });
    wx.showLoading({ title: "识别中...", mask: true });

    try {
      let fileID = "";
      try {
        fileID = await this.uploadImage(filePath);
      } catch (uploadErr) {
        console.error("图片上传失败", uploadErr);
        wx.showToast({ title: "上传失败，可手动填写", icon: "none" });
        this.resetDetectedItems();
        this.openEntrySheet();
        return;
      }

      const res = await wx.cloud.callFunction({
        name: "aiAssistant",
        data: { action: "visionRecognize", fileID }
      });

      const result = res.result || {};
      if (!result.success) throw new Error(result.message || "识别失败");

      this.setData({
        selectedImageFileID: fileID,
        detectedItems: this.normalizeItems(result.data && result.data.items)
      });
      this.openEntrySheet();
    } catch (err) {
      console.error("拍照识别失败", err);
      wx.showToast({ title: "可手动编辑", icon: "none" });
      this.resetDetectedItems();
      this.openEntrySheet();
    } finally {
      wx.hideLoading();
      this.setData({
        isRecognizing: false,
        recognizingClass: "",
        shutterText: "点击拍照识别",
        cameraTip: this.data.selectedImage ? "可重拍或从相册重新选择" : "请将药品包装或标签放入取景框"
      });
    }
  },

  chooseImageFile(sourceType = "album") {
    return new Promise((resolve, reject) => {
      const handleFail = err => {
        if (err && String(err.errMsg || "").includes("cancel")) {
          resolve("");
          return;
        }
        reject(err);
      };

      if (wx.chooseMedia) {
        wx.chooseMedia({
          count: 1,
          mediaType: ["image"],
          sourceType: [sourceType],
          sizeType: ["compressed"],
          success: res => resolve(res.tempFiles && res.tempFiles[0] && res.tempFiles[0].tempFilePath),
          fail: handleFail
        });
        return;
      }

      wx.chooseImage({
        count: 1,
        sourceType: [sourceType],
        sizeType: ["compressed"],
        success: res => resolve(res.tempFilePaths && res.tempFilePaths[0]),
        fail: handleFail
      });
    });
  },

  uploadImage(filePath) {
    const ext = this.getFileExt(filePath) || "jpg";
    return wx.cloud.uploadFile({
      cloudPath: `ai-inputs/images/${Date.now()}-${Math.random().toString(16).slice(2)}.${ext}`,
      filePath
    }).then(res => res.fileID);
  },

  normalizeItems(items) {
    const list = Array.isArray(items) && items.length
      ? items
      : [this.createEmptyItem("识别结果不确定，请手动补充。", 0)];

    return list.map(item => this.withAiChecks(this.addPickerIndexes({
      name: item.name || "",
      qty: item.qty || "",
      category: item.category || "other",
      location: item.location || "药箱",
      openState: this.normalizeOpenState(item.openState),
      date: item.date || "",
      confidence: Number(item.confidence || 0),
      note: item.note || ""
    })));
  },

  withAiChecks(item) {
    const confidence = Number(item.confidence || 0);
    const warnings = this.getItemWarnings(item);
    let confidenceText = "高";
    let confidenceClass = "confidence-high";

    if (confidence < 0.45) {
      confidenceText = "低";
      confidenceClass = "confidence-low";
    } else if (confidence < 0.75) {
      confidenceText = "中";
      confidenceClass = "confidence-mid";
    }

    const lowConfidenceWarning = "AI 识别可信度偏低，请核对药名、数量和日期。";
    if (confidence < 0.75 && !warnings.includes(lowConfidenceWarning)) {
      warnings.unshift(lowConfidenceWarning);
    }

    return this.decorateItem({ ...item, confidenceText, confidenceClass, warnings });
  },

  getItemWarnings(item) {
    const warnings = [];
    if (!String(item.name || "").trim()) warnings.push("药品名称为空。");
    if (!String(item.qty || "").trim()) warnings.push("数量为空。");
    if (!item.date) warnings.push("到期日期缺失。");

    if (item.date) {
      const days = drugService.getDaysRemaining(item.date);
      if (days < -365 || days > 3650) warnings.push("到期日期看起来异常，请确认年份。");
      if (item.openState === "已开封" && days > 180) warnings.push("已开封药品剩余时间较长，请核对开封后有效期。");
    }

    if (item.category === "antibiotic") warnings.push("抗感染类药品请遵医嘱使用。");
    return warnings;
  },

  openEntrySheet() {
    this.setData({ entrySheetClass: "show" });
  },

  closeEntrySheet() {
    this.setData({ entrySheetClass: "" });
  },

  onNameInput(e) {
    this.updateItem(e.currentTarget.dataset.index, "name", e.detail.value || "");
  },

  onQtyInput(e) {
    this.updateItem(e.currentTarget.dataset.index, "qty", e.detail.value || "");
  },

  onCategoryChange(e) {
    const option = CATEGORY_OPTIONS[Number(e.detail.value)] || CATEGORY_OPTIONS[5];
    this.updateItem(e.currentTarget.dataset.index, "category", option.value);
  },

  onLocationChange(e) {
    this.updateItem(e.currentTarget.dataset.index, "location", LOCATION_OPTIONS[Number(e.detail.value)] || "药箱");
  },

  onOpenStateChange(e) {
    this.updateItem(e.currentTarget.dataset.index, "openState", OPEN_STATE_OPTIONS[Number(e.detail.value)] || "未开封");
  },

  onDateChange(e) {
    this.updateItem(e.currentTarget.dataset.index, "date", e.detail.value || "");
  },

  updateItem(index, key, value) {
    const items = [...this.data.detectedItems];
    const itemIndex = Number(index);
    items[itemIndex] = this.withAiChecks(this.addPickerIndexes({ ...items[itemIndex], [key]: value }));
    this.setData({ detectedItems: items });
  },

  addEmptyItem() {
    this.setData({ detectedItems: [...this.data.detectedItems, this.withAiChecks(this.createEmptyItem())] });
  },

  removeItem(e) {
    const index = Number(e.currentTarget.dataset.index);
    const items = this.data.detectedItems.filter((_, itemIndex) => itemIndex !== index);
    this.setData({ detectedItems: items.length ? items : [this.withAiChecks(this.createEmptyItem())] });
  },

  saveAllDrugs() {
    const items = this.data.detectedItems.map(item => this.withAiChecks(item));

    for (let i = 0; i < items.length; i++) {
      if (!items[i].name.trim()) return wx.showToast({ title: `第 ${i + 1} 项请填写药名`, icon: "none" });
      if (!items[i].qty.trim()) return wx.showToast({ title: `第 ${i + 1} 项请填写数量`, icon: "none" });
      if (!items[i].date) return wx.showToast({ title: `第 ${i + 1} 项请选择日期`, icon: "none" });
    }

    const warningText = items
      .flatMap((item, index) => item.warnings.map(warning => `第 ${index + 1} 项：${warning}`))
      .join("\n");

    if (warningText) {
      wx.showModal({
        title: "请核对识别结果",
        content: warningText.slice(0, 500),
        confirmText: "继续保存",
        success: res => {
          if (res.confirm) this.persistDrugs(items);
        }
      });
      return;
    }

    this.persistDrugs(items);
  },

  async persistDrugs(items) {
    if (this.data.isSaving) return;
    this.setData({ isSaving: true });
    wx.showLoading({ title: "保存中...", mask: true });

    try {
      const attachments = this.data.selectedImageFileID
        ? { images: [this.data.selectedImageFileID], audios: [] }
        : { images: [], audios: [] };
      await drugService.batchCreateDrugs(
        items.map(item => ({ ...item, source: "vision", attachments })),
        { source: "vision" }
      );
      wx.showToast({
        title: `已保存 ${items.length} 项`,
        icon: "success",
        success: () => setTimeout(() => wx.switchTab({ url: "/pages/drug-list/drug-list" }), 900)
      });
    } catch (err) {
      console.error("保存识别药品失败", err);
      wx.showToast({ title: "保存失败，请检查云数据库", icon: "none" });
    } finally {
      wx.hideLoading();
      this.setData({ isSaving: false });
    }
  },

  addPickerIndexes(item) {
    return this.decorateItem({
      ...item,
      categoryIndex: Math.max(0, CATEGORY_OPTIONS.findIndex(option => option.value === item.category)),
      locationIndex: Math.max(0, LOCATION_OPTIONS.findIndex(option => option === item.location)),
      openStateIndex: Math.max(0, OPEN_STATE_OPTIONS.findIndex(option => option === item.openState))
    });
  },

  decorateItem(item) {
    return {
      ...item,
      displayName: item.name || "待确认",
      dateText: item.date || "请选择日期",
      categoryText: (CATEGORY_OPTIONS[item.categoryIndex] || CATEGORY_OPTIONS[5]).name,
      locationText: item.location || "药箱",
      openStateText: item.openState || "未开封"
    };
  },

  normalizeOpenState(value) {
    if (value === "已开封" || value === "Opened" || value === "opened") return "已开封";
    if (value === "独立包装" || value === "Independent" || value === "independent") return "独立包装";
    return "未开封";
  },

  getFileExt(filePath) {
    const match = String(filePath).match(/\.([a-zA-Z0-9]+)(?:\?|$)/);
    return match ? match[1].toLowerCase() : "";
  },

  onCameraError(err) {
    console.error("摄像头错误", err);
    wx.showToast({ title: "摄像头不可用，请检查授权", icon: "none" });
  },

  goBack() {
    wx.navigateBack();
  },

  retakePhoto() {
    this.setData({
      selectedImage: "",
      selectedImageFileID: "",
      entrySheetClass: "",
      shutterText: "点击拍照识别",
      cameraTip: "请将药品包装或标签放入取景框"
    });
  }
});
