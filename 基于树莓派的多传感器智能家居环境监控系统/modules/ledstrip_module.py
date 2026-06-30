#!/usr/bin/python3
# -*- coding: utf-8 -*-

# 树莓派4B专用：灯带DIN接GPIO18

from rpi_ws281x import PixelStrip, Color
import time

LED_PIN = 18          # GPIO18
LED_COUNT = 4         # 灯珠数量，根据实际修改
LED_FREQ_HZ = 800000
LED_DMA = 10
LED_BRIGHTNESS = 100  # 亮度 0-255
LED_INVERT = False

_strip = None
_led_available = False

def setup_ledstrip():
    global _strip, _led_available
    try:
        _strip = PixelStrip(
            LED_COUNT, LED_PIN, LED_FREQ_HZ,
            LED_DMA, LED_INVERT, LED_BRIGHTNESS
        )
        _strip.begin()
        clear_strip()
        _led_available = True
        print(f"[灯带] GPIO18, {LED_COUNT}颗灯珠, 初始化成功")
        return True
    except Exception as e:
        print(f"[灯带] 初始化失败: {e}")
        print("请检查: 1)是否已安装rpi_ws281x  2)是否用sudo运行")
        _led_available = False
        return False

def set_color(r, g, b):
    """设置所有灯珠颜色"""
    global _strip, _led_available
    if not _led_available:
        return
    color = Color(r, g, b)
    for i in range(LED_COUNT):
        _strip.setPixelColor(i, color)
    _strip.show()

def clear_strip():
    set_color(0, 0, 0)

def led_green():
    set_color(0, 100, 0)

def led_warm():
    set_color(150, 100, 30)

def led_white():
    set_color(200, 200, 180)

def led_red():
    set_color(200, 0, 0)

def led_off():
    clear_strip()

def led_blink(r, g, b, count=3):
    """闪烁灯带"""
    for _ in range(count):
        set_color(r, g, b)
        time.sleep(0.2)
        set_color(0, 0, 0)
        time.sleep(0.2)

def test_ledstrip():
    """测试灯带"""
    if not setup_ledstrip():
        return
    print("[灯带] 测试: 红")
    set_color(255, 0, 0)
    time.sleep(1)
    print("[灯带] 测试: 绿")
    set_color(0, 255, 0)
    time.sleep(1)
    print("[灯带] 测试: 蓝")
    set_color(0, 0, 255)
    time.sleep(1)
    led_off()
    print("[灯带] 测试完成")