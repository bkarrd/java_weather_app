package org.javaapp.view;

import javafx.scene.paint.Color;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Klasa zawierająca stałe używane w wykresach
 */
public class ChartConstants {
    
    // Format czasu dla osi X
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    
    // Kolory dla różnych typów danych pogodowych - używamy kodów HEX dla spójności
    private static final Map<String, String> VARIABLE_COLORS = new HashMap<>();
    static {
        VARIABLE_COLORS.put("temperature2m", "#FF0000"); // czerwony
        VARIABLE_COLORS.put("soilTemperature", "#0000FF"); // niebieski
        VARIABLE_COLORS.put("windSpeed", "#800080"); // fioletowy
        VARIABLE_COLORS.put("rain", "#0000A0"); // ciemnoniebieski
        VARIABLE_COLORS.put("surfacePressure", "#008000"); // zielony
    }
    
    /**
     * Zwraca kolor przypisany do danej zmiennej jako String
     */
    public static String getColorForVariable(String variable) {
        return VARIABLE_COLORS.getOrDefault(variable, "#000000");
    }
    
    /**
     * Zwraca kolor przypisany do danej zmiennej jako obiekt Color
     */
    public static Color getColorObjectForVariable(String variable) {
        String colorHex = VARIABLE_COLORS.getOrDefault(variable, "#000000");
        return Color.web(colorHex);
    }
    
    /**
     * Zwraca czytelną nazwę zmiennej
     */
    public static String getVariableDisplayName(String variable) {
        return switch (variable) {
            case "temperature2m" -> "Temperatura (2m) [°C]";
            case "windSpeed" -> "Prędkość wiatru [km/h]";
            case "soilTemperature" -> "Temperatura gleby [°C]";
            case "rain" -> "Opady [mm]";
            case "surfacePressure" -> "Ciśnienie [hPa]";
            default -> variable;
        };
    }
    
    /**
     * Sprawdza czy zmienna wymaga skalowania
     */
    public static boolean shouldScaleVariable(String variable) {
        return variable.equals("surfacePressure") || variable.equals("rain");
    }
    
    /**
     * Skaluje wartość do wyświetlania
     */
    public static double scaleValueForDisplay(double value, String variable) {
        if (variable.equals("surfacePressure")) {
            return (value - 1000.0) / 10.0;
        } else if (variable.equals("rain")) {
            return value * 10.0;
        }
        return value;
    }
    
    /**
     * Odwraca skalowanie wartości
     */
    public static double unscaleValueForDisplay(double scaledValue, String variable) {
        if (variable.equals("surfacePressure")) {
            return scaledValue * 10.0 + 1000.0;
        } else if (variable.equals("rain")) {
            return scaledValue / 10.0;
        }
        return scaledValue;
    }
} 