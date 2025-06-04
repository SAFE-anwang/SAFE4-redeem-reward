package com.anwang.contracts;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.web3j.crypto.Keys.getAddress;

public class ContractUtil {
    protected Web3j web3j;
    protected long chainId;
    protected String contractAddr;

    public ContractUtil(Web3j web3j, long chainId, String contractAddr) {
        this.web3j = web3j;
        this.chainId = chainId;
        this.contractAddr = contractAddr;
    }

    public BigInteger getNonce(String address) throws Exception {
        EthGetTransactionCount ethGetTransactionCount =
                web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                        .sendAsync()
                        .get();
        return ethGetTransactionCount.getTransactionCount();
    }

    public BigInteger getGasPrice() throws Exception {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        return ethGasPrice.getGasPrice();
    }

    public String getAddressFromPrivateKey(String privateKey) {
        BigInteger privKey = Numeric.toBigInt(privateKey);
        final BigInteger publicKey = Sign.publicKeyFromPrivate(privKey);
        return new Address(getAddress(publicKey)).toString();
    }

    private String calcContractAddress(String address, BigInteger nonce) {
        byte[] addressAsBytes = Numeric.hexStringToByteArray(address);
        byte[] hash =
                Hash.sha3(RlpEncoder.encode(
                        new RlpList(
                                RlpString.create(addressAsBytes),
                                RlpString.create((nonce)))));
        hash = Arrays.copyOfRange(hash, 12, hash.length);
        return Numeric.toHexString(hash);
    }

    public String deploy(String privateKey, String bin) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        String address = getAddressFromPrivateKey(privateKey);
        BigInteger nonce = getNonce(address);
        BigInteger gasPrice = getGasPrice();
        Transaction transaction = Transaction.createContractTransaction(address, nonce, gasPrice, bin);
        EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(transaction).send();
        if (ethEstimateGas.getError() != null) {
            throw new Exception(ethEstimateGas.getError().getMessage());
        }
        BigInteger gasLimit = ethEstimateGas.getAmountUsed().multiply(BigInteger.valueOf(6)).divide(BigInteger.valueOf(5));
        RawTransaction rawTransaction = RawTransaction.createContractTransaction(nonce, gasPrice, gasLimit, BigInteger.ZERO, bin);
        final String signedTransactionData = Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, chainId, credentials));
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedTransactionData).sendAsync().get();
        if (ethSendTransaction.hasError()) {
            throw new Exception(ethSendTransaction.getError().getMessage());
        }
        return calcContractAddress(address, nonce);
    }

    public String call(String privateKey, Function function) throws Exception {
        return call(privateKey, BigInteger.ZERO, function);
    }

    public String call(String privateKey, BigInteger value, Function function) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        String address = getAddressFromPrivateKey(privateKey);
        BigInteger nonce = getNonce(address);
        BigInteger gasPrice = getGasPrice();
        String data = FunctionEncoder.encode(function);
        EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(Transaction.createFunctionCallTransaction(address, nonce, gasPrice, BigInteger.ZERO, contractAddr, value, data)).send();
        if (ethEstimateGas.getError() != null) {
            throw new Exception(ethEstimateGas.getError().getMessage());
        }
        BigInteger gasLimit = ethEstimateGas.getAmountUsed().multiply(BigInteger.valueOf(6)).divide(BigInteger.valueOf(5));
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddr, value, data);
        final String signedTransactionData = Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, chainId, credentials));
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedTransactionData).sendAsync().get();
        if (ethSendTransaction.hasError()) {
            throw new Exception(ethSendTransaction.getError().getMessage());
        }
        if (function.getName().equals("submitTransaction")) {
            EthCall response = web3j.ethCall(Transaction.createEthCallTransaction(address, contractAddr, data), DefaultBlockParameterName.LATEST).send();
            List<Type> someTypes = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
            return ethSendTransaction.getTransactionHash() + "-" + someTypes.get(0).getValue().toString();
        } else {
            return ethSendTransaction.getTransactionHash();
        }
    }

    public List<Type> query(Function function) throws Exception {
        return query(function, Address.DEFAULT.getValue());
    }

    public List<Type> query(Function function, String from) throws Exception {
        EthCall response = web3j.ethCall(Transaction.createEthCallTransaction(from, contractAddr, FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST).sendAsync().get();
        if (response.getError() != null) {
            throw new Exception(response.getError().getMessage());
        }
        return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
    }

    public List<Type> queryFromPending(Function function) throws Exception {
        EthCall response = web3j.ethCall(Transaction.createEthCallTransaction(Address.DEFAULT.getValue(), contractAddr, FunctionEncoder.encode(function)), DefaultBlockParameterName.PENDING).sendAsync().get();
        if (response.getError() != null) {
            throw new Exception(response.getError().getMessage());
        }
        return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
    }
}
