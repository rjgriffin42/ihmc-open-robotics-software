package us.ihmc.simulationconstructionset.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

public class EntryBoxArrayTabbedPanel extends JTabbedPane
{
   private ArrayList<EntryBoxArrayPanel> entryBoxArrayPanels = new ArrayList<EntryBoxArrayPanel>();
   private final Container parentContainer;
   private final SelectedVariableHolder selectedVariableHolder;

   public EntryBoxArrayTabbedPanel( Container parentContainer, SelectedVariableHolder selectedVariableHolder)
   {
      this.parentContainer = parentContainer;
      this.selectedVariableHolder = selectedVariableHolder;
      TabTitleEditListener nameChanger = new TabTitleEditListener(this);
      addChangeListener(nameChanger);
      addMouseListener(nameChanger);
   }

   public void addEntryBoxArrayPanel(String name, EntryBoxArrayPanel panelToAdd)
   {
      this.add(name, panelToAdd);
      this.setSelectedComponent(panelToAdd);
   }

   public EntryBoxArrayPanel getCurrentPanel()
   {
      // TODO Auto-generated method stub
	  if(getSelectedComponent()==null)
		  addEmptyTab();
      return (EntryBoxArrayPanel) getSelectedComponent();
   }

   public void closeAndDispose()
   {
      for (int i = getTabCount() - 1; i >= 0; i--)
      {
         if (getTabComponentAt(i) instanceof EntryBoxArrayPanel)
         {
            ((EntryBoxArrayPanel) getTabComponentAt(i)).closeAndDispose();
            remove(getTabComponentAt(i));
         }
      }
   }

   class TabTitleEditListener extends MouseAdapter implements ChangeListener
   {
      private final JTextField editor = new JTextField();
      private final JTabbedPane tabbedPane;
      private int editingIdx = -1;
      private int len = -1;
      private Dimension dim;
      private Component tabComponent;

      public TabTitleEditListener(final JTabbedPane tabbedPane)
      {
         super();
         this.tabbedPane = tabbedPane;
         editor.setBorder(BorderFactory.createEmptyBorder());
         editor.addFocusListener(new FocusAdapter()
         {
            @Override
            public void focusLost(FocusEvent e)
            {
               renameTabTitle();
            }
         });
         editor.addKeyListener(new KeyAdapter()
         {
            @Override
            public void keyPressed(KeyEvent e)
            {
               if (e.getKeyCode() == KeyEvent.VK_ENTER)
               {
                  renameTabTitle();
               }
               else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
               {
                  cancelEditing();
               }
               else
               {
                  editor.setPreferredSize(editor.getText().length() > len ? null : dim);
                  tabbedPane.revalidate();
               }
            }
         });
         tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "start-editing");
         tabbedPane.getActionMap().put("start-editing", new AbstractAction()
         {
            @Override
            public void actionPerformed(ActionEvent e)
            {
               startEditing();
            }
         });
      }

      @Override
      public void stateChanged(ChangeEvent e)
      {
         renameTabTitle();
      }

      @Override
      public void mouseClicked(MouseEvent me)
      {
         Rectangle rect = tabbedPane.getUI().getTabBounds(tabbedPane, tabbedPane.getSelectedIndex());
         if (rect != null && rect.contains(me.getPoint()) && me.getClickCount() == 2)
         {
            startEditing();
         }
         else
         {
            renameTabTitle();
         }
      }

      private void startEditing()
      {
         editingIdx = tabbedPane.getSelectedIndex();
         tabComponent = tabbedPane.getTabComponentAt(editingIdx);
         tabbedPane.setTabComponentAt(editingIdx, editor);
         editor.setVisible(true);
         editor.setText(tabbedPane.getTitleAt(editingIdx));
         editor.selectAll();
         editor.requestFocusInWindow();
         len = editor.getText().length();
         dim = editor.getPreferredSize();
         editor.setMinimumSize(dim);
      }

      private void cancelEditing()
      {
         if (editingIdx >= 0)
         {
            tabbedPane.setTabComponentAt(editingIdx, tabComponent);
            editor.setVisible(false);
            editingIdx = -1;
            len = -1;
            tabComponent = null;
            editor.setPreferredSize(null);
            tabbedPane.requestFocusInWindow();
         }
      }

      private void renameTabTitle()
      {
         String title = editor.getText().trim();
         if (editingIdx >= 0 && !title.isEmpty())
         {
            tabbedPane.setTitleAt(editingIdx, title);
         }
         cancelEditing();
      }
   }

   public String getXMLRepresentationOfClass()
   {
      String returnString = "<Entry Boxes Tab Pane>";

      for (int i = 0; i < getTabCount(); i++)
      {
         returnString += "\n<EntryBoxTab>";
         returnString += "\n<Title>\n" + getTitleAt(i) + "\n</Title>\n";
         returnString += ((EntryBoxArrayPanel) getComponentAt(i)).getXMLRepresentationOfClass();
         returnString += "\n</EntryBoxTab>";
      }
      returnString += "\n</Entry Boxes Tab Pane>";
      return returnString;
   }

   public void addEntryBox(YoVariable selectedVariable)
   {
      if (getCurrentPanel() == null)
      {
         addEmptyTab();
      }
      getCurrentPanel().addEntryBox(selectedVariable);
   }

   public void addEmptyTab()
   {
      DoubleYoVariable[] varsToEnter = new DoubleYoVariable[0];

      EntryBoxArrayPanel tmpEntryBoxArrayPanel = new EntryBoxArrayPanel(parentContainer, selectedVariableHolder, varsToEnter);
      addEntryBoxArrayPanel("Tab", tmpEntryBoxArrayPanel);
   }

}
