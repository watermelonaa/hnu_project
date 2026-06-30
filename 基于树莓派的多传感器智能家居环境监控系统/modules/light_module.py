#!/usr/bin/python3
# -*- coding: utf-8 -*-

import board
import adafruit_bh1750
import time

i2c = board.I2C()
_light_available = False
_light_error_time = 0

try:
    sensor = adafruit_bh1750.BH1750(i2c)
    _light_available = True
    print("[BH1750] 初始化成功")
except Exception as e:
    print(f"[BH1750] 初始化失败: {e}")
    print("请检查I2C是否开启: sudo ls /dev/i2c*")

def read_light():
    """读取光照强度 (Lux)，失败返回 None"""
    global _light_error_time
    if not _light_available:
        return None
    try:
        lux = sensor.lux
        return round(lux, 1)
    except Exception:
        now = time.time()
        if now - _light_error_time > 30:
            print("[BH1750] 读取失败")
            _light_error_time = now
        return None