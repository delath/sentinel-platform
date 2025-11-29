package dev.delath.sentinel.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransactionEvent(
    @JsonProperty("transaction_id") String transactionId,
    @JsonProperty("user_id") String userId,
    @JsonProperty("card_token") String cardToken,
    @JsonProperty("card_bin") String cardBin,
    long amount,
    String currency,
    @JsonProperty("merchant_details") MerchantDetails merchantDetails,
    Location location,
    @JsonProperty("ip_address") String ipAddress,
    String timestamp) {
  public record MerchantDetails(
      @JsonProperty("merchant_id") String merchantId, String mcc, String name) {}

  public record Location(Double lat, Double lon) {}
}
