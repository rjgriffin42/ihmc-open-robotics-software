package us.ihmc.robotDataLogger.websocket.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;

class WebSocketLogClientHandler extends SimpleChannelInboundHandler<Object> {

      private final WebSocketClientHandshaker handshaker;
      private ChannelPromise handshakeFuture;

      public WebSocketLogClientHandler(WebSocketClientHandshaker handshaker) {
          this.handshaker = handshaker;
      }

      public ChannelFuture handshakeFuture() {
          return handshakeFuture;
      }

      @Override
      public void handlerAdded(ChannelHandlerContext ctx) {
          handshakeFuture = ctx.newPromise();
      }

      @Override
      public void channelActive(ChannelHandlerContext ctx) {
          handshaker.handshake(ctx.channel());
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) {
          System.out.println("WebSocket Client disconnected!");
      }

      @Override
      public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
          Channel ch = ctx.channel();
          if (!handshaker.isHandshakeComplete()) {
              try {
                  handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                  System.out.println("WebSocket Client connected!");
                  handshakeFuture.setSuccess();
              } catch (WebSocketHandshakeException e) {
                  System.out.println("WebSocket Client failed to connect");
                  handshakeFuture.setFailure(e);
              }
              return;
          }

          if (msg instanceof FullHttpResponse) {
              FullHttpResponse response = (FullHttpResponse) msg;
              throw new IllegalStateException(
                      "Unexpected FullHttpResponse (getStatus=" + response.status() +
                              ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
          }

          WebSocketFrame frame = (WebSocketFrame) msg;
          if (frame instanceof TextWebSocketFrame) {
              // Discard
          } 
          else if (frame instanceof BinaryWebSocketFrame)
         {
             System.out.println("Received binary package of " + frame.content());
         }
          else if (frame instanceof PongWebSocketFrame) {
              System.out.println("WebSocket Client received pong");
          } else if (frame instanceof CloseWebSocketFrame) {
              System.out.println("WebSocket Client received closing");
              ch.close();
          }
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
          cause.printStackTrace();
          if (!handshakeFuture.isDone()) {
              handshakeFuture.setFailure(cause);
          }
          ctx.close();
      }
  }