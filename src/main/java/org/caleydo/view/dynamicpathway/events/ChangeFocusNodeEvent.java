package org.caleydo.view.dynamicpathway.events;

import org.caleydo.core.event.AEvent;
import org.caleydo.view.dynamicpathway.ui.ANodeElement;

public class ChangeFocusNodeEvent extends AEvent {
	
	ANodeElement newFocusNode;

	public ChangeFocusNodeEvent(ANodeElement nodeElementToFilterBy) {	
		this.newFocusNode = nodeElementToFilterBy;
	}



	@Override
	public boolean checkIntegrity() {
		return true;
	}



	public ANodeElement getNodeElementToFilterBy() {
		return newFocusNode;
	}



	public void setNewFocusNode(ANodeElement newFocusNode) {
		this.newFocusNode = newFocusNode;
	}

}
