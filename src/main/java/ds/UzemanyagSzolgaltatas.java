package ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ds.KeszletApi.FuelStock;
import ds.KeszletApi.KeszletMuvelet;
import ds.KeszletApi.Station;
import ds.KolcsonzoApi.Kolcsonzes;
import ds.UzemanyagNaplo.Bejegyzes;

//Üzemanyag-foglalás a kölcsönzött autókhoz: mely tartályok illenek az autóhoz, a foglalás
//elküldése és megvárása, és a kölcsönzéshez rendelés.

public class UzemanyagSzolgaltatas {

    // A foglalást a szerveren egy worker dolgozza fel. Legfeljebb ennyiszer
    // kérdezzük le.
    private static final int LEKERDEZESEK = 20;
    private static final long VARAKOZAS_MS = 500;

    // Egy kút egy tartálya, amelyből az autó üzemanyaga tankolható.
    public record KutTartaly(Station kut, FuelStock tartaly) {
    }

    // Az összes kút összes olyan tartálya, amelyből az autóba tankolni lehet,
    // kutanként sorban.
    public static List<KutTartaly> illoTartalyok(AutoUzemanyag uzemanyag) throws Exception {
        List<KutTartaly> illok = new ArrayList<>();
        for (Station s : KeszletApi.stations()) {
            for (FuelStock f : KeszletApi.stocks(s.id())) {
                if (uzemanyag.tankolhato(f.fuelType())) {
                    illok.add(new KutTartaly(s, f));
                }
            }
        }
        return illok;
    }

    public static List<KutTartaly> varosban(List<KutTartaly> tartalyok, String varos) {
        return tartalyok.stream().filter(j -> j.kut().city().equalsIgnoreCase(varos)).toList();
    }

    // Elküldi a foglalást, és megvárja, amíg a worker feldolgozza. Ha nem
    // utasították el,
    // a foglalást a kölcsönzéshez rendeli, akkor is, ha még feldolgozás alatt van.
    public static KeszletMuvelet foglal(Kolcsonzes kolcsonzes, KutTartaly valasztott, long liter) throws Exception {
        FuelStock tartaly = valasztott.tartaly();
        KeszletMuvelet foglalas = KeszletApi.foglaljUzemanyagot(tartaly.id(), liter);
        for (int i = 0; i < LEKERDEZESEK && !feldolgozva(foglalas); i++) {
            Thread.sleep(VARAKOZAS_MS);
            foglalas = KeszletApi.muvelet(foglalas.id());
        }
        if (foglalas.status() != OperationStatus.FAILED) {
            UzemanyagNaplo.hozzaad(new Bejegyzes(kolcsonzes.id(), foglalas.id(), tartaly.id(),
                    valasztott.kut().city(), valasztott.kut().name(), tartaly.fuelType(), liter));
        }
        return foglalas;
    }

    // A foglalás állapota mindig a szerverről jön, a napló csak az azonosítót
    // tárolja.
    public static KeszletMuvelet allapot(Bejegyzes bejegyzes) throws Exception {
        return KeszletApi.muvelet(bejegyzes.foglalasId());
    }

    // A kölcsönzések azonosítója szerint csoportosított üzemanyag-foglalások.
    public static Map<String, List<Bejegyzes>> kolcsonzesenkent() throws Exception {
        return UzemanyagNaplo.mind().stream().collect(Collectors.groupingBy(Bejegyzes::kolcsonzesId));
    }

    public static long foglaltLiter(Kolcsonzes kolcsonzes) throws Exception {
        return UzemanyagNaplo.kolcsonzeshez(kolcsonzes.id()).stream().mapToLong(Bejegyzes::liter).sum();
    }

    private static boolean feldolgozva(KeszletMuvelet muvelet) {
        return muvelet.status() == OperationStatus.DONE || muvelet.status() == OperationStatus.FAILED;
    }
}
