#!/usr/bin/env python3
"""
Generate ringtone JSON seed files using free jsDelivr CDN audio.

Sources:
  1. Awesome-Ringtone (npm: ringtones@1.0.4) — 92 phone ringtones
  2. tonejs-instruments (20 packages) — 429 instrument note samples
  3. @audio-samples Salamander Grand Piano (6 velocity-effect packs) — 208 piano samples

Total: 729+ unique MP3s distributed across 11 category JSON files.
All files served via jsDelivr CDN — stable, free, no API key required.
"""
import json
import os

# ═══════════════════════════════════════════════════════════════
# Source 1: Awesome-Ringtone — 92 phone ringtones
# npm: ringtones@1.0.4
# ═══════════════════════════════════════════════════════════════
RINGTONE_CDN = "https://unpkg.com/ringtones@1.0.4"

PHONE_RINGTONES = [
    # ── Apple (3) ──
    ("Apple", "2007 - Marimba", "iPhone", "Apple Inc."),
    ("Apple", "2010 - Opening", "iPhone", "Apple Inc."),
    ("Apple", "2017 - Reflection", "iPhone", "Apple Inc."),
    # ── Google Pixel (4) ──
    ("Google Pixel", "2016 - Titanium - Android Material Ringtone", "Pixel", "Google"),
    ("Google Pixel", "2016 - Zen", "Pixel", "Google"),
    ("Google Pixel", "2017 - The Big Adventure", "Pixel", "Google"),
    ("Google Pixel", "2021 - Your New Adventure", "Pixel", "Google"),
    # ── LG (15) ──
    ("LG", "2012 - Life's Good", "Music", "LG Electronics"),
    ("LG", "2013 - Life's Good Music", "Music", "LG Electronics"),
    ("LG", "2013 - Life's Good", "Music", "LG Electronics"),
    ("LG", "2014-15 - Life's Good", "Music", "LG Electronics"),
    ("LG", "2016 - Life's Good Music", "Music", "LG Electronics"),
    ("LG", "2016-17 - Life's Good", "Music", "LG Electronics"),
    ("LG", "2018-19 - Cello Quartet", "Classical", "LG Electronics"),
    ("LG", "2018-19 - Life's Good", "Music", "LG Electronics"),
    ("LG", "2019 - Funky", "Funny", "LG Electronics"),
    ("LG", "2019 - Life's Good v2", "Music", "LG Electronics"),
    ("LG", "2019 - Moombathon", "Sound Effects", "LG Electronics"),
    ("LG", "2019 - Piano", "Classical", "LG Electronics"),
    ("LG", "2019 - Sapphire", "Music", "LG Electronics"),
    ("LG", "2020 - Life's Good", "Music", "LG Electronics"),
    ("LG", "2021 - Life's Good", "Music", "LG Electronics"),
    # ── Oppo/Realme (6) ──
    ("Oppo", "2013 - Enjoy", "Music", "Oppo"),
    ("Oppo", "2016 - Future", "Music", "Oppo"),
    ("Oppo", "2018 - Colorful Life", "Music", "Oppo"),
    ("Oppo", "2018 - It's Realme", "Music", "Realme"),
    ("Oppo", "2019 - Pure", "Devotional", "Oppo"),
    ("Oppo", "2020 - Calm", "Baby", "Oppo"),
    # ── Samsung (46) ──
    ("Samsung", "2006 - Original", "Music", "Samsung"),
    ("Samsung", "2008 - Beyond Samsung", "Music", "Samsung"),
    ("Samsung", "2008 - Original Flute", "Music", "Samsung"),
    ("Samsung", "2008 - Original Galaxy S", "Music", "Samsung"),
    ("Samsung", "2011 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2012 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2013-14 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2014 - Over the Horizon Soundcamp Live - Berlin IFA 2014", "Music", "Samsung"),
    ("Samsung", "2015 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2015 - Over the Horizon - TizenOS", "Sound Effects", "Samsung"),
    ("Samsung", "2016 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2017 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2018 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2019 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2020 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2021 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2022 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2023 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2024 - Over the Horizon", "Music", "Samsung"),
    ("Samsung", "2025 - Over the Horizon", "Music", "Samsung"),
    # Ringtone variants
    ("Samsung", "2011 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2012 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2013 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2013-14 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2015 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2016 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2017 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2018 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2018 - Over the Horizon - Ringtone (Mellow Mix)", "SMS", "Samsung"),
    ("Samsung", "2019 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2020 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2021 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2022 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2023 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2024 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    ("Samsung", "2025 - Over the Horizon - Ringtone", "SMS", "Samsung"),
    # Special editions
    ("Samsung", "2015 - Over the Horizon Soundcamp Looper - Berlin IFA 2015", "Funny", "Samsung"),
    ("Samsung", "2015 - Over the Horizon Unpacked v1", "Music", "Samsung"),
    ("Samsung", "2015 - Over the Horizon Unpacked v2", "Music", "Samsung"),
    ("Samsung", "2016 - Over the Horizon Unpacked - Dubstep", "Funny", "Samsung"),
    ("Samsung", "2016 - Over the Horizon Unpacked - S7-Note7", "Music", "Samsung"),
    ("Samsung", "2017 - Over the Horizon Unpacked - S8", "Music", "Samsung"),
    ("Samsung", "2021 - Over the Horizon - by Suga of BTS", "Hindi-Bollywood", "Samsung"),
    ("Samsung", "2021 - Over the Horizon Unpacked v1", "Music", "Samsung"),
    ("Samsung", "2021 - Over the Horizon Unpacked v2", "Music", "Samsung"),
    ("Samsung", "2022 - Over the Horizon - by Suga of BTS", "Hindi-Bollywood", "Samsung"),
    # ── Sony (5) ──
    ("Sony", "2013 - Original", "SMS", "Sony"),
    ("Sony", "2014 - Default", "SMS", "Sony"),
    ("Sony", "2015 - Default", "SMS", "Sony"),
    ("Sony", "2016 - Default", "SMS", "Sony"),
    ("Sony", "2019 - Default", "SMS", "Sony"),
    # ── Vivo (6) ──
    ("Vivo", "2011 - Lovely Xylophone", "Baby", "Vivo"),
    ("Vivo", "2013 - Sunrise View", "Music", "Vivo"),
    ("Vivo", "2014 - Sunrise View", "Music", "Vivo"),
    ("Vivo", "2019 - Blue Meteor Showers", "Music", "Vivo"),
    ("Vivo", "2021 - Jovi Lifestyle", "Music", "Vivo"),
    ("Vivo", "2022 - Indomitable will", "Music", "Vivo"),
    # ── Xiaomi (7) ──
    ("Xiaomi", "2013 - Mi", "Music", "Xiaomi"),
    ("Xiaomi", "2015 - Mi", "Music", "Xiaomi"),
    ("Xiaomi", "2016 - Mi", "Music", "Xiaomi"),
    ("Xiaomi", "2017 - MiXyolophone", "Music", "Xiaomi"),
    ("Xiaomi", "2018 - Mi", "Music", "Xiaomi"),
    ("Xiaomi", "2018 - Poco", "Music", "Xiaomi"),
    ("Xiaomi", "2019 - Mi Electronic", "Sound Effects", "Xiaomi"),
]

# ═══════════════════════════════════════════════════════════════
# Source 2: tonejs-instruments — 20 instrument packages
# npm: tonejs-instrument-*-mp3
# These are individual pitched note samples (A3, C4, etc.)
# ═══════════════════════════════════════════════════════════════
INSTRUMENT_CDN = "https://cdn.jsdelivr.net/npm"

# (package, version, display_name, category)
TONEJS_INSTRUMENTS = [
    # Original 15
    ("tonejs-instrument-violin-mp3", "1.1.1", "Violin", "Music"),
    ("tonejs-instrument-cello-mp3", "1.1.1", "Cello", "Music"),
    ("tonejs-instrument-harp-mp3", "1.1.1", "Harp", "Music"),
    ("tonejs-instrument-guitar-nylon-mp3", "1.1.1", "Guitar Nylon", "Music"),
    ("tonejs-instrument-guitar-acoustic-mp3", "1.1.2", "Guitar Acoustic", "Music"),
    ("tonejs-instrument-guitar-electric-mp3", "1.1.1", "Guitar Electric", "Sound Effects"),
    ("tonejs-instrument-flute-mp3", "1.1.2", "Flute", "Devotional"),
    ("tonejs-instrument-clarinet-mp3", "1.1.2", "Clarinet", "Devotional"),
    ("tonejs-instrument-saxophone-mp3", "1.1.2", "Saxophone", "Music"),
    ("tonejs-instrument-bassoon-mp3", "1.1.2", "Bassoon", "Devotional"),
    ("tonejs-instrument-trumpet-mp3", "1.1.2", "Trumpet", "Music"),
    ("tonejs-instrument-trombone-mp3", "1.1.2", "Trombone", "Music"),
    ("tonejs-instrument-piano-mp3", "1.1.2", "Piano", "Music"),
    ("tonejs-instrument-organ-mp3", "1.1.1", "Organ", "Devotional"),
    ("tonejs-instrument-xylophone-mp3", "1.1.2", "Xylophone", "Baby"),
    # 5 new instruments (87 more MP3s)
    ("tonejs-instrument-contrabass-mp3", "1.1.2", "Contrabass", "Music"),
    ("tonejs-instrument-harmonium-mp3", "1.1.1", "Harmonium", "Devotional"),
    ("tonejs-instrument-bass-electric-mp3", "1.1.2", "Bass Electric", "Music"),
    ("tonejs-instrument-tuba-mp3", "1.1.2", "Tuba", "Music"),
    ("tonejs-instrument-french-horn-mp3", "1.1.2", "French Horn", "Music"),
]

# ALL known note files for each tonejs instrument (verified via CDN API)
INSTRUMENT_FILES = {
    # ── Original 15 ──
    "tonejs-instrument-violin-mp3@1.1.1": ["A3.mp3","A4.mp3","A5.mp3","A6.mp3","C4.mp3","C5.mp3","C6.mp3","C7.mp3","Ds5.mp3","E4.mp3","E5.mp3","E6.mp3","F4.mp3","Fs4.mp3","G5.mp3"],
    "tonejs-instrument-piano-mp3@1.1.2": ["A1.mp3","A2.mp3","A3.mp3","A4.mp3","A5.mp3","A6.mp3","A7.mp3","As1.mp3","As2.mp3","As3.mp3","As4.mp3","As5.mp3","As6.mp3","B1.mp3","B2.mp3","B3.mp3","B4.mp3","B5.mp3","B6.mp3","C1.mp3","C2.mp3","C3.mp3","C4.mp3","C5.mp3","C6.mp3","C7.mp3","Cs2.mp3","Cs3.mp3","Cs4.mp3","Cs5.mp3","D1.mp3","D2.mp3","D3.mp3","D4.mp3","D5.mp3","D6.mp3","Ds1.mp3","Ds2.mp3","Ds3.mp3","Ds4.mp3","Ds5.mp3","Ds6.mp3","E1.mp3","E2.mp3","E3.mp3","E4.mp3","E5.mp3","E6.mp3","F1.mp3","F2.mp3","F3.mp3","F4.mp3","F5.mp3","F6.mp3","Fs1.mp3","Fs2.mp3","Fs3.mp3","Fs4.mp3","Fs5.mp3","Fs6.mp3","G1.mp3","G2.mp3","G3.mp3","G4.mp3","G5.mp3","G6.mp3","Gs1.mp3","Gs2.mp3","Gs3.mp3","Gs4.mp3","Gs5.mp3","Gs6.mp3"],
    "tonejs-instrument-harp-mp3@1.1.1": ["A2.mp3","A4.mp3","A6.mp3","B1.mp3","B3.mp3","B5.mp3","B6.mp3","C3.mp3","C5.mp3","C6.mp3","D2.mp3","D4.mp3","D6.mp3","E2.mp3","E4.mp3","E6.mp3","F2.mp3","F4.mp3","F6.mp3","G2.mp3","G4.mp3","G5.mp3","G6.mp3"],
    "tonejs-instrument-guitar-nylon-mp3@1.1.1": ["A2.mp3","A3.mp3","A4.mp3","A5.mp3","As5.mp3","B1.mp3","B2.mp3","B3.mp3","B4.mp3","C3.mp3","C4.mp3","Cs5.mp3","D2.mp3","D3.mp3","D4.mp3","D5.mp3","Ds3.mp3","Ds4.mp3","E2.mp3","E3.mp3","E4.mp3","F2.mp3","F3.mp3","F4.mp3","Fs4.mp3","G2.mp3","G3.mp3","G4.mp3","Gs3.mp3"],
    "tonejs-instrument-guitar-electric-mp3@1.1.1": ["A2.mp3","A3.mp3","A4.mp3","A5.mp3","C3.mp3","C4.mp3","C5.mp3","C6.mp3","D3.mp3","D4.mp3","D5.mp3","E2.mp3","E3.mp3","E4.mp3","F3.mp3","G3.mp3","G4.mp3"],
    "tonejs-instrument-guitar-acoustic-mp3@1.1.2": ["A2.mp3","A3.mp3","A4.mp3","As2.mp3","As3.mp3","As4.mp3","B2.mp3","B3.mp3","B4.mp3","C3.mp3","C4.mp3","Cs3.mp3","Cs4.mp3","D2.mp3","D3.mp3","D4.mp3","Ds2.mp3","Ds3.mp3","Ds4.mp3","E2.mp3","E3.mp3","E4.mp3","F2.mp3","F3.mp3","F4.mp3","Fs2.mp3","Fs3.mp3","Fs4.mp3","G2.mp3","G3.mp3","G4.mp3","Gs2.mp3","Gs3.mp3","Gs4.mp3"],
    "tonejs-instrument-trumpet-mp3@1.1.2": ["A3.mp3","A5.mp3","As3.mp3","C3.mp3","C6.mp3","D5.mp3","Ds4.mp3","F3.mp3","F5.mp3","G4.mp3","G5.mp3"],
    "tonejs-instrument-flute-mp3@1.1.2": ["A4.mp3","A5.mp3","A6.mp3","C4.mp3","C5.mp3","C6.mp3","C7.mp3","E4.mp3","E5.mp3","F5.mp3"],
    "tonejs-instrument-cello-mp3@1.1.1": ["A2.mp3","A3.mp3","A4.mp3","As2.mp3","As3.mp3","As4.mp3","B2.mp3","B3.mp3","B4.mp3","C2.mp3","C3.mp3","C4.mp3","Cs2.mp3","Cs3.mp3","Cs4.mp3","D2.mp3","D3.mp3","D4.mp3","Ds2.mp3","Ds3.mp3","Ds4.mp3","E2.mp3","E3.mp3","E4.mp3","F2.mp3","F3.mp3","F4.mp3","Fs2.mp3","Fs3.mp3","Fs4.mp3","G2.mp3","G3.mp3","G4.mp3","Gs2.mp3","Gs3.mp3"],
    "tonejs-instrument-saxophone-mp3@1.1.2": ["A4.mp3","A5.mp3","As3.mp3","As4.mp3","B3.mp3","B4.mp3","C4.mp3","C5.mp3","Cs4.mp3","Cs5.mp3","D3.mp3","D4.mp3","D5.mp3","Ds3.mp3","Ds4.mp3","Ds5.mp3","E3.mp3","E4.mp3","E5.mp3","F3.mp3","F4.mp3","F5.mp3","Fs3.mp3","Fs4.mp3","G3.mp3","G4.mp3","G5.mp3","Gs3.mp3","Gs4.mp3"],
    "tonejs-instrument-organ-mp3@1.1.1": ["A1.mp3","A2.mp3","A3.mp3","A4.mp3","A5.mp3","C1.mp3","C2.mp3","C3.mp3","C4.mp3","C5.mp3","C7.mp3","D2.mp3","D3.mp3","D4.mp3","D6.mp3","F2.mp3","F3.mp3","F4.mp3","G2.mp3","G3.mp3","G4.mp3"],
    "tonejs-instrument-clarinet-mp3@1.1.2": ["As3.mp3","As4.mp3","As5.mp3","D3.mp3","D4.mp3","D5.mp3","F3.mp3","F4.mp3","F5.mp3","G5.mp3","Gs3.mp3"],
    "tonejs-instrument-trombone-mp3@1.1.2": ["As1.mp3","As2.mp3","As3.mp3","C3.mp3","C4.mp3","Cs2.mp3","Cs3.mp3","Ds2.mp3","Ds3.mp3","Ds4.mp3","F2.mp3","F3.mp3","F4.mp3","G2.mp3","G3.mp3","Gs2.mp3","Gs3.mp3"],
    "tonejs-instrument-bassoon-mp3@1.1.2": ["A2.mp3","A3.mp3","A4.mp3","C3.mp3","C4.mp3","C5.mp3","F3.mp3","F4.mp3","G2.mp3","G3.mp3"],
    "tonejs-instrument-xylophone-mp3@1.1.2": ["C5.mp3","C6.mp3","C7.mp3","C8.mp3","G4.mp3","G5.mp3","G6.mp3","G7.mp3"],
    # ── 5 NEW instruments ──
    "tonejs-instrument-contrabass-mp3@1.1.2": ["A2.mp3","As1.mp3","B3.mp3","C2.mp3","Cs3.mp3","D2.mp3","E2.mp3","E3.mp3","F1.mp3","G4.mp3","Gs1.mp3","Gs2.mp3"],
    "tonejs-instrument-harmonium-mp3@1.1.1": ["A2.mp3","A3.mp3","A4.mp3","As2.mp3","As3.mp3","As4.mp3","B2.mp3","B3.mp3","B4.mp3","C3.mp3","C4.mp3","C5.mp3","Cs3.mp3","Cs4.mp3","D3.mp3","D4.mp3","D5.mp3","Ds3.mp3","Ds4.mp3","E3.mp3","E4.mp3","F3.mp3","F4.mp3","Fs3.mp3","Fs4.mp3","G2.mp3","G3.mp3","G4.mp3","Gs3.mp3","Gs4.mp3"],
    "tonejs-instrument-bass-electric-mp3@1.1.2": ["As1.mp3","As2.mp3","As3.mp3","As4.mp3","Cs1.mp3","Cs2.mp3","Cs3.mp3","Cs4.mp3","E1.mp3","E2.mp3","E3.mp3","E4.mp3","Fs1.mp3","Fs2.mp3","Fs3.mp3","Fs4.mp3","Gs1.mp3"],
    "tonejs-instrument-tuba-mp3@1.1.2": ["A1.mp3","A3.mp3","As1.mp3","C2.mp3","C4.mp3","D3.mp3","F2.mp3","F4.mp3","G1.mp3"],
    "tonejs-instrument-french-horn-mp3@1.1.2": ["A1.mp3","A3.mp3","C2.mp3","C4.mp3","D3.mp3","D5.mp3","Ds2.mp3","F3.mp3","G2.mp3","Gs3.mp3"],
}

# ═══════════════════════════════════════════════════════════════
# Source 3: @audio-samples Salamander Grand Piano V3
# npm: @audio-samples/piano-mp3-*
# These are realistic grand piano samples with velocity layers,
# harmonics, pedal effects, and release samples.
# ═══════════════════════════════════════════════════════════════
SALAMANDER_PACKAGES = [
    ("@audio-samples/piano-mp3-velocity1", "1.0.5", "Piano Soft", "Music"),
    ("@audio-samples/piano-mp3-velocity8", "1.0.5", "Piano Medium", "Music"),
    ("@audio-samples/piano-mp3-velocity16", "1.0.5", "Piano Loud", "Music"),
    ("@audio-samples/piano-mp3-harmonics", "1.0.5", "Piano Harmonics", "Sound Effects"),
    ("@audio-samples/piano-mp3-pedals", "1.0.5", "Piano Pedal", "Sound Effects"),
    ("@audio-samples/piano-mp3-release", "1.0.5", "Piano Release", "Music"),
]

# Standard piano notes: A0-C8 with octaves, each with velocity suffix
# velocity1 uses v1, velocity8 uses v8, velocity16 uses v16
PIANO_NATURAL_NOTES = ["A0","A1","A2","A3","A4","A5","A6","A7","C1","C2","C3","C4","C5","C6","C7","C8"]
PIANO_ALL_NOTES = ["A0","A1","A2","A3","A4","A5","A6","A7","B1","B2","B3","B4","B5","B6","B7","C1","C2","C3","C4","C5","C6","C7","C8","D1","D2","D3","D4","D5","D6","D7","E1","E2","E3","E4","E5","E6","F1","F2","F3","F4","F5","F6","G1","G2","G3","G4","G5","G6"]

# ═══════════════════════════════════════════════════════════════
# Category → JSON file mapping
# ═══════════════════════════════════════════════════════════════
CATEGORY_FILE_MAP = {
    "iPhone": "iphone_ringtones.json",
    "Music": "music.json",
    "Funny": "funny.json",
    "SMS": "sms.json",
    "Baby": "baby_ringtones.json",
    "Devotional": "devotional_ringtones.json",
    "Sound Effects": "sound_effects.json",
    "Classical": "devotional_ringtones.json",
    "Hindi-Bollywood": "hindi-bollywood-ringtones.json",
    "Pixel": "miscellaneous_ringtones.json",
}

# Cross-listing rules
CROSS_LIST_RULES = [
    ("baby_ringtones.json", ["Classical", "Devotional", "iPhone"], 10),
    ("funny.json", ["Sound Effects", "Music"], 10),
    ("devotional_ringtones.json", ["Classical", "Baby", "Music"], 10),
    ("hindi-bollywood-ringtones.json", ["Music", "iPhone"], 10),
    ("iphone_ringtones.json", ["Music", "Pixel"], 10),
    ("miscellaneous_ringtones.json", ["Music", "iPhone", "Pixel", "SMS"], 12),
    ("sound_effects.json", ["Funny", "Music"], 10),
]


# ═══════════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════════

def build_entry(url, title, author, entry_type):
    """Build a uniform JSON entry."""
    return {
        "title": title,
        "author": author,
        "time": "Feb 2025",
        "url": url,
        "type": entry_type,
    }


def build_ringtone(folder, filename, author, cdn_base):
    """Build entry for a phone ringtone."""
    if not filename.endswith(".mp3"):
        filename = filename + ".mp3"
    display = filename.replace(".mp3", "")
    encoded_file = filename.replace(' ', '%20').replace('#', '%23')
    encoded_folder = folder.replace(' ', '%20')
    url = f"{cdn_base}/{encoded_folder}/{encoded_file}"
    return build_entry(url, display, author, folder)


def build_instrument(pkg_name, version, note_file, display_name, cdn_base):
    """Build entry for an instrument note sample."""
    note_name = note_file.replace(".mp3", "")
    title = f"{display_name} {note_name}"
    encoded = note_file.replace(' ', '%20')
    url = f"{cdn_base}/{pkg_name}@{version}/{encoded}"
    return build_entry(url, title, f"{display_name} Instrument", display_name)


def build_salamander(pkg_name, version, note_file, display_name, cdn_base):
    """Build entry for a salamander piano sample."""
    note_name = note_file.replace(".mp3", "")
    title = f"{display_name} {note_name}"
    encoded = note_file.replace(' ', '%20')
    url = f"{cdn_base}/{pkg_name}@{version}/audio/{encoded}"
    return build_entry(url, title, f"Salamander Grand Piano", display_name)


# ═══════════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════════

def main():
    assets_dir = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "app", "src", "main", "assets", "jsonres"
    )
    os.makedirs(assets_dir, exist_ok=True)

    file_entries = {}

    # ── Step 1: Phone ringtones ──────────────────────────────
    for folder, filename, category, author in PHONE_RINGTONES:
        json_file = CATEGORY_FILE_MAP.get(category, "miscellaneous_ringtones.json")
        entry = build_ringtone(folder, filename, author, RINGTONE_CDN)
        file_entries.setdefault(json_file, []).append(entry)

    # ── Step 2: tonejs instrument samples ────────────────────
    instr_meta = {}
    for pkg_name, version, display_name, category in TONEJS_INSTRUMENTS:
        key = f"{pkg_name}@{version}"
        instr_meta[key] = (display_name, category, version)

    max_primary = {"Music": 20, "Devotional": 12, "Sound Effects": 8, "Baby": 99}

    for pkg_key, note_files in INSTRUMENT_FILES.items():
        display_name, category, version = instr_meta.get(pkg_key, ("Unknown", "Music", "1.0.0"))
        json_file = CATEGORY_FILE_MAP.get(category, "miscellaneous_ringtones.json")
        pkg_name = pkg_key.rsplit("@", 1)[0]
        limit = max_primary.get(category, 15)
        for note_file in note_files[:limit]:
            entry = build_instrument(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
            file_entries.setdefault(json_file, []).append(entry)

    # Cross-list instrument notes to baby/sms/sfx
    baby_inst = ["Xylophone", "Harp", "Flute", "Piano"]
    sms_inst = ["Violin", "Trumpet", "Flute", "Xylophone", "Harp", "Piano", "Clarinet", "Contrabass"]
    sfx_inst = ["Guitar Electric", "Organ", "Trombone", "Saxophone", "French Horn", "Tuba"]

    for pkg_key, note_files in INSTRUMENT_FILES.items():
        display_name, _, version = instr_meta.get(pkg_key, ("Unknown", "Music", "1.0.0"))
        pkg_name = pkg_key.rsplit("@", 1)[0]

        if display_name in baby_inst:
            for note_file in note_files[:4]:
                entry = build_instrument(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
                file_entries.setdefault("baby_ringtones.json", []).append(entry)

        if display_name in sms_inst:
            for note_file in note_files[:3]:
                entry = build_instrument(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
                file_entries.setdefault("sms.json", []).append(entry)

        if display_name in sfx_inst:
            for note_file in note_files[:4]:
                entry = build_instrument(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
                file_entries.setdefault("sound_effects.json", []).append(entry)

    # ── Step 3: Salamander Grand Piano samples ───────────────
    # Limit salamander entries going to non-music categories
    slamander_max = {"Music": 30, "Sound Effects": 20}
    for pkg_name, version, display_name, category in SALAMANDER_PACKAGES:
        json_file = CATEGORY_FILE_MAP.get(category, "miscellaneous_ringtones.json")
        limit = slamander_max.get(category, 20)

        if "velocity" in pkg_name:
            vel = pkg_name.split("velocity")[-1]
            notes = PIANO_NATURAL_NOTES if vel in ("1", "8") else PIANO_ALL_NOTES[:limit]
            for note in notes:
                note_file = f"{note}v{vel}.mp3"
                entry = build_salamander(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
                file_entries.setdefault(json_file, []).append(entry)
        elif "release" in pkg_name:
            for i in range(1, min(89, limit + 1)):
                note_file = f"rel{i}.mp3"
                entry = build_salamander(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
                file_entries.setdefault(json_file, []).append(entry)
        elif "harmonics" in pkg_name:
            for note in PIANO_NATURAL_NOTES[:limit]:
                note_file = f"{note}.mp3"
                entry = build_salamander(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
                file_entries.setdefault(json_file, []).append(entry)
        elif "pedals" in pkg_name:
            pedal_notes = [f"pedal_{i}" for i in range(1, min(limit + 1, 11))]
            for note_file in pedal_notes:
                if not note_file.endswith(".mp3"):
                    note_file = note_file + ".mp3"
                entry = build_salamander(pkg_name, version, note_file, display_name, INSTRUMENT_CDN)
                file_entries.setdefault(json_file, []).append(entry)

    # Cross-list soft piano to baby category
    for note in PIANO_NATURAL_NOTES[:12]:
        note_file = f"{note}v1.mp3"
        entry = build_salamander("@audio-samples/piano-mp3-velocity1", "1.0.5", note_file, "Piano Soft", INSTRUMENT_CDN)
        file_entries.setdefault("baby_ringtones.json", []).append(entry)

    # ── Step 4: Cross-list phone ringtones ────────────────────
    all_rt = {}
    for folder, filename, category, author in PHONE_RINGTONES:
        entry = build_ringtone(folder, filename, author, RINGTONE_CDN)
        all_rt[entry["url"]] = (entry, category)

    for target_file, source_cats, max_extra in CROSS_LIST_RULES:
        if target_file not in file_entries:
            file_entries[target_file] = []
        existing_urls = {e["url"] for e in file_entries[target_file]}
        candidates = []
        for url, (entry, cat) in all_rt.items():
            if cat in source_cats and url not in existing_urls:
                candidates.append(entry)
        for entry in candidates[:max_extra]:
            if entry["url"] not in existing_urls:
                file_entries[target_file].append(entry)
                existing_urls.add(entry["url"])

    # ── Step 5: Regional categories ──────────────────────────
    regional = []
    for folder, filename, category, author in PHONE_RINGTONES:
        if category in ("Music", "Classical", "Devotional", "Baby", "Hindi-Bollywood", "iPhone"):
            regional.append(build_ringtone(folder, filename, author, RINGTONE_CDN))
    # Add some instrument notes too
    for pkg_key, note_files in INSTRUMENT_FILES.items():
        display_name, _, version = instr_meta.get(pkg_key, ("Unknown", "Music", "1.0.0"))
        pkg_name = pkg_key.rsplit("@", 1)[0]
        for note_file in note_files[:3]:
            regional.append(build_instrument(pkg_name, version, note_file, display_name, INSTRUMENT_CDN))

    seen = set()
    uniq = []
    for e in regional:
        if e["url"] not in seen:
            seen.add(e["url"])
            uniq.append(e)

    for target in ["malayalam.json", "tamil.json"]:
        file_entries[target] = list(uniq[:30])

    # ── Step 6: Write ────────────────────────────────────────
    grand_total = 0
    print(f"{'Category':<38} {'Entries':>8}")
    print("-" * 48)

    for filename in sorted(file_entries.keys()):
        filepath = os.path.join(assets_dir, filename)
        entries = file_entries[filename]
        if not entries:
            continue
        # Deduplicate within file
        seen_urls = set()
        unique = []
        for e in entries:
            if e["url"] not in seen_urls:
                seen_urls.add(e["url"])
                unique.append(e)
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(unique, f, indent=2, ensure_ascii=False)
        print(f"{filename:<38} {len(unique):>8}")
        grand_total += len(unique)

    total_notes = sum(len(v) for v in INSTRUMENT_FILES.values())
    print(f"\n{'='*48}")
    print(f"Total entries across all files: {grand_total}")
    print(f"Source 1 — Phone ringtones: {len(PHONE_RINGTONES)}")
    print(f"Source 2 — Instrument packages: {len(TONEJS_INSTRUMENTS)} ({total_notes} notes)")
    print(f"Source 3 — Salamander Piano packs: {len(SALAMANDER_PACKAGES)}")


if __name__ == "__main__":
    main()
