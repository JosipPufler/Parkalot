package hr.algebra.parkalot.controllers.rest;

import hr.algebra.parkalot.models.Role;
import hr.algebra.parkalot.models.dto.PermitRequestDto;
import hr.algebra.parkalot.models.entity.User;
import hr.algebra.parkalot.services.PayPalService;
import hr.algebra.parkalot.services.utils.AuthUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/rest/purchase")
@RequiredArgsConstructor
public class PayPalController {
    private final PayPalService payPalService;
    private final AuthUtils authUtils;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody PermitRequestDto permitRequestDto) {
        Optional<User> currentUser = authUtils.getCurrentUser();
        if (currentUser.isEmpty() || currentUser.get().getRole() != Role.USER) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Build the PayPal order
        Optional<String> orderId = payPalService.createOrder(permitRequestDto);

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/verify-order")
    public ResponseEntity<Void> verifyOrder(@RequestBody Map<String, String> body, HttpSession session) {
        String orderId = body.get("orderId");
        String address = body.get("address");
        if (orderId == null || orderId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing orderId");
        }

        Map<String, Object> captureResponse;
        try {
            captureResponse = payPalService.captureOrder(orderId);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to capture PayPal order: " + ex.getMessage());
        }

        List<Map<String, Object>> purchaseUnits =
                (List<Map<String, Object>>) captureResponse.get("purchase_units");

        Map<String, String> amount = getValueMap(purchaseUnits);

        String paidValueStr = amount.get("value");
        String currency   = amount.get("currency_code");
        if (paidValueStr == null || currency == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture response: incomplete amount");
        }

        Double paidValue;
        try {
            paidValue = Double.parseDouble(paidValueStr);
        } catch (NumberFormatException nfe) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid PayPal capture amount: " + paidValueStr);
        }

        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        Long userId = user.getId();
        Optional<ShoppingCart> byUserId = shoppingService.findByUserId(userId);
        if (byUserId.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ShoppingCart shoppingCart = byUserId.get();
        Double cartTotal = shoppingCart.getTotalPrice();
        if (cartTotal.compareTo(paidValue) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Paid amount " + paidValue + " " + currency +
                            " does not match cart total " + cartTotal + " EUR");
        }
        Optional<User> byId = userService.findById(userId);
        if (byId.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ShoppingCartRecord shoppingCartRecord = shoppingService.confirmOrder(shoppingCart, byId.get(), address, PaymentMethodEnum.PAYPAL, orderId);

        session.setAttribute("purchase", new PurchaseDto(shoppingCartRecord));
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
