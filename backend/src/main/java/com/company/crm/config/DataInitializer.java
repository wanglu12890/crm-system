package com.company.crm.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysUser;
import com.company.crm.entity.SysUserRole;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.mapper.SysUserRoleMapper;
// Lombok是一个Java库，它通过注解的方式，自动生成一些样板代码，如getter、setter、toString等，从而减少了开发者的工作量，提高了代码的可读性和可维护性。
import lombok.RequiredArgsConstructor; 
import lombok.extern.slf4j.Slf4j;
// CommandLineRunner是Spring Boot提供的一个接口，它用于在应用程序启动后执行一些特定的代码逻辑。实现了CommandLineRunner接口的类会在Spring Boot应用启动完成后自动调用其run方法。
import org.springframework.boot.CommandLineRunner;  
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//Spring Boot应用程序启动时，DataInitializer类会被自动扫描和实例化，并在应用启动完成后执行其run方法，从而确保超级管理员角色、管理员账户以及它们之间的关系被正确初始化。

// @ 表示注解的开始，注解是Java的一种元数据机制，用于在代码中添加额外的信息，通常用于配置、标记或提供编译时和运行时的指令。类似于装饰器，它们可以附加在类、方法、字段等元素上，以改变其行为或提供额外的功能。
// 怎么理解这些注解呢？可以将它们看作是对代码的“标签”或“说明”，告诉编译器或运行时环境如何处理这些代码。
// 例如，@Slf4j告诉编译器为类生成一个日志对象，@Component告诉Spring框架将该类作为组件进行管理，@RequiredArgsConstructor告诉编译器生成一个包含所有final字段的构造函数，@Order指定了类的执行顺序。
// 通过这些注解，开发者可以更方便地实现依赖注入、日志记录、事务管理等功能，而无需手动编写大量样板代码。
// 注解是"标记"或"配置"。真正的功能是由注解处理器（编译器或框架）在编译时或运行时读取这些标记，然后替你生成代码或执行逻辑。
// "通过 @xxx 标记代码，让框架或编译器根据标记来提供功能"
// 注解处理器有两种类型：编译时处理器和运行时处理器。编译时处理器在代码编译阶段读取注解并生成额外的代码或资源，而运行时处理器在应用程序运行时读取注解并执行相应的逻辑。

// @Slf4j注解用于在类中启用日志记录功能，它会自动为类生成一个名为log的日志对象，开发者可以使用这个对象来记录日志信息。
@Slf4j
// @Component注解用于将类标记为Spring的组件，使其能够被Spring容器自动扫描和管理，从而实现依赖注入和生命周期管理。
@Component
// @RequiredArgsConstructor注解用于自动生成一个包含所有final字段的构造函数，从而简化了依赖注入的代码。
@RequiredArgsConstructor
// @Order注解用于指定类的执行顺序，Ordered.HIGHEST_PRECEDENCE表示该类的执行优先级最高，确保在应用启动时最先执行数据初始化逻辑。
@Order(Ordered.HIGHEST_PRECEDENCE)

// 定义一个名为DataInitializer的类，它实现了CommandLineRunner接口，用于在Spring Boot应用启动后执行数据初始化逻辑。
// implements 关键字用于表示类实现了一个接口，这意味着该类必须提供接口中定义的所有方法的具体实现。
public class DataInitializer implements CommandLineRunner {

    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";  // 定义一个常量，表示超级管理员角色的唯一标识符，用于在数据库中查找或创建该角色。
    private static final String INITIAL_ADMIN_USERNAME = "admin";
    private static final String INITIAL_ADMIN_PASSWORD = "admin123456";

    private final SysRoleMapper sysRoleMapper; // 定义一个私有的、不可变的SysRoleMapper对象，用于与数据库中的sys_role表进行交互，实现角色相关的数据操作。
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    // @Override注解用于标识该方法是对父类或接口中定义的方法的重写，确保方法签名正确并提供编译时检查。
    @Override  
    // @Transactional注解用于声明该方法是一个事务性操作，确保在方法执行过程中，如果发生异常，所有数据库操作将回滚，以保持数据的一致性和完整性。rollbackFor = Exception.class表示在发生任何异常时都会触发事务回滚。
    // 事务回滚是指在数据库操作过程中，如果发生了错误或异常，系统会撤销之前的所有操作，将数据库恢复到操作之前的状态，以确保数据的一致性和完整性。
    @Transactional(rollbackFor = Exception.class)
    // run方法是CommandLineRunner接口中定义的抽象方法，它在Spring Boot应用启动完成后被自动调用，用于执行数据初始化逻辑。
    public void run(String... args) {
        SysRole superAdminRole = getOrCreateSuperAdminRole();
        SysUser adminUser = getOrCreateAdminUser();
        ensureUserRoleRelation(adminUser.getId(), superAdminRole.getId());
    }

    // getOrCreateSuperAdminRole方法用于获取或创建超级管理员角色。它首先查询数据库中是否存在具有指定角色代码的角色，如果存在，则返回该角色；如果不存在，则创建一个新的超级管理员角色，并将其插入数据库中。
    private SysRole getOrCreateSuperAdminRole() {
        // selectOne方法用于从数据库中查询单个记录，如果查询结果有多条记录，则会抛出异常。它接受一个LambdaQueryWrapper对象作为参数，用于构建查询条件。
        SysRole existingRole = sysRoleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, SUPER_ADMIN_ROLE_CODE) // eq方法用于添加等于条件，这里表示查询角色代码等于SUPER_ADMIN_ROLE_CODE的记录。
                        .last("LIMIT 1") // last方法用于在SQL语句的末尾添加自定义的SQL片段，这里用于限制查询结果只返回一条记录。
        );
        // 如果existingRole不为null，说明数据库中已经存在超级管理员角色，则记录调试日志并返回该角色。
        if (existingRole != null) {
            log.debug("Super administrator role already exists, initialization skipped.");
            return existingRole;
        }

        SysRole role = new SysRole();
        role.setRoleCode(SUPER_ADMIN_ROLE_CODE);
        role.setRoleName("超级管理员");
        role.setDataScope("ALL");
        role.setStatus(1);
        role.setRemark("系统初始化角色");
        role.setDeleted(0);
        role.setVersion(0);
        sysRoleMapper.insert(role);
        log.info("Super administrator role initialized.");
        return role;
    }

    private SysUser getOrCreateAdminUser() {
        SysUser existingUser = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, INITIAL_ADMIN_USERNAME)
                        .last("LIMIT 1")
        );
        if (existingUser != null) {
            log.debug("Administrator account already exists, initialization skipped.");
            return existingUser;
        }

        SysUser user = new SysUser();
        user.setUsername(INITIAL_ADMIN_USERNAME);
        user.setPasswordHash(passwordEncoder.encode(INITIAL_ADMIN_PASSWORD));
        user.setRealName("系统管理员");
        user.setStatus(1);
        user.setDeleted(0);
        user.setVersion(0);
        sysUserMapper.insert(user);
        log.info("Administrator account initialized. Change the initial password after first login.");
        return user;
    }

    // ensureUserRoleRelation方法用于确保管理员用户与超级管理员角色之间的关系存在。
    // 它首先查询数据库中是否已经存在该用户与角色的关联关系，如果存在，则记录调试日志并跳过初始化；如果不存在，则创建一个新的SysUserRole对象，将用户ID和角色ID设置为对应的值，并将其插入数据库中。
    private void ensureUserRoleRelation(Long userId, Long roleId) {
        Long relationCount = sysUserRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getRoleId, roleId)
        );
        if (relationCount > 0) {
            log.debug("Administrator role relation already exists, initialization skipped.");
            return;
        }

        SysUserRole userRole = new SysUserRole(); // 创建一个新的SysUserRole对象，用于表示管理员用户与超级管理员角色之间的关联关系。
        userRole.setUserId(userId); // 设置用户角色关联表的userId字段为管理员用户的ID，表示该关联关系属于该用户。
        userRole.setRoleId(roleId); 
        sysUserRoleMapper.insert(userRole); // 将新建用户角色关联记录插入表中
        log.info("Administrator role relation initialized.");
    }
}
