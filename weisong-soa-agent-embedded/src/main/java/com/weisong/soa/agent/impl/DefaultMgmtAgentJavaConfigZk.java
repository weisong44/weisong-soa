package com.weisong.soa.agent.impl;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.weisong.common.javaconfig.CommonJmxJavaConfig;
import com.weisong.soa.core.zk.config.ZkPropertyJavaConfig;

@Configuration
@Import({
	CommonJmxJavaConfig.class
  ,	ZkPropertyJavaConfig.class
})
public class DefaultMgmtAgentJavaConfigZk extends BaseDefaultMgmtAgentJavaConfig {
}
