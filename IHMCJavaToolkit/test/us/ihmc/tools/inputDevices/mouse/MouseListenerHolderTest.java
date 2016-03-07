package us.ihmc.tools.inputDevices.mouse;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class MouseListenerHolderTest
{
   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testMouseListenerHolder()
   {
      final List<MyMouseEvent> mouseEvents1 = new ArrayList<>();
      final List<MyMouseEvent> mouseEvents2 = new ArrayList<>();
      
      MouseListenerHolder mouseListenerHolder = new MouseListenerHolder();
      mouseListenerHolder.addMouseListener(new MouseListener()
      {
         @Override
         public void mouseDragged(MouseButton mouseButton, double dx, double dy)
         {
            MyMouseEvent myMouseEvent = new MyMouseEvent();
            myMouseEvent.mouseButton = mouseButton;
            myMouseEvent.dx = dx;
            myMouseEvent.dy = dy;
            mouseEvents1.add(myMouseEvent);
         }
      });
      
      mouseListenerHolder.mouseDragged(null, 2.0, 5.0);
      
      mouseListenerHolder.addMouseListener(new MouseListener()
      {
         @Override
         public void mouseDragged(MouseButton mouseButton, double dx, double dy)
         {
            MyMouseEvent myMouseEvent = new MyMouseEvent();
            myMouseEvent.mouseButton = mouseButton;
            myMouseEvent.dx = dx;
            myMouseEvent.dy = dy;
            mouseEvents2.add(myMouseEvent);
         }
      });
      
      mouseListenerHolder.mouseDragged(null, 4.0, 2.0);
      
      assertTrue("Mouse event not registered.", mouseEvents1.size() == 2);
      assertTrue("Mouse event not registered.", mouseEvents2.size() == 1);
      
      assertEquals("Incorrect values.", mouseEvents1.get(1).dx, 4.0, 1e-7);
      assertEquals("Incorrect values.", mouseEvents2.get(0).dy, 2.0, 1e-7);
      assertEquals("Incorrect values.", mouseEvents2.get(0).mouseButton, null);
   }
   
   private class MyMouseEvent
   {
      MouseButton mouseButton;
      double dx;
      double dy;
   }
}
