package com.revature.passwordmanager.security;

import com.revature.passwordmanager.exception.RateLimitExceededException;
import com.revature.passwordmanager.service.security.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimitService rateLimitService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String ipAddress = request.getRemoteAddr();
    String path = request.getRequestURI();

    if (!rateLimitService.isAllowed(ipAddress, path)) {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
      return;
    }

    int remaining = rateLimitService.getRemainingRequests(ipAddress, path);
    response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

    filterChain.doFilter(request, response);
  }
}
