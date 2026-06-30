#!/usr/bin/python3
# -*- coding: utf-8 -*-

import RPi.GPIO as GPIO
import time

TRIG_PIN = 23  # GPIO23
ECHO_PIN = 24  # GPIO24

def setup_ultrasonic():
    GPIO.setup(TRIG_PIN, GPIO.OUT)
    GPIO.setup(ECHO_PIN, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)
    GPIO.output(TRIG_PIN, False)
    time.sleep(0.1)
    print("[超声波] GPIO23(Trig) GPIO24(Echo) 初始化成功")

def get_distance():
    """返回距离(cm)，超时或失败返回 -1"""
    try:
        # 发10us触发信号
        GPIO.output(TRIG_PIN, True)
        time.sleep(0.00001)
        GPIO.output(TRIG_PIN, False)
        
        # 等Echo变高
        timeout = time.time()
        while GPIO.input(ECHO_PIN) == 0:
            if time.time() - timeout > 0.05:
                return -1
        
        pulse_start = time.time()
        
        # 等Echo变低
        timeout = time.time()
        while GPIO.input(ECHO_PIN) == 1:
            if time.time() - timeout > 0.05:
                return -1
        
        pulse_end = time.time()
        duration = pulse_end - pulse_start
        distance = duration * 17150  # 声速340m/s / 2
        return round(distance, 1)
    
    except Exception:
        return -1