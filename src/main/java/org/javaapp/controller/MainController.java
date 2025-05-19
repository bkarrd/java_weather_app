package org.javaapp.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.javaapp.model.Location;
import org.javaapp.model.WeatherData;
import org.javaapp.util.WeatherService;
import org.javaapp.view.ChartWindow;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    // Komponenty UI do trybu
    @FXML private RadioButton forecastRadio;
    @FXML private ToggleGroup dataTypeToggle;
    
    // Komponenty UI do lokalizacji
    @FXML private RadioButton cityNameRadio;
    @FXML private ToggleGroup locationMethodToggle;
    @FXML private HBox cityNameInputBox;
    @FXML private HBox coordinatesInputBox;
    @FXML private TextField cityNameField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Button searchCityButton;
    @FXML private Button findLocationButton;
    @FXML private ComboBox<Location> locationComboBox;
    
    // Komponenty UI do ustawień prognozy
    @FXML private VBox forecastSettingsBox;
    @FXML private ComboBox<Integer> forecastDaysComboBox;
    
    // Komponenty UI do ustawień danych historycznych
    @FXML private VBox historicalSettingsBox;
    @FXML private ComboBox<Integer> pastDaysComboBox;
    
    // Komponenty UI do wizualizacji
    @FXML private CheckBox temperature2mCheck;
    @FXML private CheckBox windSpeedCheck;
    @FXML private CheckBox soilTemperatureCheck;
    @FXML private CheckBox rainCheck;
    @FXML private CheckBox surfacePressureCheck;
    
    @FXML private Button selectAllButton;
    
    // Przyciski akcji
    @FXML private Button showChartButton;
    
    // Status
    @FXML private Label statusLabel;
    
    // Serwis pogodowy
    private final WeatherService weatherService = new WeatherService();
    
    // Dane
    private WeatherData currentWeatherData;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Konfiguracja początkowa
        setupDataTypes();
        setupLocationMethods();
        
        // Przypisywanie akcji do przycisków
        searchCityButton.setOnAction(e -> searchCity());
        findLocationButton.setOnAction(e -> findLocation());
        showChartButton.setOnAction(e -> showChart());
        selectAllButton.setOnAction(e -> selectAllDataSeries());
        
        // Konfiguracja nasłuchiwacza dla checkboxów danych
        setupDataSeriesCheckboxes();
        
        // Walidacja pól współrzędnych
        setupCoordinateValidation(latitudeField);
        setupCoordinateValidation(longitudeField);
    }
    
    // Metoda do konfiguracji walidacji pól współrzędnych (tylko cyfry i kropka)
    private void setupCoordinateValidation(TextField field) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            
            // Usuń wszystkie znaki, które nie są cyframi, kropką lub minusem na początku
            if (!newValue.matches("-?\\d*\\.?\\d*")) {
                // Jeśli nowy znak jest nieprawidłowy, wróć do poprzedniej wartości
                field.setText(oldValue);
            }
            
            // Sprawdź czy minus jest tylko na początku
            if (newValue.length() > 1 && newValue.lastIndexOf('-') > 0) {
                field.setText(oldValue);
            }
            
            // Sprawdź czy jest tylko jedna kropka
            if (newValue.length() - newValue.replace(".", "").length() > 1) {
                field.setText(oldValue);
            }
        });
    }
    
    // Konfiguracja typów danych (prognoza/historia)
    private void setupDataTypes() {
        // Inicjalizacja comboboxów
        forecastDaysComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        forecastDaysComboBox.setValue(7);
        
        pastDaysComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 10, 14, 30));
        pastDaysComboBox.setValue(7);
        
        // Słuchacze dla radio buttonsów
        dataTypeToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateDataTypeUI();
            // Resetuj dane pogodowe przy zmianie trybu (prognoza/historia)
            if (oldVal != null && newVal != null && oldVal != newVal) {
                System.out.println("[DEBUG] Zmiana trybu prognozy - resetowanie danych pogodowych");
                currentWeatherData = null;
            }
        });
        
        // Słuchacze dla comboboxów z liczbą dni
        forecastDaysComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && newVal != null && !oldVal.equals(newVal)) {
                System.out.println("[DEBUG] Zmiana liczby dni prognozy z " + oldVal + " na " + newVal + " - resetowanie danych pogodowych");
                currentWeatherData = null;
            }
        });
        
        pastDaysComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && newVal != null && !oldVal.equals(newVal)) {
                System.out.println("[DEBUG] Zmiana liczby dni historycznych z " + oldVal + " na " + newVal + " - resetowanie danych pogodowych");
                currentWeatherData = null;
            }
        });
        
        // Domyślne ustawienia
        updateDataTypeUI();
    }
    
    // Konfiguracja metod lokalizacji
    private void setupLocationMethods() {
        // Słuchacze dla radio buttonsów
        locationMethodToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateLocationMethodUI());
        
        // Dodaj nasłuchiwacz dla ComboBoxa z lokalizacją
        locationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Resetuj dane pogodowe przy zmianie lokalizacji
            if (newVal != null && oldVal != null && !newVal.equals(oldVal)) {
                System.out.println("[DEBUG] Zmiana lokalizacji z " + oldVal.getName() + " na " + newVal.getName() + " - resetowanie danych pogodowych");
                currentWeatherData = null;
            }
            updateChartButtonState();
        });
        
        // Domyślne ustawienia
        updateLocationMethodUI();
    }
    
    // Aktualizacja interfejsu w zależności od wybranego trybu danych
    private void updateDataTypeUI() {
        boolean isForecast = forecastRadio.isSelected();
        forecastSettingsBox.setVisible(isForecast);
        forecastSettingsBox.setManaged(isForecast);
        historicalSettingsBox.setVisible(!isForecast);
        historicalSettingsBox.setManaged(!isForecast);
    }
    
    // Aktualizacja interfejsu w zależności od wybranej metody wprowadzania lokalizacji
    private void updateLocationMethodUI() {
        boolean isCityName = cityNameRadio.isSelected();
        cityNameInputBox.setVisible(isCityName);
        cityNameInputBox.setManaged(isCityName);
        coordinatesInputBox.setVisible(!isCityName);
        coordinatesInputBox.setManaged(!isCityName);
        
        // Dodaj domyślne współrzędne Warszawy, jeśli pola są puste
        if (!isCityName) {
            if (latitudeField.getText().trim().isEmpty()) {
                latitudeField.setText("52.2297");
            }
            if (longitudeField.getText().trim().isEmpty()) {
                longitudeField.setText("21.0122");
            }
        }
    }
    
    // Wyszukiwanie lokalizacji po nazwie miasta
    private void searchCity() {
        String cityName = cityNameField.getText().trim();
        if (cityName.isEmpty()) {
            showStatus("Wprowadź nazwę miasta", true);
            return;
        }
        
                    showStatus("Wyszukiwanie lokalizacji...", false);
        
        Task<List<Location>> task = new Task<>() {
            @Override
            protected List<Location> call() throws Exception {
                return weatherService.searchLocationsByName(cityName);
            }
            
            @Override
            protected void succeeded() {
                List<Location> locations = getValue();
                Platform.runLater(() -> {
                    if (locations.isEmpty()) {
                        showStatus("Nie znaleziono lokalizacji", true);
                    } else {
                        locationComboBox.setItems(FXCollections.observableArrayList(locations));
                        locationComboBox.setValue(locations.get(0));
                        // Jawne resetowanie danych pogodowych po wyszukaniu nowej lokalizacji
                        currentWeatherData = null;
                        System.out.println("[DEBUG] Wyszukano nową lokalizację - resetowanie danych pogodowych");
                        showStatus("Znaleziono " + locations.size() + " lokalizacji", false);
                        updateChartButtonState();
                    }
                });
            }
            
            @Override
            protected void failed() {
                showStatus("Błąd podczas wyszukiwania: " + getException().getMessage(), true);
            }
        };
        
        new Thread(task).start();
    }
    
    // Wyszukiwanie lokalizacji na podstawie współrzędnych
    private void findLocation() {
        try {
            double latitude = Double.parseDouble(latitudeField.getText().trim());
            double longitude = Double.parseDouble(longitudeField.getText().trim());
            
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                showStatus("Nieprawidłowe współrzędne geograficzne", true);
                return;
            }
            
            showStatus("Wyszukiwanie najbliższej lokalizacji...", false);
            
            // Dodajemy logowanie dla polskich lokalizacji (52°N 20°E jest w centrum Polski)
            final boolean isPotentiallyPoland = latitude >= 49.0 && latitude <= 55.0 && 
                                         longitude >= 14.0 && longitude <= 24.0;
            if (isPotentiallyPoland) {
                System.out.println("Wykryto potencjalne współrzędne Polski: lat=" + latitude + ", lon=" + longitude);
            }
            
            Task<Location> task = new Task<>() {
                @Override
                protected Location call() throws Exception {
                    return weatherService.findNearestLocation(latitude, longitude);
                }
                
                @Override
                protected void succeeded() {
                    Location location = getValue();
                    Platform.runLater(() -> {
                        if (location == null) {
                            showStatus("Nie znaleziono lokalizacji", true);
                        } else {
                            // Dodajemy logowanie dla polskich lokalizacji
                            if (isPotentiallyPoland || "Poland".equals(location.getCountry())) {
                                System.out.println("Znaleziona polska lokalizacja: " + location);
                                System.out.println("Nazwa: " + location.getName());
                                System.out.println("Kraj: " + location.getCountry());
                            }
                            
                            locationComboBox.setItems(FXCollections.observableArrayList(location));
                            locationComboBox.setValue(location);
                            // Jawne resetowanie danych pogodowych po znalezieniu nowej lokalizacji
                            currentWeatherData = null;
                            System.out.println("[DEBUG] Znaleziono nową lokalizację po współrzędnych - resetowanie danych pogodowych");
                            showStatus("Znaleziono lokalizację: " + location.getName(), false);
                            updateChartButtonState();
                        }
                    });
                }
                
                @Override
                protected void failed() {
                    System.out.println("Błąd: " + getException());
                    getException().printStackTrace();
                    showStatus("Błąd podczas wyszukiwania: " + getException().getMessage(), true);
                }
            };
            
            new Thread(task).start();
            
        } catch (NumberFormatException e) {
            showStatus("Wprowadź poprawne współrzędne geograficzne", true);
        }
    }
    
    // Wyświetlanie wykresu
    private void showChart() {
        Location location = locationComboBox.getValue();
        if (location == null) {
            showStatus("Wybierz lokalizację", true);
            return;
        }
        
        List<String> selectedVariables = getSelectedVariables();
        if (selectedVariables.isEmpty()) {
            showStatus("Wybierz co najmniej jedną zmienną do wizualizacji", true);
            return;
        }
        
        // Sprawdzenie, czy dane są aktualne dla bieżącej lokalizacji
        if (currentWeatherData != null && !location.equals(currentWeatherData.getLocation())) {
            System.out.println("[DEBUG] Wykryto niezgodność lokalizacji danych i UI - resetowanie danych");
            currentWeatherData = null;
        }
        
        // Automatyczne pobieranie danych, jeśli nie są jeszcze dostępne
        if (currentWeatherData == null) {
            showStatus("Pobieranie danych pogodowych...", false);
            showChartButton.setDisable(true);
            
            Task<WeatherData> task = new Task<>() {
                private boolean isFromCache = false;
                
                @Override
                protected WeatherData call() throws Exception {
                    if (forecastRadio.isSelected()) {
                        int days = forecastDaysComboBox.getValue();
                        // Sprawdzamy, czy dane są w cache
                        String cacheKey = weatherService.generateCacheKey(location, "forecast", days);
                        WeatherData cachedData = weatherService.getFromCache(cacheKey);
                        if (cachedData != null) {
                            isFromCache = true;
                            return cachedData;
                        }
                        return weatherService.getForecastData(location, days);
                    } else {
                        int days = pastDaysComboBox.getValue();
                        // Sprawdzamy, czy dane są w cache
                        String cacheKey = weatherService.generateCacheKey(location, "historical", days);
                        WeatherData cachedData = weatherService.getFromCache(cacheKey);
                        if (cachedData != null) {
                            isFromCache = true;
                            return cachedData;
                        }
                        return weatherService.getHistoricalData(location, days);
                    }
                }
                
                @Override
                protected void succeeded() {
                    currentWeatherData = getValue();
                    Platform.runLater(() -> {
                        if (isFromCache) {
                            showStatus("Dane załadowane z pamięci podręcznej", false);
                        } else {
                            showStatus("Dane pobrane z API", false);
                        }
                        showChartButton.setDisable(false);
                        
                        // Teraz możemy wyświetlić wykres
                        ChartWindow chartWindow = new ChartWindow(currentWeatherData, selectedVariables);
                        chartWindow.show();
                    });
                }
                
                @Override
                protected void failed() {
                    showStatus("Błąd podczas pobierania danych: " + getException().getMessage(), true);
                    showChartButton.setDisable(false);
                }
            };
            
            new Thread(task).start();
        } else {
            // Dane już są pobrane - wyświetl wykres
            ChartWindow chartWindow = new ChartWindow(currentWeatherData, selectedVariables);
            chartWindow.show();
        }
    }
    
    // Wyświetlanie statusu
    private void showStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: #666666;");
        });
    }
    
    // Pobieranie wybranych zmiennych do wizualizacji
    private List<String> getSelectedVariables() {
        List<String> variables = new ArrayList<>();
        
        if (temperature2mCheck.isSelected()) {
            variables.add("temperature2m");
        }
        if (windSpeedCheck.isSelected()) {
            variables.add("windSpeed");
        }
        if (soilTemperatureCheck.isSelected()) {
            variables.add("soilTemperature");
        }
        if (rainCheck.isSelected()) {
            variables.add("rain");
        }
        if (surfacePressureCheck.isSelected()) {
            variables.add("surfacePressure");
        }
        
        return variables;
    }
    
    // Zaznacza wszystkie checkboxy danych do wizualizacji
    private void selectAllDataSeries() {
        temperature2mCheck.setSelected(true);
        windSpeedCheck.setSelected(true);
        soilTemperatureCheck.setSelected(true);
        rainCheck.setSelected(true);
        surfacePressureCheck.setSelected(true);
        
        // Aktualizuj stan przycisku
        updateChartButtonState();
    }
    
    // Konfiguracja nasłuchiwaczy dla checkboxów danych do wizualizacji
    private void setupDataSeriesCheckboxes() {
        // Lista checkboxów
        List<CheckBox> checkBoxes = Arrays.asList(
            temperature2mCheck, windSpeedCheck, soilTemperatureCheck, 
            rainCheck, surfacePressureCheck
        );
        
        // Dodaj nasłuchiwacz do każdego checkboxa
        for (CheckBox checkBox : checkBoxes) {
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateChartButtonState());
        }
        
        // Początkowa aktualizacja stanu przycisku
        updateChartButtonState();
    }
    
    // Aktualizacja stanu przycisku Pokaż wykres
    private void updateChartButtonState() {
        List<String> selected = getSelectedVariables();
        showChartButton.setDisable(selected.isEmpty() || locationComboBox.getValue() == null);
    }
} 