package com.Zone01.lets_play;

import com.Zone01.lets_play.Mongo_repisitory.ProductRepository;
import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.models.Product;
import com.Zone01.lets_play.models.User;
import com.Zone01.lets_play.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.data.mongodb.database=test_database",
    "spring.application.rate-limiting.enabled=false"
})
public class CrudOperationsIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String userToken;
    private String adminUserId;
    private String regularUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // Clean up database before each test
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin user
        User admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin = userRepository.save(admin);
        adminUserId = admin.getId();
        adminToken = jwtService.generateToken(admin);

        // Create regular user
        User regularUser = new User();
        regularUser.setName("Regular User");
        regularUser.setEmail("user@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRole("USER");
        regularUser = userRepository.save(regularUser);
        regularUserId = regularUser.getId();
        userToken = jwtService.generateToken(regularUser);
    }

    // =================== USER CRUD OPERATIONS ===================

    @Test
    void testCreateUser() throws Exception {
        String userJson = """
                {
                    "name": "New User",
                    "email": "new@test.com",
                    "password": "newuser123",
                    "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New User")))
                .andExpect(jsonPath("$.email", is("new@test.com")))
                .andExpect(jsonPath("$.password").doesNotExist()) // Password should not be returned
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void testCreateUserWithValidationErrors() throws Exception {
        String invalidUserJson = """
                {
                    "name": "",
                    "email": "invalid-email",
                    "password": "123",
                    "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUserJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }

    @Test
    void testGetAllUsers_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].email", notNullValue()))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void testGetAllUsers_AsRegularUser_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserById_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/users/" + adminUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(adminUserId)))
                .andExpect(jsonPath("$.name", is("Admin User")))
                .andExpect(jsonPath("$.email", is("admin@test.com")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/nonexistent")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testUpdateUser_AsAdmin() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Admin User",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(put("/api/users/" + adminUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Admin User")))
                .andExpect(jsonPath("$.email", is("admin@test.com"))); // Email should remain unchanged
    }

    @Test
    void testDeleteUser_AsAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/" + regularUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify user is deleted
        mockMvc.perform(get("/api/users/" + regularUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUser_AsRegularUser_ShouldFail() throws Exception {
        mockMvc.perform(delete("/api/users/" + adminUserId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // =================== PRODUCT CRUD OPERATIONS ===================

    @Test
    void testCreateProduct_AsAuthenticatedUser() throws Exception {
        String productJson = """
                {
                    "name": "Test Product",
                    "description": "A test product description",
                    "price": 29.99
                }
                """;

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Product")))
                .andExpect(jsonPath("$.description", is("A test product description")))
                .andExpect(jsonPath("$.price", is(29.99)))
                .andExpect(jsonPath("$.userId", is(regularUserId)));
    }

    @Test
    void testCreateProduct_WithValidationErrors() throws Exception {
        String invalidProductJson = """
                {
                    "name": "",
                    "description": "Valid description",
                    "price": -10.0
                }
                """;

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidProductJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }

    @Test
    void testGetAllProducts_PublicAccess() throws Exception {
        // Create a test product first
        Product product = new Product();
        product.setName("Public Product");
        product.setDescription("A publicly viewable product");
        product.setPrice(19.99);
        product.setUserId(regularUserId);
        productRepository.save(product);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].price", notNullValue()));
    }

    @Test
    void testGetProductById_PublicAccess() throws Exception {
        Product product = new Product();
        product.setName("Single Product");
        product.setDescription("A single product for testing");
        product.setPrice(39.99);
        product.setUserId(regularUserId);
        product = productRepository.save(product);

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(product.getId())))
                .andExpect(jsonPath("$.name", is("Single Product")))
                .andExpect(jsonPath("$.price", is(39.99)));
    }

    @Test
    void testGetProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testUpdateProduct_AsOwner() throws Exception {
        // Create a product owned by regular user
        Product product = new Product();
        product.setName("Original Product");
        product.setDescription("Original description");
        product.setPrice(25.00);
        product.setUserId(regularUserId);
        product = productRepository.save(product);

        String updateJson = """
                {
                    "name": "Updated Product",
                    "description": "Updated description",
                    "price": 35.00
                }
                """;

        mockMvc.perform(put("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Product")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.price", is(35.00)));
    }

    @Test
    void testUpdateProduct_AsNonOwner_ShouldFail() throws Exception {
        // Create a product owned by admin
        Product product = new Product();
        product.setName("Admin Product");
        product.setDescription("Product owned by admin");
        product.setPrice(50.00);
        product.setUserId(adminUserId);
        product = productRepository.save(product);

        String updateJson = """
                {
                    "name": "Hacked Product",
                    "description": "Should not be allowed",
                    "price": 1.00
                }
                """;

        mockMvc.perform(put("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteProduct_AsOwner() throws Exception {
        Product product = new Product();
        product.setName("Product to Delete");
        product.setDescription("This product will be deleted");
        product.setPrice(15.00);
        product.setUserId(regularUserId);
        product = productRepository.save(product);

        mockMvc.perform(delete("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Verify product is deleted
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteProduct_AsNonOwner_ShouldFail() throws Exception {
        Product product = new Product();
        product.setName("Protected Product");
        product.setDescription("Should not be deletable by others");
        product.setPrice(100.00);
        product.setUserId(adminUserId);
        product = productRepository.save(product);

        mockMvc.perform(delete("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // =================== AUTHENTICATION TESTS ===================

    @Test
    void testLoginWithValidCredentials() throws Exception {
        String loginJson = """
                {
                    "email": "user@test.com",
                    "password": "user123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        String loginJson = """
                {
                    "email": "user@test.com",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}
