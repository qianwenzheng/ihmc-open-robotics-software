package us.ihmc.commonWalkingControlModules.controllerAPI.input;

import controller_msgs.msg.dds.InvalidPacketNotificationPacket;
import controller_msgs.msg.dds.MessageCollection;
import controller_msgs.msg.dds.MessageCollectionNotification;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.MessageCollector.MessageIDExtractor;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.controllerAPI.CommandInputManager;
import us.ihmc.communication.controllerAPI.MessageUnpackingTools.MessageUnpacker;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.Packet;
import us.ihmc.concurrent.Builder;
import us.ihmc.concurrent.ConcurrentRingBuffer;
import us.ihmc.ros2.RealtimeRos2Node;
import us.ihmc.tools.thread.CloseableAndDisposable;
import us.ihmc.util.PeriodicThreadScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The ControllerNetworkSubscriber is meant to used as a generic interface between a network packet
 * communicator and the controller API. It automatically creates all the {@link PacketConsumer} for
 * all the messages supported by the {@link CommandInputManager}. The status messages are send to
 * the network communicator on a separate thread to avoid any delay in the controller thread.
 *
 * @author Sylvain
 */
public class ControllerNetworkSubscriber implements Runnable, CloseableAndDisposable
{
   private static final boolean DEBUG = false;

   private final int buffersCapacity = 16;
   /** The input API to which the received messages should be submitted. */
   private final CommandInputManager controllerCommandInputManager;
   /** The output API that provides the status messages to send to the packet communicator. */
   private final StatusMessageOutputManager controllerStatusOutputManager;
   /** Communicator from which commands are received and status messages can send to. */
   private final PacketCommunicator packetCommunicator;
   /** Used to schedule status message sending. */
   private final PeriodicThreadScheduler scheduler;
   /** Used to filter messages coming in. */
   private final AtomicReference<MessageFilter> messageFilter;
   /** Used to filter messages coming in and report an error. */
   private final AtomicReference<MessageValidator> messageValidator;
   /** Used to synchronize the execution of a message collection. */
   private MessageCollector messageCollector = MessageCollector.createDummyCollector();

   /** All the possible status message that can be sent to the communicator. */
   private final List<Class<? extends Packet<?>>> listOfSupportedStatusMessages;

   /** All the possible messages that can be sent to the communicator. */
   private final List<Class<? extends Packet<?>>> listOfSupportedControlMessages;

   /**
    * Local buffers for each message to ensure proper copying from the controller thread to the
    * communication thread.
    */
   private final Map<Class<? extends Packet<?>>, ConcurrentRingBuffer<? extends Packet<?>>> statusMessageClassToBufferMap = new HashMap<>();

   public ControllerNetworkSubscriber(CommandInputManager controllerCommandInputManager, StatusMessageOutputManager controllerStatusOutputManager,
                                      PeriodicThreadScheduler scheduler, PacketCommunicator packetCommunicator)
   {
      this.controllerCommandInputManager = controllerCommandInputManager;
      this.controllerStatusOutputManager = controllerStatusOutputManager;
      this.scheduler = scheduler;
      this.packetCommunicator = packetCommunicator;
      listOfSupportedStatusMessages = controllerStatusOutputManager.getListOfSupportedMessages();
      listOfSupportedControlMessages = controllerCommandInputManager.getListOfSupportedMessages();
      messageFilter = new AtomicReference<>(message -> true);
      messageValidator = new AtomicReference<>(message -> null);

      if (packetCommunicator == null)
         PrintTools.error(this, "No packet communicator, " + getClass().getSimpleName() + " cannot be created.");

      listOfSupportedStatusMessages.add(InvalidPacketNotificationPacket.class);

      createAllSubscribersForSupportedMessages();
      createGlobalStatusMessageListener();
      createAllStatusMessageBuffers();

      if (scheduler != null)
         scheduler.schedule(this, 1, TimeUnit.MILLISECONDS);
   }

   public <T extends Packet<T>> void registerSubcriberWithMessageUnpacker(Class<T> multipleMessageHolderClass, int expectedMessageSize,
                                                                          MessageUnpacker<T> messageUnpacker)
   {
      final List<Packet<?>> unpackedMessages = new ArrayList<>(expectedMessageSize);
      PacketConsumer<T> packetConsumer = multipleMessageHolder -> {
         unpackMultiMessage(multipleMessageHolderClass, messageUnpacker, unpackedMessages, multipleMessageHolder);
      };
      packetCommunicator.attachListener(multipleMessageHolderClass, packetConsumer);
   }

   private <T extends Packet<T>> void unpackMultiMessage(Class<T> multipleMessageHolderClass, MessageUnpacker<T> messageUnpacker,
                                                         List<Packet<?>> unpackedMessages, T multipleMessageHolder)
   {
      if (DEBUG)
         PrintTools
               .debug(ControllerNetworkSubscriber.this, "Received message: " + multipleMessageHolder.getClass().getSimpleName() + ", " + multipleMessageHolder);

      String errorMessage = messageValidator.get().validate(multipleMessageHolder);

      if (errorMessage != null)
      {
         reportInvalidMessage(multipleMessageHolderClass, errorMessage);
         return;
      }

      if (testMessageWithMessageFilter(multipleMessageHolder))
      {
         messageUnpacker.unpackMessage(multipleMessageHolder, unpackedMessages);

         for (int i = 0; i < unpackedMessages.size(); i++)
         {
            receivedMessage(unpackedMessages.get(i));
         }
         unpackedMessages.clear();
      }
   }

   public void addMessageCollector(MessageIDExtractor messageIDExtractor)
   {
      messageCollector = new MessageCollector(messageIDExtractor, listOfSupportedControlMessages);
      createStatusMessageBuffer(MessageCollectionNotification.class);
      listOfSupportedStatusMessages.add(MessageCollectionNotification.class);
      packetCommunicator.attachListener(MessageCollection.class, message -> {
         MessageCollectionNotification notification = messageCollector.startCollecting(message);
         copyAndCommitStatusMessage(notification);
      });
   }

   public void addMessageFilter(MessageFilter newFilter)
   {
      messageFilter.set(newFilter);
   }

   public void removeMessageFilter()
   {
      messageFilter.set(null);
   }

   public void addMessageValidator(MessageValidator newValidator)
   {
      messageValidator.set(newValidator);
   }

   public void removeMessageValidator()
   {
      messageValidator.set(null);
   }

   @SuppressWarnings("unchecked")
   private <T extends Packet<T>> void createAllStatusMessageBuffers()
   {
      for (int i = 0; i < listOfSupportedStatusMessages.size(); i++)
      {
         createStatusMessageBuffer((Class<T>) listOfSupportedStatusMessages.get(i));
      }
   }

   private <T extends Packet<T>> void createStatusMessageBuffer(Class<T> statusMessageClass)
   {
      Builder<T> builder = CommandInputManager.createBuilderWithEmptyConstructor(statusMessageClass);
      ConcurrentRingBuffer<T> newBuffer = new ConcurrentRingBuffer<>(builder, buffersCapacity);
      statusMessageClassToBufferMap.put(statusMessageClass, newBuffer);
   }

   @SuppressWarnings("unchecked")
   private <T extends Packet<T>> void createAllSubscribersForSupportedMessages()
   {
      for (int i = 0; i < listOfSupportedControlMessages.size(); i++)
      {
         Class<T> messageClass = (Class<T>) listOfSupportedControlMessages.get(i);
         packetCommunicator.attachListener(messageClass, this::receivedMessage);
      }
   }

   @SuppressWarnings("unchecked")
   private <T extends Packet<T>> void receivedMessage(Packet<?> message)
   {
      if (DEBUG)
         PrintTools.debug(ControllerNetworkSubscriber.this, "Received message: " + message.getClass().getSimpleName() + ", " + message);

      if (messageCollector.isCollecting() && messageCollector.interceptMessage(message))
      {
         if (DEBUG)
            PrintTools.debug(ControllerNetworkSubscriber.this, "Collecting message: " + message.getClass().getSimpleName() + ", " + message);

         if (!messageCollector.isCollecting())
         {
            List<Packet<?>> collectedMessages = messageCollector.getCollectedMessages();
            for (int i = 0; i < collectedMessages.size(); i++)
            {
               receivedMessage(collectedMessages.get(i));
            }
            messageCollector.reset();
         }

         return;
      }

      String errorMessage = messageValidator.get().validate(message);

      if (errorMessage != null)
      {
         reportInvalidMessage((Class<? extends Packet<?>>) message.getClass(), errorMessage);
         return;
      }

      if (testMessageWithMessageFilter(message))
         controllerCommandInputManager.submitMessage((T) message);
   }

   private boolean testMessageWithMessageFilter(Packet<?> messageToTest)
   {
      if (!messageFilter.get().isMessageValid(messageToTest))
      {
         if (DEBUG)
            PrintTools.error(ControllerNetworkSubscriber.this, "Packet failed to validate filter! Filter class: "
                  + messageFilter.get().getClass().getSimpleName() + ", rejected message: " + messageToTest.getClass().getSimpleName());
         return false;
      }
      return true;
   }

   private void reportInvalidMessage(Class<? extends Packet<?>> messageClass, String errorMessage)
   {
      ConcurrentRingBuffer<?> buffer = statusMessageClassToBufferMap.get(InvalidPacketNotificationPacket.class);

      InvalidPacketNotificationPacket next = (InvalidPacketNotificationPacket) buffer.next();

      if (next != null)
      {
         next.setPacketClassSimpleName(messageClass.getSimpleName());
         next.setErrorMessage(errorMessage);
         buffer.commit();
      }

      PrintTools.error(ControllerNetworkSubscriber.this, "Packet failed to validate: " + messageClass.getSimpleName());
      PrintTools.error(ControllerNetworkSubscriber.this, errorMessage);
   }

   private void createGlobalStatusMessageListener()
   {
      controllerStatusOutputManager.attachGlobalStatusMessageListener(statusMessage -> copyAndCommitStatusMessage(statusMessage));
   }

   @SuppressWarnings("unchecked")
   private <T extends Packet<T>> void copyAndCommitStatusMessage(Packet<?> statusMessage)
   {
      ConcurrentRingBuffer<T> buffer = (ConcurrentRingBuffer<T>) statusMessageClassToBufferMap.get(statusMessage.getClass());
      T next = buffer.next();
      if (next != null)
      {
         next.set((T) statusMessage);
         buffer.commit();
      }
   }

   @Override
   public void run()
   {
      for (int i = 0; i < listOfSupportedStatusMessages.size(); i++)
      {
         ConcurrentRingBuffer<? extends Packet<?>> buffer = statusMessageClassToBufferMap.get(listOfSupportedStatusMessages.get(i));
         if (buffer.poll())
         {
            Packet<?> statusMessage;
            while ((statusMessage = buffer.read()) != null)
            {
               packetCommunicator.send(statusMessage);
            }
            buffer.flush();
         }
      }
   }

   @Override
   public void closeAndDispose()
   {
      if (scheduler != null)
         scheduler.shutdown();
   }

   public static interface MessageFilter
   {
      public boolean isMessageValid(Packet<?> message);
   }

   public static interface MessageValidator
   {
      String validate(Packet<?> message);
   }
}
