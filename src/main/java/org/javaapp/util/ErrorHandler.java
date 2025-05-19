package org.javaapp.util;

import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Klasa narzędziowa do obsługi błędów w aplikacji.
 * Dostarcza metody pomocnicze do obsługi wyjątków i rejestrowania błędów.
 */
public class ErrorHandler {
    
    /**
     * Wykonuje operację z obsługą wyjątków i zwraca wartość domyślną w przypadku błędu.
     * 
     * @param operation Operacja do wykonania
     * @param defaultValue Wartość domyślna zwracana w przypadku błędu
     * @param errorMessage Komunikat błędu do wyświetlenia
     * @param <T> Typ zwracanej wartości
     * @return Wynik operacji lub wartość domyślna
     */
    public static <T> T handleOperation(Supplier<T> operation, T defaultValue, String errorMessage) {
        try {
            return operation.get();
        } catch (Exception e) {
            logError(errorMessage, e);
            return defaultValue;
        }
    }
    
    /**
     * Wykonuje operację bez zwracania wartości, z obsługą wyjątków.
     * 
     * @param operation Operacja do wykonania
     * @param errorMessage Komunikat błędu do wyświetlenia
     */
    public static void executeOperation(Runnable operation, String errorMessage) {
        try {
            operation.run();
        } catch (Exception e) {
            logError(errorMessage, e);
        }
    }
    
    /**
     * Wykonuje operację, która może rzucić IOException lub ParseException, często używane
     * w API aplikacji.
     * 
     * @param operation Operacja do wykonania
     * @param defaultValue Wartość domyślna zwracana w przypadku błędu
     * @param errorMessage Komunikat błędu do wyświetlenia
     * @param <T> Typ zwracanej wartości
     * @return Wynik operacji lub wartość domyślna
     */
    public static <T> T handleApiOperation(ApiOperation<T> operation, T defaultValue, String errorMessage) {
        try {
            return operation.execute();
        } catch (IOException | ParseException e) {
            logError(errorMessage, e);
            return defaultValue;
        } catch (Exception e) {
            logError("Nieoczekiwany błąd: " + errorMessage, e);
            return defaultValue;
        }
    }
    
    /**
     * Rejestruje błąd w konsoli (w przyszłości można rozszerzyć o inne metody logowania).
     * 
     * @param message Komunikat błędu
     * @param e Wyjątek
     */
    public static void logError(String message, Throwable e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }
    
    /**
     * Interfejs funkcyjny dla operacji, które mogą rzucić IOException lub ParseException.
     * 
     * @param <T> Typ zwracanej wartości
     */
    @FunctionalInterface
    public interface ApiOperation<T> {
        T execute() throws IOException, ParseException;
    }
} 