const drugService = require("../../utils/drugService");

const DOSAGE_FORM_OPTIONS = [
  { value: "bottle", name: "瓶装/盒装" },
  { value: "blister", name: "板装" },
  { value: "pouch", name: "袋装/冲剂" },
  { value: "tube", name: "软膏/外用" }
];

const CATEGORY_OPTIONS = [
  { value: "antipyretic", name: "感冒退热" },
  { value: "antibiotic", name: "抗感染" },
  { value: "digestive", name: "肠胃用药" },
  { value: "vitamin", name: "维生素/保健品" },
  { value: "external", name: "外用药" },
  { value: "other", name: "其他" }
];

const OPEN_STATE_OPTIONS = [
  { value: "未开封", name: "未开封" },
  { value: "已开封", name: "已开封" }
];

Page({
  data: {
    drugId: "",
    pageTitle: "药品详情",
    drug: {
      name: "",
      qty: "",
      dosageForm: "bottle",
      category: "other",
      openState: "未开封",
      date: ""
    },
    dosageFormOptions: DOSAGE_FORM_OPTIONS,
    categoryOptions: CATEGORY_OPTIONS,
    openStateOptions: OPEN_STATE_OPTIONS,
    dosageFormIndex: 0,
    categoryIndex: 5,
    openStateIndex: 0,
    showOpenState: true,
    isSaving: false,
    dateText: "请选择到期日期",
    datePlaceholderClass: "placeholder",
    statusPreview: {
      text: "请选择到期日期",
      bgColor: "#eef2ff",
      textColor: "#2563eb"
    }
  },

  onLoad(options) {
    if (!options.id) {
      wx.showToast({ title: "缺少药品ID", icon: "none" });
      return;
    }
    this.setData({ drugId: String(options.id) });
    this.loadDrugData(String(options.id));
  },

  async loadDrugData(id) {
    wx.showLoading({ title: "加载中...", mask: true });
    try {
      const drug = await drugService.getDrug(id);
      if (!drug) {
        wx.showToast({ title: "未找到药品", icon: "none" });
        return;
      }

      const dosageForm = drug.dosageForm || "bottle";
      const category = drug.category || "other";
      const showOpenState = dosageForm === "bottle" || dosageForm === "tube";
      const openState = showOpenState ? drug.openState || "未开封" : "独立包装";
      const date = drug.date || "";

      this.setData({
        pageTitle: drug.name || "药品详情",
        drug: {
          name: drug.name || "",
          qty: drug.qty || "",
          dosageForm,
          category,
          openState,
          date
        },
        dosageFormIndex: this.getOptionIndex(DOSAGE_FORM_OPTIONS, dosageForm, 0),
        categoryIndex: this.getOptionIndex(CATEGORY_OPTIONS, category, 5),
        openStateIndex: this.getOptionIndex(OPEN_STATE_OPTIONS, openState, 0),
        showOpenState,
        dateText: date || "请选择到期日期",
        datePlaceholderClass: date ? "" : "placeholder"
      });

      this.updateStatusPreview(date);
    } catch (err) {
      console.error("加载药品详情失败", err);
      wx.showToast({ title: "加载失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },

  getOptionIndex(options, value, fallback) {
    const index = options.findIndex(item => item.value === value);
    return index >= 0 ? index : fallback;
  },

  onNameInput(e) {
    const value = e.detail.value || "";
    this.setData({
      "drug.name": value,
      pageTitle: value.trim() || "药品详情"
    });
  },

  onQtyInput(e) {
    this.setData({ "drug.qty": e.detail.value || "" });
  },

  onDosageFormChange(e) {
    const index = Number(e.detail.value);
    const option = DOSAGE_FORM_OPTIONS[index] || DOSAGE_FORM_OPTIONS[0];
    const showOpenState = option.value === "bottle" || option.value === "tube";

    this.setData({
      dosageFormIndex: index,
      "drug.dosageForm": option.value,
      "drug.openState": showOpenState ? this.data.drug.openState : "独立包装",
      showOpenState
    });
  },

  onCategoryChange(e) {
    const index = Number(e.detail.value);
    const option = CATEGORY_OPTIONS[index] || CATEGORY_OPTIONS[5];
    this.setData({
      categoryIndex: index,
      "drug.category": option.value
    });
  },

  onOpenStateChange(e) {
    const index = Number(e.detail.value);
    const option = OPEN_STATE_OPTIONS[index] || OPEN_STATE_OPTIONS[0];
    this.setData({
      openStateIndex: index,
      "drug.openState": option.value
    });
  },

  onDateChange(e) {
    const date = e.detail.value || "";
    this.setData({
      "drug.date": date,
      dateText: date || "请选择到期日期",
      datePlaceholderClass: date ? "" : "placeholder"
    });
    this.updateStatusPreview(date);
  },

  updateStatusPreview(date) {
    if (!date) {
      this.setData({
        statusPreview: {
          text: "请选择到期日期",
          bgColor: "#eef2ff",
          textColor: "#2563eb"
        }
      });
      return;
    }

    const days = drugService.getDaysRemaining(date);
    if (days < 0) {
      this.setData({
        statusPreview: {
          text: `已过期 ${Math.abs(days)} 天`,
          bgColor: "#fef2f2",
          textColor: "#dc2626"
        }
      });
      return;
    }

    if (days <= 3) {
      this.setData({
        statusPreview: {
          text: `临期，剩余 ${days} 天`,
          bgColor: "#fffbeb",
          textColor: "#d97706"
        }
      });
      return;
    }

    this.setData({
      statusPreview: {
        text: `正常，剩余 ${days} 天`,
        bgColor: "#f0fdf4",
        textColor: "#16a34a"
      }
    });
  },

  async saveDrug() {
    if (this.data.isSaving) return;

    const drug = this.data.drug;
    if (!drug.name.trim()) return wx.showToast({ title: "请输入药品名称", icon: "none" });
    if (!drug.qty.trim()) return wx.showToast({ title: "请输入数量", icon: "none" });
    if (!drug.date) return wx.showToast({ title: "请选择到期日期", icon: "none" });

    const payload = {
      ...drug,
      name: drug.name.trim(),
      qty: drug.qty.trim(),
      openState: this.data.showOpenState ? drug.openState : "独立包装"
    };

    this.setData({ isSaving: true });
    wx.showLoading({ title: "保存中...", mask: true });

    try {
      await drugService.updateDrug(this.data.drugId, payload);
      wx.showToast({
        title: "保存成功",
        icon: "success",
        success: () => setTimeout(() => wx.navigateBack(), 900)
      });
    } catch (err) {
      console.error("更新药品失败", err);
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      wx.hideLoading();
      this.setData({ isSaving: false });
    }
  },

  cancelEdit() {
    wx.navigateBack();
  }
});
