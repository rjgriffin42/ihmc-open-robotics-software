package us.ihmc.quadrupedRobotics.controller;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.math.filters.AlphaFilteredYoVariable;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.sensorProcessing.model.RobotMotionStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuadrupedSliderBoardController extends QuadrupedController
{
   private final YoVariableRegistry sliderBoardRegistry = new YoVariableRegistry(getClass().getSimpleName());

   private final Map<String, OneDoFJoint> jointMap;
   private final ArrayList<String> jointMapKeySet = new ArrayList<>();

   private final Map<String, AlphaFilteredYoVariable> alphaFilteredQDesiredMap = new HashMap<>();

   public QuadrupedSliderBoardController(SDFFullRobotModel fullRobotModel, YoVariableRegistry registry)
   {
      super(QuadrupedControllerState.SLIDER_BOARD);
      registry.addChild(sliderBoardRegistry);

      jointMap = fullRobotModel.getOneDoFJointsAsMap();
      jointMapKeySet.addAll(jointMap.keySet());

      for(String key : jointMap.keySet())
      {
         DoubleYoVariable qDesired = new DoubleYoVariable(key + "_q_d", sliderBoardRegistry);
         DoubleYoVariable qDesiredAlpha = new DoubleYoVariable(key + "_q_d_alpha", sliderBoardRegistry);
         AlphaFilteredYoVariable alphaFilteredQDesired = new AlphaFilteredYoVariable(key + "_alpha_filtered_q_d", sliderBoardRegistry, qDesiredAlpha, qDesired);
         alphaFilteredQDesiredMap.put(key, alphaFilteredQDesired);
      }
   }

   @Override public RobotMotionStatus getMotionStatus()
   {
      return null;
   }

   @Override public void doAction()
   {
      for (int i = 0; i < jointMapKeySet.size(); i++)
      {
         AlphaFilteredYoVariable alphaFilteredYoVariable = alphaFilteredQDesiredMap.get(jointMapKeySet.get(i));
         alphaFilteredYoVariable.update();
         jointMap.get(jointMapKeySet.get(i)).setQ(alphaFilteredYoVariable.getDoubleValue());
      }
   }

   @Override public void doTransitionIntoAction()
   {

   }

   @Override public void doTransitionOutOfAction()
   {

   }
}
