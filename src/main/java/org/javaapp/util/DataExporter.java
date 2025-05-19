package org.javaapp.util;

import org.javaapp.model.WeatherData;
import org.javaapp.model.WeatherData.TimeSeriesEntry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DataExporter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Eksport danych do pliku tekstowego
    public static void exportToFile(File file, WeatherData weatherData, List<String> selectedVariables) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Nagłówek pliku
            writer.write("# Dane pogodowe dla lokalizacji: " + weatherData.getLocation().getName() + 
                    ", " + weatherData.getLocation().getCountry());
            writer.newLine();
            writer.write("# Współrzędne: " + weatherData.getLocation().getLatitude() + 
                    ", " + weatherData.getLocation().getLongitude());
            writer.newLine();
            writer.write("# Strefa czasowa: " + weatherData.getTimezone());
            writer.newLine();
            writer.write("# Typ danych: " + weatherData.getDataType());
            writer.newLine();
            writer.write("# Wysokość nad poziomem morza: " + weatherData.getElevation() + " m");
            writer.newLine();
            writer.newLine();
            
            // Nagłówki kolumn
            StringBuilder header = new StringBuilder("Data i czas");
            for (String variable : selectedVariables) {
                header.append(",").append(getVariableDisplayName(variable));
            }
            writer.write(header.toString());
            writer.newLine();
            
            // Dane dla każdego punktu czasowego
            for (TimeSeriesEntry entry : weatherData.getTimeSeries()) {
                StringBuilder line = new StringBuilder(entry.getTime().format(DATE_FORMATTER));
                
                for (String variable : selectedVariables) {
                    line.append(",").append(getVariableValue(entry, variable));
                }
                
                writer.write(line.toString());
                writer.newLine();
            }
        }
    }
    
    // Pobieranie wartości dla danej zmiennej
    private static String getVariableValue(TimeSeriesEntry entry, String variable) {
        return switch (variable) {
            case "temperature2m" -> entry.getTemperature2m() != null ? entry.getTemperature2m().toString() : "";
            case "windSpeed" -> entry.getWindSpeed() != null ? entry.getWindSpeed().toString() : "";
            case "soilTemperature" -> entry.getSoilTemperature() != null ? entry.getSoilTemperature().toString() : "";
            case "rain" -> entry.getRain() != null ? entry.getRain().toString() : "";
            case "surfacePressure" -> entry.getSurfacePressure() != null ? entry.getSurfacePressure().toString() : "";
            default -> "";
        };
    }
    
    // Pobieranie czytelnej nazwy zmiennej
    private static String getVariableDisplayName(String variable) {
        return switch (variable) {
            case "temperature2m" -> "Temperatura (2m) [°C]";
            case "windSpeed" -> "Prędkość wiatru [km/h]";
            case "soilTemperature" -> "Temperatura gleby [°C]";
            case "rain" -> "Opady [mm]";
            case "surfacePressure" -> "Ciśnienie przy powierzchni [hPa]";
            default -> variable;
        };
    }
} 