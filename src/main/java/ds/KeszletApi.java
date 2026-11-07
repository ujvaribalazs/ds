package ds;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

/** Üzemanyagkészlet API: kutak és tartályaik. */
public class KeszletApi {

    private static final String URL = "http://68.210.202.139:8080";

    public record Station(Long id, String name, String city) {}

    public record FuelStock(Long id, String fuelType, Long capacityLiters, Long availableLiters) {}

    public static List<Station> stations() throws Exception {
        return JsonHttp.get(URL + "/api/stations", new TypeReference<>() {});
    }

    public static List<FuelStock> stocks(long stationId) throws Exception {
        return JsonHttp.get(URL + "/api/stations/" + stationId + "/stocks", new TypeReference<>() {});
    }
}
