package us.ihmc.robotDataLogger.websocket.client.discovery;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import us.ihmc.idl.serializers.extra.JSONSerializer;
import us.ihmc.robotDataLogger.Announcement;
import us.ihmc.robotDataLogger.AnnouncementPubSubType;
import us.ihmc.robotDataLogger.websocket.LogHTTPPaths;

public class HTTPDataServerConnection extends SimpleChannelInboundHandler<HttpObject>
{
   private final EventLoopGroup group = new NioEventLoopGroup();
   private final HTTPDataServerDescription target;
   private final HTTPDataServerConnectionListener listener;
   private final Announcement announcement = new Announcement();

   private Channel channel;

   private CompletableFuture<ByteBuf> requestFuture;
   private ByteBuf requestedBuffer;

   public interface HTTPDataServerConnectionListener
   {
      public void connected(HTTPDataServerConnection connection);

      public void disconnected(HTTPDataServerConnection connection);

      public void connectionRefused(HTTPDataServerDescription target);
   }

   public HTTPDataServerConnection(HTTPDataServerDescription target, HTTPDataServerConnectionListener listener)
   {
      this.target = target;
      this.listener = listener;

      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioSocketChannel.class).handler(new HttpSnoopClientInitializer());
      b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);

      ChannelFuture connectFuture = b.connect(target.getHost(), target.getPort());
      connectFuture.addListener((f) -> {
         if (f.isSuccess())
         {
            connected(((ChannelFuture) f.sync()).channel());
         }
         else
         {
            listener.connectionRefused(target);
            group.shutdownGracefully();
         }
      });
   }

   private void connected(Channel channel)
   {
      this.channel = channel;
      requestResource(LogHTTPPaths.announcement, (buf) -> receivedAnnouncement(buf));

   }

   private void receivedAnnouncement(ByteBuf buf)
   {
      JSONSerializer<Announcement> serializer = new JSONSerializer<Announcement>(new AnnouncementPubSubType());
      try
      {
         announcement.set(serializer.deserialize(buf.toString(CharsetUtil.UTF_8)));
         listener.connected(this);
      }
      catch (IOException e)
      {
         e.printStackTrace();
         channel.close();
      }
   }

   public Future<ByteBuf> requestResource(String path)
   {
      return requestResource(path, null);
   }

   public Future<ByteBuf> requestResource(String path, Consumer<ByteBuf> action)
   {
      if (requestFuture != null && !requestFuture.isDone())
      {
         throw new RuntimeException("Previous request still pending");
      }

      requestFuture = new CompletableFuture<ByteBuf>();

      if (action != null)
      {
         requestFuture.thenAccept(action);
      }

      if (channel != null)
      {
         // Prepare the HTTP request.
         HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
         request.headers().set(HttpHeaderNames.HOST, target);
         request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
         request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
         // Send the HTTP request.
         try
         {
            channel.writeAndFlush(request).syncUninterruptibly();
         }
         catch (Exception e)
         {
            requestFuture.completeExceptionally(e);
            channel.close();
         }
      }
      else
      {
         requestFuture.completeExceptionally(new IOException("Channel not open"));
      }

      return requestFuture;
   }

   public void close()
   {
      if (channel != null)
      {
         channel.close();
      }
   }

   public HTTPDataServerDescription getTarget()
   {
      return target;
   }

   public Announcement getAnnouncement()
   {
      return announcement;
   }

   private class HttpSnoopClientInitializer extends ChannelInitializer<SocketChannel>
   {

      public HttpSnoopClientInitializer()
      {
      }

      @Override
      public void initChannel(SocketChannel ch)
      {
         ChannelPipeline p = ch.pipeline();

         p.addLast(new HttpClientCodec());
         p.addLast(new HttpContentDecompressor());
         p.addLast(HTTPDataServerConnection.this);
      }
   }

   @Override
   protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception
   {
      if (msg instanceof HttpResponse || msg instanceof HttpContent)
      {
         if (requestFuture == null || requestFuture.isDone())
         {
            throw new IOException("HTTP response received without matching request");
         }

         if (msg instanceof HttpResponse)
         {
            HttpResponse response = (HttpResponse) msg;

            if (response.status() != HttpResponseStatus.OK)
            {
               requestFuture.completeExceptionally(new IOException("Invalid response received " + response.status()));
               ctx.close();
               return;
            }

            int contentLength = response.headers().getInt("content-length", 0);
            if (contentLength <= 0)
            {
               requestFuture.completeExceptionally(new IOException("No content-length set."));
               ctx.close();
               return;
            }

            requestedBuffer = Unpooled.buffer(contentLength);
         }
         if (msg instanceof HttpContent)
         {
            HttpContent content = (HttpContent) msg;

            if (requestedBuffer.isWritable(content.content().readableBytes()))
            {
               requestedBuffer.writeBytes(((HttpContent) msg).content());
            }
            else
            {
               requestFuture.completeExceptionally(new IOException("Content-length exceeds allocated space"));
               ctx.close();
               return;
            }

            if (content instanceof LastHttpContent)
            {
               requestFuture.complete(requestedBuffer);
               requestedBuffer = null;
            }
         }
      }
   }

   @Override
   public void channelInactive(ChannelHandlerContext ctx) throws Exception
   {
      listener.disconnected(this);
      group.shutdownGracefully();

   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
   {
      cause.printStackTrace();
      ctx.close();
   }

   public static void main(String[] args)
   {
      new HTTPDataServerConnection(new HTTPDataServerDescription("127.0.0.1", 8008, false), new HTTPDataServerConnectionListener()
      {

         @Override
         public void disconnected(HTTPDataServerConnection connection)
         {
            System.out.println("Disconnected");
         }

         @Override
         public void connectionRefused(HTTPDataServerDescription target)
         {
            System.out.println("Connection refused");
         }

         @Override
         public void connected(HTTPDataServerConnection connection)
         {
            System.out.println("Connected");
         }
      });

   }

}
