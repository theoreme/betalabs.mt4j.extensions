/***********************************************************************
 *   MT4j Extension: Toolbar
 *   
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License (LGPL)
 *   as published by the Free Software Foundation, either version 3
 *   of the License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the LGPL
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************************************************/
package org.mt4jx.components.visibleComponents.widgets.toolbar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.mt4j.components.bounds.BoundsZPlaneRectangle;
import org.mt4j.components.bounds.IBoundingShape;
import org.mt4j.components.interfaces.IclickableButton;
import org.mt4j.util.font.FontManager;
import org.mt4j.components.visibleComponents.widgets.MTTextArea;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapEvent;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapProcessor;
import org.mt4j.util.MTColor;

import processing.core.PApplet;

/**
 * @author Alexander Phleps
 *
 */
public class MTToolbarListItem  extends MTTextArea implements IclickableButton {

//	private PApplet app = null;
	private ArrayList<ActionListener> registeredActionListeners;
	
	//super(mtApp, FontManager.getInstance().createFont(mtApp, "arial", 18, new MTColor(255, 255, 255), new MTColor(0, 0, 0)));
	public MTToolbarListItem(String label, PApplet app, int fontSize) {
		super(app, FontManager.getInstance().createFont(app, "arial", fontSize, new MTColor(255, 255, 255)));
		this.registeredActionListeners = new ArrayList<ActionListener>();
		setText(label);
		setNoFill(true);
		setNoStroke(true);
		
		//Make clickable
		this.setGestureAllowance(TapProcessor.class, true);
		this.registerInputProcessor(new TapProcessor(app));
		this.addGestureListener(TapProcessor.class, new MTToolbarButtonClickAction(this));
	}
	@Override
	public boolean isSelected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setSelected(boolean selected) {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected IBoundingShape computeDefaultBounds(){
		return new BoundsZPlaneRectangle(this);
	}
	/**
	 * Adds the action listener.
	 * 
	 * @param listener the listener
	 */
	public synchronized void addActionListener(ActionListener listener){
		if (!registeredActionListeners.contains(listener)){
			registeredActionListeners.add(listener);
		}
	}
	
	/**
	 * Removes the action listener.
	 * 
	 * @param listener the listener
	 */
	public synchronized void removeActionListener(ActionListener listener){
		if (registeredActionListeners.contains(listener)){
			registeredActionListeners.remove(listener);
		}
	}
	
	/**
	 * Gets the action listeners.
	 * 
	 * @return the action listeners
	 */
	public synchronized ActionListener[] getActionListeners(){
		return (ActionListener[])registeredActionListeners.toArray(new ActionListener[this.registeredActionListeners.size()]);
	}
	
	/**
	 * Fire action performed.
	 */
	protected void fireActionPerformed() {
		ActionListener[] listeners = this.getActionListeners();
		synchronized(listeners) {
			for (int i = 0; i < listeners.length; i++) {
				ActionListener listener = (ActionListener)listeners[i];
				listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "action performed on tangible button"));
			}
		}
	}
	
	/**
	 * fires an action event with a ClickEvent Id as its ID.
	 * 
	 * @param ce the ce
	 */
	public void fireActionPerformed(TapEvent ce) {
		ActionListener[] listeners = this.getActionListeners();
		synchronized(listeners) {
			for (int i = 0; i < listeners.length; i++) {
				ActionListener listener = (ActionListener)listeners[i];
				listener.actionPerformed(new ActionEvent(this, ce.getTapID(),  "action performed on tangible button"));
			}
		}
	}

}
