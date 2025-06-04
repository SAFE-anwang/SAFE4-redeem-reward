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

    public MasterNodeSubsidy(Web3j web3j, long chainId) {
        contractUtil = new ContractUtil(web3j, chainId, "0xD5047b82ACC65c30Eefe4C3cDb652C1Bc80d9fEF");
    }

    public String subsidy(String privateKey, BigInteger value, List<BigInteger> mnIDs, List<Address> creators, List<BigInteger> amounts, List<BigInteger> lockDays) throws Exception {
        Function function = new Function(
                "subsidy",
                Arrays.asList(
                        new DynamicArray<>(Uint256.class, Utils.typeMap(mnIDs, Uint256.class)),
                        new DynamicArray<>(Address.class, creators),
                        new DynamicArray<>(Uint256.class, Utils.typeMap(amounts, Uint256.class)),
                        new DynamicArray<>(Uint256.class, Utils.typeMap(lockDays, Uint256.class))),
                Collections.emptyList());
        return contractUtil.call(privateKey, value, function);
    }
}
