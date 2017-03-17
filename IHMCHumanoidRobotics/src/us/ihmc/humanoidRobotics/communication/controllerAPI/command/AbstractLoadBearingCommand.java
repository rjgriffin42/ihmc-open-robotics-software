package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.communication.packets.AbstractLoadBearingMessage;

public abstract class AbstractLoadBearingCommand <T extends AbstractLoadBearingCommand<T, M>, M extends AbstractLoadBearingMessage<M>> implements Command<T, M>
{
   /** If set to true this will load the contact point. Otherwise the rigid body will stop bearing load. */
   private boolean load = false;

   /** Sets the coefficient of friction that the controller will use for the contact point. */
   private double coefficientOfFriction = 0.0;

   /** Sets the position of the contact point in the frame of the end effector body. */
   private Point3D contactPointInBodyFrame = new Point3D();

   /** Sets the contact normal used by the controller to load the contact point. */
   private Vector3D contactNormalInWorldFrame = new Vector3D();

   public boolean getLoad()
   {
      return load;
   }

   public double getCoefficientOfFriction()
   {
      return coefficientOfFriction;
   }

   public Point3D getContactPointInBodyFrame()
   {
      return contactPointInBodyFrame;
   }

   public Vector3D getContactNormalInWorldFrame()
   {
      return contactNormalInWorldFrame;
   }

   @Override
   public void set(T other)
   {
      load = other.getLoad();
      coefficientOfFriction = other.getCoefficientOfFriction();
      contactPointInBodyFrame.set(other.getContactPointInBodyFrame());
      contactNormalInWorldFrame.set(other.getContactNormalInWorldFrame());
   }

   @Override
   public void set(M message)
   {
      load = message.getLoad();
      coefficientOfFriction = message.getCoefficientOfFriction();
      contactPointInBodyFrame.set(message.getContactPointInBodyFrame());
      contactNormalInWorldFrame.set(message.getContactNormalInWorldFrame());
   }

   @Override
   public void clear()
   {
      load = false;
      coefficientOfFriction = 0.0;
      contactPointInBodyFrame.setToZero();
      contactNormalInWorldFrame.setToZero();
   }

   @Override
   public boolean isCommandValid()
   {
      if (coefficientOfFriction <= 0.0)
         return false;
      if (contactPointInBodyFrame.containsNaN())
         return false;
      if (contactNormalInWorldFrame.containsNaN())
         return false;
      return true;
   }

}
