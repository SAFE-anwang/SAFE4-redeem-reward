package com.anwang.contracts;

import com.anwang.types.masternode.MasterNodeInfo;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class MasterNodeStorage {

    private final ContractUtil contractUtil;

    public MasterNodeStorage(Web3j web3j, long chainId) {
        contractUtil = new ContractUtil(web3j, chainId, "0x0000000000000000000000000000000000001020");
    }

    public MasterNodeInfo getInfo(Address addr) throws Exception {
        Function function = new Function("getInfo", Collections.singletonList(addr), Collections.singletonList(new TypeReference<MasterNodeInfo>() {
        }));
        List<Type> someTypes = contractUtil.query(function);
        return (MasterNodeInfo) someTypes.get(0);
    }

    public MasterNodeInfo getInfoByID(BigInteger id) throws Exception {
        Function function = new Function("getInfoByID", Collections.singletonList(new Uint256(id)), Collections.singletonList(new TypeReference<MasterNodeInfo>() {
        }));
        List<Type> someTypes = contractUtil.query(function);
        return (MasterNodeInfo) someTypes.get(0);
    }
}
