package com.anwang.types;

import org.web3j.abi.datatypes.Address;

import java.math.BigInteger;

public class MnData {
    public BigInteger id;
    public Address addr;
    public Address creator;
    public BigInteger createHeight;
    public BigInteger type;
    public String enode;
    public BigInteger startHeight;
    public Double onlineDay;
    public BigInteger rewardAmount;
    public int state;
    public String subsidyTxid;

    public MnData(long id, Address addr, Address creator, BigInteger createHeight) {
        this.id = BigInteger.valueOf(id);
        this.addr = addr;
        this.creator = creator;
        this.createHeight = createHeight;
        this.type = BigInteger.ZERO;
        this.enode = "";
        this.startHeight = BigInteger.ZERO;
        this.onlineDay = 0.0;
        this.rewardAmount = BigInteger.ZERO;
        this.state = 0;
        this.subsidyTxid = "";
    }
}
