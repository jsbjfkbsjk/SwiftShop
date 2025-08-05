package com.heima.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.api.client.CartClient;
import com.heima.api.client.ItemClient;
import com.heima.api.dto.OrderDetailDTO;
import com.heima.trade.constant.MqConstant;
import com.heima.trade.domain.dto.OrderFormDTO;
import com.heima.trade.domain.po.Order;
import com.heima.trade.domain.po.OrderDetail;
import com.heima.trade.mapper.OrderDetailMapper;
import com.heima.trade.mapper.OrderMapper;
import com.heima.trade.service.IOrderDetailService;
import com.heima.trade.service.IOrderService;
import com.heima.api.dto.ItemDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.exception.CommonException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;

import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author j
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

   // private final IItemService itemService;
    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
   // private final ICartService cartService;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final RedisTemplate redisTemplate;

    @Override
   // @Transactional
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        //cartService.removeByItemIds(itemIds);
       // cartClient.deleteCartItemByIds(itemIds);

        // 4.扣减库存
        try {
//            itemService.deductStock(detailDTOS);
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }
        rabbitTemplate.convertAndSend("trade.topic","order.create",
                UserContext.getUser(),message -> {
            message.getMessageProperties().setHeader("item-ids",itemIds);
            return message;
                });
        rabbitTemplate.convertAndSend(MqConstant.DELAY_EXCHANGE_NAME,MqConstant.DELAY_ORDER_KEY,
                order.getId(),message -> {
            message.getMessageProperties().setDelay(5000);
            return message;
                });
        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
//        Order order = new Order();
//        order.setId(orderId);
//        order.setStatus(2);
//        order.setPayTime(LocalDateTime.now());
//        updateById(order);
        // UPDATE `order` SET status = ? , pay_time = ? WHERE id = ? AND status = 1
        lambdaUpdate()
                .set(Order::getStatus, 2)
                .set(Order::getPayTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)
                .update();
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId,String msg_id) {

            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new CommonException("订单不存在",404);
            }
        boolean first = Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent("mq:msg:" + msg_id, "1", Duration.ofHours(2)));
        if (!first) {
            // 已处理过，直接 ACK
            return ;
        }

            // 3. 幂等性判断
            if (order.getStatus()==5) {
                return; // 已关闭/取消，无需重复操作
            }
            if ((order.getStatus())==4) {
                return; // 已支付，不可取消
            }
            if (order.getStatus()==6)return ;//定时任务和延迟队列有可能处理两次，判断下是否处理完毕就行

            // 4. 修改订单状态为 CLOSED
            order.setStatus(6);
            updateById(order);

           List<OrderDetail>orderDetails = detailService.listByIds(List.of(orderId));
           List<OrderDetailDTO>orderDetailDTOS = new LinkedList<>();
           for(OrderDetail orderDetail:orderDetails){
               OrderDetailDTO orderDetailDTO = new OrderDetailDTO();
               orderDetailDTO.setNum(orderDetail.getNum());
               orderDetailDTO.setItemId(orderDetail.getItemId());
               orderDetailDTOS.add(orderDetailDTO);
           }

            // 5. 回滚库存
            itemClient.recoverStock(orderDetailDTOS);

    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
