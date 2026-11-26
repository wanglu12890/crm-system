package com.company.crm.dto.customer;
import lombok.Data;
import jakarta.validation.constraints.*;
@Data
public class FollowRecordQuery {
    @Min(1) private long page = 1;
    @Min(1) @Max(100) private long size = 10;
}
