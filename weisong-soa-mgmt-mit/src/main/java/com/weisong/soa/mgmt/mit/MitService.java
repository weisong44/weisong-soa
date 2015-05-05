package com.weisong.soa.mgmt.mit;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

public interface MitService {

	void addOrUpdate(MitNode n);

	int remove(MitNode node, boolean recursive);

	List<MitNode> getNodes(Document filter);

	MitNode getRoot();

	MitNode getById(ObjectId nodeId);

	MitNode getByPath(String path);

	List<MitNode> getChildren(ObjectId nodeId);

	long size();

	void createSingleFieldIndex(String field);
}
