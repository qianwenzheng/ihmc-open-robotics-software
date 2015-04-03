package us.ihmc.multicastLogDataProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import us.ihmc.multicastLogDataProtocol.control.LogHandshake;
import us.ihmc.robotDataCommunication.LogDataHeader;

public class StreamingDataTCPClient extends Thread
{
   public static final int TIMEOUT = 20000;
   private volatile boolean running = false;

   private final InetSocketAddress address;
   private final LogPacketHandler updateHandler;

   public StreamingDataTCPClient(InetAddress dataIP, int port, LogPacketHandler updateHandler)
   {
      this.address = new InetSocketAddress(dataIP, port);
      this.updateHandler = updateHandler;
   }

   public static int selectAndRead(SelectionKey key, ByteBuffer destination, int timeout) throws IOException
   {
      while (key.selector().select(timeout) > 0)
      {
         key.selector().selectedKeys().remove(key);
         if (key.isReadable())
         {
            return ((SocketChannel) key.channel()).read(destination);
         }
         else
         {
            System.err.println("Should not get here");
         }
      }
      throw new IOException("Connection timed out");
   }

   @Override
   public void run()
   {
      running = true;

      SocketChannel connection;
      SelectionKey key;
      try
      {
         connection = SocketChannel.open();
         connection.connect(address);

         //         connection.socket().setReceiveBufferSize(1000000);
         //         connection.socket().setKeepAlive(true);
         //         connection.socket().setTcpNoDelay(true);

         connection.configureBlocking(false);
         Selector selector = Selector.open();
         key = connection.register(selector, SelectionKey.OP_READ);
         sendRequest(connection, LogHandshake.STREAM_REQUEST);

      }
      catch (IOException e)
      {
         e.printStackTrace();
         updateHandler.timeout();
         return;
      }

      ByteBuffer headerBuffer = ByteBuffer.allocateDirect(LogDataHeader.length());

      DATALOOP: while (running)
      {
         try
         {
            headerBuffer.clear();
            while (headerBuffer.hasRemaining())
            {
               int read = selectAndRead(key, headerBuffer, TIMEOUT);
               if (read == -1)
               {
                  break DATALOOP;
               }
            }
            headerBuffer.clear();
            LogDataHeader header = new LogDataHeader();
            if (!header.readBuffer(headerBuffer))
            {
               System.err.println("Expected header, got data. Scanning till new header found.");
               continue DATALOOP; // Cannot read buffer, continue with data loop hopefully latching on to the data stream again
            }
            updateHandler.timestampReceived(header.getTimestamp());

            ByteBuffer dataBuffer = ByteBuffer.allocate(header.getDataSize());

            while (dataBuffer.hasRemaining())
            {
               int read = selectAndRead(key, dataBuffer, TIMEOUT);
               if (read == -1)
               {
                  break DATALOOP;
               }
            }

            dataBuffer.flip();
            updateHandler.newDataAvailable(header, dataBuffer);

         }
         catch (ClosedByInterruptException e)
         {
            break DATALOOP;
         }
         catch (SocketTimeoutException e)
         {
            break DATALOOP;
         }
         catch (IOException e)
         {
            e.printStackTrace();
            break DATALOOP;
         }

      }
      try
      {
         key.cancel();
         connection.close();
      }
      catch (IOException e)
      {
      }

      updateHandler.timeout();
      running = false;
   }

   public LogHandshake getHandshake() throws IOException
   {
      SocketChannel connection = SocketChannel.open();
      connection.connect(address);
      
      sendRequest(connection, LogHandshake.HANDSHAKE_REQUEST);
      connection.configureBlocking(false);
      
      Selector selector = Selector.open();
      SelectionKey key = connection.register(selector, SelectionKey.OP_READ);

      LogHandshake handshake = LogHandshake.read(key);
      key.cancel();
      connection.close();

      return handshake;

   }

   private void sendRequest(SocketChannel connection, byte request) throws IOException
   {
      ByteBuffer command = ByteBuffer.allocateDirect(1);
      command.put(request);
      command.flip();
      connection.write(command);
   }

   public boolean isRunning()
   {
      return running;
   }

   public void close()
   {
      running = false;
      interrupt();
   }
}
