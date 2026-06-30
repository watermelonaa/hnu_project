// pages/about/about.js
Page({
  data: {
    appName: '药物效期管家',
    version: 'v1.0.0',
    description: '通过拍照识别、语音解析和 AI 对话，帮助家庭用户管理药箱库存与效期，减少过期误用风险，提升用药安全。',
    features: [
      { name: '拍照识别', iconSrc: '/images/icons/ui/camera-blue.svg', desc: '智能识别药品信息' },
      { name: '语音录入', iconSrc: '/images/icons/ui/mic-blue.svg', desc: '语音快速添加药品' },
      { name: 'AI 助手', iconSrc: '/images/icons/ui/bot-blue.svg', desc: '智能问答与用药建议' },
      { name: '效期提醒', iconSrc: '/images/icons/ui/bell-blue.svg', desc: '临期药品自动提醒' }
    ],
    contactInfo: {
      email: 'support@drugmanager.com',
      wechat: 'drug_manager',
      phone: '400-888-1234'
    }
  },

  onLoad() {
    // 可以在这里获取版本信息等
    this.getAppVersion();
  },

  // 获取应用版本
  getAppVersion() {
    // 获取小程序版本信息
    const accountInfo = wx.getAccountInfoSync();
    const envVersion = accountInfo.miniProgram.envVersion;
    
    let versionText = 'v1.0.0';
    if (envVersion === 'develop') {
      versionText = 'v1.0.0（开发版）';
    } else if (envVersion === 'trial') {
      versionText = 'v1.0.0（体验版）';
    } else if (envVersion === 'release') {
      versionText = 'v1.0.0（正式版）';
    }
    
    this.setData({ version: versionText });
  },

  // 返回上一页
  goBack() {
    wx.navigateBack();
  },

  // 复制文本到剪贴板
  copyText(e) {
    const text = e.currentTarget.dataset.text;
    if (text) {
      wx.setClipboardData({
        data: text,
        success: () => {
          wx.showToast({
            title: '已复制',
            icon: 'success'
          });
        }
      });
    }
  },

  // 拨打电话
  makePhoneCall(e) {
    const phone = e.currentTarget.dataset.phone;
    if (phone) {
      wx.makePhoneCall({
        phoneNumber: phone,
        fail: () => {
          wx.showToast({
            title: '拨号失败',
            icon: 'none'
          });
        }
      });
    }
  },

  // 关注公众号（跳转）
  followWechat() {
    wx.showModal({
      title: '关注公众号',
      content: '请在微信中搜索"药物效期管家"关注我们',
      confirmText: '知道了',
      showCancel: false
    });
  }
});
