#!/usr/bin/env python3
"""Headless glide probe for bug #44 (tarantula hawk elytra).

    ./gradlew :<node>:runServer          # with enable-rcon=true in its run/server.properties
    scripts/glidetest.py [rcon-port]     # default 25575


Fall-flying is a server-side shared flag (7) that LivingEntity#updateFallFlying
re-validates every tick against the chest item, and LivingEntity saves it as the
boolean NBT tag "FallFlying". So a zombie is a perfectly good probe: put a chest
item on it, force the flag on via /data merge (readAdditionalSaveData sets shared
flag 7 from that tag), let it fall, and read the tag plus the position back.

  flag survives + travels far horizontally -> the game accepts it as a glider
  flag cleared  + drops straight down       -> it does not

Three arms per run: a vanilla elytra (proves the harness works), an iron
chestplate (proves the readout discriminates), and ours (the actual question).

They glide along +X in three separate z-lanes, over a force-loaded corridor --
an entity in an unloaded chunk does not tick AND is invisible to selectors, which
looks exactly like "the flag was cleared".
"""
import subprocess, sys, time, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
PORT = sys.argv[1] if len(sys.argv) > 1 else '25575'

def rcon(*cmds):
    out = subprocess.run([sys.executable, os.path.join(HERE, 'rcon.py'),
                          '--port', PORT, *cmds],
                         capture_output=True, text=True, timeout=90)
    return out.stdout.strip()

ARMS = [
    ('vanilla_elytra', 'minecraft:elytra'),
    ('iron_chestplate', 'minecraft:iron_chestplate'),
    ('am_elytra', 'alexsmobs:tarantula_hawk_elytra'),
]

rcon('gamerule doMobSpawning false', 'gamerule doDaylightCycle false',
     'gamerule randomTickSpeed 0', 'gamerule doFireTick false',
     'time set noon', 'weather clear',
     'kill @e[type=!player]',
     'forceload remove all',
     'forceload add -32 -32 400 64')
time.sleep(2)

START = {}
for i, (tag, item) in enumerate(ARMS):
    z = i * 8
    # No NoAI: it stops the mob ticking at all, so it hangs motionless at y=300
    # and every arm reads the same as "did not glide".
    rcon(f'summon minecraft:zombie 0 300 {z} {{Tags:["{tag}"],Invulnerable:1b,Silent:1b,'
         f'PersistenceRequired:1b,Rotation:[-90f,0f],'
         f'ArmorItems:[{{}},{{}},{{id:"{item}",Count:1b}},{{}}]}}')
time.sleep(1)

for tag, _ in ARMS:
    r = rcon(f'data merge entity @e[tag={tag},limit=1] {{FallFlying:1b}}')
    print(f'{tag:16} arm ready: {r.splitlines()[-1]}')

def sample(label):
    row = []
    for tag, _ in ARMS:
        ff = rcon(f'data get entity @e[tag={tag},limit=1] FallFlying')
        pos = rcon(f'data get entity @e[tag={tag},limit=1] Pos')
        # `data get <path>` echoes only the value, never the key: "... entity data: 1b".
        m = re.search(r'entity data: (\w+)', ff)
        p = re.findall(r'(-?[\d.]+)d', pos)
        flag = m.group(1) if m else 'GONE'
        if p:
            row.append(f'{tag:16} FallFlying={flag:5} x={float(p[0]):7.1f} y={float(p[1]):6.1f}')
        else:
            row.append(f'{tag:16} FallFlying={flag:5} (not found)')
    print(f'--- {label}')
    for r in row:
        print('    ' + r)

for wait in (2, 5, 8):
    time.sleep(wait)
    sample(f't+{wait}s')

# Durability: vanilla drains the glider component itself on >=1.21.2; on Fabric <1.21.2 the
# drain is our own EntityElytraEvents.CUSTOM handler calling AMCompat.hurtAndBreak every 20
# flight ticks. Both should track the vanilla elytra.
print('--- chest item after the glide')
for tag, _ in ARMS:
    r = rcon(f'data get entity @e[tag={tag},limit=1] ArmorItems[2]')
    print(f'    {tag:16} ' + (r.split('entity data: ')[-1] if 'entity data' in r else r.splitlines()[-1]))
