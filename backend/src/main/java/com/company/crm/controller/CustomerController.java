package com.company.crm.controller;

import com.company.crm.dto.customer.CreateCustomerDTO;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.service.CustomerService;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.CustomerListVO;
import com.company.crm.vo.customer.CustomerCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('customer:list')")
    public ResponseEntity<PageResultVO<CustomerListVO>> listCustomers(
            @Valid @ModelAttribute CustomerListQuery query
    ) {
        return ResponseEntity.ok(customerService.listCustomers(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:create')")
    public ResponseEntity<CustomerCreateVO> createCustomer(@Valid @RequestBody CreateCustomerDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(dto));
    }
}
