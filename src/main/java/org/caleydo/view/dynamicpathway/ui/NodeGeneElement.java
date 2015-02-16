package org.caleydo.view.dynamicpathway.ui;

import java.util.List;
import java.util.Set;

import org.caleydo.core.event.EventPublisher;
import org.caleydo.core.util.color.Color;
import org.caleydo.core.view.opengl.layout.Column.VAlign;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.picking.AdvancedPick;
import org.caleydo.core.view.opengl.picking.IPickingListener;
import org.caleydo.core.view.opengl.picking.Pick;
import org.caleydo.core.view.opengl.picking.PickingMode;
import org.caleydo.datadomain.pathway.graph.PathwayGraph;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertex;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertexRep;

import com.google.common.collect.Lists;

public class NodeGeneElement extends NodeElement {

	private static final int INNER_BOUNDS = 4;
	private static final int OUTER_BOUNDS = 1;
	private static final int ROUND_EDGE_RADIUS = 2;
	private static final int WIDTH_AND_HEIGHT_ADDEND = 5;
	
	public NodeGeneElement(final PathwayVertexRep vertexRep, List<PathwayVertex> pathwayVertices,
			final DynamicPathwayGraphRepresentation parentGraph, Set<PathwayGraph> pathways) {
		super(vertexRep, pathwayVertices, parentGraph, pathways,WIDTH_AND_HEIGHT_ADDEND);
		
		onPick(new IPickingListener() {

			@Override
			public void pick(Pick pick) {
				AdvancedPick p = (AdvancedPick) pick;
				
				/**
				 * inform other views about picking event
				 */
				parentGraph.onSelect(vertices, NodeGeneElement.this, pick);

				/**
				 * if the user right clicked - show context menu
				 */
				if (pick.getPickingMode() == PickingMode.RIGHT_CLICKED) {
//					parentGraph.setOrResetFilteringNode(NodeGeneElement.this);

					context.getSWTLayer().showContextMenu(Lists.newArrayList(filterPathwayMenu, focusNodeMenu));
					
				}

				/**
				 * if the user clicked on the node
				 */
				if (pick.getPickingMode() == PickingMode.CLICKED) {
					
					System.out.println(NodeGeneElement.this.toString());

					parentGraph.setOrResetSelectedNode(NodeGeneElement.this);

					if (p.isCtrlDown()) {
//						parentGraph.setOrResetFilteringNode(NodeGeneElement.this);
//						
//						parentGraph.filterPathwayList();
						
						EventPublisher.trigger(focusNodeEvent);
					}


				}
				/**
				 * if the user moved the curser over this node
				 */
				if (pick.getPickingMode() == PickingMode.MOUSE_OVER) {
					isMouseOver = true;
				}

				/**
				 * if the user's curser left the node
				 */
				if (pick.getPickingMode() == PickingMode.MOUSE_OUT) {
					isMouseOver = false;
				}

				/**
				 * renderImpl is called
				 */
				repaint();

			}
		});
		
	}



	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {

//		short width = vertexRep.getWidth();
//		short height = vertexRep.getHeight();
		
//		short width = (short)this.width;
//		short height = (short)this.height;
		
		short width = (short)w;
		short height = (short)h;	

		/**
		 * represent BORDER of node different:
		 * if it was used for filtering, clicked on, the mouse is moved over or nothing of these was done
		 */
		if (isThisNodeUsedForFiltering) {
			g.color(FILTER_CONTOUR_COLOR).fillRoundedRect(-1, -1, width,
					height, ROUND_EDGE_RADIUS);
		} else if (isThisNodeSelected) {
			g.color(SELECTION_CONTOUR_COLOR).fillRoundedRect(-1, -1, width,
					height, ROUND_EDGE_RADIUS);
		} else if (isMouseOver) {
			g.color(MOUSEROVER_CONTOUR_COLOR).fillRoundedRect(-1, -1, width,
					height, ROUND_EDGE_RADIUS);
		} else if (wasPreviouslyFocusNode) {
			g.color(PREVIOUS_FOCUS_NODE_COLOR).fillRoundedRect(-1, -1, width,
					height, ROUND_EDGE_RADIUS);
		}
		else {
			g.color(CONTOUR_COLOR).fillRoundedRect(0, 0, width -2, height -2,
					ROUND_EDGE_RADIUS);
		}
					
		g.color(NODE_FILLING_COLOR).fillRoundedRect(OUTER_BOUNDS, OUTER_BOUNDS, width - INNER_BOUNDS, height - INNER_BOUNDS, ROUND_EDGE_RADIUS);

		if (displayedVertex == null)
			displayedVertex = vertexRep.getPathwayVertices().get(0);

		g.drawText(label, 0, 3, width, FONT_SIZE, VAlign.CENTER);

	}

	@Override
	protected void renderPickImpl(GLGraphics g, float w, float h) {

		super.renderPickImpl(g, w, h);

	}



	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
	
	

}
