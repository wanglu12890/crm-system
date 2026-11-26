package com.company.crm.mapper;
import org.apache.ibatis.annotations.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crm.entity.Contact;
import com.company.crm.vo.customer.ContactVO;
import java.util.List;
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
 @Select("SELECT CAST(id AS CHAR) AS id, CAST(customer_id AS CHAR) AS customerId, contact_name, gender, position, mobile, telephone, email, is_primary, decision_role, birthday, remark FROM contact WHERE customer_id = #{id} AND deleted = 0 ORDER BY is_primary DESC, id ASC")
 List<ContactVO> selectForCustomer(@Param("id") Long id);
}
