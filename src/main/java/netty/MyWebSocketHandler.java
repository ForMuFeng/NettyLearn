package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @program: ProducerAndProvider
 * @description: 接收、处理、相应客户端WebSocket请求的核心业务处理类
 * @author: Mr.Yqy
 * @create: 2019-03-17 15:38
 **/
public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;
    private static final String  WEB_SOCKET_URL="ws://localhost:8888/websocket";

    //在客户端和服务端创建连接的时候调用
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
        System.out.println("客户端与服务端连接开启");
    }

    //客户端和服务端断开连接的时候调用
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.remove(ctx.channel());
        System.out.println("客户端与服务端连接关闭");
    }

    //服务端接收客户端发送过来的数据结束之后调用
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    //工程出现异常的时候调用
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //输出异常并且断开连接
        cause.printStackTrace();
        System.out.println("发现异常主动断开");
        ctx.close();
    }


    //服务端处理客户端websocket请求的核心方法,建立连接需要2部，首先是接收一个http请求来发起握手请求，然后才是建立链接
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
           //处理客户端向服务端发起握手请求的业务
        if(o instanceof FullHttpRequest){
         handHttpRequet(channelHandlerContext,(FullHttpRequest) o);
        }
        //处理websocket连接业务
        else  if(o instanceof WebSocketFrame)
        {
         handWebSocketFrame(channelHandlerContext,(WebSocketFrame)o);
        }
    }
    //处理客户端与服务端之间的websocket业务
    private void handWebSocketFrame(ChannelHandlerContext ctx,WebSocketFrame frame){
        //判断是否是关闭websocket
        if(frame instanceof CloseWebSocketFrame ){
            handshaker.close(ctx.channel(),((CloseWebSocketFrame) frame).retain());
        }
        //判断是否是pin消息
        if(frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        }
        //判断是否是二进制,
        if(!(frame instanceof TextWebSocketFrame)){
            System.out.println("目前我们不支持二进制消息");
            throw new RuntimeException(this.getClass().getName()+"不支持此消息");
        }
        //返回应答消息
        String request=((TextWebSocketFrame) frame).text();//获取客户端向服务端获取的消息
        System.out.println("服务端收到客户端发起的消息");
        Date date=new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss EE");
        TextWebSocketFrame tws=new TextWebSocketFrame(df.format(date)+ request);
        //群发，服务端向每个连接上来的客户端群发消息
        NettyConfig.group.writeAndFlush(tws);
    }


   //处理客户端向服务端发起http握手请求的业务,如果连接异常就返回badrequset
    private void handHttpRequet(ChannelHandlerContext ctx,FullHttpRequest req){
        if(!req.getDecoderResult().isSuccess()||!("websocket".equals(req.headers().get("Upgrade")))){
              sendHttpResponse(ctx,req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.BAD_REQUEST));
        return;
        }
        WebSocketServerHandshakerFactory wsFactory=new WebSocketServerHandshakerFactory(WEB_SOCKET_URL,null,false);
        //创建HandShacker
        handshaker =wsFactory.newHandshaker(req);
        if(handshaker==null){
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        }
        else {
            handshaker.handshake(ctx.channel(),req);
        }
    }

    //服务端向客户端响应请求的方法
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res){
         //如果连接状态为200，则返回一个默认的response
        if(res.getStatus().code()!=200){
             ByteBuf buf=Unpooled.copiedBuffer(res.getStatus().toString(),CharsetUtil.UTF_8);
             res.content().writeBytes(buf);
             buf.release();
         }
         //服务端向客户端发送数据,ChannelFuture的作用是用来保存Channel异步操作的结果。
        ChannelFuture channelFuture=ctx.channel().writeAndFlush(res);
        if(res.getStatus().code()!=200){
            //如果出错则关闭连接
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }
}