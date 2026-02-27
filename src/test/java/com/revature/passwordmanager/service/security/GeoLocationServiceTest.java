package com.revature.passwordmanager.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoLocationServiceTest {

  private GeoLocationService geoLocationService;

  @BeforeEach
  void setUp() {
    geoLocationService = new GeoLocationService();
  }

  @Test
  void getLocationFromIp_Null_ShouldReturnLocalUnknown() {
    assertEquals("Localhost", geoLocationService.getLocationFromIp(null));
  }

  @Test
  void getLocationFromIp_Empty_ShouldReturnLocalUnknown() {
    assertEquals("Localhost", geoLocationService.getLocationFromIp(""));
  }

  @Test
  void getLocationFromIp_Localhost_ShouldReturnLocalUnknown() {
    assertEquals("Localhost", geoLocationService.getLocationFromIp("127.0.0.1"));
  }

  @Test
  void getLocationFromIp_IPv6Localhost_ShouldReturnLocalUnknown() {
    assertEquals("Localhost", geoLocationService.getLocationFromIp("0:0:0:0:0:0:0:1"));
  }

  @Test
  void getLocationFromIp_PrivateIP_10_ShouldReturnInternalNetwork() {
    assertEquals("Internal Network", geoLocationService.getLocationFromIp("10.0.0.1"));
  }

  @Test
  void getLocationFromIp_PrivateIP_192_ShouldReturnInternalNetwork() {
    assertEquals("Internal Network", geoLocationService.getLocationFromIp("192.168.1.100"));
  }

  @Test
  void getLocationFromIp_PrivateIP_172_ShouldReturnInternalNetwork() {
    assertEquals("Internal Network", geoLocationService.getLocationFromIp("172.16.0.1"));
  }

  @Test
  void getLocationFromIp_PublicIP_ShouldReturnSimulatedLocation() {
    assertEquals("City, Country (Simulated)", geoLocationService.getLocationFromIp("8.8.8.8"));
  }
}
