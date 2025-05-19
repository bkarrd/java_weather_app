package org.javaapp.cache;

import org.javaapp.model.WeatherData;
import org.javaapp.util.JsonUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class RedisCache {
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final int FORECAST_EXPIRY_HOURS = 1; // Dane prognozy ważne przez 1 godzinę
    private static final int HISTORICAL_EXPIRY_DAYS = 7; // Dane historyczne ważne przez 7 dni
    
    private final JedisPool jedisPool;
    private boolean redisAvailable = false;
    
    // Cache w pamięci do użycia, gdy Redis nie jest dostępny
    private final Map<String, String> inMemoryCache = new HashMap<>();
    
    public RedisCache() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMaxWait(Duration.ofSeconds(10));
        
        this.jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
        
        // Sprawdź połączenie z Redis przy inicjalizacji
        checkRedisConnection();
    }
    
    private void checkRedisConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
            redisAvailable = true;
            System.out.println("Połączono z Redis pomyślnie");
        } catch (JedisConnectionException e) {
            redisAvailable = false;
            System.out.println("Redis niedostępny: " + e.getMessage() + ". Używanie cache w pamięci.");
        }
    }
    
    // Zapisywanie danych pogodowych do cache'a
    public void saveWeatherData(String cacheKey, WeatherData weatherData) {
        System.out.println("[DEBUG CACHE] Zapisywanie danych do cache dla klucza: " + cacheKey);
        String jsonData = JsonUtils.toJson(weatherData);
        
        if (redisAvailable) {
            System.out.println("[DEBUG CACHE] Redis dostępny, próba zapisu do Redis");
            try (Jedis jedis = jedisPool.getResource()) {
                int expirySeconds = calculateExpiryTime(weatherData.getDataType());
                jedis.setex(cacheKey, expirySeconds, jsonData);
                System.out.println("[DEBUG CACHE] Dane zapisane w Redis z czasem wygaśnięcia: " + expirySeconds + " sekund");
            } catch (JedisConnectionException e) {
                System.out.println("[DEBUG CACHE] Redis niedostępny podczas zapisu: " + e.getMessage() + ". Używanie cache w pamięci.");
                redisAvailable = false;
                // Zapisz do cache w pamięci jako fallback
                inMemoryCache.put(cacheKey, jsonData);
                System.out.println("[DEBUG CACHE] Dane zapisane w cache pamięciowym jako fallback");
            }
        } else {
            // Redis niedostępny, używaj cache w pamięci
            System.out.println("[DEBUG CACHE] Redis niedostępny, zapisywanie do cache pamięciowego");
            inMemoryCache.put(cacheKey, jsonData);
            System.out.println("[DEBUG CACHE] Dane zapisane w cache pamięciowym");
        }
    }
    
    // Pobieranie danych pogodowych z cache'a
    public WeatherData getWeatherData(String cacheKey) {
        System.out.println("[DEBUG CACHE] Sprawdzanie dostępności danych w cache dla klucza: " + cacheKey);
        
        if (redisAvailable) {
            System.out.println("[DEBUG CACHE] Redis dostępny, próba odczytu z Redis");
            try (Jedis jedis = jedisPool.getResource()) {
                String jsonData = jedis.get(cacheKey);
                if (jsonData != null) {
                    System.out.println("[DEBUG CACHE] Znaleziono dane w Redis dla klucza: " + cacheKey);
                    WeatherData data = JsonUtils.fromJson(jsonData, WeatherData.class);
                    System.out.println("[DEBUG CACHE] Pomyślnie zdekodowano dane z JSON");
                    return data;
                } else {
                    System.out.println("[DEBUG CACHE] Brak danych w Redis dla klucza: " + cacheKey);
                }
            } catch (JedisConnectionException e) {
                System.out.println("[DEBUG CACHE] Redis niedostępny podczas odczytu: " + e.getMessage() + ". Używanie cache w pamięci.");
                redisAvailable = false;
                // Spróbuj pobrać z cache w pamięci jako fallback
                String jsonData = inMemoryCache.get(cacheKey);
                if (jsonData != null) {
                    System.out.println("[DEBUG CACHE] Znaleziono dane w cache pamięciowym dla klucza: " + cacheKey);
                    return JsonUtils.fromJson(jsonData, WeatherData.class);
                }
            }
        } else {
            // Redis niedostępny, używaj cache w pamięci
            System.out.println("[DEBUG CACHE] Redis niedostępny, sprawdzanie cache pamięciowego dla klucza: " + cacheKey);
            String jsonData = inMemoryCache.get(cacheKey);
            if (jsonData != null) {
                System.out.println("[DEBUG CACHE] Znaleziono dane w cache pamięciowym dla klucza: " + cacheKey);
                return JsonUtils.fromJson(jsonData, WeatherData.class);
            } else {
                System.out.println("[DEBUG CACHE] Brak danych w cache pamięciowym dla klucza: " + cacheKey);
            }
        }
        
        System.out.println("[DEBUG CACHE] Brak danych w cache dla klucza: " + cacheKey);
        return null;
    }
    
    // Obliczanie czasu wygaśnięcia danych w cache'u w zależności od typu danych
    private int calculateExpiryTime(String dataType) {
        if ("forecast".equals(dataType)) {
            return FORECAST_EXPIRY_HOURS * 3600; // w sekundach
        } else if ("historical".equals(dataType)) {
            return HISTORICAL_EXPIRY_DAYS * 24 * 3600; // w sekundach
        } else {
            return 3600; // domyślnie 1 godzina
        }
    }
    
    // Generowanie klucza cache'a dla danych pogodowych
    public String generateWeatherCacheKey(double latitude, double longitude, String dataType, int days) {
        return String.format("weather:%s:%.4f:%.4f:%d", dataType, latitude, longitude, days);
    }
    
    // Zamknięcie puli połączeń
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
} 