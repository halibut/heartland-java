package com.hps.integrator.services;

import com.hps.integrator.abstractions.IHpsServicesConfig;
import com.hps.integrator.applepay.ecv1.PaymentData;
import com.hps.integrator.applepay.ecv1.PaymentData3DS;
import com.hps.integrator.entities.*;
import com.hps.integrator.entities.credit.*;
import com.hps.integrator.infrastructure.*;
import com.hps.integrator.infrastructure.validation.HpsGatewayResponseValidation;
import com.hps.integrator.infrastructure.validation.HpsInputValidation;
import com.hps.integrator.infrastructure.validation.HpsIssuerResponseValidation;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HpsCreditService extends HpsSoapGatewayService {
    HpsTransactionType filterBy;

    public HpsCreditService() throws HpsException {
        super();
    }

    public HpsCreditService(IHpsServicesConfig servicesConfig) throws HpsException {
        super(servicesConfig);
    }

    public HpsCreditService(IHpsServicesConfig servicesConfig, boolean enableLogging) throws HpsException {
        super(servicesConfig, enableLogging);
    }

    public HpsReportTransactionDetails get(Integer transactionId) throws HpsException {
        if(transactionId <= 0) {
            throw new HpsInvalidRequestException("Invalid transaction ID.");
        }

        Element transaction = Et.element("ReportTxnDetail");
        Et.subElement(transaction, "TxnId").text(transactionId.toString());

        ElementTree response = submitTransaction(transaction);
        return new HpsReportTransactionDetails().fromElementTree(response);
    }

    public HpsReportTransactionSummary[] list(Date start, Date end) throws HpsException {
        return this.list(start, end, null);
    }

    public HpsReportTransactionSummary[] list(Date start, Date end, HpsTransactionType filterBy) throws HpsException {
        HpsInputValidation.checkDateNotFuture(start, "Start Date");
        HpsInputValidation.checkDateNotFuture(end, "End Date");
        this.filterBy = filterBy;

        Element transaction = Et.element("ReportActivity");
        Et.subElement(transaction, "RptStartUtcDT").text(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(start));
        Et.subElement(transaction, "RptEndUtcDT").text(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(end));

        ElementTree response = submitTransaction(transaction);
        return new HpsReportTransactionSummary().fromElementTree(response, filterBy);
    }

    public HpsCharge charge(BigDecimal amount, String currency, HpsCreditCard card, HpsCardHolder cardHolder, boolean allowDuplicates) throws HpsException {
        return charge(amount, currency, card, cardHolder, allowDuplicates, false, null, null, null, false, false, false);
    }

    public HpsCharge charge(BigDecimal amount, String currency, String token, HpsCardHolder cardHolder, boolean allowDuplicates) throws HpsException {
        return charge(amount, currency, token, cardHolder, allowDuplicates, false, null, null, null, false, false, false);
    }

    public HpsCharge charge(PaymentData paymentData, HpsCardHolder cardHolder, boolean allowDuplicates) throws HpsException {
        return charge(paymentData, cardHolder, allowDuplicates, false, null, null, null);
    }

    /**
     * The <b>credit sale</b> transaction authorizes a sale purchased with a credit card. The authorization in place
     * in the current open batch (should auto-close for e-commerce transactions). If a batch is not open, this
     * transaction will create an open batch.
     *
     * @param amount               The amount (in dollars).
     * @param currency             The currency (3-letter ISO code for currency).
     * @param card                 The credit card information.
     * @param cardHolder           The card holder information (used for AVS).
     * @param allowDuplicates      Indicates whether to allow duplicate transactions.
     * @param requestMultiUseToken Request a multi-use token.
     * @param descriptor           A description that is concatenated to a configurable merchant DBA name. The resulting string
     *                             is sent to the card issuer for the Merchant Name.
     * @param details              The transaction details.
     * @return The HPS Charge
     * @throws HpsException
     */
    public HpsCharge charge(BigDecimal amount, String currency, HpsCreditCard card, HpsCardHolder cardHolder, boolean allowDuplicates,
                            boolean requestMultiUseToken, String descriptor, HpsTransactionDetails details, HpsDirectMarketData directMarketData,
                            boolean cpcRequest, boolean cardPresent, boolean readerPresent) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditSale");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "AllowDup").text(allowDuplicates ? "Y" : "N");
        Et.subElement(block1, "Amt").text(amount.toString());

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        if(card != null) {
            cardData.append(hydrateCardManualEntry(card, cardPresent, readerPresent));
            if(card.getEncryptionData() != null)
                cardData.append(hydrateEncryptionData(card.getEncryptionData()));
        }
        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");

        if(cpcRequest) Et.subElement(block1, "CPCReq").text("Y");
        if(descriptor != null)
            Et.subElement(block1, "TxnDescriptor").text(descriptor);
        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));
        if(directMarketData != null)
            block1.append(hydrateDirectMarketData(directMarketData));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsCharge().fromElementTree(response);
    }

    /**
     * The <b>credit sale</b> transaction authorizes a sale purchased with a credit card. The authorization in place
     * in the current open batch (should auto-close for e-commerce transactions). If a batch is not open, this
     * transaction will create an open batch.
     *
     * @param amount               The amount (in dollars).
     * @param currency             The currency (3-letter ISO code for currency).
     * @param token                The secure token.
     * @param cardHolder           The card holder information (used for AVS).
     * @param allowDuplicates      Indicates whether to allow duplicate transactions.
     * @param requestMultiUseToken Request a multi-use token.
     * @param descriptor           A description that is concatenated to a configurable merchant DBA name. The resulting string
     *                             is sent to the card issuer for the Merchant Name.
     * @param details              The transaction details.
     * @return The HPS Charge
     * @throws HpsException
     */
    public HpsCharge charge(BigDecimal amount, String currency, String token, HpsCardHolder cardHolder, boolean allowDuplicates,
                            boolean requestMultiUseToken, String descriptor, HpsTransactionDetails details,
                            HpsDirectMarketData directMarketData, boolean cpcRequest, boolean cardPresent, boolean readerPresent) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditSale");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "AllowDup").text(allowDuplicates ? "Y" : "N");
        Et.subElement(block1, "Amt").text(amount.toString());

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        if(token != null)
            cardData.append(hydrateTokenData(token, cardPresent, readerPresent));

        if(cpcRequest) Et.subElement(block1, "CPCReq").text("Y");
        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");
        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));
        if(descriptor != null)
            Et.subElement(block1, "TxnDescriptor").text(descriptor);
        if(directMarketData != null)
            block1.append(hydrateDirectMarketData(directMarketData));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsCharge().fromElementTree(response);
    }

    public HpsCharge charge(PaymentData paymentData, HpsCardHolder cardHolder, boolean allowDuplicates,
                            boolean requestMultiUseToken, String descriptor, HpsTransactionDetails details,
                            HpsDirectMarketData directMarketData) throws HpsException {
        BigDecimal amount = paymentData.getDollarAmount();

        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency("usd"); // TODO: this needs be parsed from the payment data

        Element transaction = Et.element("CreditSale");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "AllowDup").text(allowDuplicates ? "Y" : "N");
        Et.subElement(block1, "Amt").text(amount.toString());

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        Element manualEntry = Et.element("ManualEntry");
        Et.subElement(manualEntry, "CardNbr").text(paymentData.getApplicationPrimaryAccountNumber());
        String expDate = paymentData.getApplicationExpirationDate();
        Et.subElement(manualEntry, "ExpMonth").text(expDate.substring(2, 4));
        Et.subElement(manualEntry, "ExpYear").text("20" + expDate.substring(0, 2));
        cardData.append(manualEntry);

        block1.append(hydrateSecureECommerce(paymentData.getPaymentData()));

        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");
        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));
        if(descriptor != null)
            Et.subElement(block1, "TxnDescriptor").text(descriptor);
        if(directMarketData != null)
            block1.append(hydrateDirectMarketData(directMarketData));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsCharge().fromElementTree(response);
    }

    public HpsAccountVerify verify(HpsCreditCard card) throws HpsException {
        return this.verify(card, null, false, false, false);
    }

    public HpsAccountVerify verify(HpsCreditCard card, HpsCardHolder cardHolder) throws HpsException {
        return this.verify(card, cardHolder, false, false, false);
    }

    public HpsAccountVerify verify(HpsCreditCard card, HpsCardHolder cardHolder, boolean requestMultiUseToken, boolean cardPresent, boolean readerPresent) throws HpsException {
        Element transaction = Et.element("CreditAccountVerify");
        Element block1 = Et.subElement(transaction, "Block1");

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        if(card != null) {
            cardData.append(hydrateCardManualEntry(card, cardPresent, readerPresent));
            if(card.getEncryptionData() != null)
                cardData.append(hydrateEncryptionData(card.getEncryptionData()));
        }

        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsAccountVerify().fromElementTree(response);
    }

    public HpsAccountVerify verify(String token) throws HpsException {
        return this.verify(token, null, false, false, false);
    }

    public HpsAccountVerify verify(String token, HpsCardHolder cardHolder) throws HpsException {
        return this.verify(token, cardHolder, false, false, false);
    }

    public HpsAccountVerify verify(String token, HpsCardHolder cardHolder, boolean requestMultiUseToken, boolean cardPresent, boolean readerPresent) throws HpsException {
        Element transaction = Et.element("CreditAccountVerify");
        Element block1 = Et.subElement(transaction, "Block1");

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        cardData.append(hydrateTokenData(token, cardPresent, readerPresent));

        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsAccountVerify().fromElementTree(response);
    }

    public HpsAuthorization authorize(BigDecimal amount, String currency, HpsCreditCard card, HpsCardHolder cardHolder, boolean allowDuplicates) throws HpsException {
        return this.authorize(amount, currency, card, cardHolder, allowDuplicates, false, null, null, false, false, false);
    }

    public HpsAuthorization authorize(BigDecimal amount, String currency, String token, HpsCardHolder cardHolder, boolean allowDuplicates) throws HpsException {
        return this.authorize(amount, currency, token, cardHolder, allowDuplicates, false, null, null, false, false, false);
    }

    public HpsAuthorization authorize(PaymentData paymentData, HpsCardHolder cardHolder, boolean allowDuplicates) throws HpsException {
        return this.authorize(paymentData, cardHolder, allowDuplicates, false, null, null);
    }

    /**
     * A <b>credit authorization</b> transaction authorizes a credit card transaction. The authorization is NOT placed
     * in the batch. The <b>credit authorization</b> transaction can be committed by using the capture method.
     *
     * @param amount               The amount (in dollars).
     * @param currency             The currency (3-letter ISO code for currency).
     * @param card                 The credit card information.
     * @param cardHolder           The card holder information (used for AVS).
     * @param allowDuplicates      Indicates whether to allow duplicate transactions.
     * @param requestMultiUseToken Request a multi-use token.
     * @param descriptor           A description that is concatenated to a configurable merchant DBA name. The resulting string
     *                             is sent to the card issuer for the Merchant Name.
     * @param details              The transaction details.
     * @param cpcRequest           Commercial card request.
     * @return The HPS Authorization
     * @throws HpsException
     */
    public HpsAuthorization authorize(BigDecimal amount, String currency, HpsCreditCard card, HpsCardHolder cardHolder, boolean allowDuplicates,
                                      boolean requestMultiUseToken, String descriptor, HpsTransactionDetails details, boolean cpcRequest, boolean cardPresent, boolean readerPresent) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditAuth");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "AllowDup").text(allowDuplicates ? "Y" : "N");
        Et.subElement(block1, "Amt").text(amount.toString());

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        cardData.append(hydrateCardManualEntry(card, cardPresent, readerPresent));
        if(card.getEncryptionData() != null)
            cardData.append(hydrateEncryptionData(card.getEncryptionData()));

        if(cpcRequest) Et.subElement(block1, "CPCReq").text("Y");
        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");
        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));
        if(descriptor != null)
            Et.subElement(block1, "TxnDescriptor").text(descriptor);

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsAuthorization().fromElementTree(response);
    }

    /**
     * A <b>credit authorization</b> transaction authorizes a credit card transaction. The authorization is NOT placed
     * in the batch. The <b>credit authorization</b> transaction can be committed by using the capture method.
     *
     * @param amount               The amount (in dollars).
     * @param currency             The currency (3-letter ISO code for currency).
     * @param token                The secure token.
     * @param cardHolder           The card holder information (used for AVS).
     * @param allowDuplicates      Indicates whether to allow duplicate transactions.
     * @param requestMultiUseToken Request a multi-use token.
     * @param descriptor           A description that is concatenated to a configurable merchant DBA name. The resulting string
     *                             is sent to the card issuer for the Merchant Name.
     * @param details              The transaction details.
     * @param cpcRequest           Commercial card request.
     * @return The HPS Authorization
     * @throws HpsException
     */
    public HpsAuthorization authorize(BigDecimal amount, String currency, String token, HpsCardHolder cardHolder, boolean allowDuplicates,
                                      boolean requestMultiUseToken, String descriptor, HpsTransactionDetails details, boolean cpcRequest, boolean cardPresent, boolean readerPresent) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditAuth");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "AllowDup").text(allowDuplicates ? "Y" : "N");
        Et.subElement(block1, "Amt").text(amount.toString());
        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        cardData.append(hydrateTokenData(token, cardPresent, readerPresent));

        if(cpcRequest) Et.subElement(block1, "CPCReq").text("Y");
        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");
        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));
        if(descriptor != null)
            Et.subElement(block1, "TxnDescriptor").text(descriptor);

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsAuthorization().fromElementTree(response);
    }

    public HpsAuthorization authorize(PaymentData paymentData, HpsCardHolder cardHolder, boolean allowDuplicates,
                                      boolean requestMultiUseToken, String descriptor, HpsTransactionDetails details) throws HpsException {
        BigDecimal amount = paymentData.getDollarAmount();

        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency("usd");

        Element transaction = Et.element("CreditAuth");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "AllowDup").text(allowDuplicates ? "Y" : "N");
        Et.subElement(block1, "Amt").text(amount.toString());

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        Element manualEntry = Et.element("ManualEntry");
        Et.subElement(manualEntry, "CardNbr").text(paymentData.getApplicationPrimaryAccountNumber());
        String expDate = paymentData.getApplicationExpirationDate();
        Et.subElement(manualEntry, "ExpMonth").text(expDate.substring(2, 4));
        Et.subElement(manualEntry, "ExpYear").text("20" + expDate.substring(0, 2));
        cardData.append(manualEntry);

        block1.append(hydrateSecureECommerce(paymentData.getPaymentData()));

        Et.subElement(cardData, "TokenRequest").text(requestMultiUseToken ? "Y" : "N");
        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsAuthorization().fromElementTree(response);
    }

    public HpsTransaction captureTxn(int transactionId) throws HpsException {
        return this.captureTxn(transactionId, null, null, null);
    }

    public HpsTransaction captureTxn(int transactionId, HpsDirectMarketData directMarketData) throws HpsException {
        return this.captureTxn(transactionId, null, null, directMarketData);
    }

    public HpsTransaction captureTxn(int transactionId, BigDecimal amount) throws HpsException {
        return this.captureTxn(transactionId, amount, null, null);
    }

    public HpsTransaction captureTxn(int transactionId, BigDecimal amount, HpsDirectMarketData directMarketData) throws HpsException {
        return this.captureTxn(transactionId, amount, null, directMarketData);
    }

    public HpsTransaction captureTxn(Integer transactionId, BigDecimal amount, BigDecimal gratuity, HpsDirectMarketData directMarketData) throws HpsException {
        Element transaction = Et.element("CreditAddToBatch");
        Et.subElement(transaction, "GatewayTxnId").text(transactionId.toString());
        if(amount != null)
            Et.subElement(transaction, "Amt").text(amount.toString());

        if(gratuity != null)
            Et.subElement(transaction, "GratuityAmtInfo").text(gratuity.toString());

        if(directMarketData != null)
            transaction.append(hydrateDirectMarketData(directMarketData));

        ElementTree response = submitTransaction(transaction);
        HpsTransaction trans = new HpsTransaction().fromElementTree(response);
        trans.setResponseCode("00");
        trans.setResponseText("");
        return trans;
    }

    public HpsRefund refund(BigDecimal amount, String currency, HpsCreditCard card) throws HpsException {
        return this.refund(amount, currency, card, null, null);
    }

    public HpsRefund refund(BigDecimal amount, String currency, int transactionId) throws HpsException {
        return this.refund(amount, currency, transactionId, null, null);
    }

    public HpsRefund refund(BigDecimal amount, String currency, HpsCreditCard card, HpsCardHolder cardHolder) throws HpsException {
        return this.refund(amount, currency, card, cardHolder, null);
    }

    public HpsRefund refund(BigDecimal amount, String currency, int transactionId, HpsCardHolder cardHolder) throws HpsException {
        return this.refund(amount, currency, transactionId, cardHolder, null);
    }

    public HpsRefund refund(BigDecimal amount, String currency, HpsCreditCard card, HpsCardHolder cardHolder,
                            HpsTransactionDetails details) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditReturn");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "Amt").text(amount.toString());

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Element cardData = Et.subElement(block1, "CardData");
        cardData.append(hydrateCardManualEntry(card, false, false));

        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        HpsRefund trans = new HpsRefund().fromElementTree(response);
        trans.setResponseCode("00");
        trans.setResponseText("");
        return trans;
    }

    public HpsRefund refund(BigDecimal amount, String currency, Integer transactionId, HpsCardHolder cardHolder,
                            HpsTransactionDetails details) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditReturn");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "Amt").text(amount.toString());

        if(cardHolder != null)
            block1.append(hydrateCardHolder(cardHolder));

        Et.subElement(block1, "GatewayTxnId").text(transactionId.toString());

        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        HpsRefund trans = new HpsRefund().fromElementTree(response);
        trans.setResponseCode("00");
        trans.setResponseText("");
        return trans;
    }

    public HpsReversal reverse(HpsCreditCard card, BigDecimal amount, String currency) throws HpsException {
        return this.reverse(card, amount, currency, null);
    }

    public HpsReversal reverse(int transactionId, BigDecimal amount, String currency) throws HpsException {
        return this.reverse(transactionId, amount, currency, null);
    }

    public HpsReversal reverse(HpsCreditCard card, BigDecimal amount, String currency,
                               HpsTransactionDetails details) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditReversal");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "Amt").text(amount.toString());

        Element cardData = Et.subElement(block1, "CardData");
        cardData.append(hydrateCardManualEntry(card, false, false));

        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsReversal().fromElementTree(response);
    }

    public HpsReversal reverse(Integer transactionId, BigDecimal amount, String currency,
                               HpsTransactionDetails details) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("CreditReversal");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "Amt").text(amount.toString());
        Et.subElement(block1, "GatewayTxnId").text(transactionId.toString());

        if(details != null)
            block1.append(hydrateAdditionalTxnFields(details));

        String clientTransactionId = getClientTxnId(details);
        ElementTree response = submitTransaction(transaction, clientTransactionId);
        return new HpsReversal().fromElementTree(response);
    }

    public HpsTransaction edit(int transactionId, BigDecimal amount) throws HpsException {
        return edit(transactionId, amount, BigDecimal.ZERO);
    }

    public HpsTransaction edit(Integer transactionId, BigDecimal amount, BigDecimal gratuity) throws HpsException {
        HpsInputValidation.checkAmount(amount);
        if(transactionId <= 0) {
            throw new HpsInvalidRequestException("Invalid transaction ID.");
        }

        Element transaction = Et.element("CreditTxnEdit");
        Et.subElement(transaction, "GatewayTxnId").text(transactionId.toString());
        if(amount != null)
            Et.subElement(transaction, "Amt").text(amount.toString());

        if(gratuity != null)
            Et.subElement(transaction, "GratuityAmtInfo").text(gratuity.toString());

        ElementTree response = submitTransaction(transaction, clientTransactionId);
        HpsTransaction trans = new HpsTransaction().fromElementTree(response);
        trans.setResponseCode("00");
        trans.setResponseText("");

        return trans;
    }

    public HpsTransaction voidTxn(Integer transactionId) throws HpsException {
        if(transactionId <= 0) {
            throw new HpsInvalidRequestException("Invalid transaction ID.");
        }

        Element transaction = Et.element("CreditVoid");
        Et.subElement(transaction, "GatewayTxnId").text(transactionId.toString());

        ElementTree response = submitTransaction(transaction, clientTransactionId);
        HpsTransaction trans = new HpsTransaction().fromElementTree(response);
        trans.setResponseCode("00");
        trans.setResponseText("");
        return trans;
    }

    public HpsTransaction cpcEdit(Integer transactionId, HpsCpcData cpcData) throws HpsException {
        if(transactionId <= 0) {
            throw new HpsInvalidRequestException("Invalid transaction ID.");
        }

        Element transaction = Et.element("CreditCPCEdit");
        Et.subElement(transaction, "GatewayTxnId").text(transactionId.toString());
        transaction.append(hydrateCpcData(cpcData));

        ElementTree response = submitTransaction(transaction);
        HpsTransaction trans = new HpsTransaction().fromElementTree(response);
        trans.setResponseCode("00");
        trans.setResponseText("");
        return trans;
    }

    public ElementTree submitTransaction(Element transaction) throws HpsException {
        return this.submitTransaction(transaction, null);
    }
    public ElementTree submitTransaction(Element transaction, String clientTransactionId) throws HpsException {
        ElementTree rsp = this.doTransaction(transaction, clientTransactionId);

        BigDecimal amount = null;
        if(transaction.tag().equals("CreditSale") || transaction.tag().equals("CreditAuth"))
            amount = new BigDecimal(transaction.getString("Amt"));

        this.processGatewayResponse(rsp, transaction.tag(), amount);
        this.processIssuerResponse(rsp, transaction.tag(), amount);

        return rsp;
    }

    public void processIssuerResponse(ElementTree response, String expectedType, BigDecimal amount) throws HpsException {
        Integer transactionId = response.get("Header").getInt("GatewayTxnId");
        Element transaction = response.get(expectedType);

        if(transaction != null) {
            String responseCode = transaction.getString("RspCode");
            String responseText = transaction.getString("RspText");

            if(responseCode != null && !responseCode.equals("")) {
                if(responseCode.equals("91")){
                    try{
                        this.reverse(transactionId, amount, "usd");
                    }
                    catch(HpsGatewayException e) {
                        if(e.getDetails().getGatewayResponseCode() == 3)
                            HpsIssuerResponseValidation.checkIssuerResponse(transactionId, responseCode, responseText);
                        throw new HpsCreditException(transactionId, HpsExceptionCodes.IssuerTimeoutReversal, "Error occurred while reversing a charge due to an issuer timeout.", e);
                    }
                    catch(HpsException e) {
                        throw new HpsCreditException(transactionId, HpsExceptionCodes.IssuerTimeoutReversal, "Error occurred while reversing a charge due to an issuer timeout.", e);
                    }
                }
                HpsIssuerResponseValidation.checkIssuerResponse(transactionId, responseCode, responseText);
            }
        }
    }

    public void processGatewayResponse(ElementTree response, String expectedType, BigDecimal amount) throws HpsException {
        String responseCode = response.get("Header").getString("GatewayRspCode");
        Integer transactionId = response.get("Header").getInt("GatewayTxnId");
        if(responseCode.equals("00"))
            return;

        if(responseCode.equals("30")){
            try{
                this.reverse(transactionId, amount, "usd");
            }
            catch(HpsException e) {
                throw new HpsGatewayException(HpsExceptionCodes.GatewayTimeoutReversalError, "Error occurred while reversing a charge due to a gateway timeout.", e);
            }
        }
        HpsGatewayResponseValidation.checkGatewayResponse(response, expectedType);
    }
}