package me.viktrl.VpnTgBot.service;

import me.dynomake.yookassa.Yookassa;
import me.dynomake.yookassa.model.Amount;
import me.dynomake.yookassa.model.Confirmation;
import me.dynomake.yookassa.model.Payment;
import me.dynomake.yookassa.model.request.PaymentRequest;
import me.dynomake.yookassa.model.request.receipt.Receipt;
import me.dynomake.yookassa.model.request.receipt.ReceiptCustomer;
import me.dynomake.yookassa.model.request.receipt.ReceiptItem;
import me.dynomake.yookassa.exception.UnspecifiedShopInformation;
import me.dynomake.yookassa.exception.BadRequestException;

import java.io.IOException;
import java.util.Arrays;

public class PaymentService {
    private final Yookassa yookassa = Yookassa
            .initialize(1101285, "test_iQv5GqqvHqXXFOjKV0bUMxnaCa1W8_7W48_ZJ0a7xLs");

    public Payment createPayment() throws UnspecifiedShopInformation, BadRequestException, IOException {
        try {
            PaymentRequest request = PaymentRequest.builder()
                    .amount(new Amount("2.00", "RUB"))
                    .description("This is a test payment!")
                    .receipt(Receipt.builder()
                            .customer(ReceiptCustomer.builder().email("kamiknol@gmail.com").build())
                            .items(Arrays.asList(ReceiptItem.builder()
                                    .amount(new Amount("2.00", "RUB"))
                                    .quantity(1)
                                    .subject("service")
                                    .paymentMode("full_payment")
                                    .vat(1)
                                    .description("Test product").build()))
                            .build())
                    .savePaymentMethod(true)
                    .confirmation(Confirmation.builder()
                            .type("redirect")
                            .returnUrl("https://localhost:8080")
                            .build())
                    .build();

            Payment payment = yookassa.createPayment(request);
            System.out.println("Payment created. Bill link: " + payment.getConfirmation().getConfirmationUrl());
            return payment;
        } catch (UnspecifiedShopInformation e) {
            System.err.println("Shop information is not specified: " + e.getMessage());
            throw e;
        } catch (BadRequestException e) {
            System.err.println("Bad request to YooKassa API: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.err.println("IO error during payment creation: " + e.getMessage());
            throw e;
        }
    }
}