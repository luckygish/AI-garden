package com.agriculture.dto;

import java.util.List;

public class FeedingScheduleResponse {
    private String plantName;
    private List<FeedingScheduleItem> schedule;

    public FeedingScheduleResponse() {}

    public FeedingScheduleResponse(String plantName, List<FeedingScheduleItem> schedule) {
        this.plantName = plantName;
        this.schedule = schedule;
    }

    // Геттеры и сеттеры
    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public List<FeedingScheduleItem> getSchedule() { return schedule; }
    public void setSchedule(List<FeedingScheduleItem> schedule) { this.schedule = schedule; }

    public static class FeedingScheduleItem {
        private String period;
        private String phase;
        private String fertilizer;
        private String method;

        public FeedingScheduleItem() {}

        public FeedingScheduleItem(String period, String phase, String fertilizer, String method) {
            this.period = period;
            this.phase = phase;
            this.fertilizer = fertilizer;
            this.method = method;
        }

        // Геттеры и сеттеры
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }

        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }

        public String getFertilizer() { return fertilizer; }
        public void setFertilizer(String fertilizer) { this.fertilizer = fertilizer; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }
}