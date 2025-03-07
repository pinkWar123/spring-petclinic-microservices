package org.springframework.samples.petclinic.visits.web;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VisitResource.class)
@ActiveProfiles("test")
class VisitResourceTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private VisitRepository visitRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Test đã có: GET /pets/visits?petId=111,222
    @Test
    void shouldFetchVisits() throws Exception {
        given(visitRepository.findByPetIdIn(asList(111, 222)))
            .willReturn(
                asList(
                    Visit.VisitBuilder.aVisit().id(1).petId(111).build(),
                    Visit.VisitBuilder.aVisit().id(2).petId(222).build(),
                    Visit.VisitBuilder.aVisit().id(3).petId(222).build()
                )
            );

        mvc.perform(get("/pets/visits?petId=111,222"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(3)))
            .andExpect(jsonPath("$.items[0].id").value(1))
            .andExpect(jsonPath("$.items[1].id").value(2))
            .andExpect(jsonPath("$.items[2].id").value(3))
            .andExpect(jsonPath("$.items[0].petId").value(111))
            .andExpect(jsonPath("$.items[1].petId").value(222))
            .andExpect(jsonPath("$.items[2].petId").value(222));
    }

    // Test cho POST /owners/*/pets/{petId}/visits với dữ liệu hợp lệ
   @Test
void shouldCreateVisitSuccess() throws Exception {
    int petId = 123;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Đặt timezone thành UTC
    Date visitDate = sdf.parse("2025-03-06");
    
    // Tạo Visit yêu cầu (lưu ý: petId sẽ được ghi đè bởi controller)
    Visit visitRequest = Visit.VisitBuilder.aVisit()
            .date(visitDate)
            .description("Annual checkup")
            .build();
    
    // Giả lập visit sau khi được lưu với id và petId đã set
    Visit savedVisit = Visit.VisitBuilder.aVisit()
            .id(10)
            .date(visitDate)
            .description("Annual checkup")
            .petId(petId)
            .build();
    given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);

    mvc.perform(post("/owners/any/pets/" + petId + "/visits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(visitRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.petId").value(petId))
        .andExpect(jsonPath("$.description").value("Annual checkup"))
        .andExpect(jsonPath("$.date").value("2025-03-06"));
}

    // Test cho GET /owners/*/pets/{petId}/visits trả về danh sách visit cho pet
    @Test
    void shouldGetVisitsForPet() throws Exception {
        int petId = 456;
        Visit visit1 = Visit.VisitBuilder.aVisit().id(1).petId(petId).description("Visit 1").build();
        Visit visit2 = Visit.VisitBuilder.aVisit().id(2).petId(petId).description("Visit 2").build();
        List<Visit> visits = asList(visit1, visit2);
        given(visitRepository.findByPetId(petId)).willReturn(visits);

        mvc.perform(get("/owners/any/pets/" + petId + "/visits")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].petId").value(petId))
            .andExpect(jsonPath("$[0].description").value("Visit 1"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].petId").value(petId))
            .andExpect(jsonPath("$[1].description").value("Visit 2"));
    }

    // Test cho POST với petId không hợp lệ (ví dụ petId < 1)
    @Test
    void shouldFailCreateVisitForInvalidPetId() throws Exception {
        int invalidPetId = 0; // Không hợp lệ vì @Min(1)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date visitDate = sdf.parse("2025-03-06");
        Visit visitRequest = Visit.VisitBuilder.aVisit()
                .date(visitDate)
                .description("Invalid petId test")
                .build();

        mvc.perform(post("/owners/any/pets/" + invalidPetId + "/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(visitRequest)))
            .andExpect(status().isBadRequest());
    }

    // Test cho GET với petId không hợp lệ
    @Test
    void shouldFailGetVisitForInvalidPetId() throws Exception {
        int invalidPetId = 0;

        mvc.perform(get("/owners/any/pets/" + invalidPetId + "/visits")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
