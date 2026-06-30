const drugService = require("../../utils/drugService");

Page({
  data: {
    statusBarHeight: 20,
    expiredCount: 0,
    expiringCount: 0,
    normalCount: 0,
    wasteText: "正在加载药箱概览...",
    priorityText: "云数据库同步中，请稍候。",
    isReminderEmpty: true,
    reminderList: []
  },

  onLoad() {
    const systemInfo = wx.getSystemInfoSync();
    this.setData({ statusBarHeight: systemInfo.statusBarHeight || 20 });
    this.loadDrugsData();
  },

  onShow() {
    this.loadDrugsData();
  },

  async loadDrugsData() {
    try {
      const drugs = await drugService.listDrugs();
      const expired = drugs.filter(item => item.status === "expired").length;
      const expiring = drugs.filter(item => item.status === "expiring").length;
      const normal = drugs.filter(item => item.status === "normal").length;
      const focusCount = expired + expiring;
      const reminderList = this.getReminderList(drugs);

      this.setData({
        expiredCount: expired,
        expiringCount: expiring,
        normalCount: normal,
        wasteText: `当前需关注 ${focusCount} 件药品`,
        priorityText: expiring > 0
          ? `${expiring} 件药品即将到期，建议优先处理。`
          : "暂无紧急临期药品，继续保持定期整理。",
        isReminderEmpty: reminderList.length === 0,
        reminderList
      });
    } catch (err) {
      console.error("加载首页药品失败", err);
      this.setData({
        wasteText: "药箱概览加载失败",
        priorityText: "请检查云函数部署和网络状态。",
        isReminderEmpty: true,
        reminderList: []
      });
    }
  },

  getReminderList(drugs) {
    return drugs
      .filter(item => item.status === "expired" || item.status === "expiring")
      .sort((a, b) => this.getUrgencyValue(a) - this.getUrgencyValue(b))
      .slice(0, 3)
      .map(item => ({
        id: item.id,
        name: item.name || "未命名药品",
        openState: item.openState || "未开封",
        tagText: this.getReminderTag(item),
        tagClass: item.status === "expired" ? "tag-danger" : "tag-warning"
      }));
  },

  getReminderTag(item) {
    if (item.days === null || item.days === undefined || Number.isNaN(Number(item.days))) {
      return "日期未知";
    }
    if (item.status === "expired") {
      return `已过期 ${Math.abs(Number(item.days))} 天`;
    }
    return `剩余 ${Number(item.days)} 天`;
  },

  getUrgencyValue(item) {
    if (item.status === "expired") return -1000 + Number(item.days || 0);
    if (item.status === "expiring") return Number(item.days || 0);
    return 9999;
  },

  goToFoodList(e) {
    const filter = e.currentTarget.dataset.filter;
    if (filter) wx.setStorageSync("drugFilter", filter);
    wx.switchTab({ url: "/pages/drug-list/drug-list" });
  },

  goToCapture() {
    wx.navigateTo({ url: "/pages/capture-input/capture-input" });
  },

  goToVoice() {
    wx.navigateTo({ url: "/pages/voice-input/voice-input" });
  },

  goToManual() {
    wx.navigateTo({ url: "/pages/manual-input/manual-input" });
  },

  viewAllReminders() {
    wx.switchTab({ url: "/pages/drug-list/drug-list" });
  },

  onReminderTap(e) {
    wx.setStorageSync("focusDrugId", e.currentTarget.dataset.id);
    wx.switchTab({ url: "/pages/drug-list/drug-list" });
  }
});
