package us.ihmc.simulationconstructionset.dataExporter;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DataExporterReadmeWriter
{
   public void writeReadMe(File directory, String name, long revisionNumber)
   {
      try
      {
         String revisionNumberLine = "Revision number: " + revisionNumber + "\n\n";
         String readmeText = revisionNumberLine + getUserInputReadMeText();

         File file = new File(directory, name + ".txt");
         FileWriter out = new FileWriter(file);

         out.write(readmeText);
         out.close();
      }
      catch (IOException ex)
      {
         ex.printStackTrace();
      }
   }

   private String getUserInputReadMeText()
   {
      JTextArea message = new JTextArea();
      JScrollPane scrollPane = new JScrollPane(message);
      scrollPane.setPreferredSize(new Dimension(350, 150));
      int option = JOptionPane.showConfirmDialog(null, scrollPane, "ReadMe Text", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      String readme = "";
      if (option == 0)
         readme = message.getText();

      return readme;
   }
}
