package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.services.PayPalService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/payment")
@RequiredArgsConstructor
public class PayPalController {
    private final PayPalService payPalService;
    private final AuthUtils authUtils;


    @PostMapping("/permit/verify-order")
    public ResponseEntity<?> verifyOrder(@RequestBody Map<String, String> body) {
        String orderId = body.get("orderId");
        if (orderId == null || orderId.isEmpty()) {
            return HttpUtils.produceBadRequestResponse("Missing orderId");
        }

        Map<String, Object> captureResponse;
        try {
            captureResponse = payPalService.captureOrder(orderId);
        } catch (RuntimeException ex) {
            return HttpUtils.produceInternalErrorResponse("Failed to capture PayPal order: " + ex.getMessage());
        }

        List<Map<String, Object>> purchaseUnits = (List<Map<String, Object>>) captureResponse.get("purchase_units");

        Map<String, String> amount = getValueMap(purchaseUnits);

        String paidValueStr = amount.get("value");
        String currency   = amount.get("currency_code");
        if (paidValueStr == null || currency == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture response: incomplete amount");
        }

        if (!currency.equals("EUR")) {
            return HttpUtils.produceBadRequestResponse("Payment must be in EUR");
        }

        Double paidValue;
        try {
            paidValue = Double.parseDouble(paidValueStr);
        } catch (NumberFormatException nfe) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture amount: " + paidValueStr);
        }

        payPalService.verifyPaymentAmount(orderId, paidValue);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/mvc/history/confirmation")
                .build();
    }

    private static Map<String, String> getValueMap(List<Map<String, Object>> purchaseUnits) {
        if (purchaseUnits == null || purchaseUnits.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture response: missing purchase_units");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> firstPU = purchaseUnits.getFirst();
        @SuppressWarnings("unchecked")
        Map<String, Object> payments = (Map<String, Object>) firstPU.get("payments");
        if (payments == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture response: missing payments");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> captures = (List<Map<String, Object>>) payments.get("captures");
        if (captures == null || captures.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture response: missing captures");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> firstCapture = captures.getFirst();
        @SuppressWarnings("unchecked")
        Map<String, String> amount = (Map<String, String>) firstCapture.get("amount");
        if (amount == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture response: missing amount");
        }
        return amount;
    }
}
