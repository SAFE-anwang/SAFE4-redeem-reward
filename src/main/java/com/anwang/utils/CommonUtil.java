package com.anwang.utils;

import org.web3j.crypto.Hash;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class CommonUtil {
    public static BigInteger COIN = new BigInteger("1000000000000000000");

    public static String getBin(String contractName) throws Exception {
        Path workPath = Paths.get(System.getProperty("user.dir"));
        Path binPath = workPath.resolve("build/mainnet/" + contractName + "/" + contractName + ".bin");
        Scanner scanner = new Scanner(binPath.toFile());
        String bin = "";
        while (scanner.hasNextLine()) {
            bin += scanner.nextLine();
        }
        scanner.close();
        return bin;
    }

    public static String getEventName(String topic) {
        String hash;
        hash = Hash.sha3String("RedeemMasterNode(string,address,uint256,address)");
        if (topic.equals(hash)) {
            return "RedeemMasterNode";
        }

        hash = Hash.sha3String("MNRegister(address,address,uint256,uint256,uint256)");
        if (topic.equals(hash)) {
            return "MNRegister";
        }

        hash = Hash.sha3String("MNAddressChanged(address,address)");
        if (topic.equals(hash)) {
            return "MNAddressChanged";
        }

        hash = Hash.sha3String("MNEnodeChanged(address,string,string)");
        if (topic.equals(hash)) {
            return "MNEnodeChanged";
        }

        return "";
    }
}
