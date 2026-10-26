package com.kieumai.notifier.presentation.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateReportRequest {

    @NotNull
    @Positive
    private Long wardId;

    @NotBlank
    @Pattern(regexp = "FLOOD|TRAFFIC_JAM|OTHER")
    private String type;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String locationDescription;

    @NotNull
    @Size(max = 5)
    private List<@NotBlank @Size(max = 2048)
            @Pattern(regexp = "https://[^\\s/?#]+(?:[^\\s]*)?", message = "Must be an HTTPS URL") String> imageUrls = new ArrayList<>();

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal longitude;

    @NotNull
    @PastOrPresent
    private OffsetDateTime observedAt;

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unknown field: " + name);
    }
}
