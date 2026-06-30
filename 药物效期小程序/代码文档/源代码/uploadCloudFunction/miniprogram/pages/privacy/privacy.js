// pages/privacy/privacy.js
Page({
  data: {
    privacyContent: {
      title: '隐私协议',
      version: 'v1.0.0',
      lastUpdated: '2024年1月1日',
      sections: [
        {
          title: '信息收集范围',
          content: '我们仅在提供功能所必需范围内收集数据，包括账号信息、药物录入记录和消息提醒设置。'
        },
        {
          title: '拍照与语音权限',
          content: '拍照与语音内容仅用于识别和结构化解析，不会在未经授权情况下对外共享。处理完成后，原始图片和语音数据不会保留。'
        },
        {
          title: '数据存储与安全',
          content: '您的药物数据将存储在云数据库中。我们会采取必要的访问控制与安全措施保护您的个人信息。'
        },
        {
          title: '信息共享',
          content: '我们不会与任何第三方共享您的个人信息，除非获得您的明确同意或法律法规要求。'
        },
        {
          title: '用户权利',
          content: '您可以随时查看、编辑或删除您的药物记录。在设置页可关闭提醒推送或数据同步功能。如需注销账号，请联系客服。'
        },
        {
          title: '隐私政策更新',
          content: '我们可能不时更新本隐私政策。重大变更将通过应用内通知或弹窗方式告知您。'
        }
      ],
      contactInfo: {
        email: 'privacy@drugmanager.com',
        company: '药物效期管家团队'
      }
    }
  },

  onLoad() {
    // 可以在这里获取最新的隐私政策版本
  },

  // 返回上一页
  goBack() {
    wx.navigateBack();
  },

  // 复制邮箱
  copyEmail() {
    const email = this.data.privacyContent.contactInfo.email;
    wx.setClipboardData({
      data: email,
      success: () => {
        wx.showToast({
          title: '邮箱已复制',
          icon: 'success'
        });
      }
    });
  },

  // 分享隐私政策
  sharePrivacy() {
    // 分享功能
    console.log('分享隐私政策');
  }
});
