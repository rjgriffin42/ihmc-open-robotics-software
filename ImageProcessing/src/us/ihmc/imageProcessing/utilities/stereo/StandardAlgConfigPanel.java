/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.ihmc.imageProcessing.utilities.stereo;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

/**
 * Common base class for panels used for configuring the algorithms.
 *
 * @author Peter Abeles
 */
public class StandardAlgConfigPanel extends JPanel {

	public StandardAlgConfigPanel() {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

	}

	public void addAlignLeft( JComponent target, JPanel owner ) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(target);
		p.add(Box.createHorizontalGlue());
		owner.add(p);
	}

	public void addAlignCenter( JComponent target, JPanel owner ) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(Box.createHorizontalGlue());
		p.add(target);
		p.add(Box.createHorizontalGlue());
		owner.add(p);
	}

	protected void addCenterLabel( String text , JPanel owner ) {
		JLabel l = new JLabel(text);
		l.setAlignmentX(Component.CENTER_ALIGNMENT);
		owner.add(l);
		owner.add(Box.createRigidArea(new Dimension(1,8)));
	}

	protected void addSeparator(int width) {
		add(Box.createRigidArea(new Dimension(1,8)));
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setMaximumSize(new Dimension(width,5));
		add(separator);
		add(Box.createRigidArea(new Dimension(1,8)));
	}

	protected void addLabeled( JComponent target , String text , JPanel owner ) {
		JLabel label = new JLabel(text);
		label.setLabelFor(target);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(label);
		p.add(Box.createHorizontalGlue());
		p.add(target);
		owner.add(p);
	}

	protected void addVerticalGlue( JPanel owner ) {
		owner.add(Box.createVerticalGlue());
	}

	protected void setEnabled( int index , boolean enabled ) {
		Component c = getComponent(index);
		if( c instanceof JPanel ) {
			JPanel p = (JPanel)c;
			p.getComponent(0).setEnabled(enabled);
			p.getComponent(2).setEnabled(enabled);
		} else if( c instanceof JLabel ) {
			c.setEnabled(enabled);
		}
	}

}
