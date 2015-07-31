package us.ihmc.graphics3DAdapter;

import org.junit.Test;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.jme.JMEGraphics3DAdapter;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.BambooPlan;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.utilities.code.agileTesting.BambooPlanType;
import us.ihmc.robotics.geometry.Sphere3d;

@BambooPlan(planType={BambooPlanType.UI})
public class Graphics3DWorldTest
{

	@EstimatedDuration
	@Test(timeout=300000)
   public void testShowGui()
   {
      Graphics3DWorld world = new Graphics3DWorld("testWorld", new JMEGraphics3DAdapter());
      world.startWithGui();
      world.keepAlive(1.0);
      world.stop();
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void testWithoutGui()
   {
      Graphics3DWorld world = new Graphics3DWorld("testWorld", new JMEGraphics3DAdapter());
      world.startWithoutGui();
      world.keepAlive(1.0);
      world.stop();
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void addASphere()
   {
      Graphics3DWorld world = new Graphics3DWorld("testWorld", new JMEGraphics3DAdapter());
      world.addChild(new Graphics3DNode("Sphere", new Graphics3DObject(new Sphere3d(), YoAppearance.Glass())));
      world.startWithoutGui();
      world.keepAlive(1.0);
      world.stop();
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void addASphereAfterGuiStarted()
   {
      Graphics3DWorld world = new Graphics3DWorld("testWorld", new JMEGraphics3DAdapter());
      world.startWithoutGui();
      world.addChild(new Graphics3DNode("Sphere", new Graphics3DObject(new Sphere3d())));
      world.keepAlive(1.0);
      world.stop();
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void testSetCameraPosition()
   {
      Graphics3DWorld world = new Graphics3DWorld("testWorld", new JMEGraphics3DAdapter());
      world.addChild(new Graphics3DNode("Sphere", new Graphics3DObject(new Sphere3d(), YoAppearance.Glass(0.2))));
      world.startWithGui();
      world.setCameraPosition(5, 5, 5);
      world.keepAlive(1.0);
      world.stop();
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void fixCameraOnSphere()
   {
      Graphics3DWorld world = new Graphics3DWorld("testWorld", new JMEGraphics3DAdapter());
      
      Graphics3DNode sphereNode = new Graphics3DNode("Sphere", new Graphics3DObject(new Sphere3d()));
      world.addChild(sphereNode);
      world.startWithGui();
      world.setCameraPosition(5, 5, 5);
      world.fixCameraOnNode(sphereNode);
      
      world.keepAlive(1.0);
      world.stop();
   }
}
