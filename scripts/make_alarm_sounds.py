#!/usr/bin/env python3
"""Synthesise Drift's bundled alarm sounds. Every sound loops seamlessly.

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


def pink(n: int, rng: random.Random) -> list[float]:
    """Pink noise (Paul Kellet's filter): equal energy per octave, like surf and wind."""
    b0 = b1 = b2 = b3 = b4 = b5 = b6 = 0.0
    out = []
    for _ in range(n):
        w = rng.uniform(-1, 1)
        b0 = 0.99886 * b0 + w * 0.0555179
        b1 = 0.99332 * b1 + w * 0.0750759
        b2 = 0.96900 * b2 + w * 0.1538520
        b3 = 0.86650 * b3 + w * 0.3104856
        b4 = 0.55000 * b4 + w * 0.5329522
        b5 = -0.7616 * b5 - w * 0.0168980
        out.append((b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362) * 0.11)
        b6 = w * 0.115926
    return out


def sweep_lowpass(x: list[float], cutoff) -> list[float]:
    """A two-pole low-pass whose cutoff follows cutoff(seconds): a wave opening up as it breaks."""
    y1 = y2 = 0.0
    out = []
    for i, v in enumerate(x):
        a = 1 - math.exp(-2 * math.pi * cutoff(i / RATE) / RATE)
        y1 += a * (v - y1)
        y2 += a * (y1 - y2)
        out.append(y2)
    return out


def bandpass(x: list[float], low: float, high: float) -> list[float]:
    return lowpass(lowpass(highpass(highpass(x, low), low), high), high)


def saturate(x: list[float], drive: float) -> list[float]:
    """Soft clipping. Its overtones let a phone's small speaker hint at bass it cannot play."""
    return [math.tanh(drive * v) / math.tanh(drive) for v in x]


def compress(x: list[float], level: float = 0.6) -> list[float]:
    """Lifts the body of a sound and rounds off its few sharp peaks, so it carries without spikes deciding the volume."""
    loud = sorted(abs(v) for v in x)[int(len(x) * 0.995)] or 1.0
    return [math.tanh(level * v / loud) for v in x]


def normalise(x: list[float]) -> list[float]:
    peak = max(abs(v) for v in x) or 1.0
    return [v / peak for v in x]


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


def chime() -> list[float]:
    """A calm, rising D major pentatonic figure; the default sound."""
    out = silence(4.0)
    for start, name in [(0.0, "D5"), (0.32, "F#5"), (0.64, "A5"), (0.96, "D6"), (1.6, "A5")]:
        mix(out, tone(note(name), 2.4, SOFT_MALLET, decay=0.45), int(start * RATE))
    fade = int(0.25 * RATE)
    for i in range(fade):
        out[-1 - i] *= i / fade
    return out


def breaker(rng: random.Random, size: float, seconds: float) -> list[float]:
    """One wave on a sandy beach: it rises, breaks with a roar, then washes out as fizzing foam and draws back."""
    n = int(seconds * RATE)
    crash = 1.6  # seconds from the first swell to the break
    # The body of the wave: pink noise that opens up as it breaks and closes again as it runs out.
    body = sweep_lowpass(
        pink(n, rng),
        lambda t: 350 + 900 * t / crash if t < crash else 700 + 5200 * min(1.0, (t - crash) / 0.35) * math.exp(-(t - crash) / 1.1),
    )

    def body_level(t: float) -> float:
        if t < crash:
            return 0.15 + 0.35 * (t / crash) ** 2
        rise = math.sin(0.5 * math.pi * min(1.0, (t - crash) / 0.35))
        return 0.5 * math.exp(-(t - crash) / 3.5) + 0.9 * rise * math.exp(-(t - crash) / 0.9)

    out = [size * body_level(i / RATE) * v for i, v in enumerate(body)]
    # Foam running up the sand: bright hiss, slowly fading.
    foam = bandpass(white(n, rng), 1800, 7000)
    for i in range(int(crash * RATE), n):
        t = i / RATE - crash
        out[i] += size * 0.35 * min(1.0, t / 0.4) * math.exp(-t / 2.2) * foam[i]
    # Bubbles bursting in the foam: thousands of tiny clicks, thinning out.
    t = crash + 0.2
    while t < seconds - 0.1:
        density = 450 * math.exp(-(t - crash) / 1.8) + 25
        freq, tau = rng.uniform(2500, 8000), rng.uniform(0.0015, 0.005)
        amp = size * rng.uniform(0.02, 0.12) * math.exp(-(t - crash) / 3.0)
        mix(out, [amp * math.exp(-k / RATE / tau) * math.sin(2 * math.pi * freq * k / RATE) for k in range(int(tau * 5 * RATE))], int(t * RATE))
        t += rng.expovariate(density)
    # The backwash: the water drawing back down the beach, darker and darker.
    back_start = crash + 3.0
    back = sweep_lowpass(white(n, rng), lambda t: max(500.0, 4000 - 1200 * max(0.0, t - back_start)))
    for i in range(int(back_start * RATE), n):
        t = i / RATE - back_start
        out[i] += size * 0.12 * math.sin(math.pi * min(1.0, t / (seconds - back_start))) * back[i]
    return out


def sea(rng: random.Random) -> list[float]:
    """Waves breaking on a sandy beach, at uneven intervals, over the steady murmur of the surf further out."""
    seconds = 34.0
    n = int(seconds * RATE)
    murmur = sweep_lowpass(pink(n, rng), lambda t: 700 + 200 * math.sin(2 * math.pi * t / 13))
    out = [0.08 * v for v in murmur]
    # (start, size, length): a big wave, a smaller one, a middle one, a small one.
    for start, size, length in [(0.0, 1.0, 10.5), (9.0, 0.7, 9.0), (17.0, 0.9, 10.0), (25.5, 0.6, 8.5)]:
        mix(out, breaker(rng, size, length), int(start * RATE))
    return loop_seamlessly(compress(out), 2.5)


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


def rumble(rng: random.Random, seconds: float, rolls: int) -> list[float]:
    """Thunder rolling: a deep growl, swelling several times as the sound arrives from along the bolt."""
    n = int(seconds * RATE)
    growl = saturate(normalise(bandpass(brown(n, rng), 40, 420)), 2.5)
    # The growl again, an octave higher and rougher, so a phone's speaker carries it.
    rasp = normalise(bandpass(white(n, rng), 150, 900))
    swells = [(rng.uniform(0.05, 0.6) * seconds, rng.uniform(0.5, 1.0), rng.uniform(0.6, 1.8)) for _ in range(rolls)]
    swells.append((0.0, 1.0, 0.8))

    def level(t: float) -> float:
        roll = sum(h * math.exp(-(((t - at) / width) ** 2)) for at, h, width in swells)
        return min(1.0, t / 0.15) * math.exp(-t / (seconds / 3)) * (0.35 + roll)

    return [level(i / RATE) * (0.8 * g + 0.35 * r) for i, (g, r) in enumerate(zip(growl, rasp))]


def crack(rng: random.Random) -> list[float]:
    """A strike close by: the air tearing in a rapid burst of snaps, then a boom."""
    n = int(1.2 * RATE)
    out = [0.0] * n
    t = 0.0
    while t < 0.35:
        snap = bandpass(white(int(rng.uniform(0.004, 0.02) * RATE), rng), 600, 6000)
        amp = rng.uniform(0.4, 1.0) * (1 - t / 0.5)
        mix(out, [amp * v * math.exp(-k / RATE / 0.006) for k, v in enumerate(snap)], int(t * RATE))
        t += rng.uniform(0.008, 0.04)
    boom = lowpass(lowpass(white(n, rng), 700), 700)
    mix(out, [3.0 * v * min(1.0, k / RATE / 0.02) * math.exp(-k / RATE / 0.35) for k, v in enumerate(boom)], int(0.08 * RATE))
    return out


def thunder(rng: random.Random) -> list[float]:
    """A thunderstorm: soft rain, a distant roll, a strike close by, and one more roll moving away."""
    seconds = 38.0
    n = int(seconds * RATE)
    out = rain_bed(n, rng, 0.12)
    # (start, loudness, length of the roll, swells, close enough to crack)
    for start, loud, length, rolls, close in [(1.0, 0.55, 9.0, 3, False), (12.5, 1.0, 12.0, 5, True), (26.0, 0.7, 9.5, 4, False)]:
        if close:
            mix(out, crack(rng), int(start * RATE), 0.9 * loud)
        mix(out, rumble(rng, length, rolls), int((start + (0.25 if close else 0.0)) * RATE), loud)
    return loop_seamlessly(compress(out, 0.7), 2.0)


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


def skip(rng: random.Random, draws: int) -> None:
    for _ in range(draws):
        rng.random()


def main() -> None:
    def rng(name: str) -> random.Random:
        # Each sound draws its own random numbers, so remaking one leaves the others as they are.
        return random.Random(f"20261031-{name}")

    write("chime", chime(), quality=5)
    write("sound_sea", sea(rng("sea")))
    write("sound_thunder", thunder(rng("thunder")))
    # Rain and crickets keep the random numbers of their first version, which Martin approved by ear
    # (2026-10-31; the crickets with other numbers sounded noticeably worse). Those numbers came
    # after the first sea, and for the crickets after the first thunderstorm too, so skip their share.
    first = random.Random(20261031)
    skip(first, 2 * int(20 * RATE))  # the first sea: brown and white noise
    write("sound_rain", rain(first))
    rain_bed(int(32 * RATE), first, 0.3)  # the first thunderstorm: its rain, then two claps
    skip(first, 2 * (int(0.5 * RATE) + 2 * int(9 * RATE)))
    write("sound_crickets", crickets(first))
    write("sound_alarm_clock", alarm_clock())
    write("sound_canon_in_d", canon_in_d())
    write("sound_fur_elise", fur_elise())


if __name__ == "__main__":
    main()
