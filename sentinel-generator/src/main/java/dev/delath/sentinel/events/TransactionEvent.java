package dev.delath.sentinel.events;

public record TransactionEvent(String transactionId, String userId, String cardToken, String cardBin, long amount, String currency, MerchantDetails merchantDetails, Location location, String ipAddress, String timestamp) {
    public record MerchantDetails(String merchantId, String mcc, String name) {}
    public record Location(Double lat, Double lon) {}
}
