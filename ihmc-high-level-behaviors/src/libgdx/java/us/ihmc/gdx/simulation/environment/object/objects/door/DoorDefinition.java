package us.ihmc.gdx.simulation.environment.object.objects.door;

import us.ihmc.euclid.Axis3D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.yawPitchRoll.YawPitchRoll;
import us.ihmc.scs2.definition.robot.RevoluteJointDefinition;
import us.ihmc.scs2.definition.robot.RigidBodyDefinition;
import us.ihmc.scs2.definition.robot.RobotDefinition;
import us.ihmc.scs2.definition.robot.SixDoFJointDefinition;
import us.ihmc.scs2.definition.state.OneDoFJointState;
import us.ihmc.scs2.definition.state.SixDoFJointState;

public class DoorDefinition extends RobotDefinition
{
   private final SixDoFJointState initialSixDoFState;
   private final OneDoFJointState initialHingeState;

   public DoorDefinition()
   {
      super("door");
      RigidBodyDefinition rootBodyDefinition = new RigidBodyDefinition("doorRootBody");

      SixDoFJointDefinition rootJointDefinition = new SixDoFJointDefinition("doorRootJoint");
      rootBodyDefinition.addChildJoint(rootJointDefinition);
      initialSixDoFState = new SixDoFJointState(new YawPitchRoll(), new Point3D());
      initialSixDoFState.setVelocity(new Vector3D(), new Vector3D());
      rootJointDefinition.setInitialJointState(initialSixDoFState);

      DoorFrameDefinition doorFrameDefinition = new DoorFrameDefinition();
      rootJointDefinition.setSuccessor(doorFrameDefinition);

      RevoluteJointDefinition doorHingeJoint = new RevoluteJointDefinition("doorHingeJoint");
      doorHingeJoint.setAxis(Axis3D.Z);
      doorFrameDefinition.addChildJoint(doorHingeJoint);
      doorHingeJoint.getTransformToParent().getTranslation().add(0.0, 0.005, 0.02);
      initialHingeState = new OneDoFJointState();
      doorHingeJoint.setInitialJointState(initialHingeState);

      DoorPanelDefinition doorPanelDefinition = new DoorPanelDefinition();
      doorHingeJoint.setSuccessor(doorPanelDefinition);

      setRootBodyDefinition(rootBodyDefinition);
   }

   public SixDoFJointState getInitialSixDoFState()
   {
      return initialSixDoFState;
   }

   public OneDoFJointState getInitialHingeState()
   {
      return initialHingeState;
   }
}
