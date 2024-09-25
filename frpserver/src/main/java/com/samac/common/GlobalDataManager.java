package com.samac.common;

/*
 * Copyright (C) 2024 samac199999
 * All rights reserved.
 *
 * Contact: samac199999@gmail.com
 *
 * This code is licensed under the MIT License.
 * See the LICENSE file in the project root for more information.
 */

import com.samac.utils.RSAUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GlobalDataManager {
    public static List<PortUseBase> mappingList = new ArrayList<>();

    private static String rsaPublicKey;
    private static String rsaPrivateKey;

    static {
        try {
            Map<String, Object> keyMap = RSAUtil.genKeyPair();
            rsaPublicKey = RSAUtil.getPublicKey(keyMap);
            rsaPrivateKey = RSAUtil.getPrivateKey(keyMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public static String getRsaPrivateKey() {
        return rsaPrivateKey;
    }
}
