package info.blockchain.wallet.ethereum;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.RawTransaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;

import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.MockedResponseTest;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;

import static com.blockchain.testutils.GetStringFromResourceKt.getStringFromResource;


public class EthereumWalletTest extends MockedResponseTest {
    private final String ethereumLabel = "My Ether Wallet";
    private final String paxLabel = "My USD PAX Wallet";
    private final String usdtLabel = "My Tether Wallet";
    private final String dgldLabel = "My Wrapped-DGLD Wallet";
    private final HashMap<CryptoCurrency, String> allLabels = new HashMap();
    private final HashMap<CryptoCurrency, String> erc20Labels = new HashMap();

    private EthereumWallet subject;

    @Before
    public void setup() {
        allLabels.put(CryptoCurrency.ETHER, ethereumLabel);
        allLabels.put(CryptoCurrency.PAX, paxLabel);
        allLabels.put(CryptoCurrency.USDT, usdtLabel);
        allLabels.put(CryptoCurrency.DGLD, dgldLabel);

        erc20Labels.put(CryptoCurrency.PAX, paxLabel);
        erc20Labels.put(CryptoCurrency.USDT, usdtLabel);
        erc20Labels.put(CryptoCurrency.DGLD, dgldLabel);
    }

    private HDWallet getWallet(String seedHex) throws Exception {
        return HDWalletFactory
            .restoreWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US,
                seedHex, "", 1);
    }

    private HDWallet getWallet1() throws Exception {
        //bicycle balcony prefer kid flower pole goose crouch century lady worry flavor
        return getWallet("15e23aa73d25994f1921a1256f93f72c");
    }

    private HDWallet getWallet2() throws Exception {
        //radar blur cabbage chef fix engine embark joy scheme fiction master release
        return getWallet("b0a30c7e93a58094d213c4c0aaba22da");
    }

    private HDWallet getWallet3() throws Exception {
        //search present horn clip convince wash motion want sea desert admit increase =jaxx
        return getWallet("c21541b615a2f9eee417b7c1e7740eb9");
    }

    @Test
    public void constructor1() throws Exception {

        HDWallet wallet = getWallet1();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        //Act
        subject = new EthereumWallet(wallet.getMasterKey(), allLabels);

        //Assert
        Assert.assertFalse(subject.hasSeen());
        Assert.assertEquals(0, subject.getTxNotes().size());

        Assert.assertEquals("60e2d382449758aab3866585dc69a946e3566bca0eea274b9073cb60da636133",
            EthereumAccount.Companion.deriveECKey(wallet.getMasterKey(), 0).getPrivateKeyAsHex());

        Assert.assertTrue(subject.getAccount().getAddress()
            .equalsIgnoreCase("0x14f2BD143692B14D170c34b2eE25EE5FC61e8570"));
    }

    @Test
    public void constructor2() throws Exception {

        HDWallet wallet = getWallet2();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        //Act
        subject = new EthereumWallet(wallet.getMasterKey(), allLabels);

        //Assert
        Assert.assertFalse(subject.hasSeen());
        Assert.assertEquals(0, subject.getTxNotes().size());

        Assert.assertEquals("b96e9ccb774cc33213cbcb2c69d3cdae17b0fe4888a1ccd343cbd1a17fd98b18",
            EthereumAccount.Companion.deriveECKey(wallet.getMasterKey(), 0).getPrivateKeyAsHex());

        Assert.assertTrue(subject.getAccount().getAddress()
            .equalsIgnoreCase("0xaC39b311DCEb2A4b2f5d8461c1cdaF756F4F7Ae9"));
    }

    @Test
    public void constructor3() throws Exception {

        HDWallet wallet = getWallet3();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        //Act
        subject = new EthereumWallet(wallet.getMasterKey(), allLabels);

        //Assert
        Assert.assertFalse(subject.hasSeen());
        Assert.assertEquals(0, subject.getTxNotes().size());

        Assert.assertEquals("6e1ae089604577d31f25617297e4f50ef1b06376d7b04419c7e82e2507927857",
            EthereumAccount.Companion.deriveECKey(wallet.getMasterKey(), 0).getPrivateKeyAsHex());

        Assert.assertTrue(subject.getAccount().getAddress()
            .equalsIgnoreCase("0x351e4184A9aBe6B71a2a7a71c2628c47cC861e51"));
    }

    @Test
    public void load() throws Exception {

        //Arrange
        HDWallet wallet = getWallet3();

        EthereumWallet eth = new EthereumWallet(wallet.getMasterKey(), allLabels);
        eth.setHasSeen(true);

        //Act
        subject = EthereumWallet.load(eth.toJson());

        //Assert
        Assert.assertTrue(subject.hasSeen());
        Assert.assertEquals(eth.toJson(), subject.toJson());
    }

    @Test
    public void loadEmptyWallet() throws Exception {

        //Arrange

        //Act
        subject = EthereumWallet.load(null);

        //Assert
        Assert.assertNull(subject);
    }

    @Test
    public void paxErc20Created() throws Exception {
        //Arrange
        HDWallet wallet = getWallet3();

        //Act
        subject = new EthereumWallet(wallet.getMasterKey(), allLabels);

        //Assert
        Erc20TokenData tokenData = subject.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME);
        Assert.assertNotNull(tokenData);
    }

    @Test
    public void paxErc20UpdateWallet() throws Exception {
        //Arrange
        String json = getStringFromResource(this, "eth_wallet_no_pax.json");
        subject = EthereumWallet.fromJson(json);

        //Act
        boolean wasUpdated = subject.updateErc20Tokens(erc20Labels);

        //Assert
        Assert.assertTrue(wasUpdated);
        Erc20TokenData tokenData = subject.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME);
        Assert.assertNotNull(tokenData);
    }

    @Test
    public void load_with_erc20_txNote() throws Exception {

        //Arrange
        HDWallet wallet = getWallet3();

        EthereumWallet eth = new EthereumWallet(wallet.getMasterKey(), allLabels);
        eth.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME)
            .putTxNote("one", "two");

        //Act
        subject = EthereumWallet.load(eth.toJson());

        //Assert
        Erc20TokenData tokenData = subject.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME);
        Assert.assertEquals(tokenData.getTxNotes().size(), 1);
        Assert.assertEquals(eth.toJson(), subject.toJson());
    }

    @Test
    public void signTransaction() throws Exception {
        HDWallet wallet = getWallet1();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        subject = new EthereumWallet(wallet.getMasterKey(), allLabels);
        RawTransaction tx = createEtherTransaction();

        //Act
        byte[] signTransaction = subject.getAccount().signTransaction(tx, EthereumAccount.Companion.deriveECKey(wallet.getMasterKey(), 0));

        //Assert
        Assert.assertEquals(
            "0xf85580010a840add5355887fffffffffffffff801ca032472aef4a52"
                + "fde34912307409bc3f9d4c0be12aaa09468da6967e9ceb80ea04a01b60edf2"
                + "1a188f14b8e00ddfe5fa5e1552be20a1a5462667501d047c4a5327ed"
            , Numeric.toHexString(signTransaction));
    }

    private static RawTransaction createEtherTransaction() {
        return RawTransaction.createEtherTransaction(
            BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, "0xadd5355",
            BigInteger.valueOf(Long.MAX_VALUE));
    }
}
