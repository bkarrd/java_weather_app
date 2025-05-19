# Aplikacja Pogodowa

Aplikacja desktopowa JavaFX do wyświetlania prognoz pogody i danych historycznych z wykorzystaniem Open-Meteo API.

## Funkcjonalności

- Pobieranie i wizualizacja danych pogodowych z Open-Meteo API
- Dwa tryby pracy:
  - Prognoza pogody (do 16 dni w przód)
  - Dane historyczne (do 30 dni wstecz)
- Wyszukiwanie lokalizacji:
  - Po nazwie miasta
  - Po współrzędnych geograficznych (znajdowanie najbliższej lokalizacji)
- Wizualizacja różnych parametrów pogodowych:
  - Temperatura na wysokości 2m
  - Prędkość wiatru
  - Temperatura gleby
  - Opady
  - Ciśnienie przy powierzchni
- Wykresy:
  - Wiele serii danych na jednym wykresie
  - Osobne wykresy dla wybranych parametrów
- Eksport danych do pliku tekstowego
- Cacheowanie danych w Redis w celu optymalizacji zapytań

## Wymagania

- Java JDK 22
- Maven
- Redis (kontener Docker lub lokalna instancja)

## Uruchomienie

### Uruchomienie Redis w Dockerze

```bash
docker run -d --name weather-redis -p 6379:6379 redis:latest
```

### Kompilacja i uruchomienie aplikacji

```bash
mvn clean javafx:run
```

## Struktura projektu

- `api` - Klasy do komunikacji z API Open-Meteo i geokodowania
- `cache` - Klasy do obsługi cache'a Redis
- `controller` - Kontrolery JavaFX
- `model` - Klasy modelu danych
- `util` - Klasy narzędziowe
- `view` - Klasy widoków

## Technologie

- JavaFX - Interfejs użytkownika
- GSON - Parsowanie JSON
- Redis (Jedis) - Cache danych
- Apache HttpClient - Komunikacja HTTP
- Maven - Zarządzanie zależnościami 