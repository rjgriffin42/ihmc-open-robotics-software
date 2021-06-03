package us.ihmc.behaviors.door;

import us.ihmc.messager.MessagerAPIFactory;

public class DoorBehaviorAPI
{
   private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();
   private static final MessagerAPIFactory.Category RootCategory = apiFactory.createRootCategory("DoorBehavior");
   private static final MessagerAPIFactory.CategoryTheme DoorTheme = apiFactory.createCategoryTheme("Door");

   public static final MessagerAPIFactory.Topic<Boolean> DoorConfirmed = topic("DoorConfirmed");

   private static final <T> MessagerAPIFactory.Topic<T> topic(String name)
   {
      return RootCategory.child(DoorTheme).topic(apiFactory.createTypedTopicTheme(name));
   }

   public static final MessagerAPIFactory.MessagerAPI create()
   {
      return apiFactory.getAPIAndCloseFactory();
   }
}
