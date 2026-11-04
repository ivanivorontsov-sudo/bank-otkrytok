# Дополняет stock/ картинками CC0/PD с Wikimedia Commons (запуск из app/src/main/assets/stock)
import json,urllib.request,urllib.parse,re,os,io
from PIL import Image
UA={'User-Agent':'OtkrytkiApp/1.1 (ivan.vorontsov; card stock collection)'}
Q={'wedding':['wedding rings','wedding bouquet','white roses','wedding cake'],
'condolence':['white lilies','candle flame dark','calm lake fog','white chrysanthemum'],
'feb23':['winter forest sunrise','snowy mountains','sailing ship sea','oak tree'],
'birthday':['birthday balloons','cupcake candle','party confetti'],
'newyear':['snow covered spruce','christmas lights bokeh','winter village snow'],
'march8':['mimosa acacia flowers','spring tulips field','hyacinth flowers'],
'love':['pink roses','couple sunset silhouette','red tulips'],
'flowers':['peonies','sunflowers field','lily of the valley','chamomile field'],
'nature':['lake baikal','birch trees autumn','sea of okhotsk coast','mountain sunrise','waterfall'],
'animals':['kitten sleeping','red fox','swans lake','hedgehog'],
'victory':['red tulips bouquet','lilac blossom','memorial flowers']}
L=json.load(open('licenses.json'));seen={o['title'] for o in L}
def api(p): return json.load(urllib.request.urlopen(urllib.request.Request('https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(p),headers=UA),timeout=60))
for cat,qs in Q.items():
  for q in qs:
    try: r=api({'action':'query','format':'json','generator':'search','gsrnamespace':6,'gsrsearch':q+' filetype:bitmap','gsrlimit':40,'prop':'imageinfo','iiprop':'url|extmetadata|size|mime','iiurlwidth':900})
    except Exception as e: print('api',e);continue
    n=0
    for pg in sorted(r.get('query',{}).get('pages',{}).values(),key=lambda x:x.get('index',0)):
      ii=pg['imageinfo'][0];m=ii['extmetadata'];t=pg['title']
      lic=m.get('LicenseShortName',{}).get('value','');lu=m.get('License',{}).get('value','')
      if not re.search(r'^(CC0|Public domain|PD)',lic,re.I) and lu.lower() not in('cc0','pd'): continue
      if ii['mime']!='image/jpeg' or ii['width']<1000 or ii['height']<700: continue
      if re.search(r'logo|map|text|card|poster|stamp|sign|label|scan|book|page|cover|advert|flag|diagram|nude|naked|grave|tomb|corpse|war|weapon|swastika|soldier',t,re.I) or t in seen: continue
      try: d=urllib.request.urlopen(urllib.request.Request(ii['thumburl'],headers=UA),timeout=60).read(); im=Image.open(io.BytesIO(d)).convert('RGB')
      except Exception as e: print('fail',t,e);continue
      seen.add(t);k=len([o for o in L if o['category']==cat])+1;fn=f"{cat}_{k:02d}.jpg"
      im.thumbnail((900,900));im.save(fn,quality=78,optimize=True)
      L.append({'file':fn,'category':cat,'title':t,'license':lic,'source':ii['descriptionurl'],'author':re.sub('<[^>]+>','',m.get('Artist',{}).get('value',''))[:120]})
      n+=1
      if n>=3: break
    print(cat,q,n,flush=True)
json.dump(L,open('licenses.json','w'),ensure_ascii=False,indent=1);print(len(L))
