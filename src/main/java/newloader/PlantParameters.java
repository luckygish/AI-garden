package newloader;

import java.util.Objects;

public class PlantParameters {
    private String culture;
    private String region;
    private String gardenType;
    private String soilType;

    public PlantParameters() {}

    public PlantParameters(String culture, String region, String gardenType, String soilType) {
        this.culture = culture;
        this.region = region;
        this.gardenType = gardenType;
        this.soilType = soilType;
    }

    // Геттеры и сеттеры
    public String getCulture() { return culture; }
    public void setCulture(String culture) { this.culture = culture; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getGardenType() { return gardenType; }
    public void setGardenType(String gardenType) { this.gardenType = gardenType; }

    public String getSoilType() { return soilType; }
    public void setSoilType(String soilType) { this.soilType = soilType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlantParameters that = (PlantParameters) o;
        return Objects.equals(culture, that.culture) &&
                Objects.equals(region, that.region) &&
                Objects.equals(gardenType, that.gardenType) &&
                Objects.equals(soilType, that.soilType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(culture, region, gardenType, soilType);
    }

    @Override
    public String toString() {
        return "PlantParameters{" +
                "culture='" + culture + '\'' +
                ", region='" + region + '\'' +
                ", garden_type='" + gardenType + '\'' +
                '}';
    }
}