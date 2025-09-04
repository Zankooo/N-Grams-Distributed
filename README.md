# Analysis of N-grams (simple text analysis) - porazdeljena (distributed) izvedba

## Kaj dela
Več o tem; 
- Kaj program dela, 

## Primer uporabe 
Več si lahko prebereš na naslednji povezavi:
https://github.com/Zankooo/N-Grams-Sequential

## Uporaba
Program uporabljamo tako, da mu določimo dva argumenta.
- prvi je neko dolgo besedilo in,
- drugi n, ki je dolžina n-gramov.

Oba parametra mu določimo znotraj kode. Oba parametra določimo v funkciji; beriInPripraviPodatke(), v vrstici 104

## Testiranje
Za testiranje sem uporabil svoj lasten laptop: <ins>Apple MacBook Pro, M1 Max, 64GB/2TB</ins>.
(Komp sem kupil za 1600eur in še 16 inch je :) )
Javi virtual machine sem dal na voljo cca 16GB max heap size (rama) za izvajanje programa. Na trajanje programa je zelo pomembno koliko ga imamo na voljo, saj uporabljamo v programu podatkovno strukturo HashMap in kot input dajemo podatke ki so precej veliki. (HashMap in veliki podatki --> hitrost izvajanja programa odvisna od velikosti rama)</ins>
Testiranje je bilo opravljenju na petih različno velikih .txt file-ih. Dolzina n-gramov pa je od 2 do 5. Tesitranje je bilo opravljeno brez printanja n-gramov z pojavitvami in relativnimi frekvencami. Če bi jih printali bi program trajal občutno dlje.



| Tabela    | n = 2     | n = 3     | n = 4     | n = 5     |
|-----------|-----------|-----------|-----------|-----------|
| **123MB** | 4,26 sec  | 7,51 sec  | 10,83 sec | 12,73 sec |
| **234MB** | 15,49 sec | 25,49 sec | 31,30 sec | 36,89 sec |
| **350MB** | 22,33 sec | 38,07 sec | 46,73 sec | 49,51 sec |
| **490MB** | 16,77 sec | 26,65 sec | 38,78 sec | 48,68 sec |
| **613MB** | 18,98 sec | 31,81 sec | 47,40 sec | 60,25 sec |

Opomba 1: Pri testiranju je bilo število workerjev; 4. 
Opomba 2: številke so zapisane v evropskem formatu, kjer vejica pomeni decimalko


## Zelo pomembna navodila za uspešen zagon programa
Opomba 1: Setup za delovanje je precej daljši kot pri sekvenčni in vzporedni verziji).
Opomba 2: Ta setup je za macos, za windows je rahlo drugače

1. Če programa še nimaš lokalno, ga pridobiš z komando v terminal:
` git clone https://github.com/Zankooo/N-Grams-Distributed.git`
2. V root direktoriju ustvariš direktorij 'resources' in vanj daš datoteke iz tega linka:
https://drive.google.com/drive/folders/1GnL52MgBBja04Hhqun_TRghp_sVrtZ2F?usp=share_link
3. Za delovanje programa je potrebno manualno naložiti knjižnico (library) MPI/MPJ. To narediš na tej povezavi; 
https://sourceforge.net/projects/mpjexpress/files/releases/ 
in preneseš najnovejšo verzijo (iz: 2015-04-17)
4. Nato moraš manualno dodati MPJ v Intellij 
` file -> project structure -> libraries in dodati pot do mpj.jar`
5. Nato moraš dodati okoljke spremenljivke (MPJ_HOME in PATH). V zshrc (komanda v terminal: nano .zshrc) dodaš vrstici:
`export MPJ_HOME=~/pot-do-root-direktorija-mpi-mpj` 
`export PATH=$PATH:$MPJ_HOME/bin`
6. S komando 
`mpjrun.sh`
preveriš ali je vse okej! Če ja je to to za setup
7. Program pa poženeš ne z green button v Intellij ampak tudi to je manualno
V terminalu moraš prvo compliati Main.java in šele nato lahko poženeš. To narediš pa:
`javac -cp .:$MPJ_HOME/lib/mpj.jar Main.java` - compile
`mpjrun.sh -np 4 Main` - poženeš in dela! Cifra med -np in Main je število workerjev. In heapspace je 1/4 rama
<ins>8. Dodatno</ins>: 
- In vedno ko narediš spremembo v kodi moraš ponovno compile in pognati (dve komandi)!
- Manualno heap space lahko določaš če komandi za pogon dodaj še -Xmx8g (spremenimo cifro po želji koliko rama mu dodamo). Recimo:
`mpjrun.sh -np 4 -Xmx8g Main`  


## Druge informacije
- uporabljal sem trenutno najnovejšo verzijo Jave; JDK 24



## Viri in literatura
Primarno sem si pri izdelovanju projekta pomagal z znanjem pridobljenim na predavanjih in vajah:
- https://e.famnit.upr.si/course/view.php?id=6182 - letošnja eučilnica
- https://e.famnit.upr.si/course/view.php?id=4943 - eučilnica preteklih let

Za nekaj praktičnih nasvetov sem se obrnil tudi na prijatelja, ki je predmet že opravil. Prav tako sem redno komuniciral s sošolci preko Discorda s katerimi sem se pogovarjal o skupnih problemih. Eden od teh je bil setup MPI.
Seveda sem se pa posluževal tudi umetne inteligence, brez katere bi bilo narediti implementacijo projekta precej težje; ChatGPT-4o, Gemini 2.5 Pro in DeepSeek.

