package com.tradingdiary.config;

import com.tradingdiary.entity.SysUser;
import com.tradingdiary.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private static final String PLACEHOLDER = "{BCRYPT_PLACEHOLDER}";

    private final SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-password:admin123}")
    private String adminInitPassword;

    public AdminInitializer(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        SysUser admin = sysUserMapper.selectByUsername("admin");
        if (admin == null) {
            log.info("Admin user not found, skipping password initialization");
            return;
        }

        if (PLACEHOLDER.equals(admin.getPassword())) {
            String encodedPassword = passwordEncoder.encode(adminInitPassword);
            admin.setPassword(encodedPassword);
            sysUserMapper.updateById(admin);
            log.info("Admin password initialized");
        } else {
            log.debug("Admin password already initialized, skipping");
        }
    }
}
