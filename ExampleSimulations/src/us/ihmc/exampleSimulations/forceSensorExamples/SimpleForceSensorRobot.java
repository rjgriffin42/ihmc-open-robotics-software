package us.ihmc.exampleSimulations.forceSensorExamples;

import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.simulationconstructionset.FloatingJoint;
import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.JointWrenchSensor;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SliderJoint;
import us.ihmc.robotics.Axis;

public class SimpleForceSensorRobot extends Robot
{

   public SimpleForceSensorRobot()
   {
      super("SimpleForceSensorRobot");

      FloatingJoint baseJoint = new FloatingJoint("base", new Vector3d(), this);

      double width = 0.2;
      
      GroundContactPoint gcOne = new GroundContactPoint("gcOne", new Vector3d(width, width, 0.0), this);
      GroundContactPoint gcTwo = new GroundContactPoint("gcTwo", new Vector3d(width, -width, 0.0), this);
      GroundContactPoint gcThree = new GroundContactPoint("gcThree", new Vector3d(-width, -width, 0.0), this);
      GroundContactPoint gcFour = new GroundContactPoint("gcFour", new Vector3d(-width, width, 0.0), this);
      
      baseJoint.addGroundContactPoint(gcOne);
      baseJoint.addGroundContactPoint(gcTwo);
      baseJoint.addGroundContactPoint(gcThree);
      baseJoint.addGroundContactPoint(gcFour);
      
      
      Link baseLink = new Link("baseLink");
      
      baseLink.setMassAndRadiiOfGyration(1.0, width, width, width);
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.addCube(width, width, width);
      baseLink.setLinkGraphics(linkGraphics);
      
      baseJoint.setLink(baseLink);

      addRootJoint(baseJoint);

      
      SliderJoint sliderJoint = new SliderJoint("slider", new Vector3d(), this, Axis.Z);
      
      JointWrenchSensor jointWrenchSensor = new JointWrenchSensor("jointWrench", new Vector3d(), this);
      sliderJoint.addJointWrenchSensor(jointWrenchSensor);     
      
      
      Link sliderLink = new Link("sliderLink");
      sliderLink.setMassAndRadiiOfGyration(1.0, width, width, width);
      linkGraphics = new Graphics3DObject();
      linkGraphics.addCylinder(2.5 * width, 0.5*width, YoAppearance.Red());
      sliderLink.setLinkGraphics(linkGraphics);
      sliderJoint.setLink(sliderLink);
      
      sliderJoint.setTau(9.81 * 1.0);
      
      baseJoint.addJoint(sliderJoint);


      
   }

   
   
   
}
