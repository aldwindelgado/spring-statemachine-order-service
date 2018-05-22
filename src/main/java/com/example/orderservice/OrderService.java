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

	public Order byId(Long id) {
		return this.orderRepository.findOne(id);
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

	/**
	 * Why do we need this?
	 *
	 * @param orderId
	 * @return
	 */
	private StateMachine<OrderStates, OrderEvents> build(Long orderId) {
		Order order = this.orderRepository.findOne(orderId); // Retrieve orderId on DB
		String orderIdKey = String.valueOf(order.getId()); // Convert the ID to String
		StateMachine<OrderStates, OrderEvents> stateMachine =
				this.factory.getStateMachine(orderIdKey); // Get the StateMachine with the specific ID

		stateMachine.stop(); //stop state machine from running

		/**
		 * Validates the current state machine's state
		 */
		stateMachine.getStateMachineAccessor()
				.doWithAllRegions(sma -> {

					StateMachineInterceptor<OrderStates, OrderEvents> interceptor = null;

					/**
					 * Persists the state machine context into the Order entity itself
					 * This makes them in-sync.
					 */
					sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<OrderStates, OrderEvents>() {

						@Override
						public void preStateChange(State<OrderStates, OrderEvents> state, Message<OrderEvents> message, Transition<OrderStates, OrderEvents> transition, StateMachine<OrderStates, OrderEvents> stateMachine) {

							Optional.ofNullable(message).ifPresent(msg -> {
								Optional.ofNullable(
										Long.class.cast(msg.getHeaders().getOrDefault(ORDER_ID_HEADER, -1L)))
										.ifPresent(orderId -> {
											Order order1 = orderRepository.findOne(orderId);
											order1.setOrderState(state.getId()); // This is the one responsible for changing the state
											orderRepository.save(order1);

										});
							});
						}
					});

					// This method I don't know...
					sma.resetStateMachine(
							new DefaultStateMachineContext<>(
									order.getOrderStates(),
									null,
									null,
									null
							)
					);
				});

		stateMachine.start(); // start the state machine once again
		return stateMachine;
	}
}
