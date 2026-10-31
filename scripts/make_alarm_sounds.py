#!/usr/bin/env python3
"""Synthesise Tide's bundled alarm sounds. Every sound loops seamlessly.

All sounds are generated from scratch here, so they carry the project's licence (GPL-3.0).
The two classical pieces are public domain compositions, arranged and rendered by this script.
Usage: python3 scripts/make_alarm_sounds.py  (needs ffmpeg with libvorbis)
"""

import math
import random
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

RATE = 22_050
RAW = Path(__file__).resolve().parent.parent / "core/ringing/src/main/res/raw"
PEAK = 10 ** (-1.0 / 20)  # -1 dBFS


# --- Building blocks ---------------------------------------------------------------------------


def silence(seconds: float) -> list[float]:
    return [0.0] * int(seconds * RATE)


def white(n: int, rng: random.Random) -> list[float]:
    return [rng.uniform(-1, 1) for _ in range(n)]


def lowpass(x: list[float], cutoff: float) -> list[float]:
    a = 1 - math.exp(-2 * math.pi * cutoff / RATE)
    y, out = 0.0, []
    for v in x:
        y += a * (v - y)
        out.append(y)
    return out


def highpass(x: list[float], cutoff: float) -> list[float]:
    return [v - lp for v, lp in zip(x, lowpass(x, cutoff))]


def brown(n: int, rng: random.Random) -> list[float]:
    b, out = 0.0, []
    for _ in range(n):
        b = (b + 0.02 * rng.uniform(-1, 1)) / 1.02
        out.append(b * 3.5)
    return out


def mix(target: list[float], source: list[float], at: int = 0, gain: float = 1.0) -> None:
    for i, v in enumerate(source):
        if 0 <= at + i < len(target):
            target[at + i] += gain * v


def loop_seamlessly(x: list[float], fade_seconds: float) -> list[float]:
    """Crossfades the tail into the head, so the end flows straight into the start."""
    fade = int(fade_seconds * RATE)
    body, tail = x[: len(x) - fade], x[len(x) - fade :]
    for i in range(fade):
        w = i / fade
        body[i] = body[i] * w + tail[i] * (1 - w)
    return body


def tone(freq: float, seconds: float, partials: list[tuple[float, float]], decay: float, attack: float = 0.012) -> list[float]:
    out = []
    for i in range(int(seconds * RATE)):
        t = i / RATE
        env = min(1.0, t / attack) * math.exp(-t / decay)
        out.append(env * sum(a * math.exp(-t * r / 3) * math.sin(2 * math.pi * freq * r * t) for r, a in partials))
    return out


def note(name: str) -> float:
    """Frequency of a note such as 'C#5' or 'Bb3' (A4 = 440 Hz)."""
    steps = {"C": -9, "D": -7, "E": -5, "F": -4, "G": -2, "A": 0, "B": 2}[name[0]]
    rest = name[1:]
    if rest.startswith("#"):
        steps, rest = steps + 1, rest[1:]
    elif rest.startswith("b"):
        steps, rest = steps - 1, rest[1:]
    return 440.0 * 2 ** ((steps + 12 * (int(rest) - 4)) / 12)


def write(name: str, samples: list[float], quality: int = 3) -> None:
    scale = PEAK / max(abs(s) for s in samples)
    frames = b"".join(struct.pack("<h", round(s * scale * 32767)) for s in samples)
    out = RAW / f"{name}.ogg"
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "sound.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(frames)
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", str(quality),
             "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", str(out)],
            check=True,
        )
    print(f"{out.name}: {len(samples) / RATE:.1f} s, {out.stat().st_size // 1024} KB")


# --- Sounds ------------------------------------------------------------------------------------

SOFT_MALLET = [(1.0, 1.0), (2.0, 0.22), (3.0, 0.06)]
MUSIC_BOX = [(1.0, 1.0), (2.0, 0.35), (3.0, 0.12), (4.2, 0.05)]
PIANO = [(1.0, 1.0), (2.0, 0.45), (3.0, 0.2), (4.0, 0.1), (5.0, 0.05)]


def tide_chime() -> list[float]:
    """A calm, rising D major pentatonic figure; the default sound."""
    out = silence(4.0)
    for start, name in [(0.0, "D5"), (0.32, "F#5"), (0.64, "A5"), (0.96, "D6"), (1.6, "A5")]:
        mix(out, tone(note(name), 2.4, SOFT_MALLET, decay=0.45), int(start * RATE))
    fade = int(0.25 * RATE)
    for i in range(fade):
        out[-1 - i] *= i / fade
    return out


def sea(rng: random.Random) -> list[float]:
    """Waves rolling in every nine seconds: a rising rumble, the break, the hiss as they wash out."""
    seconds, period = 18.0, 9.0
    n = int((seconds + 2) * RATE)
    rumble = lowpass(lowpass(brown(n, rng), 400), 400)
    foam = lowpass(highpass(white(n, rng), 700), 4500)
    out = []
    for i in range(n):
        wave_index, phase = divmod(i / RATE / period, 1.0)
        size = 1.0 if int(wave_index) % 2 == 0 else 0.8
        swell = 0.25 + 0.75 * math.sin(math.pi * min(phase / 0.55, 1.0)) ** 2 if phase < 0.55 else 0.25 + 0.75 * math.exp(-(phase - 0.55) * 6)
        hiss = 0.08 + (0.9 * min(1.0, (phase - 0.5) / 0.04) * math.exp(-(phase - 0.5) * 5) if phase > 0.5 else 0.12 * phase)
        out.append(size * (0.9 * swell * rumble[i] + 0.55 * hiss * foam[i]))
    return loop_seamlessly(out, 2.0)


def rain_bed(n: int, rng: random.Random, level: float) -> list[float]:
    bed = lowpass(highpass(white(n, rng), 500), 5000)
    out = [level * v * (0.9 + 0.1 * math.sin(2 * math.pi * i / RATE / 7.0)) for i, v in enumerate(bed)]
    # Drops: short, damped plinks at random pitches.
    for _ in range(int(n / RATE * 45)):
        at = rng.randrange(n)
        freq, tau, amp = rng.uniform(900, 4200), rng.uniform(0.003, 0.012), rng.uniform(0.05, 0.3) * level * 2
        drop = [amp * math.exp(-k / RATE / tau) * math.sin(2 * math.pi * freq * k / RATE) for k in range(int(tau * 5 * RATE))]
        mix(out, drop, at)
    return out


def rain(rng: random.Random) -> list[float]:
    """Steady rain on leaves."""
    return loop_seamlessly(rain_bed(int(22 * RATE), rng, 0.5), 2.0)


def thunder(rng: random.Random) -> list[float]:
    """Rain with distant thunder rolling in twice."""
    n = int(32 * RATE)
    out = rain_bed(n, rng, 0.3)
    for start, size in [(3.0, 1.0), (17.5, 0.75)]:
        length = int(9 * RATE)
        crack = lowpass(highpass(white(int(0.5 * RATE), rng), 900), 2500)
        mix(out, [v * math.exp(-k / RATE / 0.12) for k, v in enumerate(crack)], int(start * RATE), 0.35 * size)
        rumble = lowpass(lowpass(brown(length, rng), 140), 140)
        rolls = lowpass(white(length, rng), 3)  # slow random swells of the rumble
        rolls_peak = max(abs(v) for v in rolls)
        shaped = [
            rumble[k] * min(1.0, k / RATE / 0.4) * math.exp(-k / RATE / 2.8) * (0.55 + 0.45 * rolls[k] / rolls_peak)
            for k in range(length)
        ]
        mix(out, shaped, int((start + 0.15) * RATE), 3.2 * size)
    return loop_seamlessly(out, 2.0)


def crickets(rng: random.Random) -> list[float]:
    """Crickets chirping on a summer night (Grillenzirpen)."""
    n = int(14 * RATE)
    out = [0.02 * v for v in lowpass(brown(n, rng), 300)]
    # (pitch, seconds between chirps, pulses per chirp, loudness, first chirp)
    for freq, every, pulses, amp, offset in [(4700, 0.62, 3, 0.5, 0.0), (4300, 0.83, 4, 0.3, 0.21), (5100, 0.47, 2, 0.14, 0.1)]:
        t = offset
        while t < n / RATE:
            loud = amp * rng.uniform(0.8, 1.0)
            for p in range(pulses):
                pulse = []
                for k in range(int(0.018 * RATE)):
                    s = k / RATE
                    env = min(1.0, s / 0.003, (0.018 - s) / 0.003)
                    pulse.append(loud * env * (math.sin(2 * math.pi * freq * s) + 0.15 * math.sin(4 * math.pi * freq * s)))
                mix(out, pulse, int((t + p * 0.032) * RATE))
            t += every * rng.uniform(0.95, 1.05)
    return loop_seamlessly(out, 1.5)


def alarm_clock() -> list[float]:
    """A classic digital alarm clock: four quick beeps, a pause."""
    out = silence(1.16)
    for b in range(4):
        beep = []
        for k in range(int(0.09 * RATE)):
            s = k / RATE
            env = min(1.0, s / 0.003, (0.09 - s) / 0.003)
            beep.append(env * (math.sin(2 * math.pi * 2600 * s) + 0.3 * math.sin(2 * math.pi * 7800 * s)))
        mix(out, beep, int(b * 0.16 * RATE))
    return out + out


def canon_in_d() -> list[float]:
    """Pachelbel, Canon in D (public domain): the ground bass with four variations, on a music box."""
    beat = 0.75
    bass = ["D3", "A2", "B2", "F#2", "G2", "D2", "G2", "A2"]
    triads = [("F#3", "A3"), ("C#3", "E3"), ("D3", "F#3"), ("A2", "C#3"), ("B2", "D3"), ("F#2", "A2"), ("B2", "D3"), ("C#3", "E3")]
    variations = [
        [(q, n) for q, n in enumerate(["F#5", "E5", "D5", "C#5", "B4", "A4", "B4", "C#5"])],
        [(q, n) for q, n in enumerate(["D5", "C#5", "B4", "A4", "G4", "F#4", "G4", "E4"])],
        [(e / 2, n) for e, n in enumerate("D4 F#4 A4 G4 F#4 D4 F#4 E4 D4 B3 D4 A4 G4 B4 A4 G4".split())],
        [(e / 2, n) for e, n in enumerate("F#4 D4 E4 C#5 D5 F#5 A5 A4 B4 G4 A4 F#4 D4 D5 D5 C#5".split())],
    ]
    out = silence(len(variations) * 8 * beat + 0.01)
    for v, melody in enumerate(variations):
        cycle = v * 8 * beat
        for q, name in enumerate(bass):
            at = int((cycle + q * beat) * RATE)
            mix(out, tone(note(name), 2.0, MUSIC_BOX, decay=0.9), at, 0.55)
            for third in triads[q]:
                mix(out, tone(note(third), 1.5, MUSIC_BOX, decay=0.6), at + int(0.02 * RATE), 0.16)
        for q, name in melody:
            mix(out, tone(note(name), 1.8, MUSIC_BOX, decay=0.7), int((cycle + q * beat) * RATE), 0.7)
    return loop_seamlessly(out + silence(1.0), 1.0)


def fur_elise() -> list[float]:
    """Beethoven, Für Elise (public domain): the opening theme, on a soft piano."""
    s = 0.17  # one sixteenth note
    rh = "E5 D#5 E5 D#5 E5 B4 D5 C5".split()
    events = [(i, n) for i, n in enumerate(rh)]  # pickup (2) and bar 1 (6)

    def bar(start: int, notes: list[str | None]) -> None:
        events.extend((start + i, n) for i, n in enumerate(notes) if n)

    a_bar = ["A2", "E3", "A3", "C4", "E4", "A4"]
    e_bar = ["E2", "E3", "G#3", "E4", "G#4", "B4"]
    bar(8, ["A4"] + a_bar[:3][1:] + a_bar[3:])
    events.append((8, "A2"))
    bar(14, ["B4"] + e_bar[1:])
    events.append((14, "E2"))
    bar(20, ["C5", "E3", "A3", "E4", "E5", "D#5"])
    events.append((20, "A2"))
    bar(26, "E5 D#5 E5 B4 D5 C5".split())
    bar(32, ["A4"] + a_bar[1:])
    events.append((32, "A2"))
    bar(38, ["B4", "E3", "G#3", "E4", "C5", "B4"])
    events.append((38, "E2"))
    bar(44, ["A4", "E3", "A3"])
    events.append((44, "A2"))
    length = 50 * s + 2.0
    out = silence(length)
    for step, name in events:
        loud = 0.5 if note(name) < 200 else 0.8
        mix(out, tone(note(name), 2.0, PIANO, decay=0.8), int(step * s * RATE), loud)
    return loop_seamlessly(out, 0.6)


def main() -> None:
    rng = random.Random(20261031)
    write("tide_chime", tide_chime(), quality=5)
    write("sound_sea", sea(rng))
    write("sound_rain", rain(rng))
    write("sound_thunder", thunder(rng))
    write("sound_crickets", crickets(rng))
    write("sound_alarm_clock", alarm_clock())
    write("sound_canon_in_d", canon_in_d())
    write("sound_fur_elise", fur_elise())


if __name__ == "__main__":
    main()
