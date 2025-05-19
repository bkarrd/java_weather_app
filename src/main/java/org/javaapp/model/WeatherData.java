package org.javaapp.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.util.List;

public class WeatherData {
    @SerializedName("location")
    private Location location;
    
    @SerializedName("timeSeries")
    private List<TimeSeriesEntry> timeSeries;
    
    @SerializedName("elevation")
    private double elevation;
    
    @SerializedName("timezone")
    private String timezone;
    
    @SerializedName("dataType")
    private String dataType; // "forecast" lub "historical"
    
    public WeatherData() {
    }
    
    public WeatherData(Location location, List<TimeSeriesEntry> timeSeries, double elevation, String timezone, String dataType) {
        this.location = location;
        this.timeSeries = timeSeries;
        this.elevation = elevation;
        this.timezone = timezone;
        this.dataType = dataType;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
    }
    
    public List<TimeSeriesEntry> getTimeSeries() {
        return timeSeries;
    }
    
    public void setTimeSeries(List<TimeSeriesEntry> timeSeries) {
        this.timeSeries = timeSeries;
    }
    
    public double getElevation() {
        return elevation;
    }
    
    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    // Klasa wewnętrzna dla pojedynczego wpisu w serii czasowej
    public static class TimeSeriesEntry {
        @SerializedName("time")
        private LocalDateTime time;
        
        @SerializedName("temperature2m")
        private Double temperature2m;
        
        @SerializedName("windSpeed")
        private Double windSpeed;
        
        @SerializedName("soilTemperature")
        private Double soilTemperature;
        
        @SerializedName("rain")
        private Double rain;
        
        @SerializedName("surfacePressure")
        private Double surfacePressure;
        
        public TimeSeriesEntry() {
        }
        
        public LocalDateTime getTime() {
            return time;
        }
        
        public void setTime(LocalDateTime time) {
            this.time = time;
        }
        
        public Double getTemperature2m() {
            return temperature2m;
        }
        
        public void setTemperature2m(Double temperature2m) {
            this.temperature2m = temperature2m;
        }
        
        public Double getWindSpeed() {
            return windSpeed;
        }
        
        public void setWindSpeed(Double windSpeed) {
            this.windSpeed = windSpeed;
        }
        
        public Double getSoilTemperature() {
            return soilTemperature;
        }
        
        public void setSoilTemperature(Double soilTemperature) {
            this.soilTemperature = soilTemperature;
        }
        
        public Double getRain() {
            return rain;
        }
        
        public void setRain(Double rain) {
            this.rain = rain;
        }
        
        public Double getSurfacePressure() {
            return surfacePressure;
        }
        
        public void setSurfacePressure(Double surfacePressure) {
            this.surfacePressure = surfacePressure;
        }
    }
} 