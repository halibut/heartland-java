package com.hps.integrator.tests;

import com.hps.integrator.entities.HpsDirectMarketData;
import com.hps.integrator.entities.HpsTransaction;
import com.hps.integrator.entities.HpsTransactionType;
import com.hps.integrator.entities.credit.*;
import com.hps.integrator.infrastructure.HpsException;
import com.hps.integrator.infrastructure.HpsInvalidRequestException;
import com.hps.integrator.infrastructure.HpsIssuerException;
import com.hps.integrator.infrastructure.HpsIssuerExceptionCodes;
import com.hps.integrator.infrastructure.emums.TaxTypeType;
import com.hps.integrator.services.HpsCreditService;
import com.hps.integrator.services.HpsServicesConfig;
import com.hps.integrator.tests.testdata.TestCardHolders;
import com.hps.integrator.tests.testdata.TestCreditCards;
import com.hps.integrator.tests.testdata.TestServicesConfig;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class GeneralTests {

    @Test(expected = HpsInvalidRequestException.class)
    public void Charge_WhenAmountIsLessThanZero_ShouldThrowInvalidRequest() throws HpsException {
        BigDecimal amount = new BigDecimal("-5");
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        service.charge(amount, "usd", TestCreditCards.validVisa(), TestCardHolders.validCardHolder(), true);
    }

//    @Test(expected = HpsInvalidRequestException.class)
//    public void Charge_WhenConfigIncomplete_ShouldThrowHpsInvalidRequest() throws HpsException {
//        BigDecimal amount = new BigDecimal("50");
//        HpsCreditService service = new HpsCreditService(TestServicesConfig.incompleteServicesConfig());
//        service.charge(amount, "usd", TestCreditCards.validVisa(), TestCardHolders.validCardHolder(), true);
//    }
//
//    @Test(expected = HpsAuthenticationException.class)
//    public void Charge_WhenConfigInvalid_ShouldThrowAuthenticationException() throws HpsException {
//        BigDecimal amount = new BigDecimal("50");
//        HpsCreditService service = new HpsCreditService(TestServicesConfig.badLicenseId());
//        service.charge(amount, "usd", TestCreditCards.validVisa(), TestCardHolders.validCardHolder(), true);
//    }

//    @Test(expected = HpsAuthenticationException.class)
//    public void Charge_WhenConfigContainsBothTokenAndLegacy_ShouldThrowAuthenticationException() throws HpsException {
//        BigDecimal amount = new BigDecimal("50");
//        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
//        service.charge(amount, "usd", TestCreditCards.validVisa(), TestCardHolders.validCardHolder(), true);
//    }

    @Test(expected = HpsInvalidRequestException.class)
    public void Charge_WhenCurrencyIsEmpty_ShouldThrowArgumentNull() throws HpsException {
        BigDecimal amount = new BigDecimal("50");
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        service.charge(amount, "", TestCreditCards.validVisa(), TestCardHolders.validCardHolder(), true);
    }

    @Test(expected = HpsInvalidRequestException.class)
    public void Charge_WhenCurrencyIsNotUsd_ShouldThrowArgumentException() throws HpsException {
        BigDecimal amount = new BigDecimal("50");
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        service.charge(amount, "eur", TestCreditCards.validVisa(), TestCardHolders.validCardHolder(), true);
    }

    @Test(expected = HpsException.class)
    public void Charge_WhenCardNumberIsInvalid_ShouldThrowHpsException() throws HpsException {
        try {
            BigDecimal amount = new BigDecimal("50");
            HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
            service.charge(amount, "usd", TestCreditCards.invalidCard(), TestCardHolders.validCardHolder(), true);
        } catch (HpsIssuerException e) {
            assertEquals(HpsIssuerExceptionCodes.IncorrectNumber, e.getCode());
            throw e;
        }
    }

    @Test
    public void List_WhenConfigValid_ShouldListTransactions() throws HpsException {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -10);

        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig(), true);
        HpsReportTransactionSummary[] items = service.list(start.getTime(), new Date());
        assertNotNull(items);
    }

    @Test
    public void List_WhenConfigValid_ShouldListCharges() throws HpsException {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -10);

        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        HpsReportTransactionSummary[] items = service.list(start.getTime(), Calendar.getInstance().getTime(), HpsTransactionType.Capture);
        assertNotNull(items);
    }

    @Test
    public void GetFirst_WhenConfigValid_ShouldGetTheFirstCharge() throws HpsException {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -10);

        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig(), true);
        HpsReportTransactionSummary[] items = service.list(start.getTime(), Calendar.getInstance().getTime());
        assertFalse(items.length <= 0);

        HpsReportTransactionSummary item = items[0];
        HpsReportTransactionDetails charge = service.get(item.getTransactionID());
        assertNotNull(charge);
    }

    @Test
    public void Edit_WhenConfigValid_ShouldEditACharge() throws HpsException {
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        HpsCharge charge = service.charge(new BigDecimal("50"), "usd", TestCreditCards.validAmex(), TestCardHolders.validCardHolder(), true);
        assertEquals("00", charge.getResponseCode());

        HpsTransaction edit = service.edit(charge.getTransactionID(), new BigDecimal("52"));
        assertEquals("00", edit.getResponseCode());
    }

    @Test
    public void Capture_WhenConfigValid_ShouldUpdateGratuity() throws HpsException {
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        HpsAuthorization auth = service.authorize(new BigDecimal("50"), "usd", TestCreditCards.validAmex(), TestCardHolders.validCardHolder(), true);
        assertEquals("00", auth.getResponseCode());

        HpsTransaction capture = service.captureTxn(auth.getTransactionID(), null, new BigDecimal("25"), null);
        assertEquals("00", capture.getResponseCode());
    }

    @Test
    public void Charge_WhenMarketData_ShouldReturnOk() throws HpsException {
        HpsDirectMarketData directMarketData = new HpsDirectMarketData("12345", 10, 8);
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        HpsCharge charge = service.charge(
                new BigDecimal("50"),
                "usd",
                TestCreditCards.validAmex(),
                TestCardHolders.validCardHolder(),
                true, false, null, null, directMarketData, false, false, false
        );

        assertEquals("00", charge.getResponseCode());
    }

    @Test
    public void Capture_WhenMarketData_ShouldReturnOk() throws HpsException {
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        HpsAuthorization auth = service.authorize(new BigDecimal("50"), "usd", TestCreditCards.validAmex(), TestCardHolders.validCardHolder(), true);
        assertEquals("00", auth.getResponseCode());

        HpsDirectMarketData directMarketData = new HpsDirectMarketData("12345", 10, 8);
        HpsTransaction capture = service.captureTxn(auth.getTransactionID(), null, new BigDecimal("25"), directMarketData);
        assertEquals("00", capture.getResponseCode());
    }

    @Test
    public void Charge_WhenMarketDataDefault_ShouldReturnOk() throws HpsException {
        HpsDirectMarketData directMarketData = new HpsDirectMarketData();
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        HpsCharge charge = service.charge(new BigDecimal("50"), "usd", TestCreditCards.validAmex(), TestCardHolders.validCardHolder(),
                true, false, null, null, directMarketData, false, false, false);

        assertEquals("00", charge.getResponseCode());
    }

    @Test
    public void Capture_WhenMarketDataDefault_ShouldReturnOk() throws HpsException {
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig());
        HpsAuthorization auth = service.authorize(new BigDecimal("50"), "usd", TestCreditCards.validAmex(), TestCardHolders.validCardHolder(), true);
        assertEquals("00", auth.getResponseCode());

        HpsDirectMarketData directMarketData = new HpsDirectMarketData();
        HpsTransaction capture = service.captureTxn(auth.getTransactionID(), null, new BigDecimal("25"), directMarketData);
        assertEquals("00", capture.getResponseCode());
    }

    @Test
    public void CpcEdit_WhenCpcData_ShouldExecCpcEdit() throws HpsException {
        HpsDirectMarketData directMarketData = new HpsDirectMarketData();
        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig(), true);
        HpsCharge charge = service.charge(new BigDecimal("50"), "usd", TestCreditCards.validAmex(), TestCardHolders.validCardHolder(),
                true, false, null, null, directMarketData, true, false, false);

        assertEquals("00", charge.getResponseCode());

        HpsCpcData cpcData = new HpsCpcData();
        cpcData.setCardHolderPoNumber("12345");
        cpcData.setTaxType(TaxTypeType.SalesTax);
        cpcData.setTaxAmount(new BigDecimal(1000));

        HpsTransaction cpcEdit = service.cpcEdit(charge.getTransactionID(), cpcData);

        assertEquals("00", cpcEdit.getResponseCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void CpcEdit_WhenPoNumberInvalid_ShouldThrowException() throws HpsException {
        HpsCpcData cpcData = new HpsCpcData();
        cpcData.setCardHolderPoNumber("1234567891011121314");
    }

    @Test(expected = IllegalArgumentException.class)
    public void CpcEdit_WhenCpcTaxInvalid_ShouldThrowException() throws HpsException {
        HpsCpcData cpcData = new HpsCpcData();
        cpcData.setTaxAmount(new BigDecimal(1000.001));
    }

    @Test
    public void Charge_CvvWithLeadingZero() throws HpsException {
        HpsCreditCard card = new HpsCreditCard();
        card.setNumber("4111111111111111");
        card.setExpMonth(12);
        card.setExpYear(2025);
        card.setCvv("012");

        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig(), true);
        HpsCharge response = service.charge(new BigDecimal("15.15"), "usd", card, null, false);
        assertNotNull(response);
        assertEquals("00", response.getResponseCode());
    }

    @Test
    public void List_WhenSecretKeyNeedsTRimming_ShouldListTransactions() throws HpsException {
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_MONTH, -10);

        // add some spaces to the secret API key.
        HpsServicesConfig config = (HpsServicesConfig) TestServicesConfig.validServicesConfig();
        config.setSecretAPIKey("   skapi_cert_MYl2AQAowiQAbLp5JesGKh7QFkcizOP2jcX9BrEMqQ   ");

        HpsCreditService service = new HpsCreditService(TestServicesConfig.validServicesConfig(), true);
        HpsReportTransactionSummary[] items = service.list(start.getTime(), new Date());
        assertNotNull(items);
    }
}
