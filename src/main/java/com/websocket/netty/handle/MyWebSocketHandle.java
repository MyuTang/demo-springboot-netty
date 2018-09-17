package com.websocket.netty.handle;

import com.websocket.netty.config.NettyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import sun.awt.windows.WEmbeddedFrame;
import sun.nio.ch.Net;

import java.util.Date;

/**
 * 接收、处理、响应客户端websocket请求的核心业务处理类
 */
@Slf4j
public class MyWebSocketHandle extends SimpleChannelInboundHandler<Object> {


    private WebSocketServerHandshaker handshaker;
    private static final String WEB_SOCKET_URL="ws://localhost:8888/websocket";

    /**
     * 又叫messageReceived方法
     * 服务端处理客户端websocket请求的核心方法
     * @param channelHandlerContext
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        if (msg instanceof FullHttpMessage){  //处理客户端向服务端发起的http握手请求
            handleHttpRequest(channelHandlerContext, (FullHttpRequest) msg); //强转成FullHttpRequest
        } else if (msg instanceof WebSocketFrame){ //处理websocket连接业务
            handleWebsocketFrame(channelHandlerContext, (WebSocketFrame) msg);
        }
    }

    /**
     * 处理客户端向服务端发起http握手请求的业务
     * @param ctx
     * @param request
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request){
        //如果不是websocket的http握手请求
        if(!request.decoderResult().isSuccess()||!("websocket").equals(request.headers().get("Upgrade"))){
            sendHttpResponse(ctx,request,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        //如果是websocket的http握手请求
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL,null,false);
        handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null){ //如果handshaker为空
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {  //如果handshaker不为空
            handshaker.handshake(ctx.channel(),request);
        }
    }

    /**
     * 服务端向客户端响应消息
     * @param ctx
     * @param request
     * @param response
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse response){
        if(response.status().code() != 200){
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
        }
        //服务端向客户端发送数据
        ChannelFuture channelFuture = ctx.channel().writeAndFlush(response);
        if(response.status().code() != 200){
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }


    /**
     * 处理客户端与服务端之间的websocket业务
     * @param ctx
     * @param webSocketFrame
     */
    private void handleWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame){
        if(webSocketFrame instanceof CloseWebSocketFrame){ //是否是关闭websocket的指令
            handshaker.close(ctx.channel(),((CloseWebSocketFrame) webSocketFrame).retain());
        }
        if (webSocketFrame instanceof PingWebSocketFrame){ //是否为ping消息
            ctx.channel().write(new PongWebSocketFrame(webSocketFrame.content().retain()));
            return;
        }

        //判断是否是二进制消息，如果是二进制消息，抛出异常
        if (webSocketFrame instanceof TextWebSocketFrame){
            log.error("目前我们不支持二进制消息");
            throw new RuntimeException(this.getClass().getName() + "不支持二进制消息");
        }

        //返回应答消息
        //获取客户端向服务端发送的消息
        String request = ((TextWebSocketFrame)webSocketFrame).text();
        log.info("服务端收到客户端的消息：{}",request);

        TextWebSocketFrame textWebSocketFrame
                = new TextWebSocketFrame(new Date().toString()
                + ctx.channel().id()
                + "===>"
                + request);

        //服务端向每个连接上来的客户端群发信息
        NettyConfig.group.writeAndFlush(textWebSocketFrame);
    }









    /**
     * 客户端与服务端创建连接的时候使用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
        log.info("客户端与服务端连接开启...");
    }

    /**
     * 客户端与服务端断开连接的时候使用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
       NettyConfig.group.remove(ctx.channel());
       log.info("客服端与服务端连接关闭");
    }

    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 工程出现异常的时候调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
