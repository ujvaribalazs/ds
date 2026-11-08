package ds;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

// Üzemanyagkészlet API: kutak és tartályaik.
public class KeszletApi {

    private static final String URL = "http://68.210.202.139:8080";

    public record Station(Long id, String name, String city) {
    }

    public record FuelStock(Long id, KutUzemanyag fuelType, Long capacityLiters, Long availableLiters) {
    }

    public static List<Station> stations() throws Exception {
        return JsonHttp.get(URL + "/api/stations", new TypeReference<>() {
        });
    }

    public static List<FuelStock> stocks(long stationId) throws Exception {
        return JsonHttp.get(URL + "/api/stations/" + stationId + "/stocks", new TypeReference<>() {
        });
    }

    // A készlet-szolgáltatás egy művelete (foglalás, kivétel vagy feltöltés) és
    // annak állapota.
    public record KeszletMuvelet(String id, Long fuelStockId, OperationType type, Long liters,
            OperationStatus status,
            String failureReason) {
    }

    public static KeszletMuvelet foglaljUzemanyagot(Long fuelStockId, Long liters) throws Exception {
        return JsonHttp.post(URL + "/api/stocks/" + fuelStockId + "/reservations",
                Map.of("liters", liters), null, new TypeReference<KeszletMuvelet>() {
                });
    }

    // Egy beküldött művelet (pl. foglalás) aktuális állapota.
    public static KeszletMuvelet muvelet(String operationId) throws Exception {
        return JsonHttp.get(URL + "/api/operations/" + operationId, new TypeReference<>() {
        });
    }
}
