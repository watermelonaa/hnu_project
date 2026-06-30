#!/usr/bin/python3
# -*- coding: utf-8 -*-

import RPi.GPIO as GPIO
import time

BUZZER_PIN = 13  # GPIO13，硬件PWM
_pwm = None

def setup_buzzer():
    global _pwm
    GPIO.setup(BUZZER_PIN, GPIO.OUT)
    _pwm = GPIO.PWM(BUZZER_PIN, 440)
    _pwm.start(0)
    print("[蜂鸣器] GPIO13 初始化成功")

def play_tone(frequency, duration, duty=50):
    """播放单个音调"""
    global _pwm
    if _pwm is None:
        return
    _pwm.ChangeFrequency(frequency)
    _pwm.ChangeDutyCycle(duty)
    time.sleep(duration)
    _pwm.ChangeDutyCycle(0)

def beep_short():
    """短提示音"""
    play_tone(1000, 0.1)

def beep_alert():
    """警报音（3声急促）"""
    for _ in range(3):
        play_tone(800, 0.1)
        time.sleep(0.05)

def beep_warning():
    """警告音（长响）"""
    play_tone(600, 0.5)

def stop_buzzer():
    global _pwm
    if _pwm is not None:
        _pwm.stop()
        _pwm = None