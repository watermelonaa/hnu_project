const drugService = require("../../utils/drugService");

Page({
  data: {
    messages: [],
    inputText: "",
    tipsText: "正在根据当前药箱生成 AI 建议...",
    isLoading: false,
    sendButtonClass: "",
    quickActions: [
      {
        text: "哪些药快过期？",
        prompt: "请根据当前药箱列出已经过期和3天内临期的药品，并按优先级排序。"
      },
      {
        text: "今日处理清单",
        prompt: "请根据当前药箱生成今天优先处理的药品清单。"
      },
      {
        text: "保存建议",
        prompt: "请根据当前药箱药品类型，给出家庭药箱分类保存和开封后管理建议。"
      },
      {
        text: "安全提醒",
        prompt: "请提醒我药物相互作用咨询时需要注意什么，不要直接做医疗诊断。"
      }
    ],
    aiTools: [
      {
        text: "生成风险报告",
        prompt: "请生成一份药箱风险报告，包含已过期数量、临期药品、优先处理清单和安全提醒。"
      },
      {
        text: "生成提醒计划",
        prompt: "请根据当前药箱生成今天、明天和未来3天的效期提醒计划。"
      }
    ]
  },

  onLoad() {
    this.loadInitialMessages();
    this.generatePersonalizedTips();
  },

  onShow() {
    this.generatePersonalizedTips();
  },

  loadInitialMessages() {
    const messages = wx.getStorageSync("aiChatMessages");
    if (Array.isArray(messages) && messages.length) {
      this.setData({ messages });
      return;
    }
    this.setData({
      messages: [
        {
          role: "ai",
          content: "你好，我可以帮你汇总药品效期风险、生成提醒计划，也可以回答一般药品保存问题。"
        }
      ]
    });
  },

  async generatePersonalizedTips() {
    try {
      const drugs = await drugService.listDrugs();
      const expired = drugs.filter(item => item.status === "expired");
      const expiring = drugs
        .filter(item => item.status === "expiring")
        .sort((a, b) => Number(a.days || 0) - Number(b.days || 0));

      if (expired.length || expiring.length) {
        const names = [...expired, ...expiring]
          .slice(0, 3)
          .map(item => this.formatTipItem(item))
          .join("、");
        this.setData({ tipsText: `建议优先关注：${names}` });
        return;
      }

      this.setData({
        tipsText: "当前暂无过期或临期药品，可以询问 AI 如何分类保存和设置提醒。"
      });
    } catch (err) {
      console.error("生成 AI 提示失败", err);
      this.setData({ tipsText: "药箱数据加载成功后，将自动生成 AI 建议。" });
    }
  },

  formatTipItem(item) {
    if (item.status === "expired") {
      return `${item.name} 已过期 ${Math.abs(Number(item.days || 0))} 天`;
    }
    return `${item.name} 剩余 ${Number(item.days || 0)} 天`;
  },

  onInputChange(e) {
    this.setData({ inputText: e.detail.value || "" });
  },

  sendMessage() {
    const text = this.data.inputText.trim();
    if (!text || this.data.isLoading) return;
    this.sendPrompt(text, text);
  },

  onQuickActionTap(e) {
    this.sendPrompt(e.currentTarget.dataset.prompt, e.currentTarget.dataset.text);
  },

  onToolTap(e) {
    this.sendPrompt(e.currentTarget.dataset.prompt, e.currentTarget.dataset.text);
  },

  async sendPrompt(prompt, displayText) {
    if (this.data.isLoading) return;

    const userMessage = { role: "user", content: displayText };
    const messages = [...this.data.messages, userMessage];
    this.setData({ messages, inputText: "", isLoading: true, sendButtonClass: "disabled" });
    this.saveMessages(messages);
    this.scrollToBottom();

    try {
      const drugs = await drugService.listDrugs();
      const res = await wx.cloud.callFunction({
        name: "aiAssistant",
        data: {
          action: "chat",
          question: prompt,
          messages: messages.slice(-8),
          drugs
        }
      });

      const result = res.result || {};
      if (!result.success) throw new Error(result.message || "AI 服务暂不可用");
      this.appendAiMessage(result.data && result.data.reply ? result.data.reply : "AI 暂未返回内容，请稍后重试。");
    } catch (err) {
      console.error("AI 对话失败", err);
      this.appendAiMessage("AI 服务暂不可用。你仍可以先查看药品列表中的过期和临期状态。");
    } finally {
      this.setData({ isLoading: false, sendButtonClass: "" });
      this.scrollToBottom();
    }
  },

  appendAiMessage(content) {
    const messages = [...this.data.messages, { role: "ai", content }];
    this.setData({ messages });
    this.saveMessages(messages);
  },

  saveMessages(messages) {
    wx.setStorageSync("aiChatMessages", messages.slice(-30));
  },

  clearChat() {
    wx.showModal({
      title: "清空对话",
      content: "确定要清空所有对话记录吗？",
      success: res => {
        if (!res.confirm) return;
        wx.removeStorageSync("aiChatMessages");
        this.loadInitialMessages();
        wx.showToast({ title: "已清空", icon: "success" });
      }
    });
  },

  scrollToBottom() {
    setTimeout(() => {
      wx.pageScrollTo({ scrollTop: 99999, duration: 200 });
    }, 100);
  }
});
