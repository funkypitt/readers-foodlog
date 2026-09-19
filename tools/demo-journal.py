#!/usr/bin/env python3
"""Draws a synthetic six-day journal (plates, activities, 80 days of weights) into ./demo-journal/
for screenshots: never a real photo. Then, with a DEBUG build on the emulator:
    tar -C demo-journal -cf demo.tar . && adb push demo.tar /data/local/tmp/
    adb shell "run-as com.freedomfighter.readersfoodlog sh -c 'mkdir -p files/journal && cd files/journal && tar -xf /data/local/tmp/demo.tar'"
Set the emulator clock after the last entry of the day (adb root; adb shell date 092013052026.00)
and run the capture scenario (~/.config/adv/scenarios/readersfoodlog.sh, locale en-GB for kg)."""
from PIL import Image, ImageDraw, ImageFilter
import random, os, math, datetime, sys
random.seed(11)
TODAY = datetime.date.fromisoformat(sys.argv[1]) if len(sys.argv) > 1 else datetime.date.today()
TABLES = [(172,132,92),(205,196,180),(90,96,104),(150,110,78),(228,224,214)]
FOODS = {'breakfast':[(222,170,90),(245,222,160),(150,60,50),(250,245,235),(90,50,30)],
         'lunch':[(96,150,70),(214,80,60),(236,206,120),(250,240,220),(190,120,70)],
         'dinner':[(170,90,60),(230,200,140),(80,130,80),(240,150,60),(120,70,50)],
         'snack':[(200,40,50),(250,200,80),(120,80,50),(240,230,210),(160,190,90)]}

def plate(path, meal, portrait):
    w, h = (1200, 1600) if portrait else (1600, 1200)
    t = random.choice(TABLES); im = Image.new('RGB', (w, h), t); d = ImageDraw.Draw(im)
    for y in range(0, h, random.randint(60, 110)):
        d.line((0, y + random.randint(-8, 8), w, y + random.randint(-8, 8)), fill=tuple(max(0, c - 18) for c in t), width=3)
    cx, cy = w // 2 + random.randint(-60, 60), h // 2 + random.randint(-60, 60); r = min(w, h) // 2 - 110
    sh = Image.new('RGBA', (w, h), (0, 0, 0, 0)); ImageDraw.Draw(sh).ellipse((cx-r+18, cy-r+30, cx+r+18, cy+r+30), fill=(0, 0, 0, 110))
    sh = sh.filter(ImageFilter.GaussianBlur(28)); im.paste(sh, (0, 0), sh)
    d = ImageDraw.Draw(im); d.ellipse((cx-r, cy-r, cx+r, cy+r), fill=(238, 236, 228)); d.ellipse((cx-r+70, cy-r+70, cx+r-70, cy+r-70), fill=(250, 249, 245))
    for _ in range(random.randint(16, 28)):
        a = random.uniform(0, 2 * math.pi); rr = random.uniform(0, r - 190); x = cx + rr * math.cos(a); y = cy + rr * math.sin(a); s = random.randint(35, 120)
        c = tuple(min(255, max(0, v + random.randint(-14, 14))) for v in random.choice(FOODS[meal]))
        if random.random() < 0.5: d.ellipse((x-s, y-s*0.8, x+s, y+s*0.8), fill=c)
        else: d.rounded_rectangle((x-s, y-s*0.5, x+s, y+s*0.5), radius=int(s*0.4), fill=c)
    im.filter(ImageFilter.GaussianBlur(1.6)).save(path, quality=86)

plan = {0:[("074210","breakfast"),("124108","lunch")], 1:[("071530","breakfast"),("123012","lunch"),("160544","snack"),("192230","dinner")],
        2:[("080201","breakfast"),("130101","lunch"),("194500","dinner")], 3:[("073355","breakfast"),("121940","lunch"),("153010","snack"),("190812","dinner")],
        4:[("075001","breakfast"),("124455","lunch"),("200130","dinner")], 5:[("081210","breakfast"),("131502","lunch"),("193300","dinner")]}
acts = {0:"06:50:00\t20 min yoga\n", 1:"17:30:00\tendurance\n", 3:"18:05:00\tstrength training\n", 4:"12:10:00\twalk 45 min\n"}
for i, shots in plan.items():
    day = TODAY - datetime.timedelta(days=i); os.makedirs(f"demo-journal/{day}", exist_ok=True)
    for k, (st, meal) in enumerate(shots): plate(f"demo-journal/{day}/{st}_{meal}.jpg", meal, (i + k) % 3 != 1)
    if i in acts: open(f"demo-journal/{day}/activities.txt", "w").write(acts[i])
for i in range(80):
    if i > 5 and random.random() < 0.3: continue
    day = TODAY - datetime.timedelta(days=i); os.makedirs(f"demo-journal/{day}", exist_ok=True)
    open(f"demo-journal/{day}/weight.txt", "w").write("%.2f\n" % (73.6 + i*0.04 + 0.45*math.sin(i/5) + random.uniform(-.25, .25)))
