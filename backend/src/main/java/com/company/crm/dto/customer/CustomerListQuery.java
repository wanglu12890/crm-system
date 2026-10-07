package com.company.crm.dto.customer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Query conditions only narrow the already resolved server-side data scope. */
@Data
public class CustomerListQuery {

    @Min(value = 1, message = "page must be at least 1")
    private long page = 1;

    @Min(value = 1, message = "size must be at least 1")
    @Max(value = 100, message = "size must not exceed 100")
    private long size = 20;

    @Size(max = 200, message = "keyword must not exceed 200 characters")
    private String keyword;

    @Pattern(regexp = "^(A|B|C|D)?$", message = "customerLevel is invalid")
    private String customerLevel;

    @Pattern(regexp = "^(POTENTIAL|ACTIVE|INACTIVE)?$", message = "status is invalid")
    private String status;

    @Pattern(regexp = "^(ENTERPRISE|INDIVIDUAL)?$", message = "customerType is invalid")
    private String customerType;

    private Long ownerId;
}
