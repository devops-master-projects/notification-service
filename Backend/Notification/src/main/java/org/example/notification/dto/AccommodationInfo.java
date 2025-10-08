package org.example.notification.dto;

import java.util.UUID;

public class AccommodationInfo {

    private UUID hostId;
    private String accommodationName;

    public UUID getHostId() { return hostId; }
    public void setHostId(UUID hostId) { this.hostId = hostId; }

    public String getAccommodationName() { return accommodationName; }
    public void setAccommodationName(String accommodationName) { this.accommodationName = accommodationName; }
}
