package org.javaapp.api;

import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.javaapp.model.Location;
import org.javaapp.model.WeatherData;
import org.javaapp.model.WeatherData.TimeSeriesEntry;
import org.javaapp.util.JsonUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OpenMeteoApi {
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    // Metoda do pobierania prognozy pogody
    public WeatherData getForecast(Location location, int forecastDays) throws IOException, ParseException {
        String url = buildForecastUrl(location, forecastDays);
        return fetchAndParseWeatherData(url, location, "forecast");
    }

    // Metoda do pobierania danych historycznych
    public WeatherData getHistoricalData(Location location, int pastDays) throws IOException, ParseException {
        String url = buildHistoricalUrl(location, pastDays);
        return fetchAndParseWeatherData(url, location, "historical");
    }

    // Budowanie URL dla prognozy pogody
    private String buildForecastUrl(Location location, int forecastDays) {
        return BASE_URL +
                "?latitude=" + location.getLatitude() +
                "&longitude=" + location.getLongitude() +
                "&hourly=temperature_2m,windspeed_10m,soil_temperature_0cm,rain,surface_pressure" +
                "&forecast_days=" + forecastDays +
                "&timezone=auto";
    }

    // Budowanie URL dla danych historycznych
    private String buildHistoricalUrl(Location location, int pastDays) {
        return BASE_URL +
                "?latitude=" + location.getLatitude() +
                "&longitude=" + location.getLongitude() +
                "&hourly=temperature_2m,windspeed_10m,soil_temperature_0cm,rain,surface_pressure" +
                "&past_days=" + pastDays +
                "&timezone=auto";
    }

    // Pobieranie i parsowanie danych pogodowych
    private WeatherData fetchAndParseWeatherData(String url, Location location, String dataType) throws IOException, ParseException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    return parseWeatherData(result, location, dataType);
                }
            }
        }
        throw new IOException("Nie udało się pobrać danych pogodowych");
    }

    // Parsowanie odpowiedzi JSON z API
    private WeatherData parseWeatherData(String jsonResponse, Location location, String dataType) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonResponse);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Pobranie podstawowych informacji
            double elevation = jsonObject.get("elevation").getAsDouble();
            String timezone = jsonObject.get("timezone").getAsString();
            
            // Dane godzinowe
            JsonObject hourly = jsonObject.getAsJsonObject("hourly");
            JsonArray timeArray = hourly.getAsJsonArray("time");
            JsonArray temperature2mArray = hourly.getAsJsonArray("temperature_2m");
            JsonArray windSpeedArray = hourly.getAsJsonArray("windspeed_10m");
            JsonArray soilTempArray = hourly.getAsJsonArray("soil_temperature_0cm");
            JsonArray rainArray = hourly.getAsJsonArray("rain");
            JsonArray surfacePressureArray = hourly.getAsJsonArray("surface_pressure");

            // Utworzenie serii czasowej
            List<TimeSeriesEntry> timeSeries = new ArrayList<>();
            
            // Dla każdego punktu czasowego tworzymy wpis w serii
            for (int i = 0; i < timeArray.size(); i++) {
                TimeSeriesEntry entry = new TimeSeriesEntry();
                
                // Czas
                String timeStr = timeArray.get(i).getAsString();
                entry.setTime(LocalDateTime.parse(timeStr, formatter));
                
                // Dane pogodowe - używamy wspólnej metody dla wszystkich typów danych
                updateEntryWithData(entry, i, temperature2mArray, windSpeedArray, soilTempArray, 
                                   rainArray, surfacePressureArray);
                
                timeSeries.add(entry);
            }

            return new WeatherData(location, timeSeries, elevation, timezone, dataType);
            
        } catch (Exception e) {
            System.err.println("Błąd podczas parsowania odpowiedzi API: " + e.getMessage());
            // Zwracamy puste dane w przypadku błędu
            return new WeatherData(location, new ArrayList<>(), 0, "UTC", dataType);
        }
    }

    // Pomocnicza metoda do aktualizacji wpisu w serii czasowej
    private void updateEntryWithData(TimeSeriesEntry entry, int index,
                                    JsonArray tempArray, JsonArray windArray, 
                                    JsonArray soilTempArray, JsonArray rainArray, 
                                    JsonArray pressureArray) {
        entry.setTemperature2m(getDoubleOrNull(tempArray, index));
        entry.setWindSpeed(getDoubleOrNull(windArray, index));
        entry.setSoilTemperature(getDoubleOrNull(soilTempArray, index));
        entry.setRain(getDoubleOrNull(rainArray, index));
        entry.setSurfacePressure(getDoubleOrNull(pressureArray, index));
    }

    // Pomocnicza metoda do bezpiecznego pobierania wartości Double z JsonArray
    private Double getDoubleOrNull(JsonArray array, int index) {
        JsonElement element = array.get(index);
        if (element.isJsonNull()) {
            return null;
        }
        return element.getAsDouble();
    }
} 