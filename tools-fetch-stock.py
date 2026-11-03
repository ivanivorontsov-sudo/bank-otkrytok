import json,urllib.request,urllib.parse,re,os,sys
UA={'User-Agent':'OtkrytkiApp/1.0 (ivan.vorontsov; card stock collection)'}
Q={'birthday':['birthday cake candles','balloons colorful','gift box ribbon'],
'newyear':['christmas tree decorated','fireworks night sky','snowy winter forest'],
'march8':['tulips bouquet','mimosa flowers','spring blossom'],
'love':['red roses','heart shape','romantic sunset'],
'flowers':['flower bouquet','daisies field','lilac flowers'],
'nature':['autumn forest leaves','mountain lake','sea sunset','birch forest'],
'animals':['kitten','puppy','squirrel'],
'victory':['red carnations','eternal flame']}
out=[];seen=set()
def api(p):
    return json.load(urllib.request.urlopen(urllib.request.Request('https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(p),headers=UA),timeout=60))
for cat,qs in Q.items():
  for q in qs:
    r=api({'action':'query','format':'json','generator':'search','gsrnamespace':6,'gsrsearch':q+' filetype:bitmap','gsrlimit':40,'prop':'imageinfo','iiprop':'url|extmetadata|size|mime','iiurlwidth':1024})
    n=0
    for pg in sorted(r.get('query',{}).get('pages',{}).values(),key=lambda x:x.get('index',0)):
      ii=pg['imageinfo'][0];m=ii['extmetadata'];t=pg['title']
      lic=m.get('LicenseShortName',{}).get('value','');lu=m.get('License',{}).get('value','')
      if not re.search(r'^(CC0|Public domain|PD)',lic,re.I) and lu.lower() not in('cc0','pd'): continue
      if ii['mime']!='image/jpeg' or ii['width']<1000 or ii['height']<700: continue
      if re.search(r'logo|map|text|card|poster|stamp|sign|label|scan|book|page|cover|advert|flag|diagram|nude|naked',t,re.I) or t in seen: continue
      seen.add(t);fn=f"{cat}_{len([o for o in out if o['category']==cat])+1:02d}.jpg"
      try:
        d=urllib.request.urlopen(urllib.request.Request(ii['thumburl'],headers=UA),timeout=60).read()
      except Exception as e: print('fail',t,e);continue
      open(fn,'wb').write(d)
      out.append({'file':fn,'category':cat,'title':t,'license':lic,'source':ii['descriptionurl'],'author':re.sub('<[^>]+>','',m.get('Artist',{}).get('value',''))[:120]})
      n+=1
      if n>=3: break
    print(cat,q,n,flush=True)
json.dump(out,open('licenses.json','w'),ensure_ascii=False,indent=1);print(len(out))
