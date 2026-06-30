const drugService = require("../../utils/drugService");

const MESSAGE_STATE_KEY = "messageState";

Page({
  data: {
    messages: [],
    unreadCount: 0,
    hasMessages: false,
    hasUnread: false,
    isLoading: false,
    emptyDesc: "当有药品临近过期或已经过期时，会在这里提醒您。"
  },

  onLoad() {
    this.loadMessages();
  },

  onShow() {
    this.loadMessages();
  },

  async loadMessages() {
    const settings = wx.getStorageSync("settings") || {};
    if (settings.reminderPush === false) {
      this.setData({
        messages: [],
        unreadCount: 0,
        hasMessages: false,
        hasUnread: false,
        isLoading: false,
        emptyDesc: "站内提醒已关闭，可在偏好设置中重新开启。"
      });
      return;
    }

    this.setData({ isLoading: true, emptyDesc: "正在同步云端药箱提醒..." });

    try {
      const drugs = await drugService.listDrugs();
      const messages = this.buildExpiryMessages(drugs);
      this.setMessages(messages);
      this.setData({
        emptyDesc: "当前暂无过期或临期药品提醒。"
      });
    } catch (err) {
      console.error("加载提醒消息失败", err);
      this.setMessages([]);
      this.setData({
        emptyDesc: "提醒加载失败，请检查云函数部署和网络状态。"
      });
    } finally {
      this.setData({ isLoading: false });
    }
  },

  buildExpiryMessages(drugs) {
    const state = this.getMessageState();
    const nowText = this.formatTime(new Date());

    return (drugs || [])
      .filter(item => this.shouldRemind(item))
      .sort((a, b) => this.getUrgencyValue(a) - this.getUrgencyValue(b))
      .map(item => this.toMessage(item, nowText, state))
      .filter(item => !state.deletedMap[item.id]);
  },

  shouldRemind(item) {
    if (!item) return false;
    return item.status === "expired" || item.status === "expiring";
  },

  toMessage(item, timeText, state) {
    const status = item.status === "expired" || Number(item.days) < 0 ? "expired" : "expiring";
    const id = `expiry-${status}-${item.id}-${item.date || "unknown"}`;
    const days = Number(item.days || 0);
    const name = item.name || "未命名药品";

    return {
      id,
      title: this.getMessageTitle(name, status, days),
      content: this.getMessageContent(item, status, days),
      time: timeText,
      timestamp: Date.now(),
      type: status,
      typeClass: status === "expired" ? "danger-bg" : "warning-bg",
      iconSrc: status === "expired" ? "/images/icons/ui/alert-red.svg" : "/images/icons/ui/bell-orange.svg",
      drugId: item.id,
      isRead: Boolean(state.readMap[id])
    };
  },

  getMessageTitle(name, status, days) {
    if (status === "expired") return `${name}已过期`;
    if (days === 0) return `${name}今天到期`;
    if (days === 1) return `${name}明天到期`;
    return `${name}将在 ${days} 天后到期`;
  },

  getMessageContent(item, status, days) {
    const qtyText = item.qty ? `，库存 ${item.qty}` : "";
    const openText = item.openState ? `，状态 ${item.openState}` : "";
    if (status === "expired") {
      return `已过期 ${Math.abs(days)} 天${qtyText}${openText}，建议立即核对并停止继续使用。`;
    }
    return `剩余 ${days} 天到期${qtyText}${openText}，建议优先处理或补充库存。`;
  },

  setMessages(messages) {
    const viewMessages = messages.map(item => ({
      ...item,
      readClass: item.isRead ? "read" : "unread"
    }));
    const unreadCount = viewMessages.filter(item => !item.isRead).length;

    this.setData({
      messages: viewMessages,
      unreadCount,
      hasMessages: viewMessages.length > 0,
      hasUnread: unreadCount > 0
    });
  },

  getMessageState() {
    const state = wx.getStorageSync(MESSAGE_STATE_KEY) || {};
    return {
      readMap: state.readMap || {},
      deletedMap: state.deletedMap || {}
    };
  },

  saveMessageState(state) {
    wx.setStorageSync(MESSAGE_STATE_KEY, {
      readMap: state.readMap || {},
      deletedMap: state.deletedMap || {}
    });
  },

  markAsRead() {
    const state = this.getMessageState();
    this.data.messages.forEach(item => {
      state.readMap[item.id] = true;
    });
    this.saveMessageState(state);
    this.setMessages(this.data.messages.map(item => ({ ...item, isRead: true })));
  },

  markSingleAsRead(e) {
    const id = e.currentTarget.dataset.id;
    const state = this.getMessageState();
    state.readMap[id] = true;
    this.saveMessageState(state);
    this.setMessages(this.data.messages.map(item => (
      item.id === id ? { ...item, isRead: true } : item
    )));
  },

  onMessageTap(e) {
    const id = e.currentTarget.dataset.id;
    const message = this.data.messages.find(item => item.id === id);
    if (!message) return;

    this.markSingleAsRead(e);

    if (message.drugId) {
      wx.setStorageSync("focusDrugId", message.drugId);
      wx.switchTab({ url: "/pages/drug-list/drug-list" });
      return;
    }

    wx.showModal({
      title: message.title,
      content: message.content,
      showCancel: false,
      confirmText: "知道了"
    });
  },

  deleteMessage(e) {
    const id = e.currentTarget.dataset.id;

    wx.showModal({
      title: "确认删除",
      content: "确定要删除这条提醒吗？",
      success: res => {
        if (!res.confirm) return;
        const state = this.getMessageState();
        state.deletedMap[id] = true;
        this.saveMessageState(state);
        this.setMessages(this.data.messages.filter(item => item.id !== id));
        wx.showToast({ title: "已删除", icon: "success" });
      }
    });
  },

  clearAllMessages() {
    if (this.data.messages.length === 0) {
      wx.showToast({ title: "暂无消息", icon: "none" });
      return;
    }

    wx.showModal({
      title: "清空消息",
      content: "确定要清空当前所有提醒吗？",
      confirmColor: "#ef4444",
      success: res => {
        if (!res.confirm) return;
        const state = this.getMessageState();
        this.data.messages.forEach(item => {
          state.deletedMap[item.id] = true;
        });
        this.saveMessageState(state);
        this.setMessages([]);
        wx.showToast({ title: "已清空", icon: "success" });
      }
    });
  },

  getUrgencyValue(item) {
    if (item.status === "expired") return -1000 + Number(item.days || 0);
    if (item.status === "expiring") return Number(item.days || 0);
    return 9999;
  },

  formatTime(date) {
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${month}/${day} ${hours}:${minutes}`;
  },

  goBack() {
    wx.navigateBack();
  }
});
