package com.company.crm.service;
import com.company.crm.mapper.*;
import com.company.crm.service.impl.*;
import com.company.crm.exception.CustomerNotFoundException;
import com.company.crm.dto.customer.FollowRecordQuery;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CustomerChildrenAccessTest {
 @Test void invisibleParentPreventsAllChildQueries() {
  CustomerService parent = mock(CustomerService.class);
  ContactMapper contacts = mock(ContactMapper.class);
  FollowRecordMapper follows = mock(FollowRecordMapper.class);
  when(parent.getCustomerDetail(9L)).thenThrow(new CustomerNotFoundException());
  assertThatThrownBy(() -> new ContactServiceImpl(parent, contacts).listForCustomer(9L)).isInstanceOf(CustomerNotFoundException.class);
  assertThatThrownBy(() -> new FollowRecordServiceImpl(parent, follows).listForCustomer(9L, new FollowRecordQuery())).isInstanceOf(CustomerNotFoundException.class);
  verifyNoInteractions(contacts, follows);
 }
}
