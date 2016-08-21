package us.ihmc.plotting.artifact;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.Serializable;

import javax.vecmath.Vector2d;

import us.ihmc.plotting.Plottable;

public abstract class Artifact implements Plottable, Serializable
{
   private static final long serialVersionUID = -463773605470590581L;
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

   /**
    * Must provide a draw method for plotter to render artifact
    */
   @Override
   public abstract void draw(Graphics g, int Xcenter, int Ycenter, double headingOffset, double scaleFactor);
   public void draw(Graphics2D g, int Xcenter, int Ycenter, double headingOffset, Vector2d scaleFactor)
   {
      draw(g, Xcenter, Ycenter, headingOffset, scaleFactor.getX());
   }
   public abstract void drawHistory(Graphics g, int Xcenter, int Ycenter, double scaleFactor);
   public void drawHistory(Graphics2D g, int Xcenter, int Ycenter, double headingOffset, Vector2d scaleFactor)
   {
      draw(g, Xcenter, Ycenter, headingOffset, scaleFactor.getX());
   }
   public abstract void takeHistorySnapshot();
   public abstract void drawLegend(Graphics g, int Xcenter, int Ycenter, double scaleFactor);

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

   @Override
   public String getID()
   {
      return id;
   }

   @Override
   public String getType()
   {
      return type;
   }

   @Override
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
