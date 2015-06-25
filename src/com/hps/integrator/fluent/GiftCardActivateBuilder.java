package com.hps.integrator.fluent;

import com.hps.integrator.entities.gift.HpsGiftCard;
import com.hps.integrator.entities.gift.HpsGiftCardResponse;
import com.hps.integrator.infrastructure.Element;
import com.hps.integrator.infrastructure.ElementTree;
import com.hps.integrator.infrastructure.HpsException;
import com.hps.integrator.infrastructure.validation.HpsInputValidation;
import com.hps.integrator.services.fluent.HpsFluentGiftService;

import java.math.BigDecimal;

public class GiftCardActivateBuilder extends HpsBuilderAbstract<HpsFluentGiftService, HpsGiftCardResponse> {
    BigDecimal amount;
    HpsGiftCard card;
    String currency;

    public GiftCardActivateBuilder withAmount(BigDecimal value) {
        this.amount = value;
        return this;
    }
    public GiftCardActivateBuilder withCard(HpsGiftCard value) {
        this.card = value;
        return this;
    }
    public GiftCardActivateBuilder withCurrency(String value) {
        this.currency = value;
        return this;
    }

    public GiftCardActivateBuilder(HpsFluentGiftService service) {
        super(service);
    }

    @Override
    public HpsGiftCardResponse execute() throws HpsException {
        super.execute();

        HpsInputValidation.checkAmount(amount);
        HpsInputValidation.checkCurrency(currency);

        Element transaction = Et.element("GiftCardActivate");
        Element block1 = Et.subElement(transaction, "Block1");
        Et.subElement(block1, "Amt").text(amount.toString());
        block1.append(service.hydrateGiftCardData(card));

        ElementTree response = service.submitTransaction(transaction);
        return new HpsGiftCardResponse().fromElementTree(response);
    }

    @Override
    protected void setupValidations() throws HpsException {
        this.addValidation(new HpsBuilderValidation("amountIsNotNull", "Amount is required."));
        this.addValidation(new HpsBuilderValidation("cardIsNotNull", "Card is required."));
        this.addValidation(new HpsBuilderValidation("currencyIsNotNull", "Currency is required."));
    }

    private boolean amountIsNotNull(){
        return this.amount != null;
    }

    private boolean cardIsNotNull() { return this.card != null; }

    private boolean currencyIsNotNull() { return this.currency != null; }
}
