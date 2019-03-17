package netty;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @program: ProducerAndProvider
 * @description:存储整个工程的全局配置
 * @author: Mr.Yqy
 * @create: 2019-03-17 15:40
 **/
public class NettyConfig {
    //存储每一个客户端接入时的通道
    public  static ChannelGroup group=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
}
