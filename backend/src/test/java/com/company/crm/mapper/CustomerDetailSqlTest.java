package com.company.crm.mapper;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;
import static org.assertj.core.api.Assertions.*;
class CustomerDetailSqlTest {
 @Test void childQueriesConstrainParentTypeDeletionAndOrder() throws Exception {
  String contacts = ContactMapper.class.getMethod("selectForCustomer", Long.class).getAnnotation(Select.class).value()[0];
  assertThat(contacts).contains("customer_id = #{id}", "deleted = 0", "ORDER BY is_primary DESC, id ASC", "telephone");
  String follows = FollowRecordMapper.class.getMethod("selectForCustomer", com.baomidou.mybatisplus.extension.plugins.pagination.Page.class, Long.class).getAnnotation(Select.class).value()[0];
  assertThat(follows).contains("target_type = 'CUSTOMER'", "target_id = #{id}", "f.deleted = 0", "ORDER BY f.follow_at DESC, f.id DESC", "LEFT JOIN sys_user");
 }
 @Test void detailReusesListVisibility() throws Exception {
  try (var input = getClass().getResourceAsStream("/mapper/CustomerMapper.xml")) {
   String xml = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
   String detail = xml.substring(xml.indexOf("<select id=\"selectCustomerDetail\""));
   assertThat(detail).contains("customerVisibilityConditions", "c.id = #{id}");
  }
 }
}
