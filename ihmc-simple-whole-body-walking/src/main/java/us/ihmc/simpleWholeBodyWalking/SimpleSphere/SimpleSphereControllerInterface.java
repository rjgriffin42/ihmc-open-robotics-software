package us.ihmc.simpleWholeBodyWalking.SimpleSphere;

import us.ihmc.simulationconstructionset.util.RobotController;

import java.util.List;

public interface SimpleSphereControllerInterface extends RobotController
{
   void solveForTrajectory();
}
