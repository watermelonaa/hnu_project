const drugService = require("../../utils/drugService");

const CHIP_SOURCE = [
  { key: "all", name: "全部" },
  { key: "attention", name: "需处理" },
  { key: "expired", name: "已过期" },
  { key: "expiring", name: "临期" },
  { key: "sealed", name: "未开封" },
  { key: "opened", name: "已开封" }
];

Page({
  data: {
    drugs: [],
    filteredDrugs: [],
    isEmpty: true,
    currentFilter: "all",
    currentSort: "expiryCountdown",
    searchKeyword: "",
    sortPanelClass: "",
    chips: CHIP_SOURCE.map(item => ({ ...item, activeClass: item.key === "all" ? "active" : "" })),
    focusId: ""
  },

  onLoad(options) {
    const nextData = {};
    if (options.filter) nextData.currentFilter = options.filter;
    if (options.sort) nextData.currentSort = options.sort;
    if (options.focus) nextData.focusId = String(options.focus);

    const filterFromStorage = wx.getStorageSync("drugFilter");
    if (filterFromStorage) {
      nextData.currentFilter = filterFromStorage;
      wx.removeStorageSync("drugFilter");
    }

    const focusIdFromStorage = wx.getStorageSync("focusDrugId");
    if (focusIdFromStorage) {
      nextData.focusId = String(focusIdFromStorage);
      wx.removeStorageSync("focusDrugId");
    }

    if (Object.keys(nextData).length) this.setData(nextData);
    this.updateChips();
    this.loadDrugs();
  },

  onShow() {
    this.loadDrugs();
  },

  async loadDrugs() {
    wx.showNavigationBarLoading();
    try {
      const drugs = await drugService.listDrugs({ sort: this.data.currentSort });
      this.setData({ drugs });
      this.applyFilterAndSort();
    } catch (err) {
      console.error("加载药品列表失败", err);
      wx.showToast({ title: "加载失败", icon: "none" });
      this.setData({ filteredDrugs: [], isEmpty: true });
    } finally {
      wx.hideNavigationBarLoading();
    }
  },

  applyFilterAndSort() {
    let filtered = this.filterDrugs([...this.data.drugs]);
    const keyword = String(this.data.searchKeyword || "").trim().toLowerCase();

    if (keyword) {
      filtered = filtered.filter(drug => {
        const searchable = [
          drug.name,
          drug.qty,
          drug.openState,
          this.getStatusLabel(drug.status),
          this.getFormText(drug.dosageForm)
        ].join(" ").toLowerCase();
        return searchable.includes(keyword);
      });
    }

    const viewItems = this.sortDrugs(filtered).map(item => this.toViewDrug(item));
    this.setData({
      filteredDrugs: viewItems,
      isEmpty: viewItems.length === 0
    });
  },

  filterDrugs(drugs) {
    switch (this.data.currentFilter) {
      case "attention":
        return drugs.filter(drug => drug.status === "expired" || drug.status === "expiring");
      case "expired":
        return drugs.filter(drug => drug.status === "expired");
      case "expiring":
        return drugs.filter(drug => drug.status === "expiring");
      case "sealed":
        return drugs.filter(drug => drug.openState === "未开封");
      case "opened":
        return drugs.filter(drug => drug.openState === "已开封");
      default:
        return drugs;
    }
  },

  sortDrugs(drugs) {
    const sorted = [...drugs];
    if (this.data.currentSort === "name") {
      return sorted.sort((a, b) => String(a.name || "").localeCompare(String(b.name || ""), "zh-CN"));
    }
    if (this.data.currentSort === "createdAt") {
      return sorted.sort((a, b) => Number(b.createdAt || 0) - Number(a.createdAt || 0));
    }
    return sorted.sort((a, b) => this.getSortDays(a) - this.getSortDays(b));
  },

  getSortDays(drug) {
    if (drug.days === null || drug.days === undefined || Number.isNaN(Number(drug.days))) {
      return Number.MAX_SAFE_INTEGER;
    }
    return Number(drug.days);
  },

  toViewDrug(drug) {
    const dosageForm = drug.dosageForm || "other";
    const status = drug.status || "unknown";
    const days = drug.days === null || drug.days === undefined ? null : Number(drug.days);

    return {
      ...drug,
      name: drug.name || "未命名药品",
      qty: drug.qty || "数量未填写",
      formText: this.getFormText(dosageForm),
      openState: drug.openState || "未开封",
      displayDays: this.getDisplayDays(days, status),
      statusText: this.getStatusLabel(status),
      statusClass: this.getStatusClass(status),
      iconClass: this.getIconClass(dosageForm),
      iconSrc: this.getIconSrc(dosageForm),
      focusClass: String(this.data.focusId) === String(drug.id) ? "focus" : ""
    };
  },

  getDisplayDays(days, status) {
    if (days === null || Number.isNaN(days)) return "日期未知";
    if (status === "expired" || days < 0) return `已过期 ${Math.abs(days)} 天`;
    return `剩余 ${days} 天`;
  },

  getStatusLabel(status) {
    const map = {
      expired: "已过期",
      expiring: "临期",
      normal: "正常",
      unknown: "未知"
    };
    return map[status] || "正常";
  },

  getStatusClass(status) {
    const map = {
      expired: "status-expired",
      expiring: "status-expiring",
      normal: "status-normal",
      unknown: "status-unknown"
    };
    return map[status] || "status-normal";
  },

  getFormText(form) {
    const map = {
      bottle: "瓶装",
      blister: "板装",
      pouch: "袋装",
      tube: "软膏",
      box: "盒装",
      other: "其他"
    };
    return map[form] || "其他";
  },

  getIconClass(form) {
    const map = {
      bottle: "icon-blue",
      blister: "icon-pink",
      pouch: "icon-green",
      tube: "icon-amber",
      box: "icon-blue"
    };
    return map[form] || "icon-blue";
  },

  getIconSrc(form) {
    const map = {
      bottle: "/images/icons/ui/bottle-white.svg",
      blister: "/images/icons/ui/blister-white.svg",
      pouch: "/images/icons/ui/pouch-white.svg",
      tube: "/images/icons/ui/tube-white.svg",
      box: "/images/icons/ui/box-white.svg"
    };
    return map[form] || "/images/icons/ui/pill-white.svg";
  },

  updateChips() {
    this.setData({
      chips: CHIP_SOURCE.map(item => ({
        ...item,
        activeClass: item.key === this.data.currentFilter ? "active" : ""
      }))
    });
  },

  toggleSortPanel() {
    this.setData({
      sortPanelClass: this.data.sortPanelClass ? "" : "open"
    });
  },

  switchFilter(e) {
    this.setData({ currentFilter: e.currentTarget.dataset.filter || "all" });
    this.updateChips();
    this.applyFilterAndSort();
  },

  switchSort(e) {
    this.setData({
      currentSort: e.currentTarget.dataset.sort || "expiryCountdown",
      sortPanelClass: ""
    });
    this.applyFilterAndSort();
  },

  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value || "" });
    this.applyFilterAndSort();
  },

  editDrug(e) {
    wx.navigateTo({
      url: `/pages/edit-drug/edit-drug?id=${e.currentTarget.dataset.id}`
    });
  },

  deleteDrug(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: "删除药品",
      content: "确定要删除这条药品记录吗？",
      success: async res => {
        if (!res.confirm) return;
        wx.showLoading({ title: "删除中...", mask: true });
        try {
          await drugService.deleteDrug(id);
          const drugs = this.data.drugs.filter(drug => String(drug.id) !== String(id));
          this.setData({ drugs });
          this.applyFilterAndSort();
          wx.showToast({ title: "已删除", icon: "success" });
        } catch (err) {
          console.error("删除药品失败", err);
          wx.showToast({ title: "删除失败", icon: "none" });
        } finally {
          wx.hideLoading();
        }
      }
    });
  }
});
