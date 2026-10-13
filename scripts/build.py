#!/usr/bin/env python3
"""Use the host network proxy for Gradle without persisting machine settings."""
import os,sys,subprocess,urllib.parse
from pathlib import Path
p=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',''))
env=os.environ.copy()
if p.hostname:
 env['GRADLE_OPTS']=env.get('GRADLE_OPTS','')+f' -Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port or 80} -Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port or 80} -Dhttp.nonProxyHosts=localhost|127.*'
raise SystemExit(subprocess.call([str(Path(__file__).resolve().parent.parent/'gradlew'),'--console=plain',*sys.argv[1:]],env=env))
