package org.javaapp.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.javaapp.model.WeatherData;
import org.javaapp.model.WeatherData.TimeSeriesEntry;
import org.javaapp.util.DataExporter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Klasa odpowiedzialna za wyświetlanie wykresów pogodowych
 */
public class ChartWindow {
    private final WeatherData weatherData;
    private final List<String> availableVariables;
    private final Stage stage;
    private LineChart<String, Number> mainChart;
    private StackPane chartStack;
    private ComboBox<String> variableSelector;
    private String currentVariable;

    /**
     * Tworzy nowe okno wykresu
     */
    public ChartWindow(WeatherData weatherData, List<String> selectedVariables) {
        this.weatherData = weatherData;
        this.availableVariables = new ArrayList<>(selectedVariables);

        if (selectedVariables == null || selectedVariables.isEmpty()) {
            System.err.println("BŁĄD: Brak wybranych zmiennych do wyświetlenia.");
            this.stage = new Stage();
            return;
        }

        // Ustaw pierwszą zmienną jako domyślną
        this.currentVariable = availableVariables.get(0);

        List<TimeSeriesEntry> filteredData = filterDataByType();

        BorderPane rootPane = new BorderPane();
        rootPane.setPadding(new Insets(10));
        chartStack = new StackPane();

        // Utwórz selector zmiennych
        variableSelector = createVariableSelector();
        
        // Utwórz wykres
        createChart(filteredData);

        Button exportButton = new Button("Eksportuj dane");
        exportButton.setOnAction(e -> exportData());
        exportButton.setMaxWidth(150);

        HBox controlsPane = new HBox(10);
        controlsPane.setAlignment(Pos.CENTER);
        controlsPane.getChildren().addAll(new Label("Wybierz dane:"), variableSelector, exportButton);
        controlsPane.setPadding(new Insets(10, 0, 0, 0));

        VBox chartContainer = new VBox(10);
        chartContainer.getChildren().addAll(chartStack, controlsPane);
        VBox.setVgrow(chartStack, Priority.ALWAYS);

        rootPane.setCenter(chartContainer);

        Scene scene = new Scene(rootPane, 1280, 700);

        StringBuilder cssBuilder = new StringBuilder();
        cssBuilder.append(".chart { -fx-padding: 10px; } ")
                .append(".chart-title { -fx-font-size: 16px; -fx-font-weight: bold; } ")
                .append(".chart-legend { -fx-background-color: transparent; -fx-padding: 10px 0 10px 0; -fx-alignment: center; } ")
                .append(".chart-legend-item { -fx-padding: 5px; -fx-font-weight: bold; } ")
                .append(".chart-plot-background { -fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1px; -fx-padding: 0 60px 0 40px; } ")
                .append(".chart-vertical-grid-lines { -fx-stroke: #eeeeee; } ")
                .append(".chart-horizontal-grid-lines { -fx-stroke: #eeeeee; } ")
                .append(".chart-series-line { -fx-stroke-width: 3px; } ")
                .append(".axis { -fx-tick-labels-gap: 5; -fx-tick-length: 5; } ")
                .append(".chart-content { -fx-padding: 10px 60px 10px 40px; } ") 
                .append(".axis-tick-mark { -fx-stroke: #666666; } ")
                .append(".axis-minor-tick-mark { -fx-stroke: #dddddd; } ");

        for (int idx = 0; idx < 1; idx++) {
            String color = ChartConstants.getColorForVariable(currentVariable);
            cssBuilder.append(".default-color").append(idx).append(".chart-series-line { -fx-stroke: ").append(color).append("; } ")
                    .append(".default-color").append(idx).append(".chart-line-symbol { -fx-background-color: ").append(color).append(", white; } ")
                    .append(".default-color").append(idx).append(".chart-legend-item-symbol { -fx-background-color: ").append(color).append("; } ");
        }
        
        // Dodajemy style dla etykiet osi
        cssBuilder.append(".axis-label { -fx-font-weight: bold; } ");
        cssBuilder.append(".chart-line-symbol { -fx-background-radius: 4px; -fx-padding: 4px; }");
        String css = cssBuilder.toString().replace(" ", "");
        if (!css.isEmpty()) {
            scene.getStylesheets().add("data:text/css," + css);
        }

        this.stage = new Stage();
        stage.setTitle("Wykres pogodowy - " + weatherData.getLocation().getName());
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(600);
    }

    /**
     * Tworzy wybór dostępnych zmiennych
     */
    private ComboBox<String> createVariableSelector() {
        ComboBox<String> selector = new ComboBox<>();
        
        // Przygotowanie listy czytelnych nazw zmiennych
        List<String> displayNames = new ArrayList<>();
        
        for (String variable : availableVariables) {
            displayNames.add(ChartConstants.getVariableDisplayName(variable));
        }
        
        selector.setItems(FXCollections.observableArrayList(displayNames));
        selector.getSelectionModel().select(ChartConstants.getVariableDisplayName(currentVariable));
        
        // Listener na zmianę wybranej zmiennej
        selector.setOnAction(e -> {
            int selectedIndex = selector.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < availableVariables.size()) {
                currentVariable = availableVariables.get(selectedIndex);
                updateChart();
            }
        });
        
        return selector;
    }
    
    /**
     * Tworzy wykres dla wybranej zmiennej
     */
    private void createChart(List<TimeSeriesEntry> filteredData) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Data i czas");
        xAxis.setTickLabelRotation(45);
        xAxis.setTickLabelGap(5);
        xAxis.setTickLabelFill(Color.DARKSLATEGRAY);

        NumberAxis yAxis = AxisFormatter.createYAxisForVariable(currentVariable, filteredData);
        
        // Tworzymy specjalną podklasę LineChart z nadpisaną metodą updateLegend
        mainChart = new LineChart<String, Number>(xAxis, yAxis) {
            @Override
            protected void updateLegend() {
                super.updateLegend();
                
                // Pobierz właściwy kolor dla aktualnej zmiennej
                Color variableColor = ChartConstants.getColorObjectForVariable(currentVariable);
                String colorHex = ChartConstants.getColorForVariable(currentVariable);
                
                // Zmień kolor elementów legendy
                for (Node item : lookupAll(".chart-legend-item")) {
                    Node symbol = item.lookup(".chart-legend-item-symbol");
                    if (symbol != null) {
                        // Ustaw bezpośrednio kolor fill
                        if (symbol instanceof javafx.scene.shape.Rectangle) {
                            ((javafx.scene.shape.Rectangle) symbol).setFill(variableColor);
                        }
                        // Ustaw też style
                        symbol.setStyle("-fx-background-color: " + colorHex + " !important;");
                    }
                    
                    Node text = item.lookup(".text");
                    if (text != null) {
                        text.setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        };
        
        mainChart.setTitle("Dane pogodowe dla " + weatherData.getLocation().getName());
        mainChart.setAnimated(false);
        mainChart.setCreateSymbols(false);
        mainChart.setLegendSide(Side.TOP);
        mainChart.setVerticalGridLinesVisible(true);
        mainChart.setHorizontalGridLinesVisible(true);
        Insets chartPadding = new Insets(40, 60, 60, 60);
        mainChart.setPadding(chartPadding);
        mainChart.setMinSize(400, 300);
        mainChart.setPrefSize(800, 600);

        XYChart.Series<String, Number> series = createSeries(currentVariable, filteredData);
        mainChart.getData().add(series);
        
        // Zastosuj styl wykresu po dodaniu danych
        final String colorHex = ChartConstants.getColorForVariable(currentVariable);
        final Color colorObj = ChartConstants.getColorObjectForVariable(currentVariable);
        
        Platform.runLater(() -> {
            // Stosujemy styl do linii wykresu
            series.getNode().setStyle("-fx-stroke: " + colorHex + "; -fx-stroke-width: 3px;");
            
            // Bezpośrednio modyfikujemy elementy legendy
            for (Node node : mainChart.lookupAll(".chart-legend-item-symbol")) {
                node.setStyle("-fx-background-color: " + colorHex + " !important;");
                
                // Bezpośrednio ustaw kolor dla węzła (Rectangle)
                if (node instanceof javafx.scene.shape.Rectangle) {
                    ((javafx.scene.shape.Rectangle) node).setFill(colorObj);
                }
            }
            
            // Wymuś aktualizację legendy
            mainChart.requestLayout();
        });
        
        // Dodatkowe aktualizacje po 100ms dla pewności
        new Thread(() -> {
            try {
                Thread.sleep(100);
                Platform.runLater(() -> {
                    // Ponowna aktualizacja kolorów w legendzie
                    for (Node node : mainChart.lookupAll(".chart-legend-item-symbol")) {
                        if (node instanceof javafx.scene.shape.Rectangle) {
                            ((javafx.scene.shape.Rectangle) node).setFill(colorObj);
                        }
                    }
                });
            } catch (Exception e) {
                // Ignoruj wyjątki
            }
        }).start();
        
        chartStack.getChildren().clear();
        chartStack.getChildren().add(mainChart);
    }
    
    /**
     * Aktualizuje wykres przy zmianie zmiennej
     */
    private void updateChart() {
        List<TimeSeriesEntry> filteredData = filterDataByType();
        createChart(filteredData);
        
        // Aktualizacja tytułu okna
        stage.setTitle("Wykres pogodowy - " + weatherData.getLocation().getName() + " - " + ChartConstants.getVariableDisplayName(currentVariable));
        
        // Dodatkowa aktualizacja kolorów legendy po pełnym załadowaniu wykresu
        final String colorHex = ChartConstants.getColorForVariable(currentVariable);
        final Color colorObj = ChartConstants.getColorObjectForVariable(currentVariable);
        
        Platform.runLater(() -> {
            // Próba bezpośredniej aktualizacji wyglądu legendy dla pewności
            for (Node legendItem : mainChart.lookupAll(".chart-legend-item")) {
                for (Node node : legendItem.lookupAll(".chart-legend-item-symbol")) {
                    node.setStyle("-fx-background-color: " + colorHex + " !important;");
                    
                    // Bezpośrednio ustaw kolor dla węzła (Rectangle)
                    if (node instanceof javafx.scene.shape.Rectangle) {
                        ((javafx.scene.shape.Rectangle) node).setFill(colorObj);
                    }
                }
            }
            
            // Sprawdź również linię wykresu dla pewności
            if (!mainChart.getData().isEmpty()) {
                XYChart.Series<String, Number> series = mainChart.getData().get(0);
                if (series.getNode() != null) {
                    series.getNode().setStyle("-fx-stroke: " + colorHex + "; -fx-stroke-width: 3px;");
                }
            }
        });
        
        // Dodatkowe aktualizacje po 100ms dla pewności
        new Thread(() -> {
            try {
                Thread.sleep(100);
                Platform.runLater(() -> {
                    // Ponowna aktualizacja kolorów w legendzie
                    for (Node node : mainChart.lookupAll(".chart-legend-item-symbol")) {
                        if (node instanceof javafx.scene.shape.Rectangle) {
                            ((javafx.scene.shape.Rectangle) node).setFill(colorObj);
                        }
                    }
                });
            } catch (Exception e) {
                // Ignoruj wyjątki
            }
        }).start();
    }

    /**
     * Tworzy serię danych dla wykresu
     */
    private XYChart.Series<String, Number> createSeries(String variable, List<TimeSeriesEntry> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(ChartConstants.getVariableDisplayName(variable));

        List<TimeSeriesEntry> sortedData = new ArrayList<>(data);
        sortedData.sort((a, b) -> a.getTime().compareTo(b.getTime()));

        int count = 0;
        for (TimeSeriesEntry entry : sortedData) {
            Double value = getVariableValue(entry, variable);
            if (value != null) {
                String timeKey = entry.getTime().format(ChartConstants.TIME_FORMATTER);
                series.getData().add(new XYChart.Data<>(timeKey, value));
                count++;
            }
        }
        return series;
    }

    private Double getVariableValue(TimeSeriesEntry entry, String variable) {
        Double value = switch (variable) {
            case "temperature2m" -> entry.getTemperature2m();
            case "windSpeed" -> entry.getWindSpeed();
            case "soilTemperature" -> entry.getSoilTemperature();
            case "rain" -> entry.getRain();
            case "surfacePressure" -> entry.getSurfacePressure();
            default -> null;
        };
        
        if (value == null) {
            return null;
        }
        
        if (Math.abs(value) < 0.001) {
            if (variable.equals("rain")) {
                return 0.0; // Skalowane 0.0 dla opadów to nadal 0.0
            }
        }
        
        if (ChartConstants.shouldScaleVariable(variable)) {
            value = ChartConstants.scaleValueForDisplay(value, variable);
        }
        
        return value;
    }

    /**
     * Filtruje dane pogodowe według typu (prognoza/historia)
     */
    private List<TimeSeriesEntry> filterDataByType() {
        LocalDateTime now = LocalDateTime.now();
        String dataType = weatherData.getDataType();

        List<TimeSeriesEntry> filteredData;

        if ("forecast".equals(dataType)) {
            filteredData = weatherData.getTimeSeries().stream()
                    .filter(entry -> !entry.getTime().isBefore(now))
                    .collect(Collectors.toList());
        } else if ("historical".equals(dataType)) {
            filteredData = weatherData.getTimeSeries().stream()
                    .filter(entry -> entry.getTime().isBefore(now))
                    .collect(Collectors.toList());
        } else {
            filteredData = new ArrayList<>(weatherData.getTimeSeries());
        }

        if (filteredData.isEmpty() && !weatherData.getTimeSeries().isEmpty()) {
            filteredData = new ArrayList<>(weatherData.getTimeSeries());
        }

        filteredData.sort((a, b) -> a.getTime().compareTo(b.getTime()));

        if (filteredData.size() > 100) {
            int step = Math.max(1, filteredData.size() / 100);
            List<TimeSeriesEntry> reducedData = new ArrayList<>();
            for (int i = 0; i < filteredData.size(); i += step) {
                reducedData.add(filteredData.get(i));
            }
            // Upewnij się, że ostatni punkt jest zawsze dodany, jeśli został pominięty przez krok
            if (!reducedData.isEmpty() && reducedData.get(reducedData.size() - 1) != filteredData.get(filteredData.size() - 1)) {
                if (filteredData.size() > 0) reducedData.add(filteredData.get(filteredData.size() - 1));
            }
            return reducedData.isEmpty() ? filteredData : reducedData;
        }

        return filteredData;
    }

    /**
     * Eksportuje dane do pliku
     */
    private void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz dane wykresu");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Pliki tekstowe", "*.txt"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                List<TimeSeriesEntry> dataToExport = filterDataByType();
                WeatherData dataForExport = new WeatherData(
                        weatherData.getLocation(),
                        dataToExport,
                        weatherData.getElevation(),
                        weatherData.getTimezone(),
                        weatherData.getDataType()
                );

                // Eksportuj tylko aktualnie wybraną zmienną
                List<String> variableToExport = List.of(currentVariable);
                DataExporter.exportToFile(file, dataForExport, variableToExport);
            } catch (IOException e) {
                System.err.println("Błąd podczas eksportu danych: " + e.getMessage());
            }
        }
    }

    /**
     * Wyświetla okno
     */
    public void show() {
        if (stage != null) {
            stage.show();
        }
    }
}