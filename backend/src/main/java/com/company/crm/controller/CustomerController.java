package com.company.crm.controller;

import com.company.crm.dto.customer.UpdateCustomerDTO;
import org.springframework.web.bind.annotation.PutMapping;

import com.company.crm.dto.customer.CreateCustomerDTO;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.dto.customer.FollowRecordQuery;
import com.company.crm.service.ContactService;
import com.company.crm.service.CustomerService;
import com.company.crm.service.FollowRecordService;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.ContactVO;
import com.company.crm.vo.customer.CustomerCreateVO;
import com.company.crm.vo.customer.CustomerDetailVO;
import com.company.crm.vo.customer.CustomerListVO;
import com.company.crm.vo.customer.FollowRecordVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final ContactService contactService;
    private final FollowRecordService followRecordService;

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:update')")
    public ResponseEntity<Void> updateCustomer(@PathVariable Long id,
            @Valid @RequestBody UpdateCustomerDTO dto) {
        customerService.updateCustomer(id, dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:list')")
    public CustomerDetailVO detail(@PathVariable Long id) {
        return customerService.getCustomerDetail(id);
    }
    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('contact:list')")
    public List<ContactVO> contacts(@PathVariable Long id) {
        return contactService.listForCustomer(id);
    }
    @GetMapping("/{id}/follow-records")
    @PreAuthorize("hasAuthority('follow:list')")
    public PageResultVO<FollowRecordVO> follows(@PathVariable Long id,
        @Valid @ModelAttribute FollowRecordQuery query) {
        return followRecordService.listForCustomer(id, query);
    }


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
