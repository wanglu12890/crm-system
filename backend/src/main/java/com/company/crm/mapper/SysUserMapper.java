package com.company.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crm.entity.SysUser;
import com.company.crm.vo.user.UserListVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

        /**
         * Queries by the unique login name without applying MyBatis-Plus logical-delete
         * filtering.
         * Authentication needs the deleted flag to distinguish a deleted account from a
         * missing one.
         */
        // 根据用户名查询
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

        // 根据userId查询用户角色编码
        @Select("""
                        SELECT r.role_code
                        FROM sys_user_role ur
                        JOIN sys_role r ON ur.role_id = r.id
                        WHERE ur.user_id = #{userId}
                        AND r.status = 1
                        AND r.deleted = 0
                        """)
        List<String> selectRoleCodesByUserId(Long userId);
         
        // 根据userId查询查询权限编码
        @Select("""
              SELECT DISTINCT p.permission_code
              FROM sys_user_role ur
              JOIN sys_role r ON ur.role_id = r.id
              JOIN sys_role_permission rp ON ur.role_id = rp.role_id
              JOIN sys_permission p ON rp.permission_id = p.id
              WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND p.status = 1
              ORDER BY p.permission_code
              """)
        List<String> selectPermissionCodesByUserId(Long userId);

        // 用戶列表查詢
        @Select("""
                        SELECT CAST(u.id AS CHAR) AS id,
                               u.username,
                               u.real_name AS name,
                               COALESCE(GROUP_CONCAT(DISTINCT r.role_name ORDER BY r.id SEPARATOR '、'), '未分配') AS role,
                               COALESCE(u.mobile, '') AS phone,
                               CASE u.status WHEN 1 THEN '正常' ELSE '停用' END AS status,
                               DATE_FORMAT(u.created_at, '%Y-%m-%d') AS createTime
                          FROM sys_user u
                          LEFT JOIN sys_user_role ur ON ur.user_id = u.id
                          LEFT JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
                         WHERE u.deleted = 0
                         GROUP BY u.id, u.username, u.real_name, u.mobile, u.status, u.created_at
                         ORDER BY u.created_at DESC, u.id ASC
                        """)
        List<UserListVO> selectUserList();

        @Update("""
                        UPDATE sys_user
                           SET last_login_at = #{lastLoginAt},
                               updated_at = CURRENT_TIMESTAMP(3)
                         WHERE id = #{userId}
                           AND deleted = 0
                           AND status = 1
                        """)
        int updateLastLoginAt(
                        @Param("userId") Long userId,
                        @Param("lastLoginAt") LocalDateTime lastLoginAt);

}
