package org.javaapp.view;

import javafx.geometry.Insets;
import javafx.scene.chart.NumberAxis;
import javafx.scene.paint.Color;
import org.javaapp.model.WeatherData.TimeSeriesEntry;

import java.util.List;

/**
 * Klasa odpowiedzialna za formatowanie osi wykresów
 * Dostosowanie zakresów do danych
 * ustawianie kolorów i stylów
 * formatowanie etykiet
 */
public class AxisFormatter {
    
    /**
     * Tworzy oś Y dostosowaną do wybranego typu danych
     */
    public static NumberAxis createYAxisForVariable(String variable, List<TimeSeriesEntry> data) {
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(ChartConstants.getVariableDisplayName(variable));
        
        // Zastosuj kolor osi zgodny z kolorem zmiennej
        Color axisColor = ChartConstants.getColorObjectForVariable(variable);
        yAxis.setTickLabelFill(axisColor);
        yAxis.setStyle("-fx-tick-label-fill: " + ChartConstants.getColorForVariable(variable) + "; -fx-font-weight: bold;");
        
        // Obliczanie faktycznego zakresu danych
        double[] range = calculateDataRange(variable, data);
        double minValue = range[0];
        double maxValue = range[1];
        
        // Ustawianie zakresów i podziałek dla różnych typów danych
        configureAxisRange(yAxis, variable, minValue, maxValue);
        
        // Ustawianie niestandardowych formatterów etykiet
        configureTickLabelFormatter(yAxis, variable);
        
        // Dostosowywanie padding
        if (variable.equals("surfacePressure") || variable.equals("rain")) {
            yAxis.setPadding(new Insets(0, 10, 0, 10));
        } else {
            yAxis.setPadding(new Insets(0, 15, 0, 15));
        }
        
        // Zapewnij widoczność
        yAxis.setVisible(true);
        yAxis.setOpacity(1.0);
        yAxis.setTickLabelsVisible(true);
        yAxis.setTickMarkVisible(true);
        
        return yAxis;
    }
    
    /**
     * Oblicza faktyczny zakres danych dla wybranej zmiennej
     */
    private static double[] calculateDataRange(String variable, List<TimeSeriesEntry> data) {
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;
        boolean hasData = false;
        
        // Znajdź minimum i maksimum dla danych
        for (TimeSeriesEntry entry : data) {
            Double value = getVariableValue(entry, variable);
            if (value != null) {
                // Odscaluj wartość jeśli była skalowana
                if (ChartConstants.shouldScaleVariable(variable)) {
                    value = ChartConstants.unscaleValueForDisplay(value, variable);
                }
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
                hasData = true;
            }
        }
        
        // Jeśli nie ma danych, ustaw domyślne zakresy
        if (!hasData) {
            minValue = getDefaultMinValue(variable);
            maxValue = getDefaultMaxValue(variable);
        } else {
            // Dodaj margines do zakresu
            adjustRangeWithMargin(variable, new double[] {minValue, maxValue});
            minValue = new double[] {minValue, maxValue}[0];
            maxValue = new double[] {minValue, maxValue}[1];
        }
        
        return new double[] {minValue, maxValue};
    }
    
    /**
     * Pobiera wartość zmiennej z rekordu danych
     */
    private static Double getVariableValue(TimeSeriesEntry entry, String variable) {
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
                return 0.0;
            }
        }
        
        if (ChartConstants.shouldScaleVariable(variable)) {
            value = ChartConstants.scaleValueForDisplay(value, variable);
        }
        
        return value;
    }
    
    /**
     * Zwraca domyślną minimalną wartość dla wybranego typu danych
     */
    private static double getDefaultMinValue(String variable) {
        return switch (variable) {
            case "temperature2m", "soilTemperature" -> -20;
            case "windSpeed", "rain" -> 0;
            case "surfacePressure" -> 980;
            default -> 0;
        };
    }
    
    /**
     * Zwraca domyślną maksymalną wartość dla wybranego typu danych
     */
    private static double getDefaultMaxValue(String variable) {
        return switch (variable) {
            case "temperature2m", "soilTemperature" -> 40;
            case "windSpeed" -> 100;
            case "rain" -> 10;
            case "surfacePressure" -> 1030;
            default -> 100;
        };
    }
    
    /**
     * Dostosowuje zakres danych dodając margines
     */
    private static void adjustRangeWithMargin(String variable, double[] range) {
        double minValue = range[0];
        double maxValue = range[1];
        double dataRange = maxValue - minValue;
        
        if (dataRange < 0.0001) {
            dataRange = 1.0;
        }
        
        if (variable.equals("temperature2m") || variable.equals("soilTemperature")) {
            // Dla temperatur, dodaj margines i zapewnij minimalny zakres
            double margin = Math.max(dataRange * 0.1, 5.0);
            minValue = Math.min(minValue - margin, minValue * 1.1);
            maxValue = Math.max(maxValue + margin, maxValue * 1.1);
            
            // Gwarantujemy minimalny zakres
            if (maxValue - minValue < 10) {
                double mid = (maxValue + minValue) / 2;
                minValue = mid - 5;
                maxValue = mid + 5;
            }
            
            // Zaokrąglamy
            minValue = Math.floor(minValue / 5) * 5;
            maxValue = Math.ceil(maxValue / 5) * 5;
        } else if (variable.equals("windSpeed")) {
            minValue = 0;
            maxValue = Math.max(maxValue * 1.1, maxValue + 10);
            maxValue = Math.ceil(maxValue / 10) * 10;
        } else if (variable.equals("rain")) {
            minValue = 0;
            double margin = Math.max(dataRange * 0.1, 1.0);
            maxValue = Math.max(maxValue + margin, maxValue * 1.1);
            maxValue = Math.ceil(maxValue);
            if (maxValue < 5) maxValue = 5;
        } else if (variable.equals("surfacePressure")) {
            double margin = Math.max(dataRange * 0.1, 5.0);
            minValue = minValue - margin;
            maxValue = maxValue + margin;
            minValue = Math.floor(minValue / 5) * 5;
            maxValue = Math.ceil(maxValue / 5) * 5;
        }
        
        range[0] = minValue;
        range[1] = maxValue;
    }
    
    /**
     * Konfiguruje zakres i jednostkę podziałki dla osi
     */
    private static void configureAxisRange(NumberAxis yAxis, String variable, double minValue, double maxValue) {
        if (variable.equals("temperature2m") || variable.equals("soilTemperature")) {
            yAxis.setLowerBound(minValue);
            yAxis.setUpperBound(maxValue);
            
            double range = maxValue - minValue;
            if (range <= 20) {
                yAxis.setTickUnit(2);
            } else if (range <= 50) {
                yAxis.setTickUnit(5);
            } else {
                yAxis.setTickUnit(10);
            }
        } else if (variable.equals("windSpeed")) {
            yAxis.setLowerBound(minValue);
            yAxis.setUpperBound(maxValue);
            
            double range = maxValue - minValue;
            if (range <= 50) {
                yAxis.setTickUnit(10);
            } else {
                yAxis.setTickUnit(20);
            }
            
            yAxis.setMinWidth(70);
        } else if (variable.equals("rain")) {
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(maxValue * 10);
            
            if (maxValue <= 2) {
                yAxis.setTickUnit(5);
            } else if (maxValue <= 5) {
                yAxis.setTickUnit(10);
            } else if (maxValue <= 10) {
                yAxis.setTickUnit(20);
            } else {
                yAxis.setTickUnit(50);
            }
            
            yAxis.setMinWidth(50);
        } else if (variable.equals("surfacePressure")) {
            double scaledMin = (minValue - 1000.0) / 10.0;
            double scaledMax = (maxValue - 1000.0) / 10.0;
            
            yAxis.setLowerBound(scaledMin);
            yAxis.setUpperBound(scaledMax);
            
            double range = scaledMax - scaledMin;
            if (range <= 5) {
                yAxis.setTickUnit(1.0);
            } else {
                yAxis.setTickUnit(2.5);
            }
            
            yAxis.setMinWidth(80);
        } else {
            yAxis.setLowerBound(minValue);
            yAxis.setUpperBound(maxValue);
            yAxis.setTickUnit((maxValue - minValue) / 10.0);
        }
    }
    
    /**
     * Konfiguruje formatter etykiet dla osi
     */
    private static void configureTickLabelFormatter(NumberAxis yAxis, String variable) {
        if (variable.equals("rain")) {
            yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                @Override
                public String toString(Number object) {
                    double scaledValue = object.doubleValue();
                    double originalValue = scaledValue / 10.0;
                    return String.format("%.1f", originalValue);
                }
            });
        } else if (variable.equals("surfacePressure")) {
            yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                @Override
                public String toString(Number object) {
                    double scaledValue = object.doubleValue();
                    double originalValue = ChartConstants.unscaleValueForDisplay(scaledValue, variable);
                    return String.format("%.0f", originalValue);
                }
            });
        } else if (variable.equals("windSpeed")) {
            yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                @Override
                public String toString(Number object) {
                    return String.format("%.0f", object.doubleValue());
                }
            });
        } else {
            yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                @Override
                public String toString(Number object) {
                    return String.format("%.1f", object.doubleValue());
                }
            });
        }
    }
} 