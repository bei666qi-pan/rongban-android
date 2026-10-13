#!/usr/bin/env python3
"""Download pinned upstream speech runtime/models. Assets stay out of Git."""
import urllib.request,tarfile,tempfile,shutil,concurrent.futures,hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
ASSETS=ROOT/'core/src/main/assets/models'
NATIVE=ROOT/'core/src/main/jniLibs'
JOBS=[('runtime','https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-android.tar.bz2'),('asr','https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30.tar.bz2'),('tts','https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2')]
def fetch(job):
 name,url=job
 dest=NATIVE if name=='runtime' else ASSETS/name
 if dest.exists() and any(dest.rglob('*.so' if name=='runtime' else '*.onnx')):
  print(name+': ready',flush=True);return
 with tempfile.TemporaryDirectory(prefix='rongban-speech-') as td:
  tmp=Path(td);archive=tmp/'asset.tar.bz2';print(name+': downloading',flush=True)
  urllib.request.urlretrieve(url,archive)
  digest=hashlib.sha256(archive.read_bytes()).hexdigest()
  expected=ROOT/'third_party/speech-downloads'/(name+'.json')
  if expected.exists() and json.loads(expected.read_text())['sha256']!=digest: raise RuntimeError(name+': upstream checksum changed')
  with tarfile.open(archive) as tar: tar.extractall(tmp,filter='data')
  if name=='runtime':
   for abi in ['arm64-v8a','x86_64']:
    for f in tmp.rglob('*.so'):
     if abi in f.parts:
      target=NATIVE/abi/f.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(f,target)
   if not any(NATIVE.rglob('*.so')): raise RuntimeError('Native archive layout unsupported')
  else:
   model=next(tmp.rglob('encoder.int8.onnx' if name=='asr' else 'model.onnx')).parent
   shutil.copytree(model,dest,dirs_exist_ok=True)
   shutil.rmtree(dest/'test_wavs',ignore_errors=True)
   if name=='tts': (dest/'rule.far').unlink(missing_ok=True)
  meta=ROOT/'third_party/speech-downloads';meta.mkdir(parents=True,exist_ok=True)
  (meta/(name+'.json')).write_text(json.dumps({'url':url,'sha256':digest},indent=2))
  print(name+': ready',flush=True)
if __name__=='__main__':
 with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool: list(pool.map(fetch,JOBS))
