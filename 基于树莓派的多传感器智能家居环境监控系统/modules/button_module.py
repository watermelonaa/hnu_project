#!/usr/bin/python3
# -*- coding: utf-8 -*-

import RPi.GPIO as GPIO

BUTTON_PIN = 17  # GPIO17

_mode = 1
_pressed = False

def button_callback(channel):
    global _mode, _pressed
    _mode = _mode + 1
    if _mode > 3:
        _mode = 1
    _pressed = True

def setup_button():
    GPIO.setup(BUTTON_PIN, GPIO.IN, pull_up_down=GPIO.PUD_UP)
    GPIO.add_event_detect(BUTTON_PIN, GPIO.FALLING, callback=button_callback, bouncetime=300)
    print("[按键] GPIO17 初始化成功")

def check_button():
    """返回 (是否按下, 当前模式)"""
    global _pressed
    if _pressed:
        _pressed = False
        return True, _mode
    return False, _mode

def get_mode():
    return _mode