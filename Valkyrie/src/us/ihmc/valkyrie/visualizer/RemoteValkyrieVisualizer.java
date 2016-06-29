package us.ihmc.valkyrie.visualizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import us.ihmc.communication.configuration.NetworkParameterKeys;
import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.robotDataCommunication.YoVariableClient;
import us.ihmc.robotDataCommunication.visualizer.SCSVisualizer;
import us.ihmc.robotDataCommunication.visualizer.SCSVisualizerStateListener;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.controllers.ValkyrieSliderBoard;
import us.ihmc.valkyrie.controllers.ValkyrieSliderBoard.ValkyrieSliderBoardType;
import us.ihmc.valkyrieRosControl.ValkyrieRosControlLowLevelController;

public class RemoteValkyrieVisualizer implements SCSVisualizerStateListener
{
   public static final int BUFFER_SIZE = 16384;
   
   private final ValkyrieSliderBoardType valkyrieSliderBoardType;
   private final ValkyrieRobotModel valkyrieRobotModel;

   public RemoteValkyrieVisualizer(ValkyrieSliderBoardType valkyrieSliderBoardType)
   {
      this.valkyrieSliderBoardType = valkyrieSliderBoardType;

      String host = NetworkParameters.getHost(NetworkParameterKeys.logger);
      System.out.println("Connecting to host " + host);
      valkyrieRobotModel = new ValkyrieRobotModel(DRCRobotModel.RobotTarget.REAL_ROBOT, false);

      SCSVisualizer scsVisualizer = new SCSVisualizer(BUFFER_SIZE);
      scsVisualizer.addSCSVisualizerStateListener(this);
      scsVisualizer.setShowOverheadView(false);

      YoVariableClient client = new YoVariableClient(scsVisualizer, "remote", new ValkyrieIpToNiceNameRemapper());
      client.start();
   }
   

   @Override
   public void starting(final SimulationConstructionSet scs, Robot robot, YoVariableRegistry registry)
   {
      // TODO The sliderboard throws an NPE when scrubbing, at least in Sim. If this is okay on the real robot then feel free to uncomment. -- Doug
      new ValkyrieSliderBoard(scs, registry, valkyrieRobotModel, valkyrieSliderBoardType);

      final BooleanYoVariable requestCalibration = (BooleanYoVariable) scs.getVariable(ValkyrieRosControlLowLevelController.class.getSimpleName(), "requestCalibration");

      JButton requestCalibrationButton = new JButton("Request calibration");
      requestCalibrationButton.addActionListener(new ActionListener()
      {
         @Override
         public void actionPerformed(ActionEvent e)
         {
            requestCalibration.set(true);
         }
      });
      scs.addButton(requestCalibrationButton);
   }
   
   public static void main(String[] args)
   {
      new RemoteValkyrieWalkingVisualizer();
   }
}
