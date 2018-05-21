package com.example.orderservice;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptor;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final StateMachineFactory<OrderStates, OrderEvents> factory;

	private static final String ORDER_ID_HEADER = "orderId";

	public OrderService(OrderRepository orderRepository, StateMachineFactory<OrderStates, OrderEvents> factory) {
		this.orderRepository = orderRepository;
		this.factory = factory;
	}

	public Order create(Date when) {
		return this.orderRepository
				.save(new Order(when, OrderStates.SUBMITTED));
	}

	public StateMachine<OrderStates, OrderEvents> fulfill(Long orderId) {
		StateMachine<OrderStates, OrderEvents> stateMachine = this.build(orderId);

		Message<OrderEvents> fulfillmentMessage = MessageBuilder.withPayload(OrderEvents.FULFILL)
				.setHeader(ORDER_ID_HEADER, orderId)
				.build();

		stateMachine.sendEvent(fulfillmentMessage);
		return stateMachine;
	}

	public StateMachine<OrderStates, OrderEvents> pay(Long orderId, String paymentConfirmationNumber) {
		StateMachine<OrderStates, OrderEvents> stateMachine = this.build(orderId);

		Message<OrderEvents> paymentMessage = MessageBuilder.withPayload(OrderEvents.PAY)
				.setHeader(ORDER_ID_HEADER, orderId)
				.setHeader("paymentNumber", paymentConfirmationNumber)
				.build();

		stateMachine.sendEvent(paymentMessage);
		return stateMachine;
	}

	private StateMachine<OrderStates, OrderEvents> build(Long orderId) {
		Order order = this.orderRepository.findOne(orderId);
		String orderIdKey = String.valueOf(order.getId());
		StateMachine<OrderStates, OrderEvents> stateMachine =
				this.factory.getStateMachine(orderIdKey);

		stateMachine.stop(); //stop state machine from running

		stateMachine.getStateMachineAccessor()
				.doWithAllRegions(sma -> {

					StateMachineInterceptor<OrderStates, OrderEvents> interceptor = null;
					sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<OrderStates, OrderEvents>() {

						@Override
						public void preStateChange(State<OrderStates, OrderEvents> state, Message<OrderEvents> message, Transition<OrderStates, OrderEvents> transition, StateMachine<OrderStates, OrderEvents> stateMachine) {

							Optional.ofNullable(message).ifPresent(msg -> {
								Optional.ofNullable(
										Long.class.cast(msg.getHeaders().getOrDefault(ORDER_ID_HEADER, -1L)))
										.ifPresent(orderId -> {
											Order order1 = orderRepository.findOne(orderId);
											order1.setOrderState(state.getId());
											orderRepository.save(order1);

										});
							});
						}
					});

					sma.resetStateMachine(
							new DefaultStateMachineContext<>(
									order.getOrderStates(),
									null,
									null,
									null
							)
					);
				});

		stateMachine.start();
		return stateMachine;
	}
}
