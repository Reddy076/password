package com.revature.passwordmanager.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GeoLocationService {

  private static final Logger logger = LoggerFactory.getLogger(GeoLocationService.class);

  public String getLocationFromIp(String ipAddress) {
    if (ipAddress == null || ipAddress.isEmpty() || "127.0.0.1".equals(ipAddress)
        || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
      return "Local/Unknown";
    }

    // In a real application, this would call a geolocation API or database (like
    // MaxMind GeoIP).
    // For this implementation, we will mock the location based on the IP format.

    if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.") || ipAddress.startsWith("172.")) {
      return "Internal Network";
    }

    // Simulate lookup
    logger.debug("Simulated GeoLocation lookup for IP: {}", ipAddress);
    return "City, Country (Simulated)";
  }
}
