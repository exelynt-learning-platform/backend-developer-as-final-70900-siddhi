package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String type;

    private String description;
}
