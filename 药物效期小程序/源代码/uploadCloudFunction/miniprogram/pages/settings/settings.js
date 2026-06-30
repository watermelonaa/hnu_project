// pages/settings/settings.js
Page({
  data: {
    reminderPush: true,
    dataSync: true,
    remindTime: '08:30',
    remindTimeIndex: 1,
    // 提醒时间选项
    timeOptions: ['08:00', '08:30', '09:00', '19:00', '20:00', '21:00']
  },

  onLoad() {
    this.loadSettings();
  },

  // 加载设置
  loadSettings() {
    const settings = wx.getStorageSync('settings');
    if (settings) {
      const remindTime = settings.remindTime || '08:30';
      this.setData({
        reminderPush: settings.reminderPush !== undefined ? settings.reminderPush : true,
        dataSync: settings.dataSync !== undefined ? settings.dataSync : true,
        remindTime,
        remindTimeIndex: this.getRemindTimeIndex(remindTime)
      });
    }
  },

  // 保存设置
  saveSettings() {
    const settings = {
      reminderPush: this.data.reminderPush,
      dataSync: this.data.dataSync,
      remindTime: this.data.remindTime
    };
    wx.setStorageSync('settings', settings);
  },

  // 切换站内提醒
  toggleReminderPush(e) {
    const newValue = e.detail.value;
    this.setData({ reminderPush: newValue });
    this.saveSettings();
    wx.showToast({
      title: newValue ? '已开启站内提醒' : '已关闭站内提醒',
      icon: 'success'
    });
  },

  // 切换数据同步
  toggleDataSync(e) {
    const newValue = e.detail.value;
    this.setData({ dataSync: newValue });
    this.saveSettings();
    
    if (newValue) {
      this.syncData();
    }
    
    wx.showToast({
      title: newValue ? '已开启同步' : '已关闭同步',
      icon: 'success'
    });
  },

  // 同步数据
  syncData() {
    wx.showLoading({ title: '同步中...' });

    wx.cloud.callFunction({
      name: 'database',
      data: {
        action: 'refreshExpirySnapshots'
      },
      success: () => {
        wx.showToast({
          title: '云端同步成功',
          icon: 'success'
        });
      },
      fail: (err) => {
        console.error('云端同步失败:', err);
        wx.showToast({
          title: '同步失败，请检查云数据库',
          icon: 'none'
        });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  // 选择提醒时间
  onRemindTimeChange(e) {
    const index = Number(e.detail.value || 0);
    const time = this.data.timeOptions[index] || this.data.timeOptions[0];
    this.setData({
      remindTime: time,
      remindTimeIndex: index
    });
    this.saveSettings();
    
    this.updateNotification();
    
    wx.showToast({
      title: '提醒时间已更新',
      icon: 'success'
    });
  },

  // 更新通知时间（设置定时提醒）
  updateNotification() {
    console.log('更新提醒时间至：', this.data.remindTime);
  },

  getRemindTimeIndex(time) {
    const index = this.data.timeOptions.indexOf(time);
    return index >= 0 ? index : 1;
  },

  // 清除缓存
  clearCache() {
    wx.showModal({
      title: '清除缓存',
      content: '确定要清除本地登录和偏好缓存吗？云端药品数据不会被删除。',
      confirmColor: '#ef4444',
      success: (res) => {
        if (res.confirm) {
          // 清除本地存储（保留用户登录信息）
          const userInfo = wx.getStorageSync('userInfo');
          const isLogin = wx.getStorageSync('isLogin');
          
          wx.clearStorageSync();
          
          // 恢复用户登录信息
          if (isLogin && userInfo) {
            wx.setStorageSync('userInfo', userInfo);
            wx.setStorageSync('isLogin', isLogin);
          }
          
          wx.showToast({
            title: '缓存已清除',
            icon: 'success'
          });
          
          // 重新加载数据
          setTimeout(() => {
            wx.reLaunch({
              url: '/pages/home/home'
            });
          }, 1500);
        }
      }
    });
  },

  // 返回上一页
  goBack() {
    wx.navigateBack();
  }
});
