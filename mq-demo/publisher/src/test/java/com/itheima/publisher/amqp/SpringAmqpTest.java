package com.itheima.publisher.amqp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AbstractAdvisingBeanPostProcessor abstractAdvisingBeanPostProcessor;

    @Test
    public void testSimpleQueue() {
        // 队列名称
        String queueName = "simple.queue";
        // 消息
        String message = "hello, spring amqp!";
        // 发送消息
        rabbitTemplate.convertAndSend(queueName, message);
    }


    // 向队列中不停发送消息，模拟消息堆积。
    @Test
    public void testWorkQueue() throws InterruptedException {
        // 队列名称
        String queueName = "work.queue";
        // 消息
        String message = "hello, message_";
        for (int i = 0; i < 50; i++) {
            // 发送消息，每20毫秒发送一次，相当于每秒发送50条消息
            rabbitTemplate.convertAndSend(queueName, message + i);
            Thread.sleep(20);
        }
    }

    // fanout exchange
    @Test
    public void testFanoutExchange() {
        // 交换机名称
        String exchangeName = "hmall.fanout";
        // 消息
        String message = "hello, everyone!";
        rabbitTemplate.convertAndSend(exchangeName, "", message);
    }

    // direct exchange
    @Test
    public void testSendDirectExchange1() {
        // 交换机名称
        String exchangeName = "hmall.direct";
        // 消息
        String message = "红色警报！日本乱排核废水，导致海洋生物变异，惊现哥斯拉！";
        // 发送消息
        rabbitTemplate.convertAndSend(exchangeName, "red", message);
    }

    @Test
    public void testSendDirectExchange2() {
        // 交换机名称
        String exchangeName = "hmall.direct";
        // 消息
        String message = "蓝色警报！日本拒绝承认造成海洋污染！";
        // 发送消息
        rabbitTemplate.convertAndSend(exchangeName, "blue", message);
    }

    @Test
    public void testSendTopicExchange() {
        // 交换机名称
        String exchangeName = "hmall.topic";
        // 消息
        String message = "喜报！孙悟空大战哥斯拉，胜!";
        // 发送消息
        rabbitTemplate.convertAndSend(exchangeName, "china.news", message);
    }

    @Test
    public void testSendMap() throws InterruptedException {
        // 准备消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("name", "jack");
        msg.put("age", 21);
        // 发送消息
        rabbitTemplate.convertAndSend("object.queue", msg);
    }

    @Test
    void testPublisherConfirm() {
        // 1. 创建 CorrelationData
        CorrelationData cd = new CorrelationData();
        // 2. 给 CompletableFuture 添加回调
        cd.getFuture().whenComplete((confirm, ex) -> {
            if (ex != null) {
                // Future 发生异常（基本不会发生）
                log.error("send message fail", ex);
            } else {
                if (confirm != null && confirm.isAck()) {
                    log.debug("发送消息成功，收到 ack!");
                } else {
                    log.error("发送消息失败，收到 nack, reason: {}", confirm != null ? confirm.getReason() : "null confirm");
                }
            }
        });
        // 3. 发送消息
        rabbitTemplate.convertAndSend("hmall.direct", "blue", "hello", cd);
    }

    @Test
    void testSendMessage() {
        // 自定义构建消息
        MessageBuilder.withBody("hello world!".getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
        // 发送消息
        for (int i = 0; i < 1000000; i++) {
            rabbitTemplate.convertAndSend("hmall.direct", "red", "hello world!");
        }
    }

    @Test
    void testSendDelayMessage() {
        rabbitTemplate.convertAndSend("normal.direct", "normal", "hello", message -> {
            message.getMessageProperties().setExpiration("10000");
            return message;
        });
    }

    @Test
    void testPublisherDelayMessage() {
        // 1. 创建消息
        String message = "hello, delayed message";
        // 2. 发送消息，利用消息后置处理器添加消息头
        rabbitTemplate.convertAndSend("delay.direct", "delay", message, message1 -> {
            // 添加延迟消息属性
            message1.getMessageProperties().setHeader("x-delay", 5000);
            return message1;
        });
    }

}
