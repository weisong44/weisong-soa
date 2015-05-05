package com.weisong.soa.agent.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JvmModuleJavaConfig {
    
    @Autowired private MainModule mainModule;

    @Bean JvmModule jvmModule() {
    	return new JvmModule(mainModule);
    }
}
