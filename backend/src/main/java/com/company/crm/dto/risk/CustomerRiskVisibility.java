package com.company.crm.dto.risk;

/** Minimal ownership/state projection for current-page access checks, never sent to a model. */
public record CustomerRiskVisibility(Long customerId, Long ownerId, Long departmentId, String status) {}
