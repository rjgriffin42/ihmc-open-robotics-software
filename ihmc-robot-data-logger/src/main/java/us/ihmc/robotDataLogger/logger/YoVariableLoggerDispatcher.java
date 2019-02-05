package us.ihmc.robotDataLogger.logger;

import java.io.IOException;
import java.util.HashSet;

import com.martiansoftware.jsap.JSAPException;

import us.ihmc.log.LogTools;
import us.ihmc.robotDataLogger.Announcement;
import us.ihmc.robotDataLogger.StaticHostListLoader;
import us.ihmc.robotDataLogger.interfaces.DataServerDiscoveryListener;
import us.ihmc.robotDataLogger.websocket.client.discovery.DataServerDiscoveryClient;
import us.ihmc.robotDataLogger.websocket.client.discovery.HTTPDataServerConnection;

public class YoVariableLoggerDispatcher implements DataServerDiscoveryListener
{
   private final DataServerDiscoveryClient discoveryClient;

   
   private final Object lock = new Object();
   
   /**
    * List of sessions for which we started a logger.
    * 
    * This is to avoid double logging should there be multiple known IPs for a single host.
    */
   private final HashSet<HashAnnouncement> activeLogSessions = new HashSet<HashAnnouncement>(); 
   
   private final YoVariableLoggerOptions options;

   /**
    * Create a new YovariableLoggerDispatcher.
    * 
    * For every log that comes online, a YoVariableLogger is created.
    * 
    * 
    * @param options
    * @throws IOException
    */
   public YoVariableLoggerDispatcher(YoVariableLoggerOptions options) throws IOException
   {
      this.options = options;
      LogTools.info("Starting YoVariableLoggerDispatcher");
      
      boolean enableAutoDiscovery = !options.isDisableAutoDiscovery();
      discoveryClient = new DataServerDiscoveryClient(this, enableAutoDiscovery);
      discoveryClient.addHosts(StaticHostListLoader.load());
      
      
      LogTools.info("Client started, waiting for data server sessions");
   }

   public static void main(String[] args) throws JSAPException, IOException
   {
      YoVariableLoggerOptions options = YoVariableLoggerOptions.parse(args);
      new YoVariableLoggerDispatcher(options);
   }


   @Override
   public void connected(HTTPDataServerConnection connection)
   {
      synchronized(lock)
      {
         Announcement announcement = connection.getAnnouncement();
         HashAnnouncement hashAnnouncement = new HashAnnouncement(announcement);
         LogTools.info("New control session came online on " + connection.getTarget() + ". Identifier: " + announcement.getIdentifierAsString());
         if(activeLogSessions.contains(hashAnnouncement))
         {
            LogTools.warn("A logging sessions for " + announcement.getNameAsString() + " is already started.");
         }
         else
         {
            if(announcement.getLog())
            {
               try
               {
                  new YoVariableLogger(connection, options);
                  activeLogSessions.add(hashAnnouncement);
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
               LogTools.info("Logging session started for " + announcement.getNameAsString());
            }
         }
         
         
         

      }
   }

   @Override
   public void disconnected(HTTPDataServerConnection connection)
   {
   }

   
   /**
    * Simple hashcode calculator for announcements to allow it in a HashSet
    *
    * @author Jesper Smith
    *
    */
   private static class HashAnnouncement
   {
      private final Announcement announcement;
      
      public HashAnnouncement(Announcement announcement)
      {
         this.announcement = announcement;
      }
      
      @Override
      public boolean equals(Object other)
      {
         if(other instanceof HashAnnouncement)
         {
            return announcement.equals(((HashAnnouncement) other).announcement);
         }
         else
         {
            return false;
         }
      }
      
      
      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = prime * result + announcement.getIdentifierAsString().hashCode();
         result = prime * result + (announcement.getLog() ? 1231 : 1237);
         result = prime * result + announcement.getNameAsString().hashCode();
         result = prime * result + announcement.getReconnectKeyAsString().hashCode();
         return result;
      }
   }
}
