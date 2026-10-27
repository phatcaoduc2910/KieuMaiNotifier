package com.kieumai.notifier.presentation.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CreateReportRequest {
    @NotNull @Positive
    private Long wardId;
    @NotBlank @Pattern(regexp = "FLOOD|TRAFFIC_JAM|OTHER")
    private String type;
    @NotBlank @Size(max = 2000)
    private String description;
    @NotNull @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal latitude;
    @NotNull @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal longitude;
    @NotNull @PastOrPresent
    private OffsetDateTime observedAt;

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unknown field: " + name);
    }
}
