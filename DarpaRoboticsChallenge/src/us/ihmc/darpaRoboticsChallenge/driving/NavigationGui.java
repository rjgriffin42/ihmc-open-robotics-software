package us.ihmc.darpaRoboticsChallenge.driving;

import javax.swing.*;

/**
 * @author Peter Abeles
 */
public abstract class NavigationGui<T> extends JPanel {

   public abstract void update( T alg );
}
