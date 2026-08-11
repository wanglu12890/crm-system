package com.company.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crm.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * Queries by the unique login name without applying MyBatis-Plus logical-delete filtering.
     * Authentication needs the deleted flag to distinguish a deleted account from a missing one.
     */
    @Select("""
            SELECT id,
                   username,
                   password_hash AS passwordHash,
                   real_name AS realName,
                   mobile,
                   email,
                   status,
                   last_login_at AS lastLoginAt,
                   created_by AS createdBy,
                   updated_by AS updatedBy,
                   created_at AS createdAt,
                   updated_at AS updatedAt,
                   deleted,
                   version
              FROM sys_user
             WHERE username = #{username}
             LIMIT 1
            """)
    SysUser selectByUsername(@Param("username") String username);

}
