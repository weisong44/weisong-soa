package com.weisong.soa.core.zk.config;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.weisong.soa.core.zk.ZkClient;
import com.weisong.soa.core.zk.bootstrap.BootstrapZkClient;

@Configuration
public class ZkPropertyJavaConfig {
    
	final static private String PROPERTY_FILES_PATH = "property.files.path"; 
	
    static private String[] locations = new String[] {
        "classpath*:/properties/*.properties"
      , "file:/etc/override.properties"
    };

    static private BootstrapZkClient bootstrapZkClient;
    
    @Bean
    static public BootstrapZkClient bootstrapZkClient() throws Exception {
    	if(bootstrapZkClient == null) {
    		bootstrapZkClient = new BootstrapZkClient(); 
    	}
    	return bootstrapZkClient;
    }
    
    @Bean
    static public ZkPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws Exception {
    	ZkClient zkClient = bootstrapZkClient();
    	ZkPropertyPlaceholderConfigurer c = new ZkPropertyPlaceholderConfigurer(zkClient);
        c.setIgnoreResourceNotFound(true);
        c.setIgnoreUnresolvablePlaceholders(true);

        String path = System.getProperty(PROPERTY_FILES_PATH);
        if(path != null) {
        	locations[0] = locations[0].replace("/properties/", "/" + path + "/");
        }
        c.setLocations(createResources(locations));
        return c;
    }

    static private Resource[] createResources(String... locations) throws Exception {
        List<Resource> resources = new LinkedList<>();
        for(String loc : locations) {
            Resource[] temp = new PathMatchingResourcePatternResolver().getResources(loc);
            resources.addAll(Arrays.asList(temp));
        }
        return resources.toArray(new Resource[resources.size()]);
    }
}
