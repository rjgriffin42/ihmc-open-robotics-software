package us.ihmc.plotting.artifact;

import java.awt.Color;

import us.ihmc.plotting.Graphics2DAdapter;

public abstract class Artifact
{
   public static int X_Y = 0;
   public static int X_Z = 1;
   public static int Y_Z = 2;
   
   protected final String id;
   protected String type;
   protected int level;
   protected Color color = Color.blue;
   protected boolean isVisible = true;
   private boolean showID = false;
   private boolean drawHistory = false;
   private boolean recordHistory = false;
   private String label = null;

   public Artifact(String id)
   {
      this.id = id;
   }

   public abstract void draw(Graphics2DAdapter graphics, int centerX, int centerY, double headingOffset, double scaleFactor);
   
   public abstract void drawHistory(Graphics2DAdapter graphics, int centerX, int centerY, double scaleFactor);
   
   public abstract void drawLegend(Graphics2DAdapter graphics, int centerX, int centerY, double scaleFactor);
   
   public abstract void takeHistorySnapshot();

   public void setType(String type)
   {
      this.type = type;
   }

   public void setShowID(boolean showID)
   {
      this.showID = showID;
   }

   public boolean getShowID()
   {
      return showID;
   }

   public void setDrawHistory(boolean drawHistory)
   {
      this.drawHistory = drawHistory;
   }

   public boolean getDrawHistory()
   {
      return drawHistory;
   }

   public void setRecordHistory(boolean recordHistory)
   {
      this.recordHistory = recordHistory;
   }

   public boolean getRecordHistory()
   {
      return recordHistory;
   }

   public String getID()
   {
      return id;
   }

   public String getType()
   {
      return type;
   }

   public int getLevel()
   {
      return level;
   }

   public void setLevel(int level)
   {
      this.level = level;
   }

   public void setColor(Color color)
   {
      this.color = color;
   }

   public Color getColor()
   {
      return color;
   }

   public boolean isVisible()
   {
      return isVisible;
   }

   public void setVisible(boolean isVisible)
   {
      this.isVisible = isVisible;
   }

   public String getName() {
      return getID();
   }

   @Override
   public String toString()
   {
      return getID();
   }

   public void setLabel(String label)
   {
      this.label = label;
   }

   public String getLabel()
   {
      return label;
   }
}
