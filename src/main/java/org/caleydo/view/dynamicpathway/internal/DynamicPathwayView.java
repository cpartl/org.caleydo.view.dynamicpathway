/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.dynamicpathway.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.caleydo.core.event.EventListenerManager;
import org.caleydo.core.event.EventListenerManager.ListenTo;
import org.caleydo.core.serialize.ASerializedView;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.canvas.IGLCanvas;
import org.caleydo.core.view.opengl.layout2.AGLElementView;
import org.caleydo.core.view.opengl.layout2.GLElement;
import org.caleydo.core.view.opengl.layout2.GLElementContainer;
import org.caleydo.core.view.opengl.layout2.animation.AnimatedGLElementContainer;
import org.caleydo.core.view.opengl.layout2.basic.GLButton;
import org.caleydo.core.view.opengl.layout2.basic.GLButton.ISelectionCallback;
import org.caleydo.core.view.opengl.layout2.layout.GLPadding;
import org.caleydo.core.view.opengl.layout2.layout.GLSizeRestrictiveFlowLayout;
import org.caleydo.datadomain.pathway.graph.PathwayGraph;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertex;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertexRep;
import org.caleydo.view.dynamicpathway.internal.serial.SerializedDynamicPathwayView;
import org.caleydo.view.dynamicpathway.layout.GLFruchtermanReingoldLayout;
import org.caleydo.view.dynamicpathway.layout.GLFruchtermanReingoldLayoutBuilder;
import org.caleydo.view.dynamicpathway.ranking.RankingElement;
import org.caleydo.view.dynamicpathway.ui.ChangeFocusNodeEvent;
import org.caleydo.view.dynamicpathway.ui.ChangeVertexEnvironmentEvent;
import org.caleydo.view.dynamicpathway.ui.DynamicPathwayGraphRepresentation;
import org.caleydo.view.dynamicpathway.ui.FilterPathwayEvent;
import org.caleydo.view.dynamicpathway.ui.MakeFocusPathwayEvent;
import org.caleydo.view.dynamicpathway.ui.NodeElement;
import org.caleydo.view.dynamicpathway.ui.RemoveDisplayedPathwayEvent;
import org.caleydo.view.entourage.SideWindow;
import org.caleydo.view.entourage.SlideInElement;
import org.caleydo.view.entourage.SlideInElement.ESlideInElementPosition;
import org.caleydo.view.entourage.ranking.IPathwayFilter;
import org.jgrapht.graph.DefaultEdge;

/**
 * view, which can represent different pathways combined into one pathway
 * 
 * @author Christiane Schwarzl
 * 
 */
public class DynamicPathwayView extends AGLElementView /* implements IEventBasedSelectionManagerUser */{
	public static final String VIEW_TYPE = "org.caleydo.view.dynamicpathway";
	public static final String VIEW_NAME = "DynamicPathway";

	private DynamicPathwayWindow activeWindow;
	private DynamicPathwayWindow rankingWindow;

	private RankingElement rankingElement;

	private DynamicPathwayGraphRepresentation dynamicGraphCanvas;
	private GLFruchtermanReingoldLayout pathwayLayout;

	private GLElementContainer rootContainer = new GLElementContainer(new GLSizeRestrictiveFlowLayout(true, 1,
			GLPadding.ZERO));

	private AnimatedGLElementContainer pathwayListBaseContainer = new AnimatedGLElementContainer(
			new GLSizeRestrictiveFlowLayout(true, 10, GLPadding.ZERO));

	private ControllbarContainer controllBar;

	private CommonVertexListFilter filter = null;

	public DynamicPathwayView(IGLCanvas glCanvas) {
		super(glCanvas, VIEW_TYPE, VIEW_NAME);

		createPathwayGraphView();

		createRankingSideBar();
		pathwayListBaseContainer.setSize(200, Float.NaN);

		rootContainer.add(pathwayListBaseContainer);
		rootContainer.add(dynamicGraphCanvas);
		GLElementContainer cont = new GLElementContainer(new GLSizeRestrictiveFlowLayout(false, 3, GLPadding.ZERO));
		cont.setSize(200, Float.NaN);
		this.controllBar = new ControllbarContainer(this);
		cont.add(controllBar);
		rootContainer.add(cont);

	}

	public EventListenerManager getEventListenerManager() {
		return eventListeners;
	}

	@Override
	public ASerializedView getSerializableRepresentation() {
		return new SerializedDynamicPathwayView();
	}

	@Override
	protected GLElement createRoot() {
		return rootContainer;
	}

	public void setActiveWindow(DynamicPathwayWindow activeWindow) {
		if (activeWindow != null && this.activeWindow != null && activeWindow != this.activeWindow) {
			this.activeWindow.setActive(false);
		}

		this.activeWindow = activeWindow;

	}

	/**
	 * if a pathway in the list was selected, it is added to the representation, so this pathway graph is drawn on the
	 * right side
	 * 
	 * @param pathwayToAdd
	 *            the pathway which was selected
	 */
	public void addPathway(PathwayGraph pathwayToAdd) {
		Boolean addContextPathway = (dynamicGraphCanvas.getFocusGraph() != null && (dynamicGraphCanvas
				.getFocusNode() != null)) ? true : false;
		try {
			int envSize = controllBar.getNodeEnvironmentSize();
			if (addContextPathway && envSize > 0) {
				PathwayGraph subPathway = createPathwayWithFocusVertexAndHisEnvironment(pathwayToAdd, envSize);

				if (subPathway != null)
					dynamicGraphCanvas.addPathwayRep(subPathway, !addContextPathway, true);
				else {
					addPathwayToControllBar(pathwayToAdd, false, Color.LIGHT_GRAY);
					dynamicGraphCanvas.getDynamicPathway().addFocusOrKontextPathway(pathwayToAdd, true);
					System.out.println("Pathway didn't contain the focus vrep");
				}
			} else {
				dynamicGraphCanvas.addPathwayRep(pathwayToAdd, !addContextPathway, false);
			}

		} catch (Exception e) {
			e.printStackTrace();
			// System.exit(-1);
		}

	}

	public void addPathwayToControllBar(PathwayGraph pathwayToAdd, boolean isFocusPathway, Color titleColor) {
		try {
			controllBar.addPathwayTitle(pathwayToAdd, isFocusPathway, titleColor);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * if the option display nodes with or without 0 degree (doesn't have nodes) was selected, this method is called
	 * 
	 * it repaint all displayed pathways, if the option has changed
	 * 
	 * @param addWithZeroDegreeVertices
	 * @throws Exception
	 */
	public void paintGraphWithOrWithoutZeroDegreeVertices(boolean addWithZeroDegreeVertices) {
		if (dynamicGraphCanvas.isDisplayOnlyVerticesWithEdges() == addWithZeroDegreeVertices)
			return;

		dynamicGraphCanvas.setDisplayOnlyVerticesWithEdges(addWithZeroDegreeVertices);

		List<PathwayGraph> contextPathways = new LinkedList<PathwayGraph>(dynamicGraphCanvas.getContextPathways());

		Boolean hasContextPathways = (contextPathways.size() > 0) ? true : false;

		if (dynamicGraphCanvas.getFocusGraph() != null)
			dynamicGraphCanvas.addPathwayRep(dynamicGraphCanvas.getFocusGraph(), true, !hasContextPathways);

		if (hasContextPathways) {
			for (PathwayGraph contextGraph : contextPathways)

				dynamicGraphCanvas.addPathwayRep(contextGraph, false, false);

		}

	}

	public void paintGraphWithOrWithoutDuplicateVertices(boolean addWithDuplicateVertices) {

		/**
		 * can't be called, when context graphs are already displayed -> can't merge graphs with duplicates
		 */
		if (dynamicGraphCanvas.getContextPathways().size() > 0) {
			System.out
					.println("Can't change this option, when kontext graphs are displayed, because merging graphs with duplicates is not possible");
			return;
		}

		dynamicGraphCanvas.setRemoveDuplicateVertices(!addWithDuplicateVertices);

		if (dynamicGraphCanvas.getFocusGraph() != null)
			dynamicGraphCanvas.addPathwayRep(dynamicGraphCanvas.getFocusGraph(), true, true);
	}

	@ListenTo
	public void onChangeVertexEnvironmentSize(ChangeVertexEnvironmentEvent event) {

		// controllBar.setNodeEnvironmentSize(newValue);

		if (dynamicGraphCanvas.getFocusGraph() == null)
			return;

		readdPresentPathways();
	}

	/**
	 * listens if the path should be filtered or not used for selecting a kontext pathway, which contains the requested
	 * vertex
	 * 
	 * @param event
	 */
	@ListenTo
	public void onChangeFocusNode(ChangeFocusNodeEvent event) {
		NodeElement newFocusNode = event.getNodeElementToFilterBy();
		System.out.println("WHATT??? " + newFocusNode);

		boolean focusNodeChanged = dynamicGraphCanvas.setFocusNode(newFocusNode);

		// newFocusNode.makeThisFocusNode();

		if (newFocusNode != null) {
			System.out.println("newFocus != null");

			System.out.println("newFocus: " + newFocusNode.getVertexRep());

			filterPathwayList(newFocusNode.getVertices(), dynamicGraphCanvas.getDisplayedPathways());
		}

		/**
		 * if the focus node has changed, the pathways need to be re-added, since the context pathways were "built"
		 * around the focus node, unless the full pathways (not just a subset) were added
		 */
		System.out.println("Chanigng focus node + " + focusNodeChanged);
		if (focusNodeChanged && dynamicGraphCanvas.getContextPathways().size() > 0/* && contextPathwayAddedFully */) {
			System.out.println("Chanigng focus node, am i?");
			readdPresentPathways();
			System.out.println("CHANGING");

			// List<PathwayGraph> contextPathways = new
			// LinkedList<PathwayGraph>(dynamicGraphCanvas.getContextPathways());
			//
			// dynamicGraphCanvas.addPathwayRep(dynamicGraphCanvas.getFocusGraph(), true, false, true);
			//
			// for (PathwayGraph contextPathway : contextPathways) {
			// try {
			//
			// /**
			// * if the contextGraph was a subpathway, get the full pathway, else just use the contextPathway
			// */
			// PathwayGraph fullPathway = dynamicGraphCanvas.getOriginalPathwaysOfSubpathway(contextPathway);
			// System.out.println("ContextPw: " + contextPathway + " fullPW: " + fullPathway + " envSize: " +
			// controllBar.getNodeEnvironmentSize());
			// if (fullPathway == null)
			// fullPathway = contextPathway;
			//
			// PathwayGraph pathwayToAdd;
			// if (controllBar.getNodeEnvironmentSize() < 1)
			// pathwayToAdd = fullPathway;
			// else
			// pathwayToAdd = createPathwayWithFocusVertexAndHisEnvironment(fullPathway,
			// controllBar.getNodeEnvironmentSize());
			//
			// if (pathwayToAdd != null) {
			// dynamicGraphCanvas.addPathwayRep(pathwayToAdd, false, false, false);
			// } else {
			// addPathwayToControllBar(contextPathway, false, Color.LIGHT_GRAY);
			// dynamicGraphCanvas.getDynamicPathway().addFocusOrKontextPathway(contextPathway, true);
			// System.out.println("Pathway didn't contain focus node");
			// }
			// } catch (Exception e) {
			// e.printStackTrace();
			// System.exit(-1);
			// }
			//
			// }
		}
	}

	private void readdPresentPathways() {
		List<PathwayGraph> contextPathways = new LinkedList<PathwayGraph>(dynamicGraphCanvas.getContextPathways());

		dynamicGraphCanvas.addPathwayRep(dynamicGraphCanvas.getFocusGraph(), true, false);

		for (PathwayGraph contextPathway : contextPathways) {
			try {

				PathwayGraph pathwayToAdd;

				/**
				 * if the contextGraph was a subpathway, get the full pathway, else just use the contextPathway
				 */
				PathwayGraph fullPathway = dynamicGraphCanvas.getOriginalPathwaysOfSubpathway(contextPathway);
				System.out.println("ContextPw: " + contextPathway + " fullPW: " + fullPathway + " envSize: "
						+ controllBar.getNodeEnvironmentSize());
				if (fullPathway == null)
					fullPathway = contextPathway;
				if (controllBar.getNodeEnvironmentSize() < 1)
					pathwayToAdd = fullPathway;
				else
					pathwayToAdd = createPathwayWithFocusVertexAndHisEnvironment(fullPathway,
							controllBar.getNodeEnvironmentSize());

				if (pathwayToAdd != null) {
					dynamicGraphCanvas.addPathwayRep(pathwayToAdd, false, false);
				} else {
					addPathwayToControllBar(contextPathway, false, Color.LIGHT_GRAY);
					dynamicGraphCanvas.getDynamicPathway().addFocusOrKontextPathway(contextPathway, true);
					System.out.println("Pathway didn't contain focus node");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

		}
	}

	@ListenTo
	public void onFilterPathwayList(FilterPathwayEvent filterPathway) {
		NodeElement filterNode = filterPathway.getNodeElementToFilterBy();

		if (filterNode != null) {
			dynamicGraphCanvas.setOrResetSelectedNode(filterNode);
			filterPathwayList(filterNode.getVertices(), dynamicGraphCanvas.getDisplayedPathways());

		}

	}

	@ListenTo
	public void onRemovePathway(RemoveDisplayedPathwayEvent removePathwayEvent) {
		removePathway(removePathwayEvent.getPathway());
	}

	@ListenTo
	public void onMakeThisPathwayFocus(MakeFocusPathwayEvent makeFocusPathwayEvent) throws Exception {

		PathwayGraph newFocusPathwayGraph = makeFocusPathwayEvent.getPathway();

		PathwayGraph pathwayToAdd = null;
		if (newFocusPathwayGraph == null || dynamicGraphCanvas.getDynamicPathway().isFocusGraph(newFocusPathwayGraph))
			return;
		
		

		// if the context pathway was a sub pathway, get it's original full version & make this the new focus pathway
		if (dynamicGraphCanvas.isSubPathway(newFocusPathwayGraph) == true) {
			// removePathway(newFocusPathwayGraph);
			pathwayToAdd = dynamicGraphCanvas.getOriginalPathwaysOfSubpathway(newFocusPathwayGraph);
			dynamicGraphCanvas.removeOriginalPathwayAndSubpathwayOfMap(newFocusPathwayGraph);
		} else
			pathwayToAdd = newFocusPathwayGraph;

		System.out.println("Is sub pathway? " + dynamicGraphCanvas.isSubPathway(newFocusPathwayGraph));

		/**
		 * get the old focus pathway & add it to the new context graphs
		 * 
		 * remove new focus pathway from old context pathways
		 */
		PathwayGraph oldFocusPathway = dynamicGraphCanvas.getFocusGraph();
		System.out.println("Old Title: " + oldFocusPathway.getTitle());
		List<PathwayGraph> newContextGraphs = new ArrayList<PathwayGraph>(dynamicGraphCanvas.getContextPathways());
		newContextGraphs.remove(newFocusPathwayGraph);

		// TODO: add when focus node thing was fixed
		if (controllBar.getNodeEnvironmentSize() > 0) {
			PathwayGraph subPathway = createPathwayWithFocusVertexAndHisEnvironment(oldFocusPathway,
					controllBar.getNodeEnvironmentSize());
			newContextGraphs.add(subPathway);
		} else {
			newContextGraphs.add(oldFocusPathway);
		}

		dynamicGraphCanvas.addPathwayRep(pathwayToAdd, true, false);
		System.out.println("New Title: " + dynamicGraphCanvas.getFocusGraph() + ", old Title: "
				+ oldFocusPathway.getTitle());

		for (PathwayGraph contextGraph : newContextGraphs) {
			dynamicGraphCanvas.addPathwayRep(contextGraph, false, false);
		}

	}

	/**
	 * remove a pathway from the canvas
	 * 
	 * @param pathwayToRemove
	 */
	public void removePathway(PathwayGraph pathwayToRemove) {
		try {
			// PathwayGraph pathwayToRemove =
			// currentPathwayElement.getDynamicPathway().getPathwayWithThisTitle(graphTitle);

			// if the graph to remove is the focus graph, reset everything
			if (dynamicGraphCanvas.getDynamicPathway().isFocusGraph(pathwayToRemove)) {
				dynamicGraphCanvas.clearCanvasAndInfo(true);
				dynamicGraphCanvas.getDynamicPathway().removeAllPathways();
				controllBar.removeFocusPathwayTitle(pathwayToRemove);
			} else {
				dynamicGraphCanvas.getDynamicPathway().removeContextPathway(pathwayToRemove);
				controllBar.removeContextPathwayTitle(pathwayToRemove);

				List<PathwayGraph> presentContextGraphs = new ArrayList<PathwayGraph>(
						dynamicGraphCanvas.getContextPathways());
				presentContextGraphs.remove(pathwayToRemove);

				if (dynamicGraphCanvas.isSubPathway(pathwayToRemove))
					dynamicGraphCanvas.removeOriginalPathwayAndSubpathwayOfMap(pathwayToRemove);

				dynamicGraphCanvas.addPathwayRep(dynamicGraphCanvas.getFocusGraph(), true, false);

				for (PathwayGraph contextGraph : presentContextGraphs) {
					dynamicGraphCanvas.addPathwayRep(contextGraph, false, false);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 * check if this pathway is not already the main drawn pathway so it isn't drawn again
	 * 
	 * @param pathway
	 *            the pathway which should be checked
	 * @return true if it's the same, that is already drawn, false otherwise
	 */
	public boolean isPathwayPresent(PathwayGraph pathway) {
		return dynamicGraphCanvas.isPathwayPresent(pathway);

	}

	/**
	 * filter the pathway list on the left leaves pathways which contain the given PathwayVertexRep
	 * 
	 * called when a vertex is selected @see org.caleydo.view.dynamicpathway.ui.DynamicPathwayGraphRepresentation
	 * #setOrResetSelectedNode(org.caleydo.view.dynamicpathway.ui.NodeElement)
	 * 
	 * @param currentContextVertexRep
	 *            the currently selected node, after which the list is selected
	 */
	public void filterPathwayList(List<PathwayVertex> verticesToFilter, List<PathwayGraph> displayedPathways) {

		filter = new CommonVertexListFilter(verticesToFilter, displayedPathways);
		rankingElement.setFilter(filter);
		rankingElement.relayout();
	}

	public static class CommonVertexListFilter implements IPathwayFilter {

		private Set<PathwayGraph> pathways = new HashSet<>();

		public CommonVertexListFilter(List<PathwayVertex> vertices, List<PathwayGraph> pathwaysToIgnore) {

			for (PathwayVertex vertex : vertices) {
				List<PathwayVertexRep> vertexReps = vertex.getPathwayVertexReps();
				for (PathwayVertexRep vr : vertexReps) {
					PathwayGraph pathway = vr.getPathway();
					if ((pathwaysToIgnore != null) && pathwaysToIgnore.contains(pathway))
						continue;
					pathways.add(pathway);
				}
			}

		}

		@Override
		public boolean showPathway(PathwayGraph pathway) {
			return pathways.contains(pathway);
		}

	}

	public void unfilterPathwayList() {
		rankingElement.removeFilter(filter);
		rankingElement.relayout();
	}

	public PathwayGraph getCurrentFocusPathway() {
		return dynamicGraphCanvas.getFocusGraph();
	}

	/**
	 * view for representing pathway graphs
	 */
	private void createPathwayGraphView() {
		pathwayLayout = new GLFruchtermanReingoldLayoutBuilder().repulsionMultiplier(-1.0).attractionMultiplier(18.0)
				.nodeBoundsExtension(4.0).buildLayout();

		dynamicGraphCanvas = new DynamicPathwayGraphRepresentation(pathwayLayout, this);
		// currentPathwayElement.setLocation(200, 0);
	}

	/**
	 * view for side bar, which contains a list of representable pathways
	 */
	private void createRankingSideBar() {
		AnimatedGLElementContainer column = new AnimatedGLElementContainer(new GLSizeRestrictiveFlowLayout(false, 10,
				GLPadding.ZERO));

		column.add(pathwayListBaseContainer);

		rankingWindow = new DynamicPathwaySideWindow("Pathways", this, SideWindow.SLIDE_LEFT_OUT);
		rankingElement = new RankingElement(this);
		rankingWindow.setContent(rankingElement);

		SlideInElement slideInElement = new SlideInElement(rankingWindow, ESlideInElementPosition.RIGHT);
		slideInElement.setCallBack(new ISelectionCallback() {
			@Override
			public void onSelectionChanged(GLButton button, boolean selected) {
				AnimatedGLElementContainer anim = (AnimatedGLElementContainer) rankingWindow.getParent();
				if (selected) {
					anim.resizeChild(rankingWindow, 200, Float.NaN);

				} else {
					anim.resizeChild(rankingWindow, 1, Float.NaN);
				}

			}
		});

		rankingWindow.addSlideInElement(slideInElement);
		rankingWindow.setShowCloseButton(false);
		rankingElement.setWindow(rankingWindow);

		pathwayListBaseContainer.add(rankingWindow);
	}

	/**
	 * 
	 * @param vertexEnvironmentSize
	 * @return
	 * @throws Exception
	 */
	private PathwayGraph createPathwayWithFocusVertexAndHisEnvironment(PathwayGraph pathwayToAdd,
			int vertexEnvironmentSize) throws Exception {

		/**
		 * ----------------------------------------------------------------------- <br/>
		 * STEP 1: find the FOCUS VERTEX REPRESENATION
		 * -----------------------------------------------------------------------
		 */
		NodeElement currentFilteringNode = dynamicGraphCanvas.getFocusNode();
		System.out.println("Current filtering node: " + currentFilteringNode);
		if (currentFilteringNode == null)
			return null;

		PathwayVertexRep currentFilteringVRep = currentFilteringNode.getVertexRep();

		// String limitedGraphNameExtension = "Focus: " + currentFilteringNode.getLabel() +
		// "; vertex environment size: "
		// + vertexEnvironmentSize;

		if (currentFilteringVRep == null) {
			throw new Exception("currentFilteringVRep was null");
		}

		Set<DefaultEdge> edgesOfThisNode = null;
		PathwayVertexRep alternativeVrepFromPathway = null;

		PathwayVertex focusVertex = null;

		/**
		 * if the currentFilteringVRep is in the pathway -> add this to the limited version of the graph
		 */
		if (pathwayToAdd.containsVertex(currentFilteringVRep)) {
			edgesOfThisNode = pathwayToAdd.edgesOf(currentFilteringVRep);
			focusVertex = currentFilteringNode.getVertices().get(0);
		} else {
			/**
			 * if the currentFilteringVRep isn't in the pathway -> find the vrep that is in the pathway & add this to
			 * the limited version of the graph
			 * 
			 */

			// List<PathwayVertexRep> alternativeVreps = currentFilteringNode.getVrepsWithThisNodesVerticesList();
			// for(PathwayVertexRep alternativeVrep : alternativeVreps) {
			// if(pathwayToAdd.containsVertex(alternativeVrep)) {
			// focusVertex = alternativeVrep.getPathwayVertices().get(0);
			// edgesOfThisNode = pathwayToAdd.edgesOf(alternativeVrep);
			// alternativeVrepFromPathway = alternativeVrep;
			// break;
			// }
			// }

			List<PathwayVertex> alternativeVertices = currentFilteringNode.getVertices();
			if (alternativeVertices == null)
				throw new Exception(
						"pathway didn't contain main vrep of filtering node & did not contain additional vreps");
			outerloop: for (PathwayVertex alternativeVertex : alternativeVertices) {
				// TODO: what if more than one vrep are in pathway??
				List<PathwayVertexRep> alternativeVreps = alternativeVertex.getPathwayVertexReps();
				for (PathwayVertexRep alternativeVrep : alternativeVreps) {
					if (pathwayToAdd.containsVertex(alternativeVrep)) {
						focusVertex = alternativeVertex;
						edgesOfThisNode = pathwayToAdd.edgesOf(alternativeVrep);
						alternativeVrepFromPathway = alternativeVrep;
						break outerloop;
					}
				}
			}
		}

		if (edgesOfThisNode == null || focusVertex == null)
			return null;

		PathwayGraph subPathway = new PathwayGraph(pathwayToAdd.getType(), pathwayToAdd.getName(),
				pathwayToAdd.getTitle() + " [P]", pathwayToAdd.getImage(), pathwayToAdd.getExternalLink());

		subPathway.addVertex(currentFilteringVRep);

		PathwayVertexRep vrepToCheckWith = currentFilteringVRep;
		if (alternativeVrepFromPathway != null) {
			subPathway.addVertex(alternativeVrepFromPathway);
			vrepToCheckWith = alternativeVrepFromPathway;
		}

		/**
		 * ----------------------------------------------------------------------- <br />
		 * STEP 2: find the nodes of the next level
		 * -----------------------------------------------------------------------
		 */
		Set<PathwayVertexRep> vrepsOfCurrentLevel = new HashSet<PathwayVertexRep>();
		Set<PathwayVertexRep> vrepsOfNextLevel = new HashSet<PathwayVertexRep>();

		for (DefaultEdge edgeOfCurrentFilteringVrep : edgesOfThisNode) {
			PathwayVertexRep sourceVrep = pathwayToAdd.getEdgeSource(edgeOfCurrentFilteringVrep);
			PathwayVertexRep targetVrep = pathwayToAdd.getEdgeTarget(edgeOfCurrentFilteringVrep);

			// if the main node is the target node, the other node is the source node and vice versa
			if (vrepToCheckWith.equals(targetVrep)) {
				subPathway.addVertex(sourceVrep);
				subPathway.addEdge(sourceVrep, vrepToCheckWith, edgeOfCurrentFilteringVrep);
				vrepsOfNextLevel.add(sourceVrep);
			} else if (vrepToCheckWith.equals(sourceVrep)) {
				subPathway.addVertex(targetVrep);
				subPathway.addEdge(vrepToCheckWith, targetVrep, edgeOfCurrentFilteringVrep);
				vrepsOfNextLevel.add(targetVrep);
			} else
				throw new Exception("getVertexEnvironment: vrepToCheckWith was neither source nor target");

		}

		for (int i = 1; i < (vertexEnvironmentSize - 1); i++) {
			vrepsOfCurrentLevel.clear();
			vrepsOfCurrentLevel.addAll(vrepsOfNextLevel);
			vrepsOfNextLevel.clear();

			for (PathwayVertexRep vrepOfCurrentLevel : vrepsOfCurrentLevel) {
				// get the edges of the node
				edgesOfThisNode = pathwayToAdd.edgesOf(vrepOfCurrentLevel);

				// go throw all edges
				for (DefaultEdge edgeOfCurrentFilteringVrep : edgesOfThisNode) {

					// if the edge was already added, go on with the next edge
					if (subPathway.containsEdge(edgeOfCurrentFilteringVrep))
						continue;

					PathwayVertexRep sourceVrep = pathwayToAdd.getEdgeSource(edgeOfCurrentFilteringVrep);
					PathwayVertexRep targetVrep = pathwayToAdd.getEdgeTarget(edgeOfCurrentFilteringVrep);

					// if the main node is the target node, the other node is the source node and vice versa
					if (vrepOfCurrentLevel.equals(targetVrep)) {
						subPathway.addVertex(sourceVrep);
						subPathway.addEdge(sourceVrep, vrepOfCurrentLevel, edgeOfCurrentFilteringVrep);
						vrepsOfNextLevel.add(sourceVrep);
					} else if (vrepOfCurrentLevel.equals(sourceVrep)) {
						subPathway.addVertex(targetVrep);
						subPathway.addEdge(vrepOfCurrentLevel, targetVrep, edgeOfCurrentFilteringVrep);
						vrepsOfNextLevel.add(targetVrep);
					} else
						throw new Exception("getVertexEnvironment: vrepToCheckWith was neither source nor target");
				}
			}

		}

		dynamicGraphCanvas.addOriginalPathwayAndSubpathwayToMap(subPathway, pathwayToAdd);

		return subPathway;
	}

}
