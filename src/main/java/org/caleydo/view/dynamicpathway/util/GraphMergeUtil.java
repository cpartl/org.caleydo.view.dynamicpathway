package org.caleydo.view.dynamicpathway.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.caleydo.core.util.collection.Pair;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout2.animation.AnimatedGLElementContainer;
import org.caleydo.core.view.opengl.layout2.animation.InOutTransitions.IInTransition;
import org.caleydo.datadomain.pathway.graph.PathwayGraph;
import org.caleydo.datadomain.pathway.graph.item.vertex.EPathwayVertexType;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertex;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertexGroupRep;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertexRep;
import org.caleydo.view.dynamicpathway.ui.DynamicPathwaysCanvas;
import org.caleydo.view.dynamicpathway.ui.EdgeElement;
import org.caleydo.view.dynamicpathway.ui.NodeCompoundElement;
import org.caleydo.view.dynamicpathway.ui.NodeElement;
import org.caleydo.view.dynamicpathway.ui.NodeGeneElement;
import org.caleydo.view.dynamicpathway.ui.NodeGroupElement;
import org.jgrapht.graph.DefaultEdge;

public final class GraphMergeUtil {

	private static final int DEFAULT_ADD_PATHWAY_DURATION = DynamicPathwaysCanvas.DEFAULT_ADD_PATHWAY_DURATION;

	public static final NodeElement createNewMergedNodeElement(List<PathwayVertex> sameVerticesList,
			PathwayGraph pathwayToAdd, PathwayVertexRep addingVrep, NodeElement mergingWithNode,
			boolean mergeWithinSameGraph, boolean addToSameGraph, DynamicPathwaysCanvas layoutContainer) {

		/**
		 * Create the merged Vrep
		 */
		PathwayGraph mergedVrepsPathway;
		if (addToSameGraph || mergeWithinSameGraph)
			mergedVrepsPathway = pathwayToAdd;
		else
			mergedVrepsPathway = layoutContainer.getDynamicPathway().getCombinedGraph();
		PathwayVertexRep mergedVrep = GraphMergeUtil.createNewVrep(mergingWithNode.getVertexRep(), sameVerticesList,
				mergedVrepsPathway);

		/**
		 * Create the merged node
		 */

		List<PathwayVertexRep> vreps = new LinkedList<PathwayVertexRep>();
		vreps.add(addingVrep);
		vreps.add(mergingWithNode.getVertexRep());

		List<PathwayVertexRep> vrepList = mergingWithNode.getVreps();
		if (vrepList.size() > 0) {
			for (PathwayVertexRep vrep : vrepList) {
				if (!vreps.contains(vrep))
					vreps.add(vrep);
			}
		}

		NodeElement mergedNode;
		if (mergeWithinSameGraph) {
			mergedNode = GraphMergeUtil.createNewNodeElement(mergedVrep, sameVerticesList, vreps, layoutContainer,
					pathwayToAdd);

			if (!addToSameGraph)
				mergedNode.setIsMerged(false);
		} else {
			Set<PathwayGraph> pathways = new HashSet<PathwayGraph>(mergingWithNode.getPathways());
			pathways.add(pathwayToAdd);
			mergedNode = GraphMergeUtil.createNewNodeElement(mergedVrep, sameVerticesList, vreps, layoutContainer,
					pathways);
		}

		mergedNode.setCenter(mergingWithNode.getCenterX(), mergingWithNode.getCenterY());

		return mergedNode;
	}

	/**
	 * creates a new vertex rep based on a given one with mainVertex as it's name
	 * 
	 * @param oldVrep
	 *            everything besides the name is based on this vrep
	 * @param mainVertex
	 *            defines the vreps name
	 * @return a new vrep
	 */
	public static final PathwayVertexRep createNewVrep(PathwayVertexRep oldVrep, List<PathwayVertex> newVrepVertexList,
			PathwayGraph newVrepsPathway) {
		PathwayVertexRep newVrep = new PathwayVertexRep(newVrepVertexList.get(0).getHumanReadableName(), oldVrep
				.getShapeType().name(), oldVrep.getCenterX(), oldVrep.getCenterY(), oldVrep.getWidth(),
				oldVrep.getHeight());

		for (PathwayVertex mergedVertex : newVrepVertexList)
			newVrep.addPathwayVertex(mergedVertex);

		newVrep.setPathway(newVrepsPathway);

		return newVrep;
	}

	/**
	 * convenience method for creating a new node with just one pathway <br />
	 * creates set with one pathway & calls
	 * {@link #createNewNodeElement(PathwayVertexRep, List, List, DynamicPathwaysCanvas, Color, Set)}
	 * 
	 * @return
	 */
	public static final NodeElement createNewNodeElement(PathwayVertexRep vrep, List<PathwayVertex> pathwayVertices,
			List<PathwayVertexRep> vrepsWithThisNodesVertices, DynamicPathwaysCanvas layoutContainer,
			PathwayGraph pathway) {
		Set<PathwayGraph> pathways = new HashSet<PathwayGraph>();
		pathways.add(pathway);

		return createNewNodeElement(vrep, pathwayVertices, vrepsWithThisNodesVertices, layoutContainer, pathways);
	}

	/**
	 * creates a new node element depending on it's vertices type -> either ground, gene or compound
	 * 
	 * @param vrep
	 *            the vertex representation of the node (provides information, such as the shape type, size, etc.)
	 * @param pathwayVertices
	 *            the vertices, which the node element is representing
	 * @param vrepsWithThisNodesVertices
	 *            if the node links to multiple vreps
	 * @param graphRep
	 *            needed for callback method
	 * @return the created node element
	 */
	public static final NodeElement createNewNodeElement(PathwayVertexRep vrep, List<PathwayVertex> pathwayVertices,
			List<PathwayVertexRep> vrepsWithThisNodesVertices, DynamicPathwaysCanvas layoutContainer,
			Set<PathwayGraph> pathways) {

		/**
		 * create node of correct type to vertex rep -> different shapes
		 */
		NodeElement node;

		if (pathwayVertices.size() == 0 && vrep.getPathwayVertices().size() == 0) {
			PathwayVertexGroupRep groupVrep = (PathwayVertexGroupRep) vrep;

			if (groupVrep.getGroupedVertexReps().size() > 0)
				node = new NodeGroupElement(vrep, pathwayVertices, layoutContainer, pathways);
			else
				return null;
		} else if (pathwayVertices.get(0).getType() == EPathwayVertexType.compound) {
			node = new NodeCompoundElement(vrep, pathwayVertices, layoutContainer, pathways);
		} else {
			node = new NodeGeneElement(vrep, pathwayVertices, layoutContainer, pathways);
		}

		/**
		 * so the layouting algorithm can extinguish, if it's a node or an edge
		 */
		node.setLayoutData(true);

		/**
		 * if this node contains vertices from 2 or more PathwayVertexReps, i.e. it's a merged node
		 */
		if (pathwayVertices != null) {

			// node.setVertices(pathwayVertices);

			if (vrepsWithThisNodesVertices != null) {

				for (PathwayVertexRep alternativeVrep : vrepsWithThisNodesVertices)
					node.addVrepWithThisNodesVerticesList(alternativeVrep);

				node.setIsMerged(true);
				node.setWasMerged(true);
			}
		}

		return node;

	}

	/**
	 * Redirects all edges from one node to another
	 * 
	 * @param originalNode
	 *            all of it's edges are redirected to the node
	 * @param redirectedNode
	 *            all of the other edges are redirected to this node
	 * @param edgeSet
	 *            set containing the edges
	 */
	public static final void redirectEdges(NodeElement originalNode, NodeElement redirectedNode,
			Set<EdgeElement> edgeSet) {
		List<Pair<EdgeElement, Boolean>> edgesContainingThisNode = GraphMergeUtil.getEdgeWithThisNodeAsSourceOrTarget(
				edgeSet, originalNode);
		for (Pair<EdgeElement, Boolean> edgePair : edgesContainingThisNode) {
			// node was edge source
			if (edgePair.getSecond())
				edgePair.getFirst().setSourceNode(redirectedNode);
			else
				edgePair.getFirst().setTargetNode(redirectedNode);
		}
	}

	/**
	 * Gets all the edges from original node and copies them with newNode as source/target instead
	 * 
	 * @param originalNode
	 *            get all it's edges
	 * @param newNode
	 *            new source/target
	 * @param edgeSet
	 *            set which contains the edges
	 * @param layoutContainer
	 *            add new edges to the layout container
	 */
	public static final void copyEdges(NodeElement originalNode, NodeElement newNode, Set<EdgeElement> edgeSet,
			AnimatedGLElementContainer layoutContainer) {

		List<Pair<EdgeElement, Boolean>> edgesContainingThisNode = GraphMergeUtil.getEdgeWithThisNodeAsSourceOrTarget(
				edgeSet, originalNode);
		for (Pair<EdgeElement, Boolean> edgePair : edgesContainingThisNode) {
			EdgeElement edgeOfUnmergedNode = edgePair.getFirst();
			NodeElement sourceNode;
			NodeElement targetNode;
			if (edgePair.getSecond()) {
				sourceNode = newNode;
				targetNode = edgeOfUnmergedNode.getTargetNode();
			} else {
				sourceNode = edgeOfUnmergedNode.getSourceNode();
				targetNode = newNode;
			}
			EdgeElement mergedEdge = new EdgeElement(edgeOfUnmergedNode.getDefaultEdge(), sourceNode, targetNode,
					DEFAULT_ADD_PATHWAY_DURATION);
			mergedEdge.setLayoutData(false);
			edgeSet.add(mergedEdge);
			layoutContainer.add(mergedEdge);
		}
	}

	/**
	 * finds all edgeElement, which contain node either as source or as target node
	 * 
	 * @param edgeSet
	 *            set to look for
	 * 
	 * @param node
	 *            to look for
	 * @return List<Pair<EdgeElement, Boolean>> edgesContainingThisNode, when Boolean = true: node is source node of
	 *         this EdgeElement; is target node otherwise
	 */
	public static final List<Pair<EdgeElement, Boolean>> getEdgeWithThisNodeAsSourceOrTarget(Set<EdgeElement> edgeSet,
			NodeElement node) {

		List<Pair<EdgeElement, Boolean>> edgesContainingThisNode = new LinkedList<Pair<EdgeElement, Boolean>>();

		for (EdgeElement edge : edgeSet) {
			if (edge.getSourceNode().equals(node))
				edgesContainingThisNode.add(new Pair<EdgeElement, Boolean>(edge, true));
			if (edge.getTargetNode().equals(node))
				edgesContainingThisNode.add(new Pair<EdgeElement, Boolean>(edge, false));

		}

		return edgesContainingThisNode;
	}

	/**
	 * add given edge To given edge Set
	 * 
	 * @param edge
	 * @param pathway
	 * @param vertexNodeMap
	 * @param edgeSetToAdd
	 * @throws Exception
	 */
	public static void addEdgeToEdgeSet(DefaultEdge edge, PathwayGraph pathway,
			Map<PathwayVertex, NodeElement> vertexNodeMap, Set<EdgeElement> edgeSetToAdd,
			Map<PathwayVertexRep, NodeElement> vrepToGroupNodeMap, AnimatedGLElementContainer containterToAddElementTo,
			long drawEdgeDelay) throws Exception {
		PathwayVertexRep srcVrep = pathway.getEdgeSource(edge);
		PathwayVertexRep targetVrep = pathway.getEdgeTarget(edge);

		List<NodeElement> srcNodes = new LinkedList<NodeElement>();
		List<NodeElement> targetNodes = new LinkedList<NodeElement>();

		NodeElement groupSrcNode = vrepToGroupNodeMap.get(srcVrep);
		NodeElement groupTargetNode = vrepToGroupNodeMap.get(targetVrep);
		if (groupSrcNode != null) {
			if (!srcNodes.contains(groupSrcNode))
				srcNodes.add(groupSrcNode);
		} else {
			for (PathwayVertex srcVertex : srcVrep.getPathwayVertices()) {

				NodeElement srcNode = vertexNodeMap.get(srcVertex);

				if (srcNode == null) {
					System.out.println("VertexNodeMap didn't contain srcVertex:" + srcVertex);
					continue;
				}

				if (!srcNodes.contains(srcNode))
					srcNodes.add(srcNode);
			}
		}

		if (groupTargetNode != null) {
			if (!targetNodes.contains(groupTargetNode))
				targetNodes.add(groupTargetNode);
		} else {
			for (PathwayVertex targetVertex : targetVrep.getPathwayVertices()) {
				if (!vertexNodeMap.containsKey(targetVertex) || vertexNodeMap.get(targetVertex) == null) {
					// throw new NodeMergingException("targetVertex(" + targetVertex + ") not in uniqueVertexMap");
					System.out.println("VertexNodeMap didn't contain targetVertex:" + targetVertex);
					continue;
				}

				NodeElement targetNode = vertexNodeMap.get(targetVertex);
				if (!targetNodes.contains(targetNode) && targetNode != null)
					targetNodes.add(targetNode);

				// targetNodes.add(vertexNodeMap.get(targetVertex));
			}
		}

		for (NodeElement srcNode : srcNodes) {
			for (NodeElement targetNode : targetNodes) {
				if (srcNode.equals(targetNode))
					continue;

				EdgeElement edgeEl = new EdgeElement(edge, srcNode, targetNode, drawEdgeDelay);

				edgeEl.setLayoutData(false);
				edgeSetToAdd.add(edgeEl);

				containterToAddElementTo.add(edgeEl);
			}
		}

	}

	/**
	 * return list without PathwayVertex with duplicate names
	 * 
	 * @param vertexListToFilter
	 *            list to filter
	 * @return filtered list
	 */
	private List<PathwayVertex> filterVerticesByName(List<PathwayVertex> vertexListToFilter) {
		List<PathwayVertex> filteredList = new LinkedList<PathwayVertex>();

		for (PathwayVertex vertexToCheck : vertexListToFilter) {

			boolean alreadyContainsName = false;
			for (PathwayVertex checkedVertex : filteredList) {
				String checkWith = checkedVertex.getHumanReadableName();
				String check = vertexToCheck.getHumanReadableName();
				if (checkWith.contentEquals(check)) {
					alreadyContainsName = true;
					break;
				}
			}

			if (!alreadyContainsName) {
				filteredList.add(vertexToCheck);
			}
		}

		return filteredList;
	}

}
