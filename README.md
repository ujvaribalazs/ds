# Autókölcsönző és üzemanyag-foglaló konzolos kliens

Egyszerű Java konzolalkalmazás, amely két különálló webszolgáltatást köt össze. Autót lehet vele
kölcsönözni, a kölcsönzött autóhoz pedig előre üzemanyagot foglalni egy benzinkúton.

- **Autókölcsönző API:** telephelyek, autók, regisztráció, bejelentkezés, kölcsönzés és lemondás.
- **Üzemanyagkészlet API:** benzinkutak, a kutak tartályai és az üzemanyag-foglalás.

A program az autó üzemanyag-típusához illő kutakat ajánlja fel. Egy dízel autóhoz csak dízeltartályt,
egy benzineshez vagy hibridhez 95-ös vagy 100-as benzint.

## Követelmények

- Java 21
- Maven 3

## Futtatás

```bash
mvn -q compile exec:java
```

Indításkor be kell jelentkezni, vagy előbb regisztrálni az autókölcsönzőben. Utána a főmenü
jelenik meg:

```
--- Főmenü (pelda@mail.hu) ---
  1  Autók listája
  2  Autó kölcsönzése
  3  Foglalásaim és üzemanyag
  4  Üzemanyag foglalása egy kölcsönzött autóhoz
  5  Kölcsönzés lemondása
  0  Kilépés
```

## Mire képes

- **Autók listája:** az autók telephelyenként, üzemanyag-típussal és napi árral.
- **Autó kölcsönzése:** autó, visszaadási hely és időpont kiválasztása. Ha az autó a kért napokon
  már foglalt, másik dátummal vagy másik autóval lehet próbálkozni. Sikeres kölcsönzés után a
  program rögtön felajánlja az üzemanyag-foglalást.
- **Foglalásaim és üzemanyag:** az aktív kölcsönzések, mindegyik alatt a hozzá foglalt üzemanyag
  és annak aktuális állapota.
- **Üzemanyag foglalása:** elég megadni a várost. A program megkeresi az ottani kutak megfelelő
  tartályait, és ha több is szóba jön, választani lehet közülük. A foglalást egy háttérfolyamat
  dolgozza fel, ezért a program megvárja, amíg elkészül vagy elutasítják.
- **Kölcsönzés lemondása:** megerősítés után lemondja a kiválasztott kölcsönzést.

Ahol a program választást kér, üres Enterrel vissza lehet lépni. A szerverek hibaüzeneteit
érthető formában írja ki, és ha a bejelentkezés lejár, újra kéri.

## Jó tudni

- **Az üzemanyag-foglalás nem mondható le.** A készlet-szolgáltatás jelenleg nem ad erre
  lehetőséget, ezért a kölcsönzés lemondása után is foglalva marad. A program erre lemondás előtt
  figyelmeztet.
- **Az összerendelés helyben tárolódik.** A készlet-szolgáltatás nem tudja, melyik üzemanyag-foglalás
  melyik kölcsönzéshez tartozik. Ezt a kliens az `uzemanyag-foglalasok.json` fájlban jegyzi fel,
  abban a mappában, ahonnan a programot indították. A foglalások állapotát mindig a szervertől
  kérdezi le. Másik gépről vagy másik mappából indítva a korábbi üzemanyag-foglalások nem látszanak.

## A kód felépítése

| Fájl | Szerepe |
|---|---|
| `Main.java` | belépési pont |
| `Konzol.java` | a menü és minden felhasználói párbeszéd, csak kérdez és kiír |
| `KolcsonzesSzolgaltatas.java` | a kölcsönzés szabályai: mi kölcsönözhető, mi aktív, kölcsönzés és lemondás |
| `UzemanyagSzolgaltatas.java` | az autóhoz illő tartályok, az üzemanyag-foglalás és megvárása, a kölcsönzéshez rendelés |
| `KolcsonzoApi.java` | az autókölcsönző API hívásai |
| `KeszletApi.java` | az üzemanyagkészlet API hívásai |
| `JsonHttp.java` | GET és POST kérések, JSON átalakítás |
| `HttpHiba.java` | hibás válasz: státuszkód és a szerver üzenete |
| `AutoUzemanyag.java` | az autó üzemanyaga, és hogy milyen kúti üzemanyag tankolható bele |
| `KutUzemanyag.java` | a kutak üzemanyag-típusai |
| `OperationStatus.java`, `OperationType.java` | a készlet-szolgáltatás feladatainak állapota és típusa |
| `UzemanyagNaplo.java` | a kölcsönzések és üzemanyag-foglalások helyi összerendelése |
