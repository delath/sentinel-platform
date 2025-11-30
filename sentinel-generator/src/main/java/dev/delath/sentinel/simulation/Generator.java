package dev.delath.sentinel.simulation;

import dev.delath.sentinel.domain.Transaction;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import net.datafaker.Faker;
import net.datafaker.providers.base.Finance;

@ApplicationScoped
public class Generator {

    private final Faker faker = new Faker();
    private final Random random = new Random();

    private final List<UserData> userPool = new ArrayList<>();
    private final List<MerchantData> merchantPool = new ArrayList<>();

    private final Queue<Transaction> fraudBuffer = new ArrayDeque<>();

    private static final int USER_POOL_SIZE = 1000;
    private static final int MERCHANT_POOL_SIZE = 50;

    @PostConstruct
    void init() {
        for (int i = 0; i < USER_POOL_SIZE; i++) {
            userPool.add(generateNewUser());
        }

        for (int i = 0; i < MERCHANT_POOL_SIZE; i++) {
            merchantPool.add(generateNewMerchant());
        }
    }

    public Transaction next() {
        // A. Check Buffer
        if (!fraudBuffer.isEmpty()) {
            return fraudBuffer.poll();
        }

        // B. Pick Actors & Time
        var user = userPool.get(random.nextInt(userPool.size()));
        var merchant = merchantPool.get(random.nextInt(merchantPool.size()));
        var now = Instant.now();

        // C. Chaos Switch: 3% chance of Fraud
        if (random.nextDouble() < 0.03) {
            return switch (random.nextInt(4)) {
                case 0 -> generateImpossibleTraveler(user, merchant, now);
                case 1 -> generateSmurfingAttack(user, merchant, now);
                case 2 -> generateMoneyLaundering(now); // New User + Big Money
                default -> generateWalletHoarder(merchant, now); // One User + Many Cards
            };
        }

        // D. Normal Legitimate Transaction
        double lat = merchant.baseLat + (random.nextGaussian() * 0.0001);
        double lon = merchant.baseLon + (random.nextGaussian() * 0.0001);

        return buildTransaction(user, merchant, random.nextLong(500, 15000), lat, lon, now);
    }

    private UserData generateNewUser() {
        var cardType = faker.options()
                .option(
                        Finance.CreditCardType.VISA,
                        Finance.CreditCardType.MASTERCARD,
                        Finance.CreditCardType.AMERICAN_EXPRESS);
        var fullCardNumber = faker.finance().creditCard(cardType);
        var bin = fullCardNumber.substring(0, 6);
        var last4 = fullCardNumber.substring(fullCardNumber.length() - 4);
        var token = String.format("tok_%s_%s", cardType.toString().toLowerCase(), last4);
        return new UserData("u_" + faker.number().digits(8), token, bin);
    }

    private MerchantData generateNewMerchant() {
        var address = faker.address();
        var companyName = faker.company().name();
        var slug = companyName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");

        return new MerchantData(
                "m_" + slug + "_" + faker.number().digits(4),
                companyName,
                String.valueOf(faker.number().numberBetween(5000, 6000)),
                Double.parseDouble(address.latitude().replace(",", ".")),
                Double.parseDouble(address.longitude().replace(",", ".")));
    }

    private record UserData(String userId, String cardToken, String bin) {}

    private record MerchantData(String id, String name, String mcc, double baseLat, double baseLon) {}
}
