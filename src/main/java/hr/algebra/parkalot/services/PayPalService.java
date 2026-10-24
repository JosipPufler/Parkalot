package hr.algebra.parkalot.services;

import hr.algebra.parkalot.exceptions.PayPalException;
import hr.algebra.parkalot.models.dto.PermitRequestDto;
import hr.algebra.parkalot.services.db.AreaService;
import hr.algebra.parkalot.services.db.PermitService;
import hr.algebra.parkalot.services.db.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayPalService {
    private final VehicleService vehicleService;
    private final PermitService permitService;
    private final AreaService areaService;

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${paypal.clientId}")
    private String clientId;
    @Value("${paypal.secret}")
    private String secret;

    public Optional<String> createOrder(PermitRequestDto permitRequestDto) {
        String token = getAccessToken();

        Optional<Double> price = areaService.calculateAreaPermitPrice(permitRequestDto.areaId(), permitRequestDto.permitDuration());
        if (price.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> orderPayload = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "amount", Map.of(
                                "currency_code", "EUR",
                                "value", String.format(Locale.US, "%.2f",
                                        price.get())
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
            throw new PayPalException("Error creating order");
        }

        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            if (body != null && body.get("id") != null) {
                return Optional.of((String) body.get("id"));
            }
        }
        throw new PayPalException("Error getting order id");
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
        throw new PayPalException("Error getting access token");
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
            throw new PayPalException("Failed to capture PayPal order: " + orderId);
        }

        return response.getBody();
    }
}
