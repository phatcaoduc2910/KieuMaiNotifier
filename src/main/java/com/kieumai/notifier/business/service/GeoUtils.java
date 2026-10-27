package com.kieumai.notifier.business.service;

public final class GeoUtils {
    private GeoUtils() {}

    public static double distanceMeters(double latitude1, double longitude1,
                                        double latitude2, double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))));
    }
}
