package com.noman.ecommerce_order_management.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorHandlingIntegrationTest {

    private static final String CUSTOMER_EMAIL =
            "customer@ecommerce.com";

    private static final String CUSTOMER_PASSWORD =
            "customer123";

    @Autowired
    private MockMvc mockMvc;

    /*
     * No credentials:
     *
     * must return our JSON 401 response instead
     * of Spring Security's default response.
     */
    @Test
    void shouldReturnConsistentUnauthorizedResponse()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/customer/catalog/categories"
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Unauthorized"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Authentication is required to access this resource"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/customer/catalog/categories"
                                )
                );
    }

    /*
     * Authenticated CUSTOMER trying to access
     * an ADMIN endpoint.
     */
    @Test
    void shouldReturnConsistentForbiddenResponse()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/admin/warehouses"
                        )
                                .with(
                                        httpBasic(
                                                CUSTOMER_EMAIL,
                                                CUSTOMER_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Forbidden"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "You do not have permission to access this resource"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/admin/warehouses"
                                )
                );
    }

    /*
     * Verifies @Valid errors are returned
     * per field.
     *
     * skuId missing
     * quantity invalid
     */
    @Test
    void shouldReturnFieldValidationErrors()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/customer/cart/items"
                        )
                                .with(
                                        httpBasic(
                                                CUSTOMER_EMAIL,
                                                CUSTOMER_PASSWORD
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "quantity": 0
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Bad Request"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/customer/cart/items"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.skuId"
                        )
                                .value(
                                        "SKU id is required"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.quantity"
                        )
                                .value(
                                        "Quantity must be at least 1"
                                )
                );
    }

    /*
     * Broken JSON must not expose Jackson
     * implementation details to the caller.
     */
    @Test
    void shouldReturnCleanErrorForMalformedJson()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/customer/orders/checkout"
                        )
                                .with(
                                        httpBasic(
                                                CUSTOMER_EMAIL,
                                                CUSTOMER_PASSWORD
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "paymentMethod": "UPI",
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Bad Request"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Malformed JSON request"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/customer/orders/checkout"
                                )
                );
    }

    /*
     * Verifies that ResponseStatusException
     * from the service layer is transformed
     * into our common API response.
     */
    @Test
    void shouldReturnConsistentNotFoundResponse()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/customer/catalog/products/999999999"
                        )
                                .with(
                                        httpBasic(
                                                CUSTOMER_EMAIL,
                                                CUSTOMER_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Not Found"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Product not found"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/customer/catalog/products/999999999"
                                )
                );
    }
}