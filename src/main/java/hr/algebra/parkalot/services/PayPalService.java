package hr.algebra.parkalot.services;

import hr.algebra.parkalot.exceptions.LocalizedBadRequestException;
import hr.algebra.parkalot.exceptions.LocalizedInternalException;
import hr.algebra.parkalot.models.dtos.PermitRequestDto;
import hr.algebra.parkalot.models.entities.Permit;
import hr.algebra.parkalot.models.entities.Violation;
import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import hr.algebra.parkalot.services.db.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayPalService {
    private final PermitService permitService;
    private final AreaService areaService;
    private final ViolationService violationService;

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${paypal.clientId}")
    private final String clientId;
    @Value("${paypal.secret}")
    private final String secret;

    public Optional<String> createPermitOrder(PermitRequestDto permitRequestDto) {
        Optional<Double> price = areaService.calculateAreaPermitPrice(permitRequestDto.areaId(), permitRequestDto.permitDuration());
        return price.map(this::createOrder);
    }

    public String createViolationPaymentOrder(Violation violation){
        return createOrder(violation.getPrice());
    }

    private String createOrder(Double price) {
        String token = getAccessToken();

        Map<String, Object> orderPayload = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "amount", Map.of(
                                "currency_code", "EUR",
                                "value", String.format(Locale.US, "%.2f",
                                        price)
                        )
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderPayload, headers);

        ResponseEntity<Map<String, Object>> response = new RestTemplate().postForEntity(
                "https://api-m.sandbox.paypal.com/v2/checkout/orders",
                request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        if (response.getBody() == null) {
            throw new LocalizedInternalException(LocalizedExceptionKeys.PAYMENT_CREATE_ORDER);
        }

        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            if (body != null && body.get("id") != null) {
                return (String) body.get("id");
            }
        }
        throw new LocalizedInternalException(LocalizedExceptionKeys.PAYMENT_ORDER_ID);
    }

    private String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, secret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map<String, Object>> response = new RestTemplate().postForEntity(
                "https://api-m.sandbox.paypal.com/v1/oauth2/token",
                request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            if (body != null && body.get("access_token") != null) {
                return (String) body.get("access_token");
            }
        }
        throw new LocalizedInternalException(LocalizedExceptionKeys.PAYMENT_ACCESS_TOKEN);
    }

    public Map<String, Object> captureOrder(String orderId) {
        String captureUrl =  "https://api-m.sandbox.paypal.com/v2/checkout/orders/" + orderId + "/capture";
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                captureUrl,
                HttpMethod.POST,
                request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new LocalizedInternalException(LocalizedExceptionKeys.PAYMENT_CAPTURE, orderId);
        }

        return response.getBody();
    }

    public Boolean verifyPaymentAmount(String orderId, Double amount) {
        Optional<Violation> violationOpt = violationService.findByOrderId(orderId);
        if (violationOpt.isPresent()) {
            Violation violation = violationOpt.get();

            if (Objects.equals(violation.getPrice(), amount)){
                violation.setPaymentTime(LocalDateTime.now());
                violationService.save(violation);
                return true;
            }
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.PAYMENT_AMOUNT, amount, violation.getPrice());
        }

        Optional<Permit> permitPaymentOpt = permitService.findByOrderId(orderId);
        if (permitPaymentOpt.isPresent()) {
            Permit permitPayment = permitPaymentOpt.get();

            if (Objects.equals(permitPayment.getPrice(), amount)){
                permitPayment.setPaymentTime(LocalDateTime.now());
                permitService.save(permitPayment);
                return true;
            }
            throw new LocalizedBadRequestException(LocalizedExceptionKeys.PAYMENT_AMOUNT, amount, permitPayment.getPrice());

        }
        return false;
    }
}
