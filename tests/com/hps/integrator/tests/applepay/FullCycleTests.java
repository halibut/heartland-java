package com.hps.integrator.tests.applepay;

import com.hps.integrator.entities.HpsTransaction;
import com.hps.integrator.entities.credit.HpsAuthorization;
import com.hps.integrator.entities.credit.HpsCharge;
import com.hps.integrator.infrastructure.HpsException;
import com.hps.integrator.services.HpsCreditService;
import com.hps.integrator.tests.testdata.TestCardHolders;
import com.hps.integrator.tests.testdata.TestData;
import com.hps.integrator.tests.testdata.TestServicesConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FullCycleTests {
//    DecryptService decryptService;
    HpsCreditService creditService;

    public FullCycleTests() throws HpsException {
        System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
//        this.decryptService = new DecryptService(TestData.PRIVATE_KEY_FILE_PATH, TestData.PRIVATE_KEY_PASSWORD);
        this.creditService = new HpsCreditService(TestServicesConfig.validCertMultiUseConfig(), true);
    }

    @Test
    public void visa_3DS_Decrypted_ShouldChargeOK() throws HpsException {
        HpsCharge response = creditService.charge(TestData.VisaPaymentData(), TestCardHolders.validCardHolder(), true);
        assertNotNull(response);
        assertEquals(response.getResponseCode(), "00");
        System.out.println(response.getTransactionID());
    }

    @Test
    public void visa_3DS_Decrypted_ShouldAuthOK() throws HpsException {
        HpsAuthorization response = creditService.authorize(TestData.VisaPaymentData(), TestCardHolders.validCardHolder(), true);
        assertNotNull(response);
        assertEquals(response.getResponseCode(), "00");

        HpsTransaction captureResponse = creditService.captureTxn(response.getTransactionID());
        assertNotNull(captureResponse);
    }

    @Test
    public void amex_3DS_Decrypted_ShouldChargeOK() throws HpsException {
        HpsCharge response = creditService.charge(TestData.AmexPaymentData(), TestCardHolders.validCardHolder(), true);
        assertNotNull(response);
        assertEquals(response.getResponseCode(), "00");
    }

    @Test
    public void amex_3DS_Decrypted_ShouldAuthOK() throws HpsException {
        HpsAuthorization response = creditService.authorize(TestData.AmexPaymentData(), TestCardHolders.validCardHolder(), true);
        assertNotNull(response);
        assertEquals(response.getResponseCode(), "00");

        HpsTransaction captureResponse = creditService.captureTxn(response.getTransactionID());
        assertNotNull(captureResponse);
    }
}
