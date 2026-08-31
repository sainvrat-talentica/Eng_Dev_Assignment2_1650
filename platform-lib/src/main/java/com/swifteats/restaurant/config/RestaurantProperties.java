package com.swifteats.restaurant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "swifteats.restaurant")
public class RestaurantProperties {

    private boolean cacheEnabled = true;
    private Duration menuCacheTtl = Duration.ofMinutes(10);
    private Duration listCacheTtl = Duration.ofMinutes(5);

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public Duration getMenuCacheTtl() {
        return menuCacheTtl;
    }

    public void setMenuCacheTtl(Duration menuCacheTtl) {
        this.menuCacheTtl = menuCacheTtl;
    }

    public Duration getListCacheTtl() {
        return listCacheTtl;
    }

    public void setListCacheTtl(Duration listCacheTtl) {
        this.listCacheTtl = listCacheTtl;
    }
}
