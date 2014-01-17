/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.virtualnet.transponder.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;

import dk.dma.ais.virtualnet.common.table.TargetTableEntry;

/**
 * This class defines a list of {@linkplain TargetTableEntry} elements
 * that can be filtered using a text field returned by
 * {@linkplain #getFilterField()}
 */
public class SelectTargetList extends JList<TargetTableEntry> {
    
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_FIELD_WIDTH = 20;
    
    private TargetFilterField filterField;

    /**
     * Constructor
     */
    public SelectTargetList() {
        super();
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.VERTICAL);
        setModel (new TargetFilterModel());
        filterField = new TargetFilterField (DEFAULT_FIELD_WIDTH);
    }

    /**
     * Sets the list model, ensuring that it is of type {@code FilterModel}
     * @param model the list mode to set
     */
    @Override
    public void setModel (ListModel<TargetTableEntry> model) {
        if (! (model instanceof TargetFilterModel)) {
            throw new IllegalArgumentException();
        }
        super.setModel (model);
    }

    /**
     * Returns the list model cast as a {@code FilterModel}
     * @return the list model cast as a {@code FilterModel}
     */
    public TargetFilterModel getFilterModel() {
        return (TargetFilterModel)getModel();
    }

    /**
     * Adds a new target to the list
     * @param target the target to add
     */
    public void addTarget(TargetTableEntry target) {
        getFilterModel().addTarget (target);
    }

    /**
     * Returns a reference to the associated {@code FilterField}
     * @return a reference to the associated {@code FilterField}
     */
    public JTextField getFilterField() {
        return filterField;
    }   
    
    /**
     * Sets the first target in the list as the selected one, and requests focus
     */
    protected void setlectFirstTarget() {
        setSelectedIndex(0);
        requestFocus();
    }
    
    /**
     * The {@code TargetFilterModel} class.
     */
    class TargetFilterModel extends AbstractListModel<TargetTableEntry> implements ActionListener {
        private static final long serialVersionUID = 1L;
        private static final int FILTER_UPDATE_DELAY = 300; 
        
        List<TargetTableEntry> targets;
        List<TargetTableEntry> filterTargets;
        Timer timer = new Timer(FILTER_UPDATE_DELAY, this);
        
        /**
         * Constructor
         */
        public TargetFilterModel() {
            super();
            timer.setRepeats(false);
            targets = new ArrayList<>();
            filterTargets = new ArrayList<>();
        }
        
        /**
         * Returns the target at the given index
         * @param index the index
         * @return the target at the given index
         */
        @Override
        public TargetTableEntry getElementAt (int index) {
            if (index < filterTargets.size()) {
                return filterTargets.get (index);
            } else {
                return null;
            }
        }

        /**
         * Returns the number of target in the filtered list
         * @return the number of target in the filtered list
         */
        @Override
        public int getSize() {
            return filterTargets.size();
        }

        /**
         * Adds a new element to the list
         * @param target the element to add
         */
        public void addTarget(TargetTableEntry target) {
            targets.add (target);
            refilter();
        }
        
        /**
         * Schedules a timed update of the list
         */
        private void refilter() {
            timer.restart();
        }

        /**
         * Called when the timer has timed out.
         * Updates the filtered list of targets based on the 
         * text in the filter text field.
         * @param e the action event
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            String term = getFilterField().getText().toLowerCase();
            filterTargets.clear();
            for (TargetTableEntry target : targets) {
                if (StringUtils.containsIgnoreCase(target.getName(), term) ||
                        StringUtils.containsIgnoreCase(Integer.toString(target.getMmsi()), term)) {
                    filterTargets.add(target);
                }
            }
            fireContentsChanged (this, 0, getSize());
        }
    }

    /**
     *  inner class provides filter-by-keystroke field
     */
    class TargetFilterField extends JTextField implements DocumentListener {
        
        private static final long serialVersionUID = 1L;
        private String hint = "Filter name/mmsi";
        
        /**
         * Constructor
         * @param width
         */
        public TargetFilterField (int width) {
            super(width);
            
            getDocument().addDocumentListener(this);
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    if (showClearButton() && getClearRect().contains(e.getPoint())) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                    }
                }});
            
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (showClearButton() && getClearRect().contains(e.getPoint())) {
                        setText("");
                    }
                }
            });
            
            // If the user presses the down key, transfer focus to the
            // first target in the list
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        setlectFirstTarget();
                    }
                }
            });
        }

        /**
         * Returns the hint text to display in an empty text field
         * @return the hint text to display in an empty text field
         */
        public String getHint() {
            return hint;
        }

        /**
         * Sets the hint text to display in an empty text field
         * @param hint the hint text to display in an empty text field
         */
        public void setHint(String hint) {
            this.hint = hint;
        }

        /**
         * Computes the rectangle to display a clear button in
         * @return the rectangle to display a clear button in
         */
        Rectangle getClearRect() {
            int padding = 6;
            int size = getHeight() - 2 * padding;
            return new Rectangle(getWidth() - size - padding, padding, size, size);
        }
        
        /**
         * Returns whether to display the clear text button or not
         * @return whether to display the clear text button or not
         */
        boolean showClearButton() {
            return getText().trim().length() > 0;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Paint a hint text if the field is blank
            if (getText().trim().length() == 0 && !hasFocus()) {
                FontMetrics fm = g.getFontMetrics();
                Rectangle2D rect = fm.getStringBounds(hint, g);
                g2.setColor(getForeground());
                g2.drawString(hint, 8, (getHeight() - (int)rect.getHeight()) / 2 + fm.getAscent());
            }
            
            // Draw a clear text button if there is text present
            if (showClearButton()) {
                Rectangle r = getClearRect();
                g2.setColor(Color.lightGray);
                g2.fillArc((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight(), 0, 360);
                g2.setColor(getBackground());
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Line2D.Double(r.getCenterX() - 3, r.getCenterY() - 3, r.getCenterX() + 2, r.getCenterY() + 2));
                g2.draw(new Line2D.Double(r.getCenterX() + 2, r.getCenterY() - 3, r.getCenterX() - 3, r.getCenterY() + 2));
            }
        }
        
        /** 
         * {@inheritDoc}
         */
        @Override 
        public void changedUpdate (DocumentEvent e) { 
            getFilterModel().refilter(); 
         }
        
        /** 
         * {@inheritDoc}
         */
        @Override 
        public void insertUpdate (DocumentEvent e) { 
            getFilterModel().refilter(); 
        }
        
        /** 
         * {@inheritDoc}
         */
        @Override 
        public void removeUpdate (DocumentEvent e) { 
            getFilterModel().refilter(); 
        }
    }
}
