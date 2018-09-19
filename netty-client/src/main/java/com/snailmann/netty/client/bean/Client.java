package com.snailmann.netty.client.bean;

import com.snailmann.netty.client.handle.ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

@Slf4j
@Component
public class Client {


    public Client() {
        try {
            log.error("client running");
            init();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void init() throws IOException, InterruptedException {
        EventLoopGroup workgroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workgroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) throws Exception {
                        sc.pipeline().addLast(new ClientHandler());
                    }
                });

        ChannelFuture cf1 = b.connect("127.0.0.1", 8765).sync();
        Scanner scanner = new Scanner(System.in);
        while(true){
            String str = scanner.nextLine();
            cf1.channel().writeAndFlush(Unpooled.copiedBuffer(str.getBytes()));
            if (str.equals("exit"))
                break;
        }

       /* cf1.channel().writeAndFlush(Unpooled.copiedBuffer("hello".getBytes()));*/
        cf1.channel().closeFuture().sync();
        workgroup.shutdownGracefully();
    }
}
