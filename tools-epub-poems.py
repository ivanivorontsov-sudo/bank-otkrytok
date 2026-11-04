# Извлекает стихи Ивана Воронцова из epub в app/src/main/assets/poems.json
import re,sys,json,glob,html,os
src=sys.argv[1]
rules=[("День Победы",r"побед|войн|солдат|фронт|ветеран|героя|героев|блокад|сорок"),
("Новый год",r"нов(ый|ым|ого) год|снег|зим|ёлк|елк|мороз|декабр|январ"),
("Восхищение и любовь",r"любл|любов|милая|милой|сердц|поцел|нежн|красот|женщ"),
("Природа",r"лес|реч|рек|поле|берёз|осен|весн|лето|летн|солнц|облак|море|тайг|сосн|цвет"),
("Поддержка",r"не сдав|вперёд|вперед|сил[аыу]|верь|вер[иу]|надежд|сломить|победить|держ"),
("Мудрость",r"жизн|судьб|мудр|врем|душ|истин|смысл"),
("Радость и отдых",r"радост|смех|весел|праздн|друз|друг|отдых|улыб")]
out=[]
for f in sorted(glob.glob(os.path.join(src,"poem*.xhtml"))):
    t=open(f,encoding="utf-8").read()
    title=html.unescape(re.sub("<.*?>","",re.search(r"<h2>(.*?)</h2>",t,re.S).group(1))).strip()
    st=[]
    for s in re.findall(r'<p class="stanza">(.*?)</p>',t,re.S):
        st.append("\n".join(html.unescape(re.sub("<.*?>","",v)).strip() for v in re.findall(r'<span class="v">(.*?)</span>',s,re.S)))
    text="\n\n".join(x for x in st if x)
    if not text: continue
    low=(title+" "+text).lower()
    cats=[c for c,r in rules if re.search(r,low)] or ["Мудрость"]
    out.append({"category":cats[0],"categories":cats,"title":title,"text":text})
json.dump(out,open(sys.argv[2],"w",encoding="utf-8"),ensure_ascii=False,indent=1)
print(len(out))
