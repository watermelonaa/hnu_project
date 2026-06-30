#!/usr/bin/python3
# -*- coding: utf-8 -*-

import board
import busio
import adafruit_ssd1306
from PIL import Image, ImageDraw, ImageFont

WIDTH = 128
HEIGHT = 64

_i2c = busio.I2C(board.SCL, board.SDA)
_oled = adafruit_ssd1306.SSD1306_I2C(WIDTH, HEIGHT, _i2c, addr=0x3C)
_image = Image.new("1", (WIDTH, HEIGHT))
_draw = ImageDraw.Draw(_image)

try:
    _font_cn = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 12)
    _font_num = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 11)
except:
    _font_cn = ImageFont.load_default()
    _font_num = ImageFont.load_default()

_mode_names = {1: "Monitor", 2: "Night", 3: "Alarm"}

def display_main_screen(mode, temperature, humidity, lux, distance, alert_msg=None):
    """主显示界面"""
    _draw.rectangle((0, 0, WIDTH, HEIGHT), outline=0, fill=0)
    
    # 顶栏
    mode_name = _mode_names.get(mode, "???")
    _draw.rectangle((0, 0, WIDTH, 14), outline=255, fill=255)
    _draw.text((2, 0), f" Mode:{mode_name}", font=_font_cn, fill=0)
    
    y = 16
    
    # 温度
    if temperature is not None:
        _draw.text((2, y), f"Temp: {temperature:.1f}C", font=_font_num, fill=255)
    else:
        _draw.text((2, y), "Temp: --.- C", font=_font_num, fill=128)
    
    # 湿度
    if humidity is not None:
        _draw.text((2, y+13), f"Hum : {humidity:.1f}%", font=_font_num, fill=255)
    else:
        _draw.text((2, y+13), "Hum : --.- %", font=_font_num, fill=128)
    
    # 光照
    if lux is not None:
        _draw.text((2, y+26), f"Lux : {lux:.0f} lx", font=_font_num, fill=255)
    else:
        _draw.text((2, y+26), "Lux : --- lx", font=_font_num, fill=128)
    
    # 距离
    if distance > 0:
        _draw.text((2, y+39), f"Dist: {distance:.0f} cm", font=_font_num, fill=255)
    else:
        _draw.text((2, y+39), "Dist: --- cm", font=_font_num, fill=128)
    
    # 底部状态栏
    _draw.line((0, 55, WIDTH, 55), fill=128)
    if alert_msg:
        _draw.text((2, 56), alert_msg[:21], font=_font_num, fill=255)
    else:
        _draw.text((2, 56), "System OK", font=_font_num, fill=128)
    
    _oled.image(_image)
    _oled.show()

def display_clear():
    _draw.rectangle((0, 0, WIDTH, HEIGHT), outline=0, fill=0)
    _oled.image(_image)
    _oled.show()
    print("[OLED] 屏幕已清除")