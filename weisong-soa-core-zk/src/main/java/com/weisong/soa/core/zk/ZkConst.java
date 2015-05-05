package com.weisong.soa.core.zk;

public class ZkConst {
	//
	// System property names
	//
	/** System property, host:port */
	final static public String ZK_CONN_STR = "ZK_CONN_STR";
	/** System property, default to /bootstrap/data */
	final static public String ZK_BOOTSTRAP_NODE_PATH = "ZK_BOOTSTRAP_NODE_PATH";
	/** System property, the path to a node where an application reads its configuration */
	final static public String ZK_NODE_PATH = "ZK_NODE_PATH";
	/** System property, boolean */
	final static public String ZK_DISABLED = "ZK_DISABLED";
	
	
	//
	// ZK directories
	//
	/** The root directory for bootstrap */
	static public String ZK_BOOTSTRAP_ROOT = "/bootstrap";
	/** The root directory for configuration */
	static public String ZK_CONFIG_ROOT = "/config";
	/** The root directory for configuration */
	static public String ZK_REGISTRY_ROOT = "/registry";
}
