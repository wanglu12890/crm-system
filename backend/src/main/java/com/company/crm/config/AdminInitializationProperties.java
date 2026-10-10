package com.company.crm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "crm.init.admin")
public class AdminInitializationProperties {

    /** Privileged account bootstrap is opt-in and disabled by default. */
    private boolean enabled = false;

    /** Supplied externally only; no default credential is defined in source control. */
    private String password;
}
