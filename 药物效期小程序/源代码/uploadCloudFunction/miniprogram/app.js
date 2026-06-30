App({
  onLaunch() {
    if (!wx.cloud) {
      console.error("请使用微信基础库 2.2.3 或以上版本");
      return;
    }

    wx.cloud.init({
      env: "cloud1-d6g8tzu652c61fd0f",
      traceUser: true
    });
  }
});
