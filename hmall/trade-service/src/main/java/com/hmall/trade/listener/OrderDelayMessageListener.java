package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.domain.dto.PayOrderDTO;
import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.vo.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {

    private final IOrderService orderService;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_ORDER_QUEUE_NAME),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME, delayed = "true", type = ExchangeTypes.DIRECT),
            key = MQConstants.DELAY_ORDER_KEY
    ))
    public void listenOrderDelayMessage(Long orderId){
        // 1. 查询订单
        Order order = orderService.getById(orderId);
        // 2. 检测订单状态，判断是否已支付
        if(order == null || order.getStatus() != 1){
            // 订单不存在或者未支付
            return;
        }
        // 3. 未支付，需要查询支付流水状态
        PayOrderDTO payOrder = payClient.queryPayOrderByBizOrderNo(orderId);
        // 4. 再次判断是否支付
        if(payOrder != null && payOrder.getStatus() == 3){
            // 已支付，标记订单状态为已支付
            orderService.markOrderPaySuccess(orderId);
        }else{
            // 未支付，取消订单，回复库存
            orderService.cancelOrder(orderId);
        }
    }
}
