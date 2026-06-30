const drugService = require("../../utils/drugService");

Page({
  data: {
    userInfo: {
      nickName: "家庭药箱用户",
      avatarUrl: "/images/avatar.png",
      description: "药品效期云端管理已开启"
    },
    profileDraft: {
      nickName: "",
      avatarUrl: "",
      description: ""
    },
    showProfileEditor: false,
    profileEditorClass: "",
    isProfileSaving: false,
    totalCount: 0,
    remindCount: 0,
    expiredCount: 0,
    expiringCount: 0,
    normalCount: 0,
    healthScore: 100,
    healthTitle: "药箱状态良好",
    healthDesc: "暂无需要处理的药品，建议定期检查效期。",
    lastSyncText: "等待同步",
    quickActions: [
      { iconSrc: "/images/icons/ui/plus-white.svg", title: "新增药品", desc: "手动补充药品信息", type: "navigate", url: "/pages/manual-input/manual-input" },
      { iconSrc: "/images/icons/ui/alert-white.svg", title: "处理提醒", desc: "查看过期和临期药品", type: "filter", filter: "attention" }
    ],
    menuGroups: [
      {
        title: "数据管理",
        items: [
          { iconSrc: "/images/icons/ui/list-blue.svg", name: "全部药品", desc: "查看完整药箱库存", type: "filter", filter: "all" },
          { iconSrc: "/images/icons/ui/bell-blue.svg", name: "消息提醒", desc: "查看过期和临期提醒", type: "navigate", url: "/pages/message-center/message-center" },
          { iconSrc: "/images/icons/ui/sync-blue.svg", name: "刷新数据", desc: "重新同步云端效期状态", type: "refresh" }
        ]
      },
      {
        title: "账号",
        items: [
          { iconSrc: "/images/icons/ui/settings-blue.svg", name: "偏好设置", desc: "提醒时间与基础偏好", type: "navigate", url: "/pages/settings/settings" },
          { iconSrc: "/images/icons/ui/logout-blue.svg", name: "退出登录", desc: "清除当前登录状态", type: "logout" }
        ]
      },
      {
        title: "支持与说明",
        items: [
          { iconSrc: "/images/icons/ui/info-blue.svg", name: "关于我们", desc: "了解项目与团队信息", type: "navigate", url: "/pages/about/about" },
          { iconSrc: "/images/icons/ui/shield-blue.svg", name: "隐私协议", desc: "查看数据与隐私说明", type: "navigate", url: "/pages/privacy/privacy" },
          { iconSrc: "/images/icons/ui/mail-blue.svg", name: "意见反馈", desc: "复制反馈邮箱", type: "feedback" }
        ]
      }
    ]
  },

  onLoad() {
    this.loadUserProfile();
    this.loadStatistics();
  },

  onShow() {
    this.loadStatistics();
  },

  async loadUserProfile() {
    try {
      const profile = await this.callDatabase({
        action: "getUserProfile"
      });
      this.setProfile(profile);
    } catch (err) {
      console.error("加载用户资料失败", err);
      wx.showToast({ title: "资料加载失败", icon: "none" });
    }
  },

  setProfile(profile = {}) {
    const userInfo = {
      nickName: profile.nickName || "家庭药箱用户",
      avatarUrl: profile.avatarUrl || "/images/avatar.png",
      description: profile.description || "药品效期云端管理已开启"
    };
    this.setData({ userInfo });
  },

  async loadStatistics() {
    try {
      const drugs = await drugService.listDrugs();
      const expiredCount = drugs.filter(item => item.status === "expired").length;
      const expiringCount = drugs.filter(item => item.status === "expiring").length;
      const normalCount = drugs.filter(item => item.status === "normal").length;
      const remindCount = expiredCount + expiringCount;
      const health = this.getHealthSummary(drugs.length, expiredCount, expiringCount);
      this.setData({
        totalCount: drugs.length,
        remindCount,
        expiredCount,
        expiringCount,
        normalCount,
        healthScore: health.score,
        healthTitle: health.title,
        healthDesc: health.desc,
        lastSyncText: this.formatTime(new Date())
      });
    } catch (err) {
      console.error("加载个人页统计失败", err);
      this.setData({
        totalCount: 0,
        remindCount: 0,
        expiredCount: 0,
        expiringCount: 0,
        normalCount: 0,
        healthScore: 0,
        healthTitle: "云端数据加载失败",
        healthDesc: "请检查云开发环境和数据库权限后重试。",
        lastSyncText: "同步失败"
      });
    }
  },

  getHealthSummary(total, expired, expiring) {
    if (!total) {
      return {
        score: 100,
        title: "药箱还没有药品",
        desc: "可以先通过手动、拍照或语音录入建立药品清单。"
      };
    }

    const score = Math.max(0, 100 - expired * 28 - expiring * 14);
    if (expired > 0) {
      return {
        score,
        title: "存在过期药品",
        desc: `有 ${expired} 个药品已过期，建议立即核对并处理。`
      };
    }
    if (expiring > 0) {
      return {
        score,
        title: "有临期药品待关注",
        desc: `有 ${expiring} 个药品即将到期，建议优先查看。`
      };
    }
    return {
      score,
      title: "药箱状态良好",
      desc: "暂无过期或临期药品，保持定期检查即可。"
    };
  },

  onMenuItemTap(e) {
    const { type, url, filter } = e.currentTarget.dataset;
    if (type === "logout") {
      this.handleLogout();
      return;
    }
    if (type === "refresh") {
      this.refreshCloudData();
      return;
    }
    if (type === "feedback") {
      this.feedback();
      return;
    }
    if (type === "filter") {
      if (filter && filter !== "all") wx.setStorageSync("drugFilter", filter);
      wx.switchTab({ url: "/pages/drug-list/drug-list" });
      return;
    }
    if (type === "switch") {
      wx.switchTab({ url });
      return;
    }
    if (!url) return;
    wx.navigateTo({
      url,
      fail: err => {
        console.error("页面跳转失败", err);
        wx.showToast({ title: "页面暂不可用", icon: "none" });
      }
    });
  },

  onQuickActionTap(e) {
    const { type, url, filter } = e.currentTarget.dataset;
    if (type === "filter") {
      if (filter && filter !== "all") wx.setStorageSync("drugFilter", filter);
      wx.switchTab({ url: "/pages/drug-list/drug-list" });
      return;
    }
    if (type === "switch") {
      wx.switchTab({ url });
      return;
    }
    wx.navigateTo({
      url,
      fail: err => {
        console.error("快捷入口跳转失败", err);
        wx.showToast({ title: "页面暂不可用", icon: "none" });
      }
    });
  },

  refreshCloudData() {
    wx.showLoading({ title: "同步中...", mask: true });
    this.callDatabase({ action: "refreshExpirySnapshots" })
      .then(() => this.loadStatistics())
      .then(() => {
        wx.showToast({ title: "同步完成", icon: "success" });
      })
      .catch(err => {
        console.error("刷新云端数据失败", err);
        wx.showToast({ title: "同步失败", icon: "none" });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  feedback() {
    wx.showModal({
      title: "意见反馈",
      content: "请将你的建议发送至：support@drugmanager.com",
      confirmText: "复制邮箱",
      success: res => {
        if (!res.confirm) return;
        wx.setClipboardData({
          data: "support@drugmanager.com",
          success: () => wx.showToast({ title: "邮箱已复制", icon: "success" })
        });
      }
    });
  },

  formatTime(date) {
    const hour = String(date.getHours()).padStart(2, "0");
    const minute = String(date.getMinutes()).padStart(2, "0");
    return `今日 ${hour}:${minute}`;
  },

  handleLogout() {
    wx.showModal({
      title: "退出登录",
      content: "确定要退出当前账号吗？",
      success: res => {
        if (!res.confirm) return;
        wx.removeStorageSync("isLogin");
        wx.removeStorageSync("userInfo");
        wx.removeStorageSync("loginTime");
        wx.showToast({
          title: "已退出",
          icon: "success",
          success: () => setTimeout(() => wx.reLaunch({ url: "/pages/login/login" }), 1200)
        });
      }
    });
  },

  editProfile() {
    this.setData({
      profileDraft: { ...this.data.userInfo },
      showProfileEditor: true,
      profileEditorClass: "show"
    });
  },

  closeProfileEditor() {
    if (this.data.isProfileSaving) return;
    this.setData({
      showProfileEditor: false,
      profileEditorClass: ""
    });
  },

  noop() {},

  onProfileNameInput(e) {
    this.setData({ "profileDraft.nickName": e.detail.value || "" });
  },

  onProfileDescriptionInput(e) {
    this.setData({ "profileDraft.description": e.detail.value || "" });
  },

  async chooseProfileAvatar() {
    if (this.data.isProfileSaving) return;
    try {
      const filePath = await this.chooseImageFile();
      if (!filePath) return;
      wx.showLoading({ title: "上传头像...", mask: true });
      const fileID = await wx.cloud.uploadFile({
        cloudPath: `profiles/avatars/${Date.now()}-${Math.random().toString(16).slice(2)}.${this.getFileExt(filePath) || "jpg"}`,
        filePath
      }).then(res => res.fileID);
      this.setData({ "profileDraft.avatarUrl": fileID });
      wx.showToast({ title: "头像已更新", icon: "success" });
    } catch (err) {
      console.error("头像上传失败", err);
      wx.showToast({ title: "头像上传失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },

  chooseImageFile() {
    return new Promise((resolve, reject) => {
      if (wx.chooseMedia) {
        wx.chooseMedia({
          count: 1,
          mediaType: ["image"],
          sourceType: ["album", "camera"],
          sizeType: ["compressed"],
          success: res => resolve(res.tempFiles && res.tempFiles[0] && res.tempFiles[0].tempFilePath),
          fail: reject
        });
        return;
      }

      wx.chooseImage({
        count: 1,
        sourceType: ["album", "camera"],
        sizeType: ["compressed"],
        success: res => resolve(res.tempFilePaths && res.tempFilePaths[0]),
        fail: reject
      });
    });
  },

  getFileExt(filePath) {
    const match = String(filePath).match(/\.([a-zA-Z0-9]+)(?:\?|$)/);
    return match ? match[1].toLowerCase() : "";
  },

  async saveProfile() {
    if (this.data.isProfileSaving) return;
    const profile = {
      nickName: String(this.data.profileDraft.nickName || "").trim(),
      avatarUrl: this.data.profileDraft.avatarUrl || "/images/avatar.png",
      description: String(this.data.profileDraft.description || "").trim()
    };

    if (!profile.nickName) return wx.showToast({ title: "请填写昵称", icon: "none" });
    if (profile.nickName.length > 24) return wx.showToast({ title: "昵称最多 24 个字", icon: "none" });
    if (profile.description.length > 60) return wx.showToast({ title: "简介最多 60 个字", icon: "none" });

    this.setData({ isProfileSaving: true });
    wx.showLoading({ title: "保存中...", mask: true });
    try {
      const savedProfile = await this.callDatabase({
        action: "updateUserProfile",
        profile
      });
      this.setProfile(savedProfile);
      this.setData({
        showProfileEditor: false,
        profileEditorClass: ""
      });
      wx.showToast({ title: "保存成功", icon: "success" });
    } catch (err) {
      console.error("保存用户资料失败", err);
      wx.showToast({ title: "保存失败，请检查云数据库", icon: "none" });
    } finally {
      wx.hideLoading();
      this.setData({ isProfileSaving: false });
    }
  },

  callDatabase(data) {
    return wx.cloud.callFunction({
      name: "database",
      data
    }).then(res => {
      const result = res.result || {};
      if (!result.success) throw new Error(result.message || result.code || "database call failed");
      return result.data;
    });
  },

  switchTab(e) {
    wx.switchTab({ url: e.currentTarget.dataset.url });
  }
});
