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
      return "Localhost";
    }

    if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.") || ipAddress.startsWith("172.")) {
      return "Internal Network";
    }

    logger.debug("Simulated GeoLocation lookup for IP: {}", ipAddress);
    return "City, Country (Simulated)";
  }
}
