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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.WindowConstants;

import dk.dma.ais.virtualnet.common.message.TargetTableMessage;

public class SelectTargetDialog extends JDialog {
    
    private static final long serialVersionUID = 1L;
    
    private Integer selectedTarget;
    
    public SelectTargetDialog(JFrame parent, TargetTableMessage targets) {
        super(parent, "Select target", true);
        setResizable(false);
        setSize(200, 300);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        getContentPane().setLayout(null);
        
        // TODO: Find examples
        // TODO: Border
        JList list = new JList();
        list.setBounds(6, 6, 188, 226);
        getContentPane().add(list);

        
    }
    
    
    public Integer getSelectedTarget() {
        return selectedTarget;
    }
}
