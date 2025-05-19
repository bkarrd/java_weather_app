package org.javaapp.util;

import org.apache.hc.core5.http.ParseException;
import org.javaapp.api.GeocodingApi;
import org.javaapp.api.OpenMeteoApi;
import org.javaapp.cache.RedisCache;
import org.javaapp.model.Location;
import org.javaapp.model.WeatherData;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class WeatherService {
    private final OpenMeteoApi weatherApi;
    private final GeocodingApi geocodingApi;
    private final RedisCache redisCache;
    
    public WeatherService() {
        this.weatherApi = new OpenMeteoApi();
        this.geocodingApi = new GeocodingApi();
        this.redisCache = new RedisCache();
    }
    
    // Pobieranie prognozy pogody
    public WeatherData getForecastData(Location location, int forecastDays) {
        String cacheKey = redisCache.generateWeatherCacheKey(
                location.getLatitude(), location.getLongitude(), "forecast", forecastDays);
        
        System.out.println("[DEBUG] Próba pobrania prognozy pogody dla lokalizacji: " + location.getName());
        System.out.println("[DEBUG] Klucz cache: " + cacheKey);
        
        // Próba pobrania danych z cache'a
        WeatherData cachedData = redisCache.getWeatherData(cacheKey);
        if (cachedData != null) {
            System.out.println("[DEBUG] Znaleziono dane prognozy w cache dla: " + location.getName() + ", dni: " + forecastDays);
            return cachedData;
        }
        
        // Jeśli nie ma w cache'u, pobierz z API
        System.out.println("[DEBUG] Brak danych w cache, pobieranie z API dla: " + location.getName() + ", dni: " + forecastDays);
        return ErrorHandler.handleApiOperation(
            () -> {
                System.out.println("[DEBUG] Wywołanie API prognozy pogody: " + location.getLatitude() + "," + location.getLongitude());
                WeatherData forecastData = weatherApi.getForecast(location, forecastDays);
                System.out.println("[DEBUG] Otrzymano dane z API, zapisywanie do cache");
                // Zapisz do cache'a
                redisCache.saveWeatherData(cacheKey, forecastData);
                return forecastData;
            },
            createEmptyWeatherData(location, "forecast"),
            "Nie udało się pobrać prognozy pogody"
        );
    }
    
    // Pobieranie danych historycznych
    public WeatherData getHistoricalData(Location location, int pastDays) {
        String cacheKey = redisCache.generateWeatherCacheKey(
                location.getLatitude(), location.getLongitude(), "historical", pastDays);
        
        System.out.println("[DEBUG] Próba pobrania danych historycznych dla lokalizacji: " + location.getName());
        System.out.println("[DEBUG] Klucz cache: " + cacheKey);
        
        // Próba pobrania danych z cache'a
        WeatherData cachedData = redisCache.getWeatherData(cacheKey);
        if (cachedData != null) {
            System.out.println("[DEBUG] Znaleziono dane historyczne w cache dla: " + location.getName() + ", dni wstecz: " + pastDays);
            return cachedData;
        }
        
        // Jeśli nie ma w cache'u, pobierz z API
        System.out.println("[DEBUG] Brak danych historycznych w cache, pobieranie z API dla: " + location.getName() + ", dni wstecz: " + pastDays);
        return ErrorHandler.handleApiOperation(
            () -> {
                System.out.println("[DEBUG] Wywołanie API danych historycznych: " + location.getLatitude() + "," + location.getLongitude());
                WeatherData historicalData = weatherApi.getHistoricalData(location, pastDays);
                System.out.println("[DEBUG] Otrzymano dane historyczne z API, zapisywanie do cache");
                // Zapisz do cache'a
                redisCache.saveWeatherData(cacheKey, historicalData);
                return historicalData;
            },
            createEmptyWeatherData(location, "historical"),
            "Nie udało się pobrać danych historycznych"
        );
    }
    
    // Wyszukiwanie lokalizacji po nazwie
    public List<Location> searchLocationsByName(String locationName) {
        return ErrorHandler.handleApiOperation(
            () -> geocodingApi.searchLocationsByName(locationName),
            Collections.emptyList(),
            "Nie udało się wyszukać lokalizacji"
        );
    }
    
    // Wyszukiwanie najbliższej lokalizacji na podstawie współrzędnych
    public Location findNearestLocation(double latitude, double longitude) {
        return ErrorHandler.handleApiOperation(
            () -> geocodingApi.findNearestLocation(latitude, longitude),
            null,
            "Nie udało się znaleźć najbliższej lokalizacji"
        );
    }
    
    // Tworzenie pustych danych pogodowych
    private WeatherData createEmptyWeatherData(Location location, String dataType) {
        return new WeatherData(location, Collections.emptyList(), 0, "UTC", dataType);
    }
    
    // Metoda generująca klucz cache dla kontrolera
    public String generateCacheKey(Location location, String dataType, int days) {
        return redisCache.generateWeatherCacheKey(location.getLatitude(), location.getLongitude(), dataType, days);
    }
    
    // Metoda pobierająca dane z cache dla kontrolera
    public WeatherData getFromCache(String cacheKey) {
        return redisCache.getWeatherData(cacheKey);
    }
    
    // Zamknięcie zasobów
    public void close() {
        ErrorHandler.executeOperation(
            redisCache::close,
            "Błąd podczas zamykania połączenia Redis"
        );
    }
} 