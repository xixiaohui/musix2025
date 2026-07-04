#!/usr/bin/env python3
"""
Generate trendy/fashionable free music JSON files for the home page.

Source: Sonic Pi audio samples from supersonic-scsynth-samples@0.70.0
Format: FLAC (audio/x-flac) — fully supported by ExoPlayer
CDN: jsDelivr

Output:
  - electronic.json     (44 entries) — 电子音乐
  - loops_beats.json    (35 entries) — 节奏循环
  - ambient.json        (15 entries) — 氛围音景
  - Also enriches existing categories with drum/percussion sounds
"""
import json
import os

# ═══════════════════════════════════════════════════════════════
# Configuration
# ═══════════════════════════════════════════════════════════════
CDN_BASE = "https://cdn.jsdelivr.net/npm/supersonic-scsynth-samples@0.70.0/samples"

# All 206 verified Sonic Pi samples, grouped by prefix
# Source: unpkg.com directory listing of supersonic-scsynth-samples
SONIC_PI_SAMPLES = {
    # ── Electronic ──
    "elec": {
        "category": "Electronic",
        "json_file": "electronic.json",
        "description": "Electronic",
        "files": [
            "elec_beep.flac", "elec_bell.flac", "elec_blip.flac", "elec_blip2.flac",
            "elec_blup.flac", "elec_bong.flac", "elec_chime.flac", "elec_chirp.flac",
            "elec_cymbal.flac", "elec_ding.flac", "elec_filt_beep.flac", "elec_flip.flac",
            "elec_fuzz_beep.flac", "elec_hi_snare.flac", "elec_hollow_kick.flac",
            "elec_lo_snare.flac", "elec_mid_snare.flac", "elec_ping.flac",
            "elec_plip.flac", "elec_pop.flac", "elec_siren_beep.flac",
            "elec_soft_kick.flac", "elec_tick.flac", "elec_triangle.flac",
            "elec_twang.flac", "elec_twip.flac", "elec_wood.flac",
        ],
    },
    "glitch": {
        "category": "Electronic",
        "json_file": "electronic.json",
        "description": "Glitch Electronic",
        "files": [
            "glitch_bass_g.flac", "glitch_perc1.flac", "glitch_perc2.flac",
            "glitch_perc3.flac", "glitch_perc4.flac", "glitch_perc5.flac",
            "glitch_perc6.flac", "glitch_perc7.flac",
        ],
    },
    "tbd": {
        "category": "Electronic",
        "json_file": "electronic.json",
        "description": "Electronic Pad",
        "files": [
            "tbd_fxbed_loop.flac", "tbd_highkey_c4.flac",
            "tbd_pad_1.flac", "tbd_pad_2.flac", "tbd_pad_3.flac", "tbd_pad_4.flac",
            "tbd_pad_5.flac", "tbd_pad_6.flac", "tbd_pad_7.flac", "tbd_pad_8.flac",
            "tbd_plankton.flac",
        ],
    },

    # ── Loops & Beats ──
    "loop": {
        "category": "Loops & Beats",
        "json_file": "loops_beats.json",
        "description": "Music Loop",
        "files": [
            "loop_3d_printer.flac", "loop_amen.flac", "loop_amen_full.flac",
            "loop_breakbeat.flac", "loop_compus.flac", "loop_drone_g_97.flac",
            "loop_electric.flac", "loop_garzul.flac", "loop_industrial.flac",
            "loop_mika.flac", "loop_mehackit_1.flac", "loop_mehackit_2.flac",
            "loop_perc1.flac", "loop_perc2.flac", "loop_safari.flac",
            "loop_tabla.flac", "loop_weirdo.flac",
        ],
    },
    "bass": {
        "category": "Loops & Beats",
        "json_file": "loops_beats.json",
        "description": "Bass",
        "files": [
            "bass_dnb_f.flac", "bass_drop_c.flac", "bass_hard_c.flac",
            "bass_hit_c.flac", "bass_thick_c.flac", "bass_trance_c.flac",
            "bass_voxy_c.flac", "bass_voxy_hit_c.flac", "bass_woodsy_c.flac",
        ],
    },
    "arovane": {
        "category": "Loops & Beats",
        "json_file": "loops_beats.json",
        "description": "Electronic Beat",
        "files": [
            "arovane_beat_a.flac", "arovane_beat_b.flac", "arovane_beat_c.flac",
            "arovane_beat_d.flac", "arovane_beat_e.flac",
        ],
    },
    "vinyl": {
        "category": "Loops & Beats",
        "json_file": "loops_beats.json",
        "description": "Vinyl FX",
        "files": [
            "vinyl_backspin.flac", "vinyl_hiss.flac",
            "vinyl_rewind.flac", "vinyl_scratch.flac",
        ],
    },

    # ── Ambient ──
    "ambi": {
        "category": "Ambient",
        "json_file": "ambient.json",
        "description": "Ambient",
        "files": [
            "ambi_choir.flac", "ambi_dark_woosh.flac", "ambi_drone.flac",
            "ambi_glass_hum.flac", "ambi_glass_rub.flac", "ambi_haunted_hum.flac",
            "ambi_lunar_land.flac", "ambi_piano.flac", "ambi_sauna.flac",
            "ambi_swoosh.flac", "ambi_woosh.flac",
        ],
    },
    "guit": {
        "category": "Ambient",
        "json_file": "ambient.json",
        "description": "Guitar",
        "files": [
            "guit_e_fifths.flac", "guit_e_slide.flac",
            "guit_em9.flac", "guit_harmonics.flac",
        ],
    },

    # ── Drums & Percussion → sound_effects.json ──
    "bd": {
        "category": "Sound Effects",
        "json_file": "sound_effects.json",
        "description": "Bass Drum",
        "files": [
            "bd_808.flac", "bd_ada.flac", "bd_boom.flac", "bd_chip.flac",
            "bd_fat.flac", "bd_gas.flac", "bd_haus.flac", "bd_klub.flac",
            "bd_mehackit.flac", "bd_pure.flac", "bd_sone.flac", "bd_tek.flac",
            "bd_zome.flac", "bd_zum.flac",
        ],
    },
    "drum": {
        "category": "Sound Effects",
        "json_file": "sound_effects.json",
        "description": "Drum",
        "files": [
            "drum_bass_hard.flac", "drum_bass_soft.flac", "drum_cowbell.flac",
            "drum_cymbal_closed.flac", "drum_cymbal_hard.flac", "drum_cymbal_open.flac",
            "drum_cymbal_pedal.flac", "drum_cymbal_soft.flac", "drum_heavy_kick.flac",
            "drum_roll.flac", "drum_snare_hard.flac", "drum_snare_soft.flac",
            "drum_splash_hard.flac", "drum_splash_soft.flac",
            "drum_tom_hi_hard.flac", "drum_tom_hi_soft.flac",
            "drum_tom_lo_hard.flac", "drum_tom_lo_soft.flac",
            "drum_tom_mid_hard.flac", "drum_tom_mid_soft.flac",
        ],
    },
    "hat": {
        "category": "Sound Effects",
        "json_file": "sound_effects.json",
        "description": "Hi-Hat",
        "files": [
            "hat_bdu.flac", "hat_cab.flac", "hat_cats.flac",
            "hat_gem.flac", "hat_gnu.flac", "hat_gump.flac",
            "hat_kat.flac", "hat_klip.flac", "hat_metal.flac",
            "hat_mess.flac", "hat_musk.flac", "hat_para.flac",
            "hat_perf.flac", "hat_perl.flac", "hat_roxy.flac",
            "hat_snap.flac", "hat_star.flac", "hat_tics.flac",
            "hat_truk.flac", "hat_zen.flac", "hat_zild.flac",
        ],
    },
    "sn": {
        "category": "Sound Effects",
        "json_file": "sound_effects.json",
        "description": "Snare",
        "files": ["sn_dolf.flac", "sn_dub.flac", "sn_generic.flac", "sn_zome.flac"],
    },
    "ride": {
        "category": "Sound Effects",
        "json_file": "sound_effects.json",
        "description": "Ride Cymbal",
        "files": ["ride_tri.flac", "ride_via.flac"],
    },
    "perc": {
        "category": "Sound Effects",
        "json_file": "sound_effects.json",
        "description": "Percussion",
        "files": [
            "perc_bell.flac", "perc_bell2.flac", "perc_door.flac",
            "perc_impact1.flac", "perc_impact2.flac", "perc_snap.flac",
            "perc_snap2.flac", "perc_swoosh.flac", "perc_till.flac",
            "perc_woosh.flac",
        ],
    },

    # ── World / Misc → miscellaneous_ringtones.json ──
    "tabla": {
        "category": "Miscellaneous",
        "json_file": "miscellaneous_ringtones.json",
        "description": "Tabla",
        "files": [
            "tabla_dhec.flac", "tabla_ghe1.flac", "tabla_ghe2.flac",
            "tabla_ghe3.flac", "tabla_ghe4.flac", "tabla_ghe5.flac",
            "tabla_ghe6.flac", "tabla_ghe7.flac", "tabla_ghe8.flac",
            "tabla_ke1.flac", "tabla_ke2.flac", "tabla_ke3.flac",
            "tabla_na.flac", "tabla_na_s.flac", "tabla_re.flac",
            "tabla_tas1.flac", "tabla_tas2.flac", "tabla_tas3.flac",
            "tabla_te1.flac", "tabla_te2.flac", "tabla_te_m.flac",
            "tabla_te_ne.flac", "tabla_tun1.flac", "tabla_tun2.flac",
            "tabla_tun3.flac",
        ],
    },
    "mehackit": {
        "category": "Miscellaneous",
        "json_file": "miscellaneous_ringtones.json",
        "description": "Robot FX",
        "files": [
            "mehackit_phone1.flac", "mehackit_phone2.flac", "mehackit_phone3.flac",
            "mehackit_phone4.flac", "mehackit_robot1.flac", "mehackit_robot2.flac",
            "mehackit_robot3.flac", "mehackit_robot4.flac", "mehackit_robot5.flac",
            "mehackit_robot6.flac", "mehackit_robot7.flac",
        ],
    },
    "misc": {
        "category": "Miscellaneous",
        "json_file": "miscellaneous_ringtones.json",
        "description": "Sound FX",
        "files": [
            "misc_burp.flac", "misc_cineboom.flac", "misc_crow.flac",
        ],
    },
}


# ═══════════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════════

def make_title(filename, description):
    """Convert filename like 'elec_beep.flac' → 'Electronic Beep'"""
    name = filename.replace(".flac", "").replace(".mp3", "")
    parts = name.split("_", 1)
    if len(parts) == 2:
        prefix, rest = parts
        rest = rest.replace("_", " ").title()
        return f"[{description}] {rest}"
    return f"[{description}] {name.replace('_', ' ').title()}"


def build_entry(filename, description, cdn_base):
    """Build a JSON entry."""
    title = make_title(filename, description)
    encoded = filename.replace(" ", "%20")
    url = f"{cdn_base}/{encoded}"
    return {
        "title": title,
        "author": f"Sonic Pi - {description}",
        "time": "Sonic Pi",
        "url": url,
        "type": description,
    }


# ═══════════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════════

def main():
    assets_dir = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "app", "src", "main", "assets", "jsonres"
    )
    os.makedirs(assets_dir, exist_ok=True)

    # Collect entries per JSON file
    file_entries = {}

    for prefix, group in SONIC_PI_SAMPLES.items():
        json_file = group["json_file"]
        description = group["description"]
        for filename in group["files"]:
            entry = build_entry(filename, description, CDN_BASE)
            file_entries.setdefault(json_file, []).append(entry)

    # Write all files
    print(f"{'JSON File':<35} {'New':>6} {'Total':>6}")
    print("-" * 49)

    total = 0
    for filename in sorted(file_entries.keys()):
        filepath = os.path.join(assets_dir, filename)
        entries = file_entries[filename]

        # Merge with existing entries if file already exists
        existing = []
        if os.path.exists(filepath):
            with open(filepath, "r", encoding="utf-8") as f:
                try:
                    existing = json.load(f)
                except json.JSONDecodeError:
                    existing = []

        # Deduplicate by URL
        existing_urls = {e["url"] for e in existing}
        new_entries = [e for e in entries if e["url"] not in existing_urls]
        merged = existing + new_entries

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(merged, f, indent=2, ensure_ascii=False)

        print(f"{filename:<35} {len(new_entries):>6} {len(merged):>6}")
        total += len(new_entries)

    print(f"\n{'='*49}")
    print(f"New entries added: {total}")
    print(f"New categories for home page:")
    print(f"  Electronic     -> electronic.json")
    print(f"  Loops & Beats  -> loops_beats.json")
    print(f"  Ambient        -> ambient.json")
    print(f"\nEnriched existing:")
    print(f"  sound_effects.json")
    print(f"  miscellaneous_ringtones.json")
    print(f"\nFormat: FLAC (ExoPlayer-compatible)")
    print(f"CDN: jsDelivr - free, global, no API key")


if __name__ == "__main__":
    main()
