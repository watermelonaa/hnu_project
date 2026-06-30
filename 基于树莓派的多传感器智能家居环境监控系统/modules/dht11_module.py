#!/usr/bin/python3
# -*- coding: utf-8 -*-

import time
import board
import adafruit_dht

DHT_PIN = board.D4  # GPIO4
dhtDevice = adafruit_dht.DHT11(DHT_PIN)
_last_error_time = 0

def read_dht11():
    """读取温湿度，返回 (温度°C, 湿度%)，失败返回 (None, None)"""
    global _last_error_time
    try:
        temperature = dhtDevice.temperature
        humidity = dhtDevice.humidity
        _last_error_time = 0
        return temperature, humidity
    except RuntimeError:
        now = time.time()
        if now - _last_error_time > 30:  # 每30秒最多打印一次错误
            print("[DHT11] 读取失败，请检查连接")
            _last_error_time = now
        return None, None