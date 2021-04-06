package info.blockchain.wallet.payment;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.data.UnspentOutput;
import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.BlockchainFramework;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import retrofit2.Call;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class Coins {

    // Size added to combined tx using dust-service to approximate fee
    static final int DUST_INPUT_TX_SIZE_ADAPT = 150;
    private static final Logger log = LoggerFactory.getLogger(Coins.class);

    static Call<UnspentOutputs> getUnspentCoins(List<String> addresses) {
        BlockExplorer blockExplorer = new BlockExplorer(BlockchainFramework.getRetrofitExplorerInstance(),
                BlockchainFramework.getRetrofitApiInstance(), BlockchainFramework.getApiCode());
        return blockExplorer.getUnspentOutputs("btc", addresses, null, null);
    }

    static Call<UnspentOutputs> getUnspentBchCoins(List<String> addresses) {
        BlockExplorer blockExplorer = new BlockExplorer(BlockchainFramework.getRetrofitExplorerInstance(),
                BlockchainFramework.getRetrofitApiInstance(), BlockchainFramework.getApiCode());
        return blockExplorer.getUnspentOutputs("bch", addresses, null, null);
    }

    /**
     * Computes the available amount to send and the associated fee in satoshis provided a list of
     * coins and the fee per Kilobyte.
     *
     * @param coins               the UTXOs
     * @param feePerKb            the fee per KB
     * @param addReplayProtection boolean whether replay protection should be considered
     * @return a Pair of maximum available amount to send and the associated fee in satoshis.
     */
    public static Pair<BigInteger, BigInteger> getMaximumAvailable(UnspentOutputs coins,
                                                                   BigInteger feePerKb,
                                                                   boolean addReplayProtection,
                                                                   boolean useNewCoinSelection) {
        if (useNewCoinSelection) {
            CoinSortingMethod coinSortingMethod = null;

            if (addReplayProtection) {
                coinSortingMethod = new ReplayProtection(getPlaceholderDustInput());
            }

            SpendableUnspentOutputs selection =
                    new CoinSelection(coins.getUnspentOutputs(), feePerKbToFeePerByte(feePerKb))
                            .selectAll(coinSortingMethod);

            return Pair.of(selection.getSpendableBalance(), selection.getAbsoluteFee());
        }

        BigInteger sweepBalance = BigInteger.ZERO;

        ArrayList<UnspentOutput> usableCoins = new ArrayList<>();
        ArrayList<UnspentOutput> unspentOutputs;

        // Sort inputs
        if (addReplayProtection) {
            unspentOutputs = getSortedCoins(coins.getUnspentOutputs());
        } else {
            unspentOutputs = coins.getUnspentOutputs();
            Collections.sort(unspentOutputs, new UnspentOutputAmountComparatorDesc());
        }

        double inputCost = inputCost(feePerKb);

        final boolean includesReplayDust = addReplayProtection && requiresReplayProtection(unspentOutputs);

        if (includesReplayDust) {
            log.info("Calculating maximum available with non-replayable dust included.");
            unspentOutputs.add(0, getPlaceholderDustInput());
        }

        for (int i = 0; i < unspentOutputs.size(); i++) {
            UnspentOutput output = unspentOutputs.get(i);
            // Filter usable coins
            if (output.isForceInclude() || output.getValue().doubleValue() > inputCost) {
                usableCoins.add(output);
                sweepBalance = sweepBalance.add(output.getValue());
            }
        }

        // All inputs, 1 output = no change. (Correct way)
        int outputCount = 1;

        BigInteger sweepFee = calculateFee(usableCoins.size(),
                outputCount,
                feePerKb,
                includesReplayDust);

        sweepBalance = sweepBalance.subtract(sweepFee);

        sweepBalance = BigInteger.valueOf(Math.max(sweepBalance.longValue(), 0));

        log.info("Filtering sweepable coins. Sweepable Balance = {}, Fee required for sweep = {}", sweepBalance, sweepFee);
        return Pair.of(sweepBalance, sweepFee);
    }

    /**
     * Sort in order - 1 smallest non-replayable coin, descending replayable, descending
     * non-replayable
     */
    private static ArrayList<UnspentOutput> getSortedCoins(ArrayList<UnspentOutput> unspentOutputs) {
        ArrayList<UnspentOutput> sortedCoins = new ArrayList<>();

        // Select 1 smallest non-replayable coin
        Collections.sort(unspentOutputs, new UnspentOutputAmountComparatorAsc());
        for (UnspentOutput coin : unspentOutputs) {
            if (!coin.isReplayable()) {
                coin.setForceInclude(true);
                sortedCoins.add(coin);
                break;
            }
        }

        // Descending value. Add all replayable coins.
        Collections.reverse(unspentOutputs);
        for (UnspentOutput coin : unspentOutputs) {
            if (!sortedCoins.contains(coin) && coin.isReplayable()) {
                sortedCoins.add(coin);
            }
        }

        // Still descending. Add all non-replayable coins.
        for (UnspentOutput coin : unspentOutputs) {
            if (!sortedCoins.contains(coin) && !coin.isReplayable()) {
                sortedCoins.add(coin);
            }
        }

        return sortedCoins;
    }

    /**
     * Returns the spendable coins provided the desired amount to send.
     *
     * @param coins               a list of coins
     * @param paymentAmount       the desired amount to send
     * @param feePerKb            the fee per KB
     * @param addReplayProtection whether or no replay protection should be considered
     * @return a list of spendable coins
     */
    public static SpendableUnspentOutputs getMinimumCoinsForPayment(UnspentOutputs coins,
                                                                    BigInteger paymentAmount,
                                                                    BigInteger feePerKb,
                                                                    boolean addReplayProtection,
                                                                    boolean useNewCoinSelection) {
        if (useNewCoinSelection) {
            CoinSortingMethod coinSortingMethod;

            if (addReplayProtection) {
                coinSortingMethod = new ReplayProtection(getPlaceholderDustInput());
            } else {
                coinSortingMethod = DescentDraw.INSTANCE;
            }

            return new CoinSelection(coins.getUnspentOutputs(), feePerKbToFeePerByte(feePerKb))
                    .select(paymentAmount, coinSortingMethod);
        }

        log.info("Select the minimum number of outputs necessary for payment");
        List<UnspentOutput> spendWorthyList = new ArrayList<>();

        ArrayList<UnspentOutput> unspentOutputs;

        // Sort inputs
        if (addReplayProtection) {
            unspentOutputs = getSortedCoins(coins.getUnspentOutputs());
        } else {
            unspentOutputs = coins.getUnspentOutputs();
            Collections.sort(unspentOutputs, new UnspentOutputAmountComparatorDesc());
        }

        BigInteger collectedAmount = BigInteger.ZERO;
        BigInteger consumedAmount = BigInteger.ZERO;

        double inputCost = inputCost(feePerKb);


        final boolean requiresReplayProtection = requiresReplayProtection(unspentOutputs);
        final boolean includesReplayDust = addReplayProtection && requiresReplayProtection;
        if (includesReplayDust) {
            log.info("Adding non-replayable dust to selected coins.");
            unspentOutputs.add(0, getPlaceholderDustInput());
        }

        // initially assume change
        int outputCount = 2;
        for (int i = 0; i < unspentOutputs.size(); i++) {
            UnspentOutput output = unspentOutputs.get(i);

            // Filter coins not worth spending
            if (output.getValue().doubleValue() < inputCost && !output.isForceInclude()) {
                continue;
            }

            // Skip script with no type
            if (!output.isForceInclude() &&
                    new Script(Hex.decode(output.getScript().getBytes())).getScriptType() == Script.ScriptType.NO_TYPE) {
                continue;
            }

            // Collect coin
            spendWorthyList.add(output);

            collectedAmount = collectedAmount.add(output.getValue());

            // Fee
            int coinCount = spendWorthyList.size();
            BigInteger paymentAmountNoChange = estimateAmount(coinCount, paymentAmount, feePerKb, 1);
            BigInteger paymentAmountWithChange = estimateAmount(coinCount, paymentAmount, feePerKb, 2);

            // No change = 1 output (Exact amount)
            if (paymentAmountNoChange.compareTo(collectedAmount) == 0) {
                outputCount = 1;
                break;
            }

            // No change = 1 output (Don't allow dust to be sent back as change - consume it rather)
            if (paymentAmountNoChange.compareTo(collectedAmount) < 0
                    && paymentAmountNoChange.compareTo(collectedAmount.subtract(Payment.DUST)) >= 0) {
                consumedAmount = consumedAmount.add(paymentAmountNoChange.subtract(collectedAmount));
                outputCount = 1;
                break;
            }

            // Expect change = 2 outputs
            if (collectedAmount.compareTo(paymentAmountWithChange) >= 0) {
                // [multiple inputs, 2 outputs] - assume change
                outputCount = 2;
                break;
            }
        }

        BigInteger absoluteFee = calculateFee(spendWorthyList.size(),
                outputCount,
                feePerKb,
                includesReplayDust);

        SpendableUnspentOutputs paymentBundle = new SpendableUnspentOutputs();
        paymentBundle.setSpendableOutputs(spendWorthyList);
        paymentBundle.setAbsoluteFee(absoluteFee);
        paymentBundle.setConsumedAmount(consumedAmount);
        paymentBundle.setReplayProtected(!requiresReplayProtection);
        return paymentBundle;
    }

    private static BigInteger calculateFee(int inputCount, int outputCount, BigInteger feePerKb, boolean includesReplayDust) {
        if (inputCount == 0) {
            return BigInteger.ZERO;
        }
        if (includesReplayDust) {
            // No non-replayable outputs in wallet - a dust input and output will be added to tx later
            log.info("Modifying tx size for fee calculation.");
            int size = Fees.estimatedSize(inputCount, outputCount) + DUST_INPUT_TX_SIZE_ADAPT;
            return Fees.calculateFee(size, feePerKb);
        } else {
            return Fees.estimatedFee(inputCount, outputCount, feePerKb);
        }
    }

    private static BigInteger estimateAmount(int coinCount, BigInteger paymentAmount, BigInteger feePerKb, int outputCount) {
        BigInteger fee = Fees.estimatedFee(coinCount, outputCount, feePerKb);
        return paymentAmount.add(fee);
    }

    private static double inputCost(BigInteger feePerKb) {
        double d = Math.ceil(feePerKb.doubleValue() * 0.148);
        return Math.ceil(d);
    }

    private static boolean requiresReplayProtection(final List<UnspentOutput> unspentOutputs) {
        return !unspentOutputs.isEmpty() && unspentOutputs.get(0).isReplayable();
    }

    /**
     * Sort unspent outputs by amount in descending order.
     */
    private static class UnspentOutputAmountComparatorDesc implements Comparator<UnspentOutput> {

        @Override
        public int compare(UnspentOutput o1, UnspentOutput o2) {
            return o2.getValue().compareTo(o1.getValue());
        }
    }

    private static class UnspentOutputAmountComparatorAsc implements Comparator<UnspentOutput> {

        @Override
        public int compare(UnspentOutput o1, UnspentOutput o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    }

    private static UnspentOutput getPlaceholderDustInput() {
        UnspentOutput dust = new UnspentOutput();
        dust.setValue(Payment.DUST);
        dust.setForceInclude(true);
        return dust;
    }

    private static BigInteger feePerKbToFeePerByte(BigInteger feePerKb) {
        return new BigDecimal(feePerKb)
                .divide(BigDecimal.valueOf(1000L), 0, RoundingMode.CEILING)
                .toBigIntegerExact();
    }
}