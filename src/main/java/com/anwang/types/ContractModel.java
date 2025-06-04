package com.anwang.types;

import com.anwang.contracts.MasterNodeStorage;
import com.anwang.contracts.MasterNodeSubsidy;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

public class ContractModel {
    private static volatile ContractModel instance;
    private long chainId;
    private String url;
    private Web3j web3j;
    private MasterNodeStorage masterNodeStorage;
    private MasterNodeSubsidy masterNodeSubsidy;

    private ContractModel() {
        init(6666665, "http://47.107.89.150:8545");
    }

    public static ContractModel getInstance() {
        if (instance == null) {
            synchronized (ContractModel.class) {
                if (instance == null) {
                    instance = new ContractModel();
                }
            }
        }
        return instance;
    }

    public String init(long chainId, String url) {
        Web3j newWeb3j = Web3j.build(new HttpService(url));
        try {
            Web3ClientVersion clientVersion = newWeb3j.web3ClientVersion().send();
            if (clientVersion.hasError()) {
                return clientVersion.getError().getMessage();
            }
        } catch (Exception e) {
            return e.getMessage();
        }

        this.chainId = chainId;
        this.url = url;
        this.web3j = newWeb3j;
        this.masterNodeStorage = new MasterNodeStorage(web3j, chainId);
        this.masterNodeSubsidy = new MasterNodeSubsidy(web3j, chainId);
        return null;
    }

    public Web3j getWeb3j() {
        return web3j;
    }

    public MasterNodeStorage getMasterNodeStorage() {
        return masterNodeStorage;
    }

    public MasterNodeSubsidy getMasterNodeSubsidy() {
        return masterNodeSubsidy;
    }
}
