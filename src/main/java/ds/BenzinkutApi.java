package ds;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

/** Benzinkutak API: benzinkutak és üzemanyagáraik. */
public class BenzinkutApi {

    private static final String URL = "http://130.61.42.205:8080";

    public record Benzinkut(Long id, String nev, String cim, String benzinkutVezeto,
                            List<Uzemanyag> uzemanyagok) {}

    public record Uzemanyag(Long id, String uzemanyagTipus, Double arForint) {}

    public static List<Benzinkut> benzinkutak() throws Exception {
        return JsonHttp.get(URL + "/api/kutak", new TypeReference<>() {});
    }

    public static List<Uzemanyag> uzemanyagok(long id) throws Exception {
        Benzinkut kut = JsonHttp.get(URL + "/api/kutak/" + id, new TypeReference<>() {});
        return kut.uzemanyagok() == null ? List.of() : kut.uzemanyagok();
    }
}
