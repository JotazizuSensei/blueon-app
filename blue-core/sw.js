const VERSION='blue-core-13.6-shell';
const CORE_CACHE=`core-${VERSION}`;
const RUNTIME_CACHE=`runtime-${VERSION}`;
const SHELL=['./','./index.html','./manifest.webmanifest','../icon-192.png','../icon-512.png','../icon-maskable-512.png','../apple-touch-icon.png'];
self.addEventListener('install',event=>{event.waitUntil(caches.open(CORE_CACHE).then(c=>c.addAll(SHELL)).catch(()=>{}).then(()=>self.skipWaiting()));});
self.addEventListener('activate',event=>{event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>![CORE_CACHE,RUNTIME_CACHE].includes(k)).map(k=>caches.delete(k)))).then(()=>self.clients.claim()));});
self.addEventListener('fetch',event=>{
 const req=event.request;if(req.method!=='GET')return;
 const url=new URL(req.url);if(url.origin!==self.location.origin)return;
 if(req.mode==='navigate'||req.destination==='document'){
  event.respondWith(fetch(req).then(res=>{const copy=res.clone();caches.open(RUNTIME_CACHE).then(c=>c.put(req,copy));return res;}).catch(()=>caches.match(req).then(r=>r||caches.match('./index.html'))));return;
 }
 event.respondWith(caches.match(req).then(cached=>cached||fetch(req).then(res=>{if(res&&res.status===200){const copy=res.clone();caches.open(RUNTIME_CACHE).then(c=>c.put(req,copy));}return res;})));
});
self.addEventListener('message',event=>{if(event.data&&event.data.type==='SKIP_WAITING')self.skipWaiting();});