package com.weisong.soa.mgmt.mit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.bson.types.ObjectId;

@EqualsAndHashCode
public class MitNode implements Serializable {
	
	private static final long serialVersionUID = 1L;
		
	@Getter @Setter private ObjectId id;
	@Getter private ObjectId parentId;
	@Getter private String name;
	@Getter private String path;
	
	@Getter @Setter private Map<String, Object> properties = new HashMap<>();

	protected MitNode() {}
	
    public MitNode(MitNode parent, String name) {
    	this(
			parent == null ? null : parent.getId(), 
			parent == null ? null : parent.getPath(), 
			name);
    }
    
    public MitNode(ObjectId parentId, String parentPath, String name) {
    	this.name = name;
    	this.id = ObjectId.get();
    	this.parentId = parentId != null ?
			parentId
		  :	null;
    	this.path = parentPath != null ?
			parentPath + "/" + this.name
		  :	"/" + this.name;
    }
	
    public MitNode(ObjectId id, ObjectId parentId, String path, String name) {
    	this.name = name;
    	this.id = id == null ? 
			ObjectId.get()
		  :	id;
    	this.parentId = parentId != null ?
			parentId
		  :	null;
    	this.path = path;
    }
	
    public Object getProperty(String name) {
    	return properties.get(name);
    }
    
    public void setProperty(String name, Object value) {
    	properties.put(name, value);
    }
}
