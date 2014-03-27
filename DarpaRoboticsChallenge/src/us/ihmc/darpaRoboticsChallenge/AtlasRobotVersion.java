package us.ihmc.darpaRoboticsChallenge;

import java.io.InputStream;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import us.ihmc.darpaRoboticsChallenge.handControl.DRCHandModel;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;

public enum AtlasRobotVersion {
	ATLAS_NO_HANDS_ADDED_MASS, ATLAS_SANDIA_HANDS, ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS, DRC_NO_HANDS, DRC_HANDS, DRC_EXTENDED_HANDS, DRC_HOOKS, DRC_TASK_HOSE, DRC_EXTENDED_HOOKS;
	
	private static Class<AtlasRobotVersion> thisClass = AtlasRobotVersion.class;
	private static String[] resourceDirectories;
	private final SideDependentList<Transform> offsetHandFromWrist = new SideDependentList<Transform>(); 
	
	public DRCHandModel getHandModel() {
		switch (this) 
		{
		case DRC_HANDS:
		case DRC_EXTENDED_HANDS:
		case DRC_TASK_HOSE:
			return DRCHandModel.IROBOT;

		case ATLAS_SANDIA_HANDS:
			return DRCHandModel.SANDIA;

		case DRC_HOOKS:
			return DRCHandModel.HOOK;
			
		case ATLAS_NO_HANDS_ADDED_MASS:
		case DRC_NO_HANDS:
		case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS:
		case DRC_EXTENDED_HOOKS:
		default:
			return DRCHandModel.NONE;
		}
	}

	public boolean hasArmExtensions() {
      switch (this)
      {
      case DRC_EXTENDED_HANDS:
         return true;

      default:
         return false;
      }
	}
	
	public boolean hasIrobotHands() {
		return getHandModel() == DRCHandModel.IROBOT;
	}
	
	public boolean hasHookHands() {
		return getHandModel() == DRCHandModel.HOOK;
	}
	
	public boolean hasSandiaHands() {
		return getHandModel() == DRCHandModel.SANDIA;
	}

	public String getSdfFile() {
		switch (this)
	      {
	         case ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS :
	            return "models/GFE/atlas.sdf";
	         case ATLAS_SANDIA_HANDS :
	            return "models/GFE/atlas_sandia_hands.sdf";
	         case ATLAS_NO_HANDS_ADDED_MASS :
	            return "models/GFE/atlas_addedmass.sdf";
	         case DRC_NO_HANDS:
	            return "models/GFE/drc_no_hands.sdf";
	         case DRC_HANDS:
	            return "models/GFE/drc_hands.sdf";
	         case DRC_EXTENDED_HANDS:
	            return "models/GFE/drc_extended_hands.sdf";
	         case DRC_HOOKS:
	            return "models/GFE/drc_hooks.sdf";
	         case DRC_TASK_HOSE:
	            return "models/GFE/drc_task_hose.sdf";
	         case DRC_EXTENDED_HOOKS:
	            return "models/GFE/drc_extended_hooks.sdf";
	         default :
	            throw new RuntimeException("AtlasRobotVersion: Unimplemented enumeration case : " + this);
	      }
	}

	public String[] getResourceDirectories() {

		if(resourceDirectories == null)
		{
			resourceDirectories = new String[] {
			         thisClass.getResource("models/GFE/gazebo_models/atlas_description").getFile(),
			         thisClass.getResource("models/GFE/gazebo_models/multisense_sl_description").getFile(),
			         thisClass.getResource("models/GFE/gazebo_models/irobot_hand_description").getFile()
				};
		}
		return resourceDirectories;
	}

	public InputStream getSdfFileAsStream() {
		return thisClass.getResourceAsStream(getSdfFile());
	}

	public Transform getOffsetFromWrist(RobotSide side) {
		
		if(offsetHandFromWrist.get(side) == null)
		{
			createTransforms();
		}
		return offsetHandFromWrist.get(side);
	}
	
	private void createTransforms()
	{
		for(RobotSide robotSide : RobotSide.values())
		{
			Vector3f centerOfHandToWristTranslation = new Vector3f();
			float[] angles = new float[3];
			if (hasArmExtensions())
			{
				centerOfHandToWristTranslation = new Vector3f(0.31f, (float) robotSide.negateIfLeftSide(0f), 0f);
				angles[0] = (float) robotSide.negateIfLeftSide(Math.toRadians(90));
				angles[1] = 0.0f;
				angles[2] = (float) robotSide.negateIfLeftSide(Math.toRadians(0));
			}
			
			else if (hasIrobotHands())
			{
				centerOfHandToWristTranslation = new Vector3f(0.1f, (float) robotSide.negateIfLeftSide(0f), 0f);
				angles[0] = (float) robotSide.negateIfLeftSide(Math.toRadians(-90));
				angles[1] = 0.0f;    
				angles[2] = (float) robotSide.negateIfLeftSide(Math.toRadians(0));
			}
			else if (hasHookHands())
			{
				centerOfHandToWristTranslation = new Vector3f(0.1f, (float) robotSide.negateIfLeftSide(0f), 0f);
				angles[0] = (float) robotSide.negateIfLeftSide(Math.toRadians(-90));
				angles[1] = 0.0f;   
				angles[2] = (float) robotSide.negateIfLeftSide(Math.toRadians(0));
			}
			
			else if (hasSandiaHands())
			{
				centerOfHandToWristTranslation = new Vector3f(0.10f, (float) robotSide.negateIfLeftSide(0.018f), 0.05f);
				angles[0] = (float) robotSide.negateIfLeftSide(Math.toRadians(90));
				angles[1] = 0.0f;   
				angles[2] = (float) robotSide.negateIfLeftSide(Math.toRadians(40));
			}
			Quaternion centerOfHandToWristRotation = new Quaternion(angles);
			offsetHandFromWrist.set(robotSide, new Transform(centerOfHandToWristTranslation, centerOfHandToWristRotation));
		}
	}
}
