package us.ihmc.plotting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import us.ihmc.plotting.plotter2d.PlotterColors;
import us.ihmc.plotting.plotter2d.PlotterPoint2d;
import us.ihmc.plotting.plotter2d.frames.MetersReferenceFrame;
import us.ihmc.plotting.plotter2d.frames.PixelsReferenceFrame;
import us.ihmc.plotting.plotter2d.frames.PlotterFrameSpace;
import us.ihmc.plotting.plotter2d.frames.PlotterSpaceConverter;
import us.ihmc.plotting.shapes.LineArtifact;
import us.ihmc.plotting.shapes.PointArtifact;
import us.ihmc.robotics.geometry.Line2d;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.FormattingTools;
import us.ihmc.tools.io.printing.PrintTools;

/**
 * TODO Deprecate archaic methods
 * TODO Factor out artifacts.
 * TODO Fix Artifact interface.
 * TODO Remove PlotterPanel
 */
@SuppressWarnings("serial")
public class Plotter
{
   private static final boolean SHOW_LABELS_BY_DEFAULT = true;
   private static final boolean SHOW_SELECTION_BY_DEFAULT = false;
   private static final boolean SHOW_HISTORY_BY_DEFAULT = false;
   
   private boolean showLabels = SHOW_LABELS_BY_DEFAULT;
   private boolean showSelection = SHOW_SELECTION_BY_DEFAULT;
   private boolean showHistory = SHOW_HISTORY_BY_DEFAULT;
   
   private final JPanel panel;
   
   private final PlotterMouseAdapter mouseAdapter;
   private final PlotterComponentAdapter componentAdapter;
   
   private final Vector2d metersToPixels = new Vector2d(50.0, 50.0);
   private final Rectangle visibleRectangle = new Rectangle();
   private final Dimension preferredSize = new Dimension(500, 500);
   private final Vector2d gridSizePixels = new Vector2d();
   private BufferedImage backgroundImage = null;
   
   private final PlotterSpaceConverter spaceConverter;
   private final PixelsReferenceFrame pixelsFrame;
   private final PixelsReferenceFrame screenFrame;
   private final MetersReferenceFrame metersFrame;
   
   private final PlotterPoint2d screenPosition;
   private final PlotterPoint2d upperLeftCorner;
   private final PlotterPoint2d lowerRightCorner;
   private final PlotterPoint2d focusPoint;
   private final PlotterPoint2d origin;
   private final PlotterPoint2d drawGuy;
   private final PlotterPoint2d selected;
   private final PlotterPoint2d selectionAreaStart;
   private final PlotterPoint2d selectionAreaEnd;
   
   // Artifact stuff
   private final ArrayList<ArtifactsChangedListener> artifactsChangedListeners = new ArrayList<ArtifactsChangedListener>();
   private final HashMap<String, Artifact> artifacts = new HashMap<String, Artifact>();
   
   public Plotter()
   {
      panel = new JPanel()
      {
         @Override
         protected void paintComponent(Graphics graphics)
         {
            updateFrames();
            
            super.paintComponent(graphics);
            
            Plotter.this.paintComponent((Graphics2D) graphics);
         }
         
         @Override
         public Dimension getPreferredSize()
         {
            return Plotter.this.preferredSize;
         }
         
         @Override
         public void setPreferredSize(Dimension preferredSize)
         {
            Plotter.this.preferredSize.setSize(preferredSize);
         }
      };
      
      spaceConverter = new PlotterSpaceConverter()
      {
         private Vector2d scaleVector = new Vector2d();
         
         @Override
         public Vector2d getConversionToSpace(PlotterFrameSpace plotterFrameType)
         {
            if (plotterFrameType == PlotterFrameSpace.METERS)
            {
               scaleVector.set(1.0 / metersToPixels.getX(), 1.0 / metersToPixels.getY());
            }
            else
            {
               scaleVector.set(metersToPixels.getX(), metersToPixels.getY());
            }
            return scaleVector;
         }
      };
      pixelsFrame = new PixelsReferenceFrame("pixelsFrame", ReferenceFrame.getWorldFrame(), spaceConverter)
      {
         @Override
         protected void updateTransformToParent(RigidBodyTransform transformToParent)
         {
         }
      };
      screenFrame = new PixelsReferenceFrame("screenFrame", pixelsFrame, spaceConverter)
      {
         @Override
         protected void updateTransformToParent(RigidBodyTransform transformToParent)
         {
            screenPosition.changeFrame(pixelsFrame);
            transformToParent.setRotationEulerAndZeroTranslation(0.0, Math.PI, Math.PI);
            transformToParent.setTranslation(screenPosition.getX(), screenPosition.getY(), 0.0);
         }
      };
      metersFrame = new MetersReferenceFrame("metersFrame", ReferenceFrame.getWorldFrame(), spaceConverter)
      {
         @Override
         protected void updateTransformToParent(RigidBodyTransform transformToParent)
         {
         }
      };
      
      screenPosition = new PlotterPoint2d(pixelsFrame);
      upperLeftCorner = new PlotterPoint2d(screenFrame);
      lowerRightCorner = new PlotterPoint2d(screenFrame);
      origin = new PlotterPoint2d(metersFrame);
      focusPoint = new PlotterPoint2d(screenFrame);
      drawGuy = new PlotterPoint2d(screenFrame);
      selected = new PlotterPoint2d(screenFrame);
      selectionAreaStart = new PlotterPoint2d(screenFrame);
      selectionAreaEnd = new PlotterPoint2d(screenFrame);
      
      screenPosition.set(-preferredSize.getWidth() / 2.0, preferredSize.getHeight() / 2.0);
      focusPoint.setIncludingFrame(metersFrame, 0.0, 0.0);
      
      updateFrames();
      
      panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createLoweredBevelBorder()));
      panel.setBackground(PlotterColors.BACKGROUND);
      
      mouseAdapter = new PlotterMouseAdapter();
      componentAdapter = new PlotterComponentAdapter();
      
      panel.addMouseListener(mouseAdapter);
      panel.addMouseMotionListener(mouseAdapter);
      panel.addComponentListener(componentAdapter);
   }
   
   private void updateFrames()
   {
      panel.computeVisibleRect(visibleRectangle);
      
      pixelsFrame.update();
      screenFrame.update();
      metersFrame.update();
      
      upperLeftCorner.setIncludingFrame(screenFrame, 0.0, 0.0);
      lowerRightCorner.setIncludingFrame(screenFrame, visibleRectangle.getWidth(), visibleRectangle.getHeight());
      origin.setIncludingFrame(metersFrame, 0.0, 0.0);
   }
   
   public void setScale(double pixelsPerMeterX, double pixelsPerMeterY)
   {
      focusPoint.changeFrame(metersFrame);
      metersToPixels.set(pixelsPerMeterX, pixelsPerMeterY);
      
      centerOnFocusPoint();
   }
   
   private void centerOnFocusPoint()
   {
      focusPoint.changeFrame(pixelsFrame);
      
      screenPosition.changeFrame(pixelsFrame);
      screenPosition.set(focusPoint);
      if (isInitialized())
      {
         screenPosition.add(-visibleRectangle.getWidth() / 2.0, visibleRectangle.getHeight() / 2.0);
      }
      else
      {
         screenPosition.add(-preferredSize.getWidth() / 2.0, preferredSize.getHeight() / 2.0);
      }
      
      updateFrames();
   }

   private boolean isInitialized()
   {
      return visibleRectangle.getWidth() > 0.0;
   }

   private void paintComponent(final Graphics2D graphics2d)
   {
      origin.changeFrame(screenFrame);
      forAllArtifacts(86, new ArtifactIterator()
      {
         @Override
         public void drawArtifact(Artifact artifact)
         {
            artifact.draw(graphics2d, (int) Math.round(origin.getX()), (int) Math.round(origin.getY()), 0.0, metersToPixels);
         }
      });
      
      if (backgroundImage != null)
      {
         graphics2d.drawImage(backgroundImage, (int) Math.round(upperLeftCorner.getX()),
                                               (int) Math.round(upperLeftCorner.getY()),
                                               (int) Math.round(lowerRightCorner.getX()),
                                               (int) Math.round(lowerRightCorner.getY()),
                                               0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), panel);
      }
      else
      {
         // change grid line scale from 1m to 10cm ehn below 10m
         gridSizePixels.set(calculateGridSizePixels(metersToPixels.getX()), calculateGridSizePixels(metersToPixels.getY()));
         
         upperLeftCorner.changeFrame(pixelsFrame);
         lowerRightCorner.changeFrame(pixelsFrame);
         
         double overShoot = upperLeftCorner.getX() % gridSizePixels.getX();
         for (double gridX = upperLeftCorner.getX() - overShoot; gridX < lowerRightCorner.getX(); gridX += gridSizePixels.getX())
         {
            drawGuy.changeFrame(pixelsFrame);
            drawGuy.set(upperLeftCorner);
            drawGuy.setX(gridX);
            
            int nthGridLineFromOrigin = (int) (Math.abs(drawGuy.getX()) / gridSizePixels.getX());
            applyColorForGridline(graphics2d, nthGridLineFromOrigin);
   
            drawGuy.changeFrame(screenFrame);
            graphics2d.drawLine((int) Math.round(drawGuy.getX()), 0, (int) Math.round(drawGuy.getX()), (int) visibleRectangle.getHeight());
            
            if (showLabels)
            {
               Color tempColor = graphics2d.getColor();
               graphics2d.setColor(PlotterColors.LABEL_COLOR);
               drawGuy.changeFrame(metersFrame);
               String labelString = FormattingTools.getFormattedToSignificantFigures(drawGuy.getX(), 2);
               drawGuy.changeFrame(screenFrame);
               graphics2d.drawString(labelString, (int) Math.round(drawGuy.getX()) + 1, (int) Math.round(origin.getY()) - 1);
               graphics2d.setColor(tempColor);
            }
         }
         
         overShoot = lowerRightCorner.getY() % gridSizePixels.getY();
         for (double gridY = lowerRightCorner.getY() - overShoot; gridY < upperLeftCorner.getY(); gridY += gridSizePixels.getY())
         {
            drawGuy.changeFrame(pixelsFrame);
            drawGuy.set(upperLeftCorner);
            drawGuy.setY(gridY);
            
            int nthGridLineFromOrigin = (int) (Math.abs(drawGuy.getY()) / gridSizePixels.getY());
            applyColorForGridline(graphics2d, nthGridLineFromOrigin);
   
            drawGuy.changeFrame(screenFrame);
            graphics2d.drawLine(0, (int) Math.round(drawGuy.getY()), (int) visibleRectangle.getWidth(), (int) Math.round(drawGuy.getY()));
            
            if (showLabels)
            {
               Color tempColor = graphics2d.getColor();
               graphics2d.setColor(PlotterColors.LABEL_COLOR);
               drawGuy.changeFrame(metersFrame);
               String labelString = FormattingTools.getFormattedToSignificantFigures(drawGuy.getY(), 2);
               drawGuy.changeFrame(screenFrame);
               graphics2d.drawString(labelString, (int) Math.round(origin.getX()) + 1, (int) Math.round(drawGuy.getY()) - 1);
               graphics2d.setColor(tempColor);
            }
         }
      }
      
      // paint grid centerline
      graphics2d.setColor(PlotterColors.GRID_AXIS);
      graphics2d.drawLine((int) Math.round(origin.getX()), 0, (int) Math.round(origin.getX()), (int) visibleRectangle.getHeight());
      graphics2d.drawLine(0, (int) Math.round(origin.getY()), (int) visibleRectangle.getWidth(), (int) Math.round(origin.getY()));
      
      for (int artifactLevel = 0; artifactLevel < 5; artifactLevel++)
      {
         forAllArtifacts(artifactLevel, new ArtifactIterator()
         {
            @Override
            public void drawArtifact(Artifact artifact)
            {
               if (showHistory && artifact.getDrawHistory())
               {
                  artifact.drawHistory(graphics2d, (int) Math.round(origin.getX()), (int) Math.round(origin.getY()), 0.0, metersToPixels);
               }
            }
         });
      }
      
      for (int artifactLevel = 0; artifactLevel < 5; artifactLevel++)
      {
         forAllArtifacts(artifactLevel, new ArtifactIterator()
         {
            @Override
            public void drawArtifact(Artifact artifact)
            {
               artifact.draw(graphics2d, (int) Math.round(origin.getX()), (int) Math.round(origin.getY()), 0.0, metersToPixels);
            }
         });
      }
      
      // paint selected destination
      if (showSelection)
      {
         selected.changeFrame(screenFrame);
         graphics2d.setColor(PlotterColors.SELECTION);
         double crossSize = 8.0;
         graphics2d.drawLine((int) Math.round(selected.getX() - crossSize),
                             (int) Math.round(selected.getY() - crossSize),
                             (int) Math.round(selected.getX() + crossSize),
                             (int) Math.round(selected.getY() + crossSize));
         graphics2d.drawLine((int) Math.round(selected.getX() - crossSize),
                             (int) Math.round(selected.getY() + crossSize),
                             (int) Math.round(selected.getX() + crossSize),
                             (int) Math.round(selected.getY() - crossSize));
      }

      // paint selected area
      if (showSelection)
      {
         graphics2d.setColor(PlotterColors.SELECTION);
         double Xmin, Xmax, Ymin, Ymax;
         if (selectionAreaStart.getX() > selectionAreaEnd.getX())
         {
            Xmax = selectionAreaStart.getX();
            Xmin = selectionAreaEnd.getX();
         }
         else
         {
            Xmax = selectionAreaEnd.getX();
            Xmin = selectionAreaStart.getX();
         }

         if (selectionAreaStart.getY() > selectionAreaEnd.getY())
         {
            Ymax = selectionAreaStart.getY();
            Ymin = selectionAreaEnd.getY();
         }
         else
         {
            Ymax = selectionAreaEnd.getY();
            Ymin = selectionAreaStart.getY();
         }

         graphics2d.drawRect((int) Math.round(Xmin),
                             (int) Math.round(Ymin),
                             (int) Math.round(Xmax - Xmin),
                             (int) Math.round(Ymax - Ymin));
      }
   }

   private double calculateGridSizePixels(double pixelsPerMeter)
   {
      double medianGridWidthInPixels = Toolkit.getDefaultToolkit().getScreenResolution();
      double desiredMeters = medianGridWidthInPixels / pixelsPerMeter;
      double decimalPlace = Math.log10(desiredMeters);
      double orderOfMagnitude = Math.floor(decimalPlace);
      double nextOrderOfMagnitude = Math.pow(10, orderOfMagnitude + 1);
      double percentageToNextOrderOfMagnitude = desiredMeters / nextOrderOfMagnitude;
      
      double remainder = percentageToNextOrderOfMagnitude % 0.5;
      double roundToNearestPoint5 = remainder >= 0.25 ? percentageToNextOrderOfMagnitude + (0.5 - remainder) : percentageToNextOrderOfMagnitude - remainder;
      
      double gridSizeMeters;
      if (roundToNearestPoint5 > 0.0)
      {
         gridSizeMeters = nextOrderOfMagnitude * roundToNearestPoint5;
      }
      else
      {
         gridSizeMeters = Math.pow(10, orderOfMagnitude);
      }
      double gridSizePixels = gridSizeMeters * pixelsPerMeter;
      
      return gridSizePixels;
   }

   private void applyColorForGridline(final Graphics2D graphics2d, int nthGridLineFromOrigin)
   {
      if (nthGridLineFromOrigin % 10 == 0)
      {
         graphics2d.setColor(PlotterColors.GRID_EVERY_10);
      }
      else if (nthGridLineFromOrigin % 5 == 0)
      {
         graphics2d.setColor(PlotterColors.GRID_EVERY_5);
      }
      else
      {
         graphics2d.setColor(PlotterColors.GRID_EVERY_1);
      }
   }
   
   private void forAllArtifacts(int level, ArtifactIterator artifactIterator)
   {
      synchronized (artifacts)
      {
         for (Artifact artifact : artifacts.values())
         {
            if (artifact != null)
            {
               if (artifact.getLevel() == level)
               {
                  artifactIterator.drawArtifact(artifact);
               }
            }
            else
            {
               PrintTools.error("Plotter: one of the artifacts you added was null");
            }
         }
      }
   }
   
   private interface ArtifactIterator
   {
      public void drawArtifact(Artifact artifact); 
   }
   
   private class PlotterMouseAdapter extends MouseAdapter
   {
      private int buttonPressed;
      private PlotterPoint2d middleMouseDragStart = new PlotterPoint2d(screenFrame);
      private PlotterPoint2d middleMouseDragEnd = new PlotterPoint2d(screenFrame);
      private PlotterPoint2d rightMouseDragStart = new PlotterPoint2d(screenFrame);
      private PlotterPoint2d rightMouseDragEnd = new PlotterPoint2d(screenFrame);

      @Override
      public void mousePressed(MouseEvent e)
      {
         buttonPressed = e.getButton();

         if (buttonPressed == MouseEvent.BUTTON1)
         {
            selected.setIncludingFrame(screenFrame, e.getX(), e.getY());
            selectionAreaStart.setIncludingFrame(screenFrame, e.getX(), e.getY());
         }
         else if (buttonPressed == MouseEvent.BUTTON2)
         {
            middleMouseDragStart.setIncludingFrame(screenFrame, e.getX(), e.getY());
         }
         else if (buttonPressed == MouseEvent.BUTTON3)
         {
            rightMouseDragStart.setIncludingFrame(screenFrame, e.getX(), e.getY());
            
            if (e.getClickCount() > 1)
            {
               focusPoint.setIncludingFrame(screenFrame, e.getX(), e.getY());
               centerOnFocusPoint();
               panel.repaint();
            }
         }
      }

      @Override
      public void mouseDragged(MouseEvent e)
      {
         if (buttonPressed == MouseEvent.BUTTON1)
         {
            selectionAreaEnd.changeFrame(screenFrame);
            selectionAreaEnd.set(e.getX(), e.getY());
            
            panel.repaint();
         }
         else if (buttonPressed == MouseEvent.BUTTON2)
         {
            middleMouseDragEnd.setIncludingFrame(screenFrame, e.getX(), e.getY());
            
            double deltaDragY = middleMouseDragStart.getY() - middleMouseDragEnd.getY();
            
            double scaledXChange = 1.0 + (deltaDragY * 0.005);
            double scaledYChange = 1.0 + (deltaDragY * 0.005);
            
            focusPoint.setIncludingFrame(screenFrame, visibleRectangle.getWidth() / 2.0, visibleRectangle.getHeight() / 2.0);
            
            setScale(metersToPixels.getX() * scaledXChange, metersToPixels.getY() * scaledYChange);
            
            middleMouseDragStart.set(middleMouseDragEnd);
            
            panel.repaint();
         }
         else if (buttonPressed == MouseEvent.BUTTON3)
         {
            rightMouseDragEnd.setIncludingFrame(screenFrame, e.getX(), e.getY());
            
            focusPoint.changeFrame(screenFrame);
            
            rightMouseDragEnd.sub(rightMouseDragStart);
            rightMouseDragEnd.negate();
            focusPoint.add(rightMouseDragEnd);
            centerOnFocusPoint();
            panel.repaint();
            
            rightMouseDragStart.setIncludingFrame(screenFrame, e.getX(), e.getY());
         }
      }
   }
   
   private class PlotterComponentAdapter extends ComponentAdapter
   {
      @Override
      public void componentResized(ComponentEvent componentEvent)
      {
         centerOnFocusPoint();
         panel.repaint();
      }
   }
   
   public Dimension getPreferredSize()
   {
      return preferredSize;
   }
   
   public void setPreferredSize(int width, int height)
   {
      preferredSize.setSize(width, height);
   }

   public void setFocusPointY(double focusPointY)
   {
      focusPoint.changeFrame(metersFrame);
      focusPoint.setY(focusPointY);
      
      centerOnFocusPoint();
   }
   
   public void setFocusPointX(double focusPointX)
   {
      focusPoint.changeFrame(metersFrame);
      focusPoint.setX(focusPointX);
      
      centerOnFocusPoint();
   }
   
   public double getFocusPointX()
   {
      focusPoint.changeFrame(metersFrame);
      return focusPoint.getX();
   }
   
   public double getFocusPointY()
   {
      focusPoint.changeFrame(metersFrame);
      return focusPoint.getY();
   }
   
   /**
    * Specify amount of meters that occupy view in X and Y.
    */
   public void setViewRange(double viewRangeInX, double viewRangeInY)
   {
      if (isInitialized())
      {
         setScale(visibleRectangle.getWidth() / viewRangeInX, visibleRectangle.getHeight() / viewRangeInY);
      }
      else
      {
         setScale(getPreferredSize().getWidth() / viewRangeInX, getPreferredSize().getHeight() / viewRangeInY);
      }
   }
   
   public void setViewRange(double minimumViewRange)
   {
      double smallestDimension;
      if (isInitialized())
      {
         smallestDimension = Math.min(visibleRectangle.getWidth(), visibleRectangle.getHeight());
      }
      else
      {
         smallestDimension = Math.min(getPreferredSize().getWidth(), getPreferredSize().getHeight());
      }
      double newPixelsPerMeter = smallestDimension / minimumViewRange;
      setScale(newPixelsPerMeter, newPixelsPerMeter);
   }
   
   public void setScale(double pixelsPerMeter)
   {
      setScale(pixelsPerMeter, pixelsPerMeter);
   }

   public void setShowLabels(boolean showLabels)
   {
      this.showLabels = showLabels;
   }

   public void showInNewWindow()
   {
      showInNewWindow("Plotter");
   }
   
   public void showInNewWindow(String title)
   {
      JFrame frame = new JFrame(title);
      frame.getContentPane().add(panel, BorderLayout.CENTER);
      frame.pack();
      frame.setVisible(true);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }
   
   public double getSelectedX()
   {
      selected.changeFrame(metersFrame);
      return selected.getX();
   }

   public double getSelectedY()
   {
      selected.changeFrame(metersFrame);
      return selected.getY();
   }
   
   public void setBackgroundImage(BufferedImage backgroundImage)
   {
      this.backgroundImage = backgroundImage;
      panel.repaint();
   }

   public double getViewRange()
   {
      if (visibleRectangle.getWidth() <= visibleRectangle.getHeight())
      {
         return metersToPixels.getX() * visibleRectangle.getWidth();
      }
      else
      {
         return metersToPixels.getY() * visibleRectangle.getHeight();
      }
   }

   public void setDrawHistory(boolean drawHistory)
   {
      this.showHistory = drawHistory;
   }

   public void update()
   {
      panel.repaint();
   }
   
   public JPanel getJPanel()
   {
      return panel;
   }

   public PlotterLegendPanel createPlotterLegendPanel()
   {
      PlotterLegendPanel plotterLegendPanel = new PlotterLegendPanel();
      addArtifactsChangedListener(plotterLegendPanel);
      return plotterLegendPanel;
   }

   public JPanel createAndAttachPlotterLegendPanel()
   {
      JPanel flashyNewJayPanel = new JPanel();
      PlotterLegendPanel plotterLegendPanel = new PlotterLegendPanel();
      addArtifactsChangedListener(plotterLegendPanel);
      flashyNewJayPanel.setLayout(new BorderLayout());
      flashyNewJayPanel.add(panel, "Center");
      flashyNewJayPanel.add(plotterLegendPanel, "South");
      return flashyNewJayPanel;
   }
   
   // BEGIN ARTIFACT GARBAGE //
   
   public void updateArtifacts(Vector<Artifact> artifacts)
   {
      this.artifacts.clear();

      for (Artifact a : artifacts)
      {
         this.artifacts.put(a.getID(), a);
      }

      notifyArtifactsChangedListeners();
      panel.repaint();
   }

   public void updateArtifact(Artifact newArtifact)
   {
      synchronized (artifacts)
      {
         artifacts.put(newArtifact.getID(), newArtifact);
      }

      notifyArtifactsChangedListeners();
      panel.repaint();
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
      panel.repaint();
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
      panel.repaint();
   }

   public void removeArtifact(String id)
   {
      synchronized (artifacts)
      {
         artifacts.remove(id);
      }

      notifyArtifactsChangedListeners();
      panel.repaint();
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
      panel.repaint();
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
   
   // END ARTIFACT GARBAGE //
}
