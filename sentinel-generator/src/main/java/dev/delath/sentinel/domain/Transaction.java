package dev.delath.sentinel.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Transaction(
    @JsonProperty("transaction_id") String transactionId,
    @JsonProperty("user_id") String userId,
    @JsonProperty("card_token") String cardToken,
    @JsonProperty("card_bin") String cardBin,
    BigDecimal amount,
    String currency,
    @JsonProperty("merchant_details") MerchantDetails merchantDetails,
    Location location,
    @JsonProperty("ip_address") String ipAddress,
    String timestamp) {
  public record MerchantDetails(
      @JsonProperty("merchant_id") String merchantId, String mcc, String name) {}

  public record Location(Double lat, Double lon) {}
}
