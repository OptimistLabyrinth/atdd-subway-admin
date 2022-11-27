package nextstep.subway.station;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철역 관련 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StationAcceptanceTest {
    @LocalServerPort
    int port;

    @BeforeEach
    public void setUp() {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = port;
        }
    }

    void createStation(String value) {
        final Map<String, String> params = new HashMap<>();
        params.put("name", value);
        RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().post("/stations")
                .then().log().all();
    }

    /**
     * When 지하철역을 생성하면
     * Then 지하철역이 생성된다
     * Then 지하철역 목록 조회 시 생성한 역을 찾을 수 있다
     */
    @Nested
    @DisplayName("지하철역을 생성한다.")
    class CreateStation {
        @Test
        void success() {
            // when
            final Map<String, String> params = new HashMap<>();
            params.put("name", "잠실역");

            final ExtractableResponse<Response> response =
                    RestAssured.given().log().all()
                            .body(params)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .when().post("/stations")
                            .then().log().all()
                            .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

            // then
            final List<String> stationNames =
                    RestAssured.given().log().all()
                            .when().get("/stations")
                            .then().log().all()
                            .extract().jsonPath().getList("name", String.class);
            assertThat(stationNames).containsAnyOf("강남역");
        }
    }

    /**
     * Given 지하철역을 생성하고
     * When 기존에 존재하는 지하철역 이름으로 지하철역을 생성하면
     * Then 지하철역 생성이 안된다
     */
    @Nested
    @DisplayName("기존에 존재하는 지하철역 이름으로 지하철역을 생성할 수 없다.")
    class CreateStationWithDuplicateName {
        @Test
        void badRequest() {
            // given
            final String stationName = "강남역";
            createStation(stationName);

            // given
            final Map<String, String> params = new HashMap<>();
            params.put("name", stationName);

            // when
            final ExtractableResponse<Response> response =
                    RestAssured.given().log().all()
                            .body(params)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .when().post("/stations")
                            .then().log().all()
                            .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }
    }

    /**
     * Given 2개의 지하철역을 생성하고
     * When 지하철역 목록을 조회하면
     * Then 2개의 지하철역을 응답 받는다
     */
    @Nested
    @DisplayName("지하철역을 조회한다.")
    class GetStations {
        @Test
        void ok() {
            // given
            final String stationName1 = "삼성역";
            createStation(stationName1);
            final String stationName2 = "역삼역";
            createStation(stationName2);

            // when
            final ExtractableResponse<Response> response =
                    RestAssured.given().log().all()
                            .when().get("/stations")
                            .then().log().all()
                            .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            // then
            final List<String> stationNames =
                    RestAssured.given().log().all()
                            .when().get("/stations")
                            .then().log().all()
                            .extract().jsonPath().getList("name", String.class);
            assertThat(stationNames).contains(stationName1, stationName2);
        }
    }

    /**
     * Given 지하철역을 생성하고
     * When 그 지하철역을 삭제하면
     * Then 그 지하철역 목록 조회 시 생성한 역을 찾을 수 없다
     */
    @Nested
    @DisplayName("지하철역을 제거한다.")
    class DeleteStation {
        @Test
        void notFoundInList() {
            // given
            final String targetStationName = "교대역";
            final Map<String, String> params = new HashMap<>();
            params.put("name", targetStationName);
            final Response createdResponse =
                    RestAssured.given().log().all()
                            .body(params)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .when().post("/stations");

            // given
            final Long createdId = createdResponse.as(StationResponse.class).getId();
            final ExtractableResponse<Response> deleteResponse =
                    RestAssured.given().log().all()
                            .pathParam("id", createdId)
                            .when().delete("/stations/{id}")
                            .then().log().all()
                            .extract();

            // when
            final ExtractableResponse<Response> response =
                    RestAssured.given().log().all()
                            .when().get("/stations")
                            .then().log().all()
                            .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            // then
            final List<String> stationNames = response.jsonPath().getList("name", String.class);
            assertThat(stationNames).doesNotContain(targetStationName);
        }
    }
}
