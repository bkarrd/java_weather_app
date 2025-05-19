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
    @FXML private Button loadDataButton;
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
        loadDataButton.setOnAction(e -> loadData());
        showChartButton.setOnAction(e -> showChart());
        selectAllButton.setOnAction(e -> selectAllDataSeries());
        
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
        dataTypeToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateDataTypeUI());
        
        // Domyślne ustawienia
        updateDataTypeUI();
    }
    
    // Konfiguracja metod lokalizacji
    private void setupLocationMethods() {
        // Słuchacze dla radio buttonsów
        locationMethodToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateLocationMethodUI());
        
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
        loadDataButton.setDisable(true);
        showChartButton.setDisable(true);
        
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
                        showStatus("Znaleziono " + locations.size() + " lokalizacji", false);
                        loadDataButton.setDisable(false);
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
            loadDataButton.setDisable(true);
            showChartButton.setDisable(true);
            
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
                            showStatus("Znaleziono lokalizację: " + location.getName(), false);
                            loadDataButton.setDisable(false);
                        }
                    });
                }
                
                @Override
                protected void failed() {
                    System.out.println("Błąd: " + getException());
                    getException().printStackTrace();
                    showStatus("Błąd podczas wyszukiwania: " + getException().getMessage(), true);
                    loadDataButton.setDisable(false);
                }
            };
            
            new Thread(task).start();
            
        } catch (NumberFormatException e) {
            showStatus("Wprowadź poprawne współrzędne geograficzne", true);
        }
    }
    
    // Ładowanie danych pogodowych
    private void loadData() {
        Location location = locationComboBox.getValue();
        if (location == null) {
            showStatus("Wybierz lokalizację", true);
            return;
        }
        
        showStatus("Pobieranie danych pogodowych...", false);
        loadDataButton.setDisable(true);
        showChartButton.setDisable(true);
        
        Task<WeatherData> task = new Task<>() {
            @Override
            protected WeatherData call() throws Exception {
                if (forecastRadio.isSelected()) {
                    int days = forecastDaysComboBox.getValue();
                    return weatherService.getForecastData(location, days);
                } else {
                    int days = pastDaysComboBox.getValue();
                    return weatherService.getHistoricalData(location, days);
                }
            }
            
            @Override
            protected void succeeded() {
                currentWeatherData = getValue();
                Platform.runLater(() -> {
                    showStatus("Dane pogodowe załadowane pomyślnie", false);
                    loadDataButton.setDisable(false);
                    showChartButton.setDisable(false);
                });
            }
            
            @Override
            protected void failed() {
                showStatus("Błąd podczas pobierania danych: " + getException().getMessage(), true);
                loadDataButton.setDisable(false);
            }
        };
        
        new Thread(task).start();
    }
    
    // Wyświetlanie wykresu
    private void showChart() {
        if (currentWeatherData == null) {
            showStatus("Brak danych do wyświetlenia", true);
            return;
        }
        
        List<String> selectedVariables = getSelectedVariables();
        if (selectedVariables.isEmpty()) {
            showStatus("Wybierz co najmniej jedną zmienną do wizualizacji", true);
            return;
        }
        
        // Utwórz okno z wykresem dla wszystkich wybranych zmiennych
        ChartWindow chartWindow = new ChartWindow(currentWeatherData, selectedVariables);
        chartWindow.show();
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
    }
} 