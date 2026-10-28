#!/usr/bin/env python3
"""Provision only the selected MiMo key into a debuggable installation, never the APK."""
import argparse, json, os, re, shutil, subprocess
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--file', type=Path, required=True)
parser.add_argument('--name', default='')
parser.add_argument('--student', default='')
parser.add_argument('--wipe', action='store_true', help='Remove injected credential files from the debug install')
args = parser.parse_args()

def adb_bin():
    env = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or ''
    candidates = [
        shutil.which('adb') or '',
        '/opt/homebrew/share/android-commandlinetools/platform-tools/adb',
        str(Path(env) / 'platform-tools' / 'adb') if env else '',
        str(Path.home() / 'Android' / 'Sdk' / 'platform-tools' / 'adb'),
    ]
    for candidate in candidates:
        if candidate and Path(candidate).exists():
            return candidate
    raise SystemExit('adb not found; set ANDROID_HOME or install platform-tools')

adb = adb_bin()
pkg = 'io.github.bei666qipan.rongban'

def run(cmd, **kwargs):
    return subprocess.run([adb, *cmd], check=True, capture_output=True, **kwargs)

if args.wipe:
    run(['shell', 'run-as', pkg, 'rm', '-f', 'files/credential-once.json'])
    print('Injected credential file removed if it existed.')
    raise SystemExit(0)

s = args.file.read_text()
m = re.search(r'小米tokenplan的apikey\s*[:：=]\s*([^\s`]+)', s, re.I)
if not m:
    m = re.search(r'小米tokenplan的apikey\s*\n\s*([^\s`]+)', s, re.I)
if not m:
    raise SystemExit('MiMo credential entry not found')
run(['shell', 'run-as', pkg, 'mkdir', '-p', 'files'])
run(['shell', 'run-as', pkg, 'sh', '-c', "'cat > files/credential-once.json'"], input=json.dumps({'id': 'mimo', 'key': m.group(1), 'studentName': args.name, 'studentNumber': args.student}).encode())
run(['shell', 'am', 'force-stop', pkg])
run(['shell', 'am', 'start', '-n', pkg + '/.MainActivity'])
print('MiMo credential provisioned; value withheld.')
