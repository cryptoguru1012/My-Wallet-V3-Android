package info.blockchain.wallet.payment;

import info.blockchain.api.data.UnspentOutput;
import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.MockedResponseTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PaymentReplayProtectionTest extends MockedResponseTest {

    private Payment subject = new Payment();

    private boolean addReplayProtection = true;

    private String getTestData(String file) throws Exception {
        URI uri = getClass().getClassLoader().getResource(file).toURI();
        return new String(Files.readAllBytes(Paths.get(uri)), Charset.forName("utf-8"));
    }

    private long calculateFee(int outputs, int inputs, BigInteger feePerKb) {
        // Manually calculated fee
        long size = (outputs * 34) + (inputs * 149) + 10;
        double txBytes = ((double) size / 1000.0);
        return (long) Math.ceil(feePerKb.doubleValue() * txBytes);
    }

    @Test
    public void getMaximumAvailable_simple() {
        UnspentOutputs unspentOutputs = new UnspentOutputs();

        ArrayList<UnspentOutput> list = new ArrayList<>();
        UnspentOutput coin = new UnspentOutput();
        coin.setValue(BigInteger.valueOf(1323));
        list.add(coin);
        unspentOutputs.setUnspentOutputs(list);

        BigInteger feePerKb = BigInteger.valueOf(1000);

        Pair<BigInteger, BigInteger> sweepBundle = subject
                .getMaximumAvailable(unspentOutputs, feePerKb, addReplayProtection);

        // Added extra input and output for dust-service
        long feeManual = calculateFee(1, 2, feePerKb);

        assertEquals(feeManual, sweepBundle.getRight().longValue());
        // Available would be our amount + fake dust
        assertEquals(1323 + 546 - feeManual, sweepBundle.getLeft().longValue());
    }


    @Test
    public void getSpendableCoins_all_replayable() throws Exception {
        String response = getTestData("unspent/unspent_all_replayable.txt");

        UnspentOutputs unspentOutputs = UnspentOutputs.fromJson(response);
        subject = new Payment();

        long spendAmount = 4134L;
        SpendableUnspentOutputs paymentBundle = subject.getSpendableCoins(
                unspentOutputs,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(35000),
                addReplayProtection);

        List<UnspentOutput> unspentList = paymentBundle.getSpendableOutputs();

        // Descending values (only spend worthy)
        assertEquals(5, unspentList.size());
        assertEquals(546, unspentList.get(0).getValue().intValue());
        assertEquals(8324, unspentList.get(1).getValue().intValue());
        assertEquals(8140, unspentList.get(2).getValue().intValue());
        assertEquals(8139, unspentList.get(3).getValue().intValue());
        assertEquals(6600, unspentList.get(4).getValue().intValue());

        // All replayable
        assertTrue(unspentList.get(0).isReplayable());
        assertTrue(unspentList.get(1).isReplayable());
        assertTrue(unspentList.get(2).isReplayable());
        assertTrue(unspentList.get(3).isReplayable());
        assertTrue(unspentList.get(4).isReplayable());

        assertFalse(paymentBundle.isReplayProtected());
    }

    @Test
    public void getSpendableCoins_1_non_worthy_non_replayable() throws Exception {
        String response = getTestData("unspent/unspent_1_replayable.txt");

        UnspentOutputs unspentOutputs = UnspentOutputs.fromJson(response);
        subject = new Payment();

        long spendAmount = 34864L;
        SpendableUnspentOutputs paymentBundle = subject.getSpendableCoins(
                unspentOutputs,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(7000L),
                addReplayProtection);

        List<UnspentOutput> unspentList = paymentBundle.getSpendableOutputs();

        // Descending values (only spend worthy)
        assertEquals(7, unspentList.size());
        assertEquals(1323, unspentList.get(0).getValue().intValue());
        assertEquals(8324, unspentList.get(1).getValue().intValue());
        assertEquals(8140, unspentList.get(2).getValue().intValue());
        assertEquals(8139, unspentList.get(3).getValue().intValue());
        assertEquals(6600, unspentList.get(4).getValue().intValue());
        assertEquals(5000, unspentList.get(5).getValue().intValue());
        assertEquals(4947, unspentList.get(6).getValue().intValue());

        // Only first not replayable
        assertFalse(unspentList.get(0).isReplayable());
        assertTrue(unspentList.get(1).isReplayable());
        assertTrue(unspentList.get(2).isReplayable());
        assertTrue(unspentList.get(3).isReplayable());
        assertTrue(unspentList.get(4).isReplayable());
        assertTrue(unspentList.get(5).isReplayable());
        assertTrue(unspentList.get(6).isReplayable());

        assertTrue(paymentBundle.isReplayProtected());
    }

    @Test
    public void getSpendableCoins_3_replayable() throws Exception {
        String response = getTestData("unspent/unspent_3_replayable.txt");

        UnspentOutputs unspentOutputs = UnspentOutputs.fromJson(response);
        subject = new Payment();

        long spendAmount = 31770L;
        SpendableUnspentOutputs paymentBundle = subject.getSpendableCoins(
                unspentOutputs,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(10000L),
                addReplayProtection);

        List<UnspentOutput> unspentList = paymentBundle.getSpendableOutputs();

        // Descending values (only spend worthy)
        assertEquals(6, unspentList.size());
        assertEquals(6600, unspentList.get(0).getValue().intValue());

        assertEquals(8324, unspentList.get(1).getValue().intValue());
        assertEquals(5000, unspentList.get(2).getValue().intValue());
        assertEquals(4947, unspentList.get(3).getValue().intValue());

        assertEquals(8140, unspentList.get(4).getValue().intValue());
        assertEquals(8139, unspentList.get(5).getValue().intValue());

        // First + two last = not replayable
        assertFalse(unspentList.get(0).isReplayable());
        assertTrue(unspentList.get(1).isReplayable());
        assertTrue(unspentList.get(2).isReplayable());
        assertTrue(unspentList.get(3).isReplayable());
        assertFalse(unspentList.get(4).isReplayable());
        assertFalse(unspentList.get(5).isReplayable());

        assertTrue(paymentBundle.isReplayProtected());
    }

    @Test
    public void getSpendableCoins_empty_list() throws Exception {
        UnspentOutputs unspentOutputs = UnspentOutputs.fromJson("{\"unspent_outputs\":[]}");
        subject = new Payment();

        long spendAmount = 1500000L;
        SpendableUnspentOutputs paymentBundle = subject.getSpendableCoins(
                unspentOutputs,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(30000L),
                addReplayProtection
        );

        assertTrue(paymentBundle.getSpendableOutputs().isEmpty());
        assertTrue(paymentBundle.isReplayProtected());
        assertEquals(BigInteger.ZERO, paymentBundle.getAbsoluteFee());
    }

    @Test
    public void getMaximumAvailable_empty_list() throws Exception {
        UnspentOutputs unspentOutputs = UnspentOutputs.fromJson("{\"unspent_outputs\":[]}");
        subject = new Payment();

        final Pair<BigInteger, BigInteger> pair = subject.getMaximumAvailable(
                unspentOutputs,
                BigInteger.valueOf(1000L),
                addReplayProtection
        );

        assertEquals(BigInteger.ZERO, pair.getLeft());
        assertEquals(BigInteger.ZERO, pair.getRight());
    }
}
