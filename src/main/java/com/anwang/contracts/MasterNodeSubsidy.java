package com.anwang.contracts;

import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MasterNodeSubsidy {

    private final ContractUtil contractUtil;

    public final static String contractAddr = "0xba922751E3f7F0B0616E950b17665D33fdD18a06";

    public MasterNodeSubsidy(Web3j web3j, long chainId) {
        contractUtil = new ContractUtil(web3j, chainId, contractAddr);
    }

    public String subsidy(String privateKey, BigInteger value, List<BigInteger> mnIDs, List<Address> creators, List<BigInteger> amounts) throws Exception {
        Function function = new Function(
                "subsidy",
                Arrays.asList(
                        new DynamicArray<>(Uint256.class, Utils.typeMap(mnIDs, Uint256.class)),
                        new DynamicArray<>(Address.class, creators),
                        new DynamicArray<>(Uint256.class, Utils.typeMap(amounts, Uint256.class))),
                Collections.emptyList());
        return contractUtil.call(privateKey, value, function);
    }
}
