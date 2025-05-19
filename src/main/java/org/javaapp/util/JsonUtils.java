package org.javaapp.util;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Klasa narzędziowa zawierająca wspólne metody do obsługi JSON w aplikacji.
// Dostarcza pojedynczą instancję Gson z adapterem dla LocalDateTime.
public class JsonUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Gson gson = createGson();

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    // Zwraca instancję Gson skonfigurowaną z adapterem dla LocalDateTime.
    // @return Obiekt Gson
    public static Gson getGson() {
        return gson;
    }

    // Serializuje obiekt do formatu JSON.
    // @param object Obiekt do serializacji
    // @return String JSON
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    // Deserializuje JSON do obiektu określonego typu.
    // @param json String JSON
    // @param classOfT Klasa docelowego obiektu
    // @param <T> Typ docelowego obiektu
    // @return Zdeserializowany obiekt
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    // Adapter dla LocalDateTime zapewniający poprawną serializację/deserializację.
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(DATE_FORMATTER.format(value));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateStr = in.nextString();
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        }
    }
} 