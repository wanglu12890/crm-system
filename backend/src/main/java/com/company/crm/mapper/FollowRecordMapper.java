package com.company.crm.mapper;
import org.apache.ibatis.annotations.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crm.entity.FollowRecord;
import com.company.crm.vo.customer.FollowRecordVO;
@Mapper
public interface FollowRecordMapper extends BaseMapper<FollowRecord> {
 @Select("SELECT CAST(f.id AS CHAR) AS id, CAST(f.contact_id AS CHAR) AS contactId, f.follow_type, f.content, f.follow_at, f.next_follow_at, CAST(f.owner_id AS CHAR) AS ownerId, u.real_name AS ownerName FROM follow_record f LEFT JOIN sys_user u ON u.id = f.owner_id WHERE f.target_type = 'CUSTOMER' AND f.target_id = #{id} AND f.deleted = 0 ORDER BY f.follow_at DESC, f.id DESC")
 IPage<FollowRecordVO> selectForCustomer(Page<FollowRecordVO> page, @Param("id") Long id);
}
