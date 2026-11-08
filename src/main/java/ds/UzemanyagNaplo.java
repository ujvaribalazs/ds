package ds;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Helyi feljegyzés arról, melyik üzemanyag-foglalás melyik autókölcsönzéshez tartozik.
 * A készlet-szerver nem tudja, ki foglalt, ezért ezt a kliens tárolja egy JSON fájlban.
 * Az üzemanyag-foglalás állapotát mindig a szervertől kérdezzük le, itt csak az azonosítója van.
 */
public class UzemanyagNaplo {

    public record Bejegyzes(String kolcsonzesId, String foglalasId, Long tartalyId, String varos,
            String kut, KutUzemanyag uzemanyag, Long liter) {
    }

    private static final Path FAJL = Path.of("uzemanyag-foglalasok.json");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<Bejegyzes> mind() throws IOException {
        if (!Files.exists(FAJL)) {
            return List.of();
        }
        return MAPPER.readValue(FAJL.toFile(), new TypeReference<>() {
        });
    }

    public static List<Bejegyzes> kolcsonzeshez(String kolcsonzesId) throws IOException {
        return mind().stream().filter(b -> b.kolcsonzesId().equals(kolcsonzesId)).toList();
    }

    public static void hozzaad(Bejegyzes bejegyzes) throws IOException {
        List<Bejegyzes> bejegyzesek = new ArrayList<>(mind());
        bejegyzesek.add(bejegyzes);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(FAJL.toFile(), bejegyzesek);
    }
}
