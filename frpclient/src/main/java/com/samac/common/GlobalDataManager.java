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

import java.util.ArrayList;
import java.util.List;

public class GlobalDataManager {
    public static List<PortUseBase> mappingList = new ArrayList<>();

    public static void setMappingList(List<String> list) {
        for (String s : list
        ) {
            try {
                PortMapping portMapping = new PortMapping();
                String[] a1 = s.split(";");
                portMapping.setMark(s);
                portMapping.setServerPort(Integer.parseInt(a1[0]));
                portMapping.setFlip(Boolean.parseBoolean(a1[2]));
                String[] a2 = a1[1].split(":");
                portMapping.setLocalAddress(a2[0]);
                portMapping.setLocalPort(Integer.parseInt(a2[1]));
                portMapping.setIndex((short) mappingList.size());
                mappingList.add(portMapping);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setUdpMappingList(List<String> list) {
        for (String s : list
        ) {
            try {
                UdpPortMapping portMapping = new UdpPortMapping();
                String[] a1 = s.split(";");
                portMapping.setMark(s);
                portMapping.setServerPort(Integer.parseInt(a1[0]));
                portMapping.setFlip(Boolean.parseBoolean(a1[2]));
                String[] a2 = a1[1].split(":");
                portMapping.setLocalAddress(a2[0]);
                portMapping.setLocalPort(Integer.parseInt(a2[1]));
                portMapping.setIndex((short) mappingList.size());
                mappingList.add(portMapping);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
