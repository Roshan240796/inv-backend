package com.synergy.invoicedemo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoiceDemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void xmlImportEndpointAcceptsAuthenticatedMultipartUpload() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "invoice.xml",
            "application/xml",
            "<Invoice><invoiceNumber>XML-TEST-001</invoiceNumber><customer>XML Customer</customer><amount>125.50</amount><currency>EUR</currency><issuedOn>2026-09-03</issuedOn></Invoice>".getBytes()
        );

        mockMvc.perform(multipart("/api/invoices/import/xml")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customer").value("XML Customer"))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}
