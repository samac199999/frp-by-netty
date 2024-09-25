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

public interface PortUseBase {

    public short getIndex();

    public int getPort();

    public byte getType();

    public boolean isFlip();

}
