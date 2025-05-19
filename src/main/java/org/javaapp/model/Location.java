package org.javaapp.model;

public class Location {
    private double latitude;
    private double longitude;
    private String name;      // nazwa miejscowości (admin3)
    private String county;    // powiat (admin2)
    private String region;    // województwo (admin1)
    private String country;   // kraj

    public Location() {
    }

    public Location(double latitude, double longitude, String name, String country) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.country = country;
    }

    public Location(double latitude, double longitude, String name, String county, String region, String country) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.county = county;
        this.region = region;
        this.country = country;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (name != null && !name.isEmpty()) {
            sb.append(name);  // admin3 (miasto)
        }
        
        if (county != null && !county.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(county);  // admin2 (powiat)
        }
        
        if (region != null && !region.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(region);  // admin1 (województwo)
        }
        
        if (country != null && !country.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(country);  // kraj
        }
        
        return sb.toString();
    }
} 