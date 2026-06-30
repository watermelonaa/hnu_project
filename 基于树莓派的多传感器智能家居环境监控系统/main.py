#!/usr/bin/python3
# -*- coding: utf-8 -*-

"""
================================================================
  智能家居环境监控系统 v2.0 - 树莓派4B专用版
  集成模块：按键 + 温湿度 + 光照 + 超声波 + 蜂鸣器 + OLED + 灯带
================================================================
  模式1 - 监控模式：显示所有数据，灯带绿色，有人靠近提醒
  模式2 - 夜间模式：低光照自动亮暖色灯带，有人靠近变亮
  模式3 - 报警模式：温湿度超标报警，灯带闪红
  按按键循环切换模式
================================================================
"""

import RPi.GPIO as GPIO
import time
import datetime
import sys
import os

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'modules'))

from dht11_module import read_dht11
from light_module import read_light
from oled_module import display_main_screen, display_clear
from button_module import setup_button, check_button, get_mode
from ultrasonic_module import setup_ultrasonic, get_distance
from buzzer_module import setup_buzzer, beep_short, beep_alert, beep_warning, stop_buzzer
from ledstrip_module import setup_ledstrip, led_green, led_warm, led_white, led_red, led_off

# ==================== 配置 ====================
LOG_FILE = "/usr/local/develop/task3/data.log"
SCAN_INTERVAL = 1
DISTANCE_THRESHOLD = 50
LIGHT_THRESHOLD = 50
TEMP_HIGH = 31
HUMIDITY_HIGH = 80
ALERT_COOLDOWN = 5

# ==================== 初始化 ====================
def setup():
    print("\n正在初始化系统...")
    GPIO.setmode(GPIO.BCM)
    GPIO.setwarnings(False)
    
    setup_button()
    setup_ultrasonic()
    setup_buzzer()
    led_ok = setup_ledstrip()
    
    beep_short()
    
    print("\n" + "=" * 55)
    print("  智能家居环境监控系统 v2.0")
    print("  树莓派 4 Model B  |  7模块集成")
    print("  按按键切换模式 | Ctrl+C 退出")
    if not led_ok:
        print("  [警告] 灯带不可用，灯光功能跳过")
    print("=" * 55 + "\n")
    
    return led_ok

# ==================== 日志 ====================
def write_log(mode, temp, hum, lux, dist, alert):
    try:
        ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        line = f"[{ts}] Mode:{mode} T:{temp} H:{hum} L:{lux} D:{dist} {'!'+alert if alert else 'OK'}"
        with open(LOG_FILE, 'a') as f:
            f.write(line + '\n')
    except:
        pass

# ==================== 主循环 ====================
def main():
    led_ok = setup()
    last_alert_time = 0
    
    try:
        while True:
            # --- 1. 检查按键 ---
            changed, mode = check_button()
            if changed:
                beep_short()
                print(f"\n>>> 切换到模式 {mode} <<<\n")
            
            # --- 2. 读取传感器 ---
            temp, hum = read_dht11()
            lux = read_light()
            dist = get_distance()
            
            # --- 3. 模式逻辑 ---
            alert_msg = None
            now = time.time()
            
            if mode == 1:  # 监控模式
                if led_ok:
                    led_green()
                if 0 < dist < DISTANCE_THRESHOLD:
                    alert_msg = f"有人靠近! {dist:.0f}cm"
                    if now - last_alert_time > ALERT_COOLDOWN:
                        beep_alert()
                        last_alert_time = now
                else:
                    alert_msg = "监控正常"
            
            elif mode == 2:  # 夜间模式
                if lux is not None and lux < LIGHT_THRESHOLD:
                    if 0 < dist < DISTANCE_THRESHOLD:
                        if led_ok:
                            led_white()
                        alert_msg = "夜间(有人)高亮"
                    else:
                        if led_ok:
                            led_warm()
                        alert_msg = "夜间(待机)暖光"
                else:
                    if led_ok:
                        led_off()
                    alert_msg = "光线足,灯关"
            
            elif mode == 3:  # 报警模式
                alarm = False
                if temp is not None and temp >= TEMP_HIGH:
                    alert_msg = f"高温!{temp:.1f}C"
                    alarm = True
                if hum is not None and hum >= HUMIDITY_HIGH:
                    alert_msg = f"高湿!{hum:.1f}%"
                    alarm = True
                
                if alarm:
                    if led_ok:
                        led_red()
                    if now - last_alert_time > ALERT_COOLDOWN:
                        beep_warning()
                        last_alert_time = now
                else:
                    if led_ok:
                        led_off()
                    alert_msg = "参数正常"
            
            # --- 4. OLED显示 ---
            display_main_screen(mode, temp, hum, lux, dist, alert_msg)
            
            # --- 5. 日志 ---
            write_log(mode, temp, hum, lux, dist, alert_msg)
            
            # --- 6. 控制台 ---
            print(f"[Mode{mode}] T:{temp} H:{hum} L:{lux} D:{dist} -> {alert_msg}")
            
            time.sleep(SCAN_INTERVAL)
    
    except KeyboardInterrupt:
        print("\n\n用户中断...")
    except Exception as e:
        print(f"\n系统异常: {e}")
    finally:
        print("正在清理资源...")
        display_clear()
        stop_buzzer()
        if led_ok:
            led_off()
        GPIO.cleanup()
        print("系统安全退出\n")

if __name__ == "__main__":
    main()