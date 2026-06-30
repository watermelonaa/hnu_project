Page({
  data: {
    privacyModalClass: "",
    agreementModalClass: ""
  },

  onLoad() {
    this.checkLoginStatus();
  },

  checkLoginStatus() {
    const isLogin = wx.getStorageSync("isLogin");
    const loginTime = wx.getStorageSync("loginTime");

    if (isLogin === "true" && loginTime) {
      const sevenDays = 7 * 24 * 60 * 60 * 1000;
      if (Date.now() - Number(loginTime) < sevenDays) {
        wx.switchTab({ url: "/pages/home/home" });
      }
    }
  },

  handleWechatLogin() {
    this.setData({
      privacyModalClass: "show"
    });
  },

  agreePrivacy() {
    this.closePrivacyModal();
    this.doWechatLogin();
  },

  disagreePrivacy() {
    this.closePrivacyModal();
    wx.showToast({
      title: "已取消授权",
      icon: "none",
      duration: 2000
    });
  },

  closePrivacyModal() {
    this.setData({
      privacyModalClass: ""
    });
  },

  noop() {},

  openUserAgreement() {
    this.setData({
      agreementModalClass: "show"
    });
  },

  closeUserAgreement() {
    this.setData({
      agreementModalClass: ""
    });
  },

  openPrivacyPolicy() {
    wx.navigateTo({
      url: "/pages/privacy/privacy",
      fail: err => {
        console.error("隐私政策页面跳转失败", err);
        wx.showToast({ title: "隐私政策暂不可用", icon: "none" });
      }
    });
  },

  async doWechatLogin() {
    wx.showLoading({
      title: "登录中...",
      mask: true
    });

    try {
      const loginRes = await this.wxLogin();
      if (!loginRes.code) throw new Error("wx.login 未返回 code");

      const profile = await this.callDatabase({
        action: "getUserProfile"
      });

      const userInfo = {
        nickName: profile.nickName || "家庭药箱用户",
        avatarUrl: profile.avatarUrl || "/images/avatar.png",
        description: profile.description || "药品效期云端管理已开启"
      };

      wx.setStorageSync("userInfo", userInfo);
      wx.setStorageSync("isLogin", "true");
      wx.setStorageSync("loginTime", Date.now());

      wx.hideLoading();
      wx.showToast({
        title: "登录成功",
        icon: "success",
        duration: 1000,
        success: () => {
          setTimeout(() => {
            wx.switchTab({ url: "/pages/home/home" });
          }, 1000);
        }
      });
    } catch (err) {
      console.error("微信登录失败", err);
      wx.hideLoading();
      wx.showToast({
        title: "登录失败，请检查云函数",
        icon: "none"
      });
    }
  },

  wxLogin() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: resolve,
        fail: reject
      });
    });
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

  onModalTap(e) {
    if (e.target.dataset.type === "mask") {
      this.closePrivacyModal();
    }
  },

  onAgreementModalTap(e) {
    if (e.target.dataset.type === "mask") {
      this.closeUserAgreement();
    }
  }
});
