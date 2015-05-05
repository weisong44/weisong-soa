package com.weisong.soa.agent.impl;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.weisong.common.javaconfig.CommonJmxJavaConfig;
import com.weisong.common.javaconfig.CommonPropertyJavaConfig;

@Configuration
@Import({
	CommonJmxJavaConfig.class
  ,	CommonPropertyJavaConfig.class
})
public class DefaultMgmtAgentJavaConfig extends BaseDefaultMgmtAgentJavaConfig {
}
