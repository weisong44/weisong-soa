package com.weisong.soa.core.zk.config;

import java.util.Properties;

public interface ZkPropertyChangeRegistry {

	static public interface Listener {
		void propertyChanged(Properties props);
	}

	void addPropertyChangeListener(Listener listener);
}
