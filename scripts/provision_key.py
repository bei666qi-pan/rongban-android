#!/usr/bin/env python3
"""Provision only the selected MiMo key into a debuggable installation, never the APK."""
import argparse,re,json,subprocess
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--file',type=Path,required=True);args=parser.parse_args()
s=args.file.read_text();m=re.search(r'小米tokenplan的apikey\s*[:：=]\s*([^\s`]+)',s,re.I)
if not m:m=re.search(r'小米tokenplan的apikey\s*\n\s*([^\s`]+)',s,re.I)
if not m:raise SystemExit('MiMo credential entry not found')
adb='/opt/homebrew/share/android-commandlinetools/platform-tools/adb';pkg='io.github.bei666qipan.rongban'
subprocess.run([adb,'shell','run-as',pkg,'mkdir','-p','files'],check=True,capture_output=True)
subprocess.run([adb,'shell','run-as',pkg,'sh','-c','cat > files/credential-once.json'],input=json.dumps({'id':'mimo','key':m.group(1)}).encode(),check=True,capture_output=True)
subprocess.run([adb,'shell','am','force-stop',pkg],check=True,capture_output=True)
subprocess.run([adb,'shell','am','start','-n',pkg+'/.MainActivity'],check=True,capture_output=True)
print('MiMo credential provisioned; value withheld.')
