/*****************************************************************************/
//	Function: M365 BLE message builder
//	Author:   Salvador Mart�n
//	Date:    12/02/2018
//
//	This library is free software; you can redistribute it and/or
//	modify it under the terms of the GNU Lesser General Public
//	License as published by the Free Software Foundation; either
//	version 2.1 of the License, or (at your option) any later version.
//
//	This library is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//	Lesser General Public License for more details.
//
//	I am not responsible of any damage caused by the misuse of this library.
//	Use at your own risk.
//
//	If you modify or use this, please don't delete my name and give me credits.
/*****************************************************************************/

package com.vorsk.M365.toolbox.util;

import java.util.ArrayList;
import java.util.List;

public class NbMessage {

    private int direction;
    private int rw;
    private int position;
    private List<Integer> payload;

    public NbMessage() {
        direction = 0;
        rw = 0;
        position = 0;
        payload = null;
    }

    public NbMessage setDirection(NbCommands drct) {
        direction = drct.getCommand();

        return this;
    }

    public NbMessage setRW(NbCommands readOrWrite) { // read or write
        rw = readOrWrite.getCommand();

        return this;
    }

    public NbMessage setPosition(int pos) {
        position = pos;

        return this;
    }

    public NbMessage setPayload(byte[] bytesToSend) {
        this.payload = new ArrayList<>();

        for (byte b : bytesToSend) {
            this.payload.add(new Integer(b));
        }

        return this;
    }

    public NbMessage setPayload(List<Integer> bytesToSend) {
        payload = bytesToSend;

        return this;
    }

    public NbMessage setPayload(int singleByteToSend) {
        payload = new ArrayList<>();
        payload.add(singleByteToSend);

        return this;
    }

    public byte[] build() {
        List<Integer> msg = new ArrayList<>(0);
        // header
        msg.add(0x55);
        msg.add(0xAA);

        //body
        msg.add(payload.size() + 2);
        msg.add(direction);
        msg.add(rw);
        msg.add(position);

        // body payload
        msg.addAll(payload);

        // checksum
        int checksum = calculateChecksum();
        msg.add((checksum & 0xff));
        msg.add(checksum >> 8);

        byte[] result = new byte[msg.size()];
        int i = 0;
        for (Integer d : msg) {
            result[i++] = d.byteValue();
        }
        return result;
    }

    private int calculateChecksum() {
        int checksum = 0;
        checksum += direction;
        checksum += rw;
        checksum += position;

        checksum += payload.size() + 2;

        for (int i : payload) {
            checksum += i;
        }

        checksum ^= 0xffff;

        return checksum;
    }
}