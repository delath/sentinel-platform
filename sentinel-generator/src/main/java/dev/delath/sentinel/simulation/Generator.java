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
import java.util.UUID;
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
                case 2 -> generateMoneyLaundering(now);
                default -> generateWalletHoarder(merchant, now);
            };
        }

        // D. Normal Legitimate Transaction
        var lat = merchant.baseLat + (random.nextGaussian() * 0.0001);
        var lon = merchant.baseLon + (random.nextGaussian() * 0.0001);

        return buildTransaction(user, merchant, random.nextLong(500, 15000), lat, lon, now);
    }

    /**
     * PATTERN 1: IMPOSSIBLE TRAVELER
     * Milan -> New York in 5 seconds.
     */
    private Transaction generateImpossibleTraveler(UserData user, MerchantData merchant, Instant now) {
        // 1. Valid Transaction (Home Base)
        var validTx =
                buildTransaction(user, merchant, random.nextLong(1000, 5000), merchant.baseLat, merchant.baseLon, now);

        // 2. Impossible Transaction (New York) - 5 seconds later
        var nyMerchant = new MerchantData("m_starbucks_ny", "Starbucks NY", "5812", 40.7128, -74.0060);
        var impossibleTx = buildTransaction(
                user,
                nyMerchant,
                random.nextLong(1000, 5000),
                nyMerchant.baseLat,
                nyMerchant.baseLon,
                now.plusSeconds(5));

        fraudBuffer.add(impossibleTx);
        return validTx;
    }

    /**
     * PATTERN 2: SMURFING (Velocity)
     * 10 transactions, 100ms apart.
     */
    private Transaction generateSmurfingAttack(UserData user, MerchantData merchant, Instant now) {
        for (int i = 0; i < 10; i++) {
            long lowAmount = random.nextLong(100, 1000);
            var tx = buildTransaction(
                    user, merchant, lowAmount, merchant.baseLat, merchant.baseLon, now.plusMillis(i * 100));
            fraudBuffer.add(tx);
        }
        return fraudBuffer.poll();
    }

    /**
     * PATTERN 3: MONEY LAUNDERING
     * Requirement: New User (not in pool) + Risky MCC + Huge Amount.
     */
    private Transaction generateMoneyLaundering(Instant now) {
        // 1. Create a Fresh User (Not in our pool, simulating a new signup)
        var freshUser = generateNewUser();

        // 2. Select Risky Merchant (Casino)
        var casino = new MerchantData(
                "m_casino_" + UUID.randomUUID().toString().substring(0, 5),
                "Royal Casino Online",
                "7995",
                45.4642,
                9.1900);

        // 3. Huge Amount (e.g., 20,000 EUR)
        long amount = random.nextLong(2000000, 5000000);

        return buildTransaction(freshUser, casino, amount, casino.baseLat, casino.baseLon, now);
    }

    /**
     * PATTERN 4: THE WALLET HOARDER
     * One User -> 4 Different Cards -> Rapid succession.
     */
    private Transaction generateWalletHoarder(MerchantData merchant, Instant now) {
        var maliciousUser = userPool.get(random.nextInt(userPool.size()));

        // Generate 4 DIFFERENT card tokens for this single user
        for (int i = 0; i < 4; i++) {
            var fakeToken = "tok_stolen_" + UUID.randomUUID().toString().substring(0, 8);
            var fakeBin = String.valueOf(random.nextInt(100000, 999999));

            // Temporarily override the user's card data for this transaction
            var specificCardUser = new UserData(maliciousUser.userId(), fakeToken, fakeBin);

            var tx = buildTransaction(
                    specificCardUser,
                    merchant,
                    random.nextLong(2000, 8000),
                    merchant.baseLat,
                    merchant.baseLon,
                    now.plusSeconds(i * 10)); // 10 seconds apart

            fraudBuffer.add(tx);
        }

        return fraudBuffer.poll();
    }

    private Transaction buildTransaction(
            UserData user, MerchantData merchant, long amount, double lat, double lon, Instant timestamp) {
        return new Transaction(
                UUID.randomUUID().toString(),
                user.userId(),
                user.cardToken(),
                user.bin(),
                amount,
                "EUR",
                new Transaction.MerchantDetails(merchant.id(), merchant.mcc(), merchant.name()),
                new Transaction.Location(lat, lon),
                faker.internet().ipV4Address(),
                timestamp.toString());
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
