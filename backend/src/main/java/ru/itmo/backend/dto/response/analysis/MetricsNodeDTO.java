package ru.itmo.backend.dto.response.analysis;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PackageMetricsNodeDTO.class, name = "package"),
    @JsonSubTypes.Type(value = ClassMetricsNodeDTO.class, name = "class")
})
@Schema(description = "Base interface for metrics nodes (package or class)", 
        subTypes = {PackageMetricsNodeDTO.class, ClassMetricsNodeDTO.class})
public interface MetricsNodeDTO {
    String getName();
}

