package info.blockchain.wallet.multiaddress;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.blockexplorer.FilterType;
import info.blockchain.api.data.AddressSummary;
import info.blockchain.api.data.Input;
import info.blockchain.api.data.MultiAddress;
import info.blockchain.api.data.Output;
import info.blockchain.api.data.Transaction;
import info.blockchain.api.data.Xpub;
import info.blockchain.wallet.bip44.HDChain;
import info.blockchain.wallet.exceptions.ApiException;
import info.blockchain.wallet.multiaddress.TransactionSummary.TransactionType;
import info.blockchain.wallet.payload.data.AddressLabel;
import retrofit2.Call;
import retrofit2.Response;

public class MultiAddressFactory {

    private BlockExplorer blockExplorer;

    private HashMap<String, Integer> nextReceiveAddressMap;
    private HashMap<String, Integer> nextChangeAddressMap;

    //Field for testing if address belongs to us - Quicker than derivation
    private HashMap<String, String> addressToXpubMap;

    public static final String ADDRESS_DECODE_ERROR = "[--address_decode_error--]";

    public MultiAddressFactory(BlockExplorer blockExplorer) {
        this.blockExplorer = blockExplorer;
        this.addressToXpubMap = new HashMap<>();
        this.nextReceiveAddressMap = new HashMap<>();
        this.nextChangeAddressMap = new HashMap<>();
    }

    public String getXpubFromAddress(String address) {
        return addressToXpubMap.get(address);
    }

    private MultiAddress getMultiAddress(List<String> allActive, String onlyShow, int limit,
                                         int offset) throws IOException, ApiException {

        Response<MultiAddress> call = getMultiAddress(allActive, limit, offset, onlyShow).execute();

        if (call.isSuccessful()) {
            return call.body();
        } else {
            throw new ApiException(call.errorBody().string());
        }
    }

    protected Call<MultiAddress> getMultiAddress(List<String> allActive, int limit, int offset,
                                                 String context) {
        return getBlockExplorer()
            .getMultiAddress("btc", allActive, context, FilterType.RemoveUnspendable, limit,
                offset);
    }

    BlockExplorer getBlockExplorer() {
        return blockExplorer;
    }

    /**
     * @param all          A list of all xpubs and imported addresses whose transactions are to
     *                     be retrieved from API.
     * @param activeImported (Hacky! Needs a rethink) Only set this when fetching a transaction list
     *                     for imported addresses, otherwise set as Null.
     *                     A list of all active imorted addresses. Used for 'Imported address' transaction list.
     * @param onlyShow     Xpub or imported address. Used to fetch transaction only relating to this
     *                     address. Set as Null for a consolidated list like 'All Accounts' or 'Imported'.
     * @param limit        Maximum amount of transactions fetched
     * @param offset       Page offset
     */
    public List<TransactionSummary> getAccountTransactions(
        List<String> all,
        List<String> activeImported,
        String onlyShow,
        int limit,
        int offset,
        int startingBlockHeight) throws IOException, ApiException {

        MultiAddress multiAddress = getMultiAddress(all, onlyShow, limit, offset);
        if (multiAddress == null || multiAddress.getTxs() == null) {
            return new ArrayList<>();
        }

        return summarize(all, multiAddress, activeImported, startingBlockHeight);
    }

    public int getNextChangeAddressIndex(String xpub) {
        if (!nextChangeAddressMap.containsKey(xpub)) {
            return 0;
        }

        return nextChangeAddressMap.get(xpub);
    }

    public int getNextReceiveAddressIndex(String xpub, List<AddressLabel> reservedAddresses) {
        if (!nextReceiveAddressMap.containsKey(xpub)) {
            return 0;
        }

        Integer receiveIndex = nextReceiveAddressMap.get(xpub);

        //Skip reserved addresses
        for (AddressLabel reservedAddress : reservedAddresses) {
            if (reservedAddress.getIndex() == receiveIndex) {
                receiveIndex++;
            }
        }

        return receiveIndex;
    }

    public void sort(ArrayList<Transaction> txs) {
        if (txs == null) {
            return;
        }
        Collections.sort(txs, new TxMostRecentDateComparator());
    }

    public boolean isOwnHDAddress(String address) {
        return addressToXpubMap.containsKey(address);
    }

    public void incrementNextReceiveAddress(String xpub, List<AddressLabel> reservedAddresses) {

        int receiveIndex = getNextReceiveAddressIndex(xpub, reservedAddresses);
        receiveIndex++;

        nextReceiveAddressMap.put(xpub, receiveIndex);
    }

    public void incrementNextChangeAddress(String xpub) {
        int index = getNextChangeAddressIndex(xpub);
        index++;

        nextChangeAddressMap.put(xpub, index);
    }

    public class TxMostRecentDateComparator implements Comparator<Transaction> {

        public int compare(Transaction t1, Transaction t2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            int ret;

            //noinspection UseCompareMethod -> Long.compare() can break on specific Android versions
            if (t1.getTime() > t2.getTime()) {
                ret = BEFORE;
            } else if (t1.getTime() < t2.getTime()) {
                ret = AFTER;
            } else {
                ret = EQUAL;
            }

            return ret;
        }
    }

    public List<TransactionSummary> summarize(List<String> ownAddressesAndXpubs,
                                              MultiAddress multiAddress,
                                              List<String> imported,
                                              int startingBlockHeight) {

        List<TransactionSummary> summaryList = new ArrayList<>();

        //Set next address indexes
        for (AddressSummary address : multiAddress.getAddresses()) {
            nextReceiveAddressMap.put(address.getAddress(), address.getAccountIndex());
            nextChangeAddressMap.put(address.getAddress(), address.getChangeIndex());
        }

        List<Transaction> txs = multiAddress.getTxs();
        if (txs == null) {
            //Address might not contain transactions
            return summaryList;
        }

        for (Transaction tx : txs) {

            if (tx.getBlockHeight() != 0 && tx.getBlockHeight() < startingBlockHeight) {
                //Filter out txs before blockHeight (mainly for BCH)
                //Block height will be 0 until included in a block
                continue;
            }

            boolean isImported = false;

            TransactionSummary txSummary = new TransactionSummary();
            txSummary.inputsMap = new HashMap<>();
            txSummary.outputsMap = new HashMap<>();

            //Map which address belongs to which xpub.
            txSummary.inputsXpubMap = new HashMap<>();
            txSummary.outputsXpubMap = new HashMap<>();

            if (tx.getResult().add(tx.getFee()).signum() == 0) {
                txSummary.setTransactionType(TransactionType.TRANSFERRED);
            } else if (tx.getResult().signum() > 0) {
                txSummary.setTransactionType(TransactionType.RECEIVED);
            } else {
                txSummary.setTransactionType(TransactionType.SENT);
            }

            //Inputs
            String inputAddr;
            BigInteger inputValue;
            for (Input input : tx.getInputs()) {

                Output prevOut = input.getPrevOut();
                if (prevOut != null) {

                    inputAddr = prevOut.getAddr();
                    inputValue = prevOut.getValue();
                    if (inputAddr != null) {

                        //Transaction from HD account
                        Xpub xpubBody = prevOut.getXpub();
                        if (xpubBody != null) {
                            //xpubBody will only show if it belongs to our account
                            //inputAddr belongs to our own account - add it, it's a transfer/send
                            ownAddressesAndXpubs.add(inputAddr);
                            txSummary.inputsXpubMap.put(inputAddr, xpubBody.getM());
                        }

                        //Flag as imported address
                        if (imported != null && imported.contains(inputAddr)) {
                            isImported = true;
                        }

                        //Keep track of inputs
                        BigInteger existingBalance = txSummary.inputsMap.containsKey(inputAddr)
                            ? txSummary.inputsMap.get(inputAddr) : BigInteger.ZERO;
                        txSummary.inputsMap.put(inputAddr, existingBalance.add(inputValue));

                    } else {
                        //No input address available
                        txSummary.inputsMap.put(ADDRESS_DECODE_ERROR, inputValue);
                    }

                } else {
                    //Newly generated coin
                }
            }

            HashMap<String, BigInteger> changeMap = new HashMap<>();
            String outputAddr;
            BigInteger outputValue;
            for (Output output : tx.getOut()) {

                outputAddr = output.getAddr();
                outputValue = output.getValue();
                if (outputAddr != null) {

                    Xpub xpubBody = output.getXpub();
                    if (xpubBody != null) {

                        //inputAddr belongs to our own account - add it
                        ownAddressesAndXpubs.add(outputAddr);
                        if (xpubBody.getPath().startsWith("M/" + HDChain.RECEIVE_CHAIN + "/")) {
                            BigInteger existingBalance = txSummary.outputsMap.containsKey(outputAddr)
                                ? txSummary.outputsMap.get(outputAddr) : BigInteger.ZERO;
                            txSummary.outputsMap.put(outputAddr, existingBalance.add(outputValue));
                            txSummary.outputsXpubMap.put(outputAddr, xpubBody.getM());
                        } else {
                            //Change
                            changeMap.put(outputAddr, outputValue);
                        }

                    } else {
                        //If we own this address and it's not change coming back, it's a transfer
                        if (ownAddressesAndXpubs.contains(outputAddr)
                            && !txSummary.inputsMap.keySet().contains(outputAddr)) {

                            if (txSummary.getTransactionType() == TransactionType.SENT) {
                                txSummary.setTransactionType(TransactionType.TRANSFERRED);
                            }

                            //Don't add change coming back
                            if (!txSummary.inputsMap.containsKey(outputAddr)) {
                                BigInteger existingBalance = txSummary.outputsMap.containsKey(outputAddr)
                                    ? txSummary.outputsMap.get(outputAddr) : BigInteger.ZERO;
                                txSummary.outputsMap.put(outputAddr, existingBalance.add(outputValue));
                            } else {
                                changeMap.put(outputAddr, outputValue);
                            }

                        } else if (txSummary.inputsMap.keySet().contains(outputAddr)) {
                            //Our change
                            changeMap.put(outputAddr, outputValue);
                        } else {
                            //Address does not belong to us
                            BigInteger existingBalance = txSummary.outputsMap.containsKey(outputAddr)
                                ? txSummary.outputsMap.get(outputAddr) : BigInteger.ZERO;
                            txSummary.outputsMap.put(outputAddr, existingBalance.add(outputValue));
                        }
                    }

                    //Flag as imported address
                    if (imported != null && imported.contains(outputAddr)) {
                        isImported = true;
                    }
                } else {
                    //No output address available
                    txSummary.outputsMap.put(ADDRESS_DECODE_ERROR, outputValue);
                }
            }

            //If we are filtering for imported and nothing found
            if (imported != null && !isImported) {
                continue;
            }

            //Remove input addresses not ours
            filterOwnedAddresses(
                ownAddressesAndXpubs,
                txSummary.inputsMap,
                txSummary.outputsMap,
                txSummary.getTransactionType());

            txSummary.setHash(tx.getHash());
            txSummary.setTime(tx.getTime());
            txSummary.setDoubleSpend(tx.isDoubleSpend());
            txSummary.setFee(tx.getFee());

            if (txSummary.getTransactionType() == TransactionType.RECEIVED) {
                BigInteger total = calculateTotalReceived(txSummary.outputsMap);
                txSummary.setTotal(total);
            } else {
                BigInteger total = calculateTotalSent(
                    txSummary.inputsMap,
                    changeMap,
                    tx.getFee(),
                    txSummary.getTransactionType());
                txSummary.setTotal(total);
            }

            //Set confirmations
            long latestBlock = multiAddress.getInfo().getLatestBlock().getHeight();
            long txBlockHeight = tx.getBlockHeight();
            if (latestBlock > 0 && txBlockHeight > 0) {
                txSummary.setConfirmations((int) ((latestBlock - txBlockHeight) + 1));
            } else {
                txSummary.setConfirmations(0);
            }

            addressToXpubMap.putAll(txSummary.getInputsXpubMap());
            addressToXpubMap.putAll(txSummary.getOutputsXpubMap());

            summaryList.add(txSummary);
        }

        return summaryList;
    }

    private void filterOwnedAddresses(List<String> ownAddressesAndXpubs,
                                      HashMap<String, BigInteger> inputsMap,
                                      HashMap<String, BigInteger> outputsMap, TransactionType transactionType) {

        Iterator<Entry<String, BigInteger>> iterator = inputsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, BigInteger> item = iterator.next();
            if (!ownAddressesAndXpubs.contains(item.getKey()) && transactionType.equals(TransactionType.SENT)) {
                iterator.remove();
            }
        }

        iterator = outputsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, BigInteger> item = iterator.next();
            if (!ownAddressesAndXpubs.contains(item.getKey()) && transactionType.equals(TransactionType.RECEIVED)) {
                iterator.remove();
            }
        }
    }

    private BigInteger calculateTotalReceived(HashMap<String, BigInteger> outputsMap) {

        BigInteger total = BigInteger.ZERO;

        for (BigInteger output : outputsMap.values()) {
            total = total.add(output);
        }

        return total;
    }

    private BigInteger calculateTotalSent(HashMap<String, BigInteger> inputsMap,
                                          HashMap<String, BigInteger> changeMap,
                                          BigInteger fee, TransactionType transactionType) {

        BigInteger total = BigInteger.ZERO;

        for (BigInteger input : inputsMap.values()) {
            total = total.add(input);
        }

        for (BigInteger change : changeMap.values()) {
            total = total.subtract(change);
        }

        if (transactionType == TransactionType.TRANSFERRED) {
            total = total.subtract(fee);
        }

        return total;
    }

}