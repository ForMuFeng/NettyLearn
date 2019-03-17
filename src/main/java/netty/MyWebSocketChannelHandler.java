package netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @program: NettyLearn
 * @description:初始化连接时候的各个组件
 * @author: Mr.Yqy
 * @create: 2019-03-17 17:55
 **/
public class MyWebSocketChannelHandler extends ChannelInitializer<SocketChannel> {

    protected void initChannel(SocketChannel socketChannel) throws Exception {

        socketChannel.pipeline().addLast("http-codec",new HttpServerCodec());
        socketChannel.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
        socketChannel.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
        socketChannel.pipeline().addLast("handler",new MyWebSocketHandler());
    }
}
