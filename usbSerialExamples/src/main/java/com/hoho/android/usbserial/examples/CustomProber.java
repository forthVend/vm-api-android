package com.hoho.android.usbserial.examples;

import com.forth.vm.vmc.driver.CdcAcmSerialDriver;
import com.forth.vm.vmc.driver.ProbeTable;
import com.forth.vm.vmc.driver.UsbSerialProber;

/**
 * add devices here, that are not known to DefaultProber
 *
 * if the App should auto start for these devices, also
 * add IDs to app/src/main/res/xml/device_filter.xml
 */
class CustomProber {

    static UsbSerialProber getCustomProber() {
        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x16d0, 0x087e, CdcAcmSerialDriver.class); // e.g. Digispark CDC
        customTable.addProduct(0x16d0, 0x0bd7, CdcAcmSerialDriver.class); // e.g. Digispark CDC
        return new UsbSerialProber(customTable);
    }

}
