package org.javaapp.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.javaapp.model.Location;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GeocodingApi {
    private static final String GEOCODING_API_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String LOCATIONIQ_API_URL = "https://eu1.locationiq.com/v1/reverse";
    private static final String LOCATIONIQ_API_KEY = "pk.dcf9b95dd2788b4ce97ccdf58b048d04"; // Zastąpione własnym kluczem
    
    // Wyszukiwanie lokalizacji po nazwie
    public List<Location> searchLocationsByName(String locationName) throws IOException, ParseException {
        String encodedName = URLEncoder.encode(locationName, StandardCharsets.UTF_8);
        String url = GEOCODING_API_URL + "?name=" + encodedName + "&count=5&language=pl&format=json";
        
        return fetchLocations(url);
    }

    // Wyszukiwanie najbliższej lokalizacji na podstawie współrzędnych geograficznych
    public Location findNearestLocation(double latitude, double longitude) throws IOException, ParseException {
        // Używamy LocationIQ API do odwrotnego geokodowania
        String url = LOCATIONIQ_API_URL + 
                "?key=" + LOCATIONIQ_API_KEY + 
                "&lat=" + latitude + 
                "&lon=" + longitude + 
                "&format=json";
        
        System.out.println("Wyszukiwanie lokalizacji z URL: " + url);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    System.out.println("Odpowiedź z API LocationIQ: " + result);
                    
                    // Parsowanie odpowiedzi z LocationIQ
                    JsonParser parser = new JsonParser();
                    JsonElement jsonElement = parser.parse(result);
                    
                    // Sprawdź czy odpowiedź nie zawiera błędu
                    if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("error")) {
                        String errorMessage = jsonElement.getAsJsonObject().get("error").getAsString();
                        System.out.println("Błąd API LocationIQ: " + errorMessage);
                        
                        // Jeśli współrzędne są na Oceanie lub w miejscu bez nazwy
                        if ("Unable to geocode".equals(errorMessage)) {
                            if (latitude == 0.0 && longitude == 0.0) {
                                // Użyj domyślnych współrzędnych Warszawy
                                return findNearestLocation(52.2297, 21.0122);
                            }
                            return new Location(latitude, longitude, "Lokalizacja nieokreślona", "Brak danych", "Brak danych", "");
                        }
                        
                        throw new IOException("Błąd geokodowania: " + errorMessage);
                    }
                    
                    JsonObject locationObj = jsonElement.getAsJsonObject();
                    
                    String city = "";
                    String county = "";
                    String state = "";
                    String country = "";
                    
                    // Pobieramy informacje z sekcji address
                    if (locationObj.has("address")) {
                        JsonObject address = locationObj.getAsJsonObject("address");
                        
                        // Miasto (admin3)
                        if (address.has("city")) {
                            city = address.get("city").getAsString();
                        } else if (address.has("town")) {
                            city = address.get("town").getAsString();
                        } else if (address.has("village")) {
                            city = address.get("village").getAsString();
                        } else if (address.has("suburb")) {
                            city = address.get("suburb").getAsString();
                        } else if (address.has("municipality")) {
                            city = address.get("municipality").getAsString();
                        }
                        
                        // Powiat (admin2)
                        if (address.has("county")) {
                            county = address.get("county").getAsString();
                        } else if (address.has("district")) {
                            county = address.get("district").getAsString();
                        } else if (address.has("city_district")) {
                            county = address.get("city_district").getAsString();
                        }
                        
                        // Województwo/Stan/Region (admin1)
                        if (address.has("state")) {
                            state = address.get("state").getAsString();
                        } else if (address.has("province")) {
                            state = address.get("province").getAsString();
                        } else if (address.has("region")) {
                            state = address.get("region").getAsString();
                        } else if (address.has("state_district")) {
                            state = address.get("state_district").getAsString();
                        }
                        
                        // Kraj
                        if (address.has("country")) {
                            country = address.get("country").getAsString();
                        }
                    }
                    
                    // Jeśli nie znaleziono miasta, użyj domyślnej nazwy
                    if (city.isEmpty()) {
                        city = String.format("Lokalizacja (%.4f, %.4f)", latitude, longitude);
                    }
                    
                    // Zwracamy lokalizację z odpowiednimi danymi
                    return new Location(latitude, longitude, city, county, state, country);
                }
            }
        }
        
        // Jeśli coś poszło nie tak, tworzymy domyślną lokalizację
        return new Location(latitude, longitude, 
                String.format("Lokalizacja (%.4f, %.4f)", latitude, longitude), "", "", "");
    }
    
    // Pobieranie i parsowanie danych lokalizacji
    private List<Location> fetchLocations(String url) throws IOException, ParseException {
        List<Location> locations = new ArrayList<>();
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    System.out.println("Odpowiedź z API: " + result);
                    locations = parseLocationResponse(result);
                }
            }
        }
        
        return locations;
    }

    // Parsowanie odpowiedzi z API geokodowania
    private List<Location> parseLocationResponse(String jsonResponse) {
        List<Location> locations = new ArrayList<>();
        
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(jsonResponse);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        
        if (jsonObject.has("results")) {
            JsonArray results = jsonObject.getAsJsonArray("results");
            
            for (JsonElement result : results) {
                JsonObject locationObj = result.getAsJsonObject();
                
                double latitude = locationObj.get("latitude").getAsDouble();
                double longitude = locationObj.get("longitude").getAsDouble();
                String name = locationObj.get("name").getAsString(); // admin3 (miasto)
                
                String county = ""; // admin2 (powiat)
                if (locationObj.has("admin2")) {
                    county = locationObj.get("admin2").getAsString();
                }
                
                String region = ""; // admin1 (województwo)
                if (locationObj.has("admin1")) {
                    region = locationObj.get("admin1").getAsString();
                }
                
                String country = ""; // kraj
                if (locationObj.has("country")) {
                    country = locationObj.get("country").getAsString();
                }
                
                Location location = new Location(latitude, longitude, name, county, region, country);
                locations.add(location);
            }
        } else {
            System.out.println("Odpowiedź API nie zawiera wyników. Pełna odpowiedź: " + jsonResponse);
        }
        
        return locations;
    }
} 