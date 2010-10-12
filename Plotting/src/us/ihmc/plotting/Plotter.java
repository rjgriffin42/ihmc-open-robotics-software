package us.ihmc.plotting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.vecmath.Point2d;

import us.ihmc.plotting.shapes.LineArtifact;
import us.ihmc.plotting.shapes.PointArtifact;
import us.ihmc.plotting.shapes.PolygonArtifact;
import us.ihmc.plotting.shapes.ShapeArtifact;
import us.ihmc.utilities.math.geometry.Line2d;

public class Plotter extends JPanel
{
   /**
    * 
    */
   private static final long serialVersionUID = 3113130298799362369L;

   private final ArrayList<ArtifactsChangedListener> artifactsChangedListeners = new ArrayList<ArtifactsChangedListener>();

   // show selections
   private boolean SHOW_SELECTION = false;

   private long updateDelayInMillis = 0;
   private long lastUpdate = 0;

   private BufferedImage backgroundImage = null;

   // simpanel
   private Dimension preferredSize = new Dimension(275, 275);
   private final HashMap<String, Artifact> artifacts = new HashMap<String, Artifact>();
   int _Xcenter;
   int _Ycenter;
   double _Xoffset = 0;
   double _Yoffset = 0;
   double _scale = 20;
   double _scaleFactor;
   double _origScale = -1.0;
   double _ullon, _ullat, _lrlon, _lrlat;
   int _origRange = 0;
   int _orientation = Plottable.X_Y;
   double _Xselected = 0;
   double _Yselected = 0;
   boolean _isRoboCentric = false;

   // rectangle area selection
   double _areaX1 = 0;
   double _areaY1 = 0;
   double _areaX2 = 0;
   double _areaY2 = 0;
   double _areaX1TEMP = 0;
   double _areaY1TEMP = 0;

   // drag tracking
   protected int buttonPressed;
   protected int dragStartX;
   protected int dragStartY;
   protected double xOffsetStartOfDrag;
   protected double yOffsetStartOfDrag;

   // double click listener
   private DoubleClickListener _listener;

   // polygon tracking
   public boolean MAKING_POLYGON = false;
   protected PolygonArtifact polygonArtifact;



   public Plotter()
   {
      // Initialize class variables
      this.setDoubleBuffered(true);

      // Set LayoutManager to null
      this.setLayout(null);

      // simpane
      Border raisedBevel = BorderFactory.createRaisedBevelBorder();
      Border loweredBevel = BorderFactory.createLoweredBevelBorder();
      Border compound = BorderFactory.createCompoundBorder(raisedBevel, loweredBevel);
      setBorder(compound);

      super.setBackground(new Color(180, 220, 240));

      PlotterMouseListener myListener = new PlotterMouseListener();
      this.addMouseListener(myListener);
      this.addMouseMotionListener(myListener);
   }

   public void paintComponent(Graphics gO)
   {
      long currentTime = System.currentTimeMillis();
      if ((currentTime - lastUpdate) > updateDelayInMillis)
      {
         Graphics2D g = (Graphics2D) gO;

         // get current size and determine scaling factor
         Dimension d = this.getSize();
         int h = (int)Math.round(d.getHeight());
         int w = (int)Math.round(d.getWidth());
         _Xcenter = w / 2;    // 0;//w/2;
         _Ycenter = h / 2;    // h;//h/2;
         _scaleFactor = h / _scale;

         // set current offset
         _Xcenter = _Xcenter - (int)Math.round(_Xoffset * _scaleFactor);
         _Ycenter = _Ycenter + (int)Math.round(_Yoffset * _scaleFactor);

         // paint background
         super.paintComponent(g);

         // Paint all artifacts that want to be under grid (86)
         synchronized (artifacts)
         {
            for (Artifact artifact : artifacts.values())
            {
               if (artifact != null)
               {
                  if (artifact.getLevel() == 86)
                  {
                     artifact.draw(g, _Xcenter, _Ycenter, _scaleFactor);
                  }
               }
               else
               {
                  System.out.println("Plotter: one of the artifacts you added was null");
               }
            }
         }

         // _scale == range
         // if we start out at 100%, then the range will be equal to the extent of the map
         // if we zoom in, the range will be less, and so we need to compute from the current
         // center the zoomed in area.
         if (backgroundImage != null)
         {
            Coordinate ul = new Coordinate(_ullon, _ullat, Coordinate.METER);
            Coordinate lr = new Coordinate(_lrlon, _lrlat, Coordinate.METER);
            Coordinate pul = convertFromMetersToPixels(ul);
            Coordinate plr = convertFromMetersToPixels(lr);

            int ulx = (int) pul.getX();
            int uly = (int) pul.getY();
            int lrx = (int) plr.getX();
            int lry = (int) plr.getY();

            g.drawImage(backgroundImage, ulx, uly, lrx, lry, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), this);
         }
         else
         {
            // change grid line scale from 1m to 10cm ehn below 10m
            double interval = 1.0;
            if (getRange() < 10)
            {
               interval = 0.1;
            }

            // paint grid lines
            Coordinate ulCoord = convertFromPixelsToMeters(new Coordinate(0, 0, Coordinate.PIXEL));
            Coordinate lrCoord = convertFromPixelsToMeters(new Coordinate(w, h, Coordinate.PIXEL));
            double minX = ulCoord.getX();
            double maxX = lrCoord.getX();
            double diff = maxX - minX;
            int count = (int)Math.round(Math.ceil(diff / interval));
            for (int i = 0; i < count; i++)
            {
               double distance = Math.floor(minX) + (i * interval);
               if (distance / interval % 10 == 0)
                  g.setColor(new Color(180, 190, 210));
               else if (distance / interval % 5 == 0)
                  g.setColor(new Color(180, 210, 230));
               else
                  g.setColor(new Color(180, 230, 250));

               // get pixel from meter for positive
               Coordinate coord = convertFromMetersToPixels(new Coordinate(distance, distance, Coordinate.METER));
               int x = (int)Math.round(coord.getX());
               int y = (int)Math.round(coord.getY());

               // draw line
               g.drawLine(x, 0, x, h);
               g.drawLine(0, y, w, y);
            }
         }

         // paint grid centerline
         g.setColor(Color.gray);

         // get pixel from meter for positive
         Coordinate coord = convertFromMetersToPixels(new Coordinate(0, 0, Coordinate.METER));
         int x0 = (int)Math.round(coord.getX());
         int y0 = (int)Math.round(coord.getY());

         // draw line
         g.drawLine(x0, 0, x0, h);
         g.drawLine(0, y0, w, y0);

         // Paint all artifacts by level
         // (assumes 5 levels (0-4)
         synchronized (artifacts)
         {
            for (int i = 0; i < 5; i++)
            {
               for (Artifact artifact : artifacts.values())
               {
                  // get next element
                  // System.out.println("drawing " + a.getID());
                  if (artifact != null)
                  {
                     if (artifact.getLevel() == i)
                     {
                        artifact.draw(g, _Xcenter, _Ycenter, _scaleFactor);    // , _orientation);
                     }
                  }
                  else
                  {
                     System.out.println(">>> a = " + artifact);
                  }
               }
            }
         }

         // paint selected destination
         if (SHOW_SELECTION)
         {
            g.setColor(Color.red);
            int xSize = 8;
            int xX = _Xcenter + ((int)Math.round(_Xselected * _scaleFactor));
            int yX = _Ycenter - ((int)Math.round(_Yselected * _scaleFactor));
            g.drawLine(xX - xSize, yX - xSize, xX + xSize, yX + xSize);
            g.drawLine(xX - xSize, yX + xSize, xX + xSize, yX - xSize);
         }

         // paint selected area
         if (SHOW_SELECTION)
         {
            g.setColor(Color.red);
            int areaX1 = _Xcenter + ((int)Math.round(_areaX1 * _scaleFactor));
            int areaY1 = _Ycenter - ((int)Math.round(_areaY1 * _scaleFactor));
            int areaX2 = _Xcenter + ((int)Math.round(_areaX2 * _scaleFactor));
            int areaY2 = _Ycenter - ((int)Math.round(_areaY2 * _scaleFactor));
            int Xmin, Xmax, Ymin, Ymax;
            if (areaX1 > areaX2)
            {
               Xmax = areaX1;
               Xmin = areaX2;
            }
            else
            {
               Xmax = areaX2;
               Xmin = areaX1;
            }

            if (areaY1 > areaY2)
            {
               Ymax = areaY1;
               Ymin = areaY2;
            }
            else
            {
               Ymax = areaY2;
               Ymin = areaY1;
            }

            // System.out.println("drawing cal: (" +
            // areaX1 + "," + areaY1 + ") to (" +
            // areaX2 + "," + areaY2 + ")");
            g.drawRect(Xmin, Ymin, (Xmax - Xmin), (Ymax - Ymin));
         }

         /*
          *  //check reference
          *  if(_isRoboCentric){
          *   e = _artifacts.elements();
          *   while(e.hasMoreElements()){
          *    //find robot
          *    Artifact a = (Artifact)e.nextElement();
          *    if(a.getType().equals("robot")){
          *     _Xcenter = _Xcenter - ((int)Math.round(a.getX()*_scaleFactor));
          *     _Ycenter = _Ycenter + ((int)Math.round(a.getY()*_scaleFactor));
          *     break;
          *    }
          *   }
          *  }
          *
          */

         lastUpdate = currentTime;
      }
   }

   public void setBackgroundImage(BufferedImage bgi)
   {
      if (bgi == null)
      {
         System.out.println("Passed in NULL as a background image");
      }
      else
      {
         backgroundImage = bgi;
         repaint();
      }
   }

   public void updateArtifacts(Vector<Artifact> artifacts)
   {
      this.artifacts.clear();

      for (Artifact a : artifacts)
      {
         this.artifacts.put(a.getID(), a);
      }

      notifyArtifactsChangedListeners();
      repaint();
   }

   public void updateArtifact(Artifact newArtifact)
   {
      synchronized (artifacts)
      {
         artifacts.put(newArtifact.getID(), newArtifact);
      }

      notifyArtifactsChangedListeners();
      repaint();
   }

   public void updateArtifactNoRePaint(Artifact newArtifact)
   {
      synchronized (artifacts)
      {
         artifacts.put(newArtifact.getID(), newArtifact);
      }

      notifyArtifactsChangedListeners();
   }

   public LineArtifact createAndAddLineArtifact(String name, Line2d line, Color color)
   {
      LineArtifact lineArtifact = new LineArtifact(name, line);
      lineArtifact.setColor(color);
      addArtifact(lineArtifact);

      return lineArtifact;
   }

   public PointArtifact createAndAddPointArtifact(String name, Point2d point, Color color)
   {
      PointArtifact pointArtifact = new PointArtifact(name, point);
      pointArtifact.setColor(color);
      addArtifact(pointArtifact);

      return pointArtifact;
   }



   public void addArtifact(Artifact newArtifact)
   {
      synchronized (artifacts)
      {
         artifacts.put(newArtifact.getID(), newArtifact);
      }

      notifyArtifactsChangedListeners();
      repaint();
   }

   public void addArtifactNoRepaint(Artifact newArtifact)
   {
      synchronized (artifacts)
      {
         artifacts.put(newArtifact.getID(), newArtifact);
      }

      notifyArtifactsChangedListeners();
   }


   public ArrayList<Artifact> getArtifacts()
   {
      ArrayList<Artifact> ret = new ArrayList<Artifact>();

      ret.addAll(artifacts.values());

      return ret;
   }

   public Artifact getArtifact(String id)
   {
      return artifacts.get(id);
   }


   public void replaceArtifact(String id, Artifact newArtifact)
   {
      synchronized (artifacts)
      {
         artifacts.put(newArtifact.getID(), newArtifact);
      }

      notifyArtifactsChangedListeners();
   }

   public void removeAllArtifacts()
   {
      synchronized (artifacts)
      {
         artifacts.clear();
      }

      notifyArtifactsChangedListeners();
      repaint();
   }

   public void removeArtifact(String id)
   {
      synchronized (artifacts)
      {
         artifacts.remove(id);
      }

      notifyArtifactsChangedListeners();
      repaint();
   }


   public void removeArtifactNoRepaint(String id)
   {
      synchronized (artifacts)
      {
         artifacts.remove(id);
      }

      notifyArtifactsChangedListeners();
   }

   public void removeArtifactsStartingWith(String id)
   {
      synchronized (artifacts)
      {
         ArrayList<String> toBeRemoved = new ArrayList<String>();
         for (String key : artifacts.keySet())
         {
            if (key.startsWith(id))
               toBeRemoved.add(key);
         }

         for (String key : toBeRemoved)
         {
            artifacts.remove(key);
         }
      }

      notifyArtifactsChangedListeners();
      repaint();
   }

   public double getSelectedX()
   {
      return _Xselected;
   }

   public double getSelectedY()
   {
      return _Yselected;
   }

   public double getAreaX1()
   {
      return _areaX1;
   }

   public double getAreaY1()
   {
      return _areaY1;
   }

   public double getAreaX2()
   {
      return _areaX2;
   }

   public double getAreaY2()
   {
      return _areaY2;
   }

   public ArrayList<Point2d> getPolygon()
   {
      if (polygonArtifact == null)
         return null;

      return polygonArtifact.getPoints();
   }

   public void clearPolygon()
   {
      this.removeArtifactsStartingWith("polygon");
      polygonArtifact = null;
      repaint();
   }


   @SuppressWarnings("unused")
   private Point convertCoordinates(JPanel plot, Point2D.Double pt)
   {
      // get current size and determine scaling factor
      Dimension d = this.getSize();
      int h = (int)Math.round(d.getHeight());
      int w = (int)Math.round(d.getWidth());

      // _Xcenter = w/2;
      // _Ycenter = h/2;
      _scaleFactor = h / _scale;

      // detemine plot size
      Dimension plotD = plot.getSize();
      int plotH = (int)Math.round(plotD.getHeight());
      int plotW = (int)Math.round(plotD.getWidth());

      // place plot so bottom is cenetered at location
      int xnew = (int)Math.round((pt.x * _scaleFactor) - (plotW / 2));
      int ynew = (int)Math.round((h - (pt.y * _scaleFactor)) - (plotH));

      return new Point(xnew, ynew);

      // return new Point(200,200);
   }

   private double unConvertXCoordinate(int coordinate)
   {
      double x = new Integer(coordinate - _Xcenter).doubleValue() / _scaleFactor;

      // System.out.println("x=" + x);
      return x;
   }

   private double unConvertYCoordinate(int coordinate)
   {
      double y = new Integer((_Ycenter - coordinate)).doubleValue() / _scaleFactor;

      // System.out.println("y=" + y);
      return y;
   }

   private Coordinate convertFromMetersToPixels(Coordinate coordinate)
   {
      double x = coordinate.getX();
      double y = coordinate.getY();

      x = new Double(_Xcenter + ((int)Math.round(x * _scaleFactor))).doubleValue();

      // System.out.println("x(pixels)=" + x);
      y = new Double(_Ycenter - ((int)Math.round(y * _scaleFactor))).doubleValue();

      // System.out.println("y(pixels)=" + y);
      return new Coordinate(x, y, Coordinate.PIXEL);
   }

   private Coordinate convertFromPixelsToMeters(Coordinate coordinate)
   {
      double x = coordinate.getX();
      double y = coordinate.getY();

      x = (x - new Integer(_Xcenter).doubleValue()) / _scaleFactor;

      // System.out.println("x=" + x);
      y = ((new Integer(_Ycenter).doubleValue()) - y) / _scaleFactor;

      // System.out.println("y=" + y);
      return new Coordinate(x, y, Coordinate.METER);
   }

   public void setRangeLimit(int range, double origMapScale, double ullon, double ullat, double lrlon, double lrlat)
   {
      _scale = range;
      _origRange = range;
      _origScale = origMapScale;
      _ullon = ullon;
      _ullat = ullat;
      _lrlon = lrlon;
      _lrlat = lrlat;

      repaint();
   }

   public void setRange(double range)
   {
      _scale = range;
      repaint();
   }

   public double getRange()
   {
      return _scale;
   }

   public void setOrientation(int orientation)
   {
      _orientation = orientation;
      repaint();
   }

   public void setXoffset(double x)
   {
      _Xoffset = x;
   }

   public void setYoffset(double y)
   {
      _Yoffset = y;
   }

   public double getXoffset()
   {
      return _Xoffset;
   }

   public double getYoffset()
   {
      return _Yoffset;
   }

   public void setRoboCentric(boolean x)
   {
      _isRoboCentric = x;
      repaint();
   }

   public Dimension getPreferredSize()
   {
      return preferredSize;
   }

   public void setPreferredSize(int h, int w)
   {
      preferredSize = new Dimension(h, w);    // (150, 150);
   }

   public void setDoubleClickListener(DoubleClickListener listener)
   {
      _listener = listener;
   }


   public void update(String objectID, ShapeArtifact shapeArtifact)
   {
      if (shapeArtifact == null)
      {
         removeArtifact(objectID);
      }
      else
      {
         if (shapeArtifact.getCoordinate() == null)
         {
            removeArtifact(shapeArtifact.getID());
         }
         else
         {
            ShapeArtifact targetArtifact = (ShapeArtifact) getArtifact(objectID);
            if (targetArtifact == null)
            {
               addArtifact(shapeArtifact);
            }
            else
            {
               updateArtifact(shapeArtifact);
            }
         }
      }

      repaint();
   }

   public void update(String objectID, PolygonArtifact polygonArtifact)
   {
      if (polygonArtifact == null)
      {
         removeArtifact(objectID);
      }
      else
      {
         if (polygonArtifact.getPoints().size() == 0)
         {
            removeArtifact(polygonArtifact.getID());
         }
         else
         {
            ShapeArtifact targetArtifact = (ShapeArtifact) getArtifact(objectID);
            if (targetArtifact == null)
            {
               addArtifact(polygonArtifact);
            }
            else
            {
               updateArtifact(polygonArtifact);
            }
         }
      }

      repaint();
   }

   public void setUpdateDelayInMillis(long timeInMillis)
   {
      updateDelayInMillis = timeInMillis;
   }

   public long getUpdateDelayInMillis()
   {
      return updateDelayInMillis;
   }

   // test driver //////////////////////////////////////////////////
   public static void main(String[] args)
   {
      // plotter
      Plotter p = new Plotter();

//    BufferedImage image = null;
//    try
//    {
//        image = ImageIO.read(new File(args[0]));
//    }
//    catch (IOException e)
//    {
//        JOptionPane.showMessageDialog(null, e.getMessage(), "",
//                JOptionPane.ERROR_MESSAGE);
//        System.exit(1);
//    }
//    p.setBackgroundImage(image);



      JFrame f = new JFrame("Plotter Test");
      f.addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent e)
         {
            System.exit(0);
         }
      });

      f.getContentPane().add(p, BorderLayout.CENTER);
      f.pack();
      f.setVisible(true);
   }

   // ////////////////////////////////////////////////////////////////////////
   private class PlotterMouseListener extends MouseInputAdapter
   {
      public void mouseClicked(MouseEvent e)
      {
         if (buttonPressed == MouseEvent.BUTTON1)
         {
//          _Xselected = unConvertXCoordinate(e.getX());
//          _Yselected = unConvertYCoordinate(e.getY());
            removeArtifact("path");
            removeArtifact("polygon");
            polygonArtifact = null;
            MAKING_POLYGON = false;
         }
         else if (buttonPressed == MouseEvent.BUTTON3)
         {
//          if (MAKING_POLYGON)
//          {
//              Point2d point = new Point2d(unConvertXCoordinate(e.getX()), unConvertYCoordinate(e.getY()));
//              polygonArtifact.addPoint(point);
//              updateArtifact(polygonArtifact);
//          }
//          else
//          {
//              MAKING_POLYGON = true;
//              polygonArtifact = new PolygonArtifact("polygon");
//              Point2d point = new Point2d(unConvertXCoordinate(e.getX()), unConvertYCoordinate(e.getY()));
//              polygonArtifact.addPoint(point);
//              polygonArtifact.setColor(Color.red);
//              addArtifact(polygonArtifact);
//          }
         }
      }

      public void mousePressed(MouseEvent e)
      {
         buttonPressed = e.getButton();

         if (buttonPressed == MouseEvent.BUTTON1)
         {
            _Xselected = unConvertXCoordinate(e.getX());
            _Yselected = unConvertYCoordinate(e.getY());

//          _areaX1TEMP = unConvertXCoordinate(e.getX());
//          _areaY1TEMP = unConvertYCoordinate(e.getY());
         }
         else if (buttonPressed == MouseEvent.BUTTON3)
         {
            _Xselected = unConvertXCoordinate(e.getX());
            _Yselected = unConvertYCoordinate(e.getY());

//          dragStartX = e.getX();
//          dragStartY = e.getY();
//          xOffsetStartOfDrag = _Xoffset;
//          yOffsetStartOfDrag = _Yoffset;
         }
         else if (buttonPressed == MouseEvent.BUTTON2)
         {
            dragStartY = e.getY();
         }

         // check for double-clicks
         if (e.getClickCount() > 1)
         {
            if (buttonPressed == MouseEvent.BUTTON1)
            {
               if (_listener != null)
               {
                  _listener.doubleClicked();
               }
            }
            else if (buttonPressed == MouseEvent.BUTTON3)
            {
               Coordinate offset = convertFromPixelsToMeters(new Coordinate(e.getX(), e.getY(), Coordinate.PIXEL));
               setXoffset(offset.getX());
               setYoffset(offset.getY());
               repaint();
            }
         }
      }

      public void mouseDragged(MouseEvent e)
      {
         if (buttonPressed == MouseEvent.BUTTON1)
         {
            _areaX1 = _areaX1TEMP;
            _areaY1 = _areaY1TEMP;
            _areaX2 = unConvertXCoordinate(e.getX());
            _areaY2 = unConvertYCoordinate(e.getY());
         }
         else if (buttonPressed == MouseEvent.BUTTON3)
         {
//          int idx = e.getX() - dragStartX;
//          int idy = -(e.getY() - dragStartY);
//          double dx = idx / _scaleFactor;
//          double dy = idy / _scaleFactor;
//          setXoffset(xOffsetStartOfDrag - dx);
//          setYoffset(yOffsetStartOfDrag - dy);
         }
         else if (buttonPressed == MouseEvent.BUTTON2)
         {
            int yDifferenceFromStartOfDrag = -(e.getY() - dragStartY);
            double scaledYChange = new Double(yDifferenceFromStartOfDrag * 0.5);
            if (getRange() < 10)
               scaledYChange = new Double(yDifferenceFromStartOfDrag * 0.01);
            double newRange = getRange() + scaledYChange;
            if (newRange < 0.1)
            {
               newRange = 0.1;
            }

            setRange(newRange);
            dragStartY = e.getY();
         }

         repaint();
      }

      public void mouseReleased(MouseEvent e)
      {
      }
   }


   public void addArtifactsChangedListener(ArtifactsChangedListener artifactsChangedListener)
   {
      this.artifactsChangedListeners.add(artifactsChangedListener);
   }

   public void notifyArtifactsChangedListeners()
   {
      for (ArtifactsChangedListener artifactsChangedListener : artifactsChangedListeners)
      {
         artifactsChangedListener.artifactsChanged(getArtifacts());
      }
   }

   public PlotterLegendPanel createPlotterLegendPanel()
   {
      PlotterLegendPanel ret = new PlotterLegendPanel();

      this.addArtifactsChangedListener(ret);

      return ret;
   }

   public JPanel createAndAttachPlotterLegendPanel()
   {
      JPanel ret = new JPanel();

      PlotterLegendPanel plotterLegendPanel = new PlotterLegendPanel();

      this.addArtifactsChangedListener(plotterLegendPanel);


      ret.setLayout(new BorderLayout());
      ret.add(this, "Center");
      ret.add(plotterLegendPanel, "South");

      return ret;

   }
}
