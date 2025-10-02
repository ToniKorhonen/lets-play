package com.Zone01.lets_play;

import com.Zone01.lets_play.Mongo_repisitory.UserRepository;
import com.Zone01.lets_play.Mongo_repisitory.ProductRepository;
import com.Zone01.lets_play.dto.UserDtos;
import com.Zone01.lets_play.dto.ProductDtos;
import com.Zone01.lets_play.dto.AuthDtos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.data.mongodb.database=test_database",
    "spring.application.rate-limiting.enabled=false"
})
public class RestApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDb() {
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getJwtForUser(String email, String password, String role) throws Exception {
        String json = "{" +
                "\"name\":\"" + email.split("@")[0] + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"" +
                (role != null ? ",\"role\":\"" + role + "\"}" : "}");
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        String loginJson = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk());

        String responseJson = result.andReturn().getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseJson);
        return node.get("token").asText();
    }

    private String getJwtForAdminUser(String email, String password) throws Exception {
        return getJwtForUser(email, password, "ADMIN");
    }

    @Test
    @DisplayName("GET /api/products is public and returns 200")
    void getProductsIsPublic() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/users creates user and does not expose password")
    void createUserNoPasswordExposure() throws Exception {
        String json = "{" +
                "\"name\":\"Alice\"," +
                "\"email\":\"alice@example.com\"," +
                "\"password\":\"SuperSecret123\"}";
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/users with duplicate email returns 409")
    void createUserDuplicateEmail() throws Exception {
        String json = "{" +
                "\"name\":\"Bob\"," +
                "\"email\":\"bob@example.com\"," +
                "\"password\":\"AnotherSecret123\"}";
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/products without auth returns 401")
    void createProductRequiresAuth() throws Exception {
        String json = "{" +
                "\"name\":\"Ball\"," +
                "\"description\":\"A nice ball\"," +
                "\"price\":10.0}";
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/{id} without auth returns 401")
    void getUserRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/users/123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 for unknown id")
    void getProductNotFound() throws Exception {
        mockMvc.perform(get("/api/products/unknownid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users with invalid email returns 400")
    void createUserInvalidEmail() throws Exception {
        String json = "{" +
                "\"name\":\"Charlie\"," +
                "\"email\":\"notanemail\"," +
                "\"password\":\"Secret123\"}";
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    private String getJwtForAdminUser() throws Exception {
        // Crée un utilisateur admin si besoin
        String email = "admin@example.com";
        String password = "AdminPass123";
        String json = "{" +
                "\"name\":\"Admin\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"," +
                "\"role\":\"ADMIN\"}";
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
        // Login pour obtenir le JWT
        String loginJson = "{" +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"}";
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);
        return node.get("token").asText();
    }

    @Test
    @DisplayName("POST /api/auth/login returns JWT on valid credentials")
    void loginReturnsJwt() throws Exception {
        // Crée un utilisateur pour le test
        String json = "{" +
                "\"name\":\"Alice\"," +
                "\"email\":\"alice@example.com\"," +
                "\"password\":\"SuperSecret123\"}";
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
        // Teste le login
        String loginJson = "{" +
                "\"email\":\"alice@example.com\"," +
                "\"password\":\"SuperSecret123\"}";
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("PUT /api/users/{id} updates user info with auth")
    void updateUserWithAuth() throws Exception {
        // Crée un utilisateur admin et récupère son JWT
        String token = getJwtForAdminUser();
        // Crée un utilisateur à mettre à jour
        String json = "{" +
                "\"name\":\"Alice\"," +
                "\"email\":\"alice2@example.com\"," +
                "\"password\":\"SuperSecret123\"}";
        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);
        String userId = node.get("id").asText();
        // Met à jour l'utilisateur avec le JWT admin
        String updateJson = "{" +
                "\"name\":\"Alice Updated\"}";
        mockMvc.perform(put("/api/users/" + userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Updated")));
    }

    @Test
    @DisplayName("POST /api/auth/login invalid password returns 401")
    void loginInvalidPassword() throws Exception {
        // create user
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"U1\",\"email\":\"u1@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated());
        // wrong password
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"u1@example.com\",\"password\":\"badpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login unknown user returns 401")
    void loginUnknownUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nouser@example.com\",\"password\":\"Whatever123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/users/{id} without token returns 401")
    void updateUserWithoutToken() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Toto\",\"email\":\"toto@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String userId = new ObjectMapper().readTree(response).get("id").asText();
        mockMvc.perform(put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Toto2\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/users/{id} forbidden for non-admin token")
    void updateUserForbiddenForNonAdmin() throws Exception {
        // create target user
        String userResp = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"UserX\",\"email\":\"userx@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String targetId = new ObjectMapper().readTree(userResp).get("id").asText();
        // create non-admin and get token
        String token = getJwtForUser("simple@example.com", "Password123!", null);
        mockMvc.perform(put("/api/users/" + targetId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"HackTry\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products with valid user token creates product")
    void createProductWithToken() throws Exception {
        String token = getJwtForUser("produser@example.com", "Password123!", null);
        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ball\",\"description\":\"Nice ball\",\"price\":5.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Ball")));
    }

    @Test
    @DisplayName("PUT /api/products/{id} by non-owner returns 403")
    void updateProductByNonOwnerForbidden() throws Exception {
        String tokenOwner = getJwtForUser("owner@example.com", "Password123!", null);
        String createResp = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + tokenOwner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Item1\",\"description\":\"Desc\",\"price\":10}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productId = new ObjectMapper().readTree(createResp).get("id").asText();
        String tokenOther = getJwtForUser("other@example.com", "Password123!", null);
        mockMvc.perform(put("/api/products/" + productId)
                .header("Authorization", "Bearer " + tokenOther)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Item1Updated\",\"description\":\"Desc2\",\"price\":12}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products with tampered token returns 401")
    void tamperedTokenUnauthorized() throws Exception {
        String token = getJwtForUser("tokuser@example.com", "Password123!", null);
        String badToken = token + "x"; // simple corruption
        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bad\",\"description\":\"ShouldFail\",\"price\":1}"))
                .andExpect(status().isUnauthorized());
    }
}
