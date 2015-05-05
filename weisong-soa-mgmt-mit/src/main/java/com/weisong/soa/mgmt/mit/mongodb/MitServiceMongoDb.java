package com.weisong.soa.mgmt.mit.mongodb;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.weisong.soa.mgmt.mit.MitNode;
import com.weisong.soa.mgmt.mit.MitService;

public class MitServiceMongoDb implements MitService {
	
	private MongoClient client;
	private MongoDatabase db;
	private UpdateOptions updateOptions;
	
	public MitServiceMongoDb(MongoClient client) {
		this.client = client;
		this.db = this.client.getDatabase("tree");
		this.updateOptions = new UpdateOptions();
		this.updateOptions.upsert(true);
		if(getRoot() == null) {
			// Create index
			createSingleFieldIndex("parentId");
			createSingleFieldIndex("name");
			createSingleFieldIndex("path");
			// Create root
			MitNode root = new MitNode(null, "ROOT");
			addOrUpdate(root);
		}
	}
	
	private Document toDoc(MitNode node) {
		Document props = new Document();
		for(Map.Entry<String, Object> e : node.getProperties().entrySet()) {
			props.append(e.getKey(), e.getValue());
		}
		Document doc = new Document()
			.append("_id", node.getId())
			.append("name", node.getName())
			.append("parentId", node.getParentId())
			.append("path", node.getPath())
			.append("properties", props);
		return doc;
	}
	
	private MitNode fromDoc(Document doc) {
		ObjectId id = doc.getObjectId("_id");
		ObjectId parentId = doc.getObjectId("parentId");
		String name = doc.getString("name");
		String path = doc.getString("path");
		
		MitNode node = new MitNode(id, parentId, path, name);
		Document properties = (Document) doc.get("properties");
		for(String key : properties.keySet()) {
			Object value = properties.get(key);
			node.setProperty(key, value);
		}
		return node;
	}
	
	private MongoCollection<Document> getCollection() {
		return db.getCollection("treenode");
	}
	
	@Override
	public void addOrUpdate(MitNode n) {
		Document doc = toDoc(n);
		getCollection().replaceOne(new Document("_id", n.getId()), doc, updateOptions);
	}

	@Override
	public int remove(MitNode n, boolean recursive) {
		List<MitNode> children = getChildren(n.getId());
		if(recursive == false && children.isEmpty() == false) {
			throw new RuntimeException(String.format(
				"Node %s [%s] can't be deleted since it still have children", 
				n.getName(), n.getId()));
		}
		// Do the deletion
		AtomicInteger count = new AtomicInteger();
		removeRecursive(n, count);
		return count.intValue();
	}

	private void removeRecursive(MitNode n, AtomicInteger count) {
		for(MitNode c : getChildren(n.getId())) {
			removeRecursive(c, count);
		}
		getCollection().deleteOne(new Document("_id", n.getId()));
		count.incrementAndGet();
	}
	
	private MitNode getOneNode(Document filter) {
		for(Document doc : getCollection().find(filter)) {
			return fromDoc(doc);
		}
		return null;
	}
	
	@Override
	public List<MitNode> getNodes(Document filter) {
		List<MitNode> result = new LinkedList<>();
		for(Document doc : getCollection().find(filter)) {
			result.add(fromDoc(doc));
		}
		return result;
	}

	@Override
	public MitNode getRoot() {
		return getByPath("/ROOT");
	}

	@Override
	public MitNode getById(ObjectId nodeId) {
        return getOneNode(new Document("_id", nodeId));
	}

	@Override
	public MitNode getByPath(String path) {
        return getOneNode(new Document("path", path));
	}

	@Override
	public List<MitNode> getChildren(ObjectId nodeId) {
		return getNodes(new Document("parentId", nodeId));
	}

	@Override
	public long size() {
		return getCollection().count();
	}

	@Override
	public void createSingleFieldIndex(String field) {
		getCollection().createIndex(new Document(field, 1));
	}
}
