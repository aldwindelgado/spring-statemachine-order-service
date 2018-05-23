package com.example.orderservice;

import lombok.extern.java.Log;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
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
@Log
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
	 * This method ensures that the state machine is 'rehydrated',
	 * by this we mean that the ORDER entity and the state machine is
	 * aligned with one another.
	 *
	 * This will make sure the state machine is in a correct state in which
	 * the state will come from the ORDER entity itself. That's why the 'state'
	 * field exist on the ORDER entity.
	 *
	 * If the order on the DB is not on a valid state yet, then this 'build()'
	 * method needs to reflect that to state machine. Otherwise the state machine
	 * and the entity from the DB will have a mismatch, and we do not want that mismatch
	 * because the event/transition will not be handled properly.
	 *
	 * Take note that the 'STATE MACHINE' and the 'ORDER' entity is not the same, in this method
	 * we make sure that they're on the same side and not on the different side.
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
		 * Override the state machine's state/event/transition
		 *
		 * This is useful when you need to add new metadata for the
		 * state machine before going to a certain state/event/transition.
		 *
		 */
		stateMachine.getStateMachineAccessor()
				.doWithAllRegions(sma -> {

					StateMachineInterceptor<OrderStates, OrderEvents> interceptor = null;

					/**
					 * This interceptor exists if you want to persist the state of the state machine
					 * to the DB or something else, maybe add some metadata on the DB about the current
					 * information of the state machine's state or something like that.
					 */
					sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<OrderStates, OrderEvents>() {

						@Override
						public Message<OrderEvents> preEvent(Message<OrderEvents> message, StateMachine<OrderStates, OrderEvents> stateMachine) {

							log.info("[XXXX] PRE EVENT");
							log.info("[XXXX] MESSAGE " + message);
							log.info("[XXXX] STATE MACHINE " + stateMachine);

							return super.preEvent(message, stateMachine);
						}

						@Override
						public StateContext<OrderStates, OrderEvents> preTransition(StateContext<OrderStates, OrderEvents> stateContext) {

							log.info("[XXXX] PRE TRANSITION:");
							log.info("[XXXX] STATE CONTEXT " + stateContext);

							return super.preTransition(stateContext);
						}

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

						@Override
						public StateContext<OrderStates, OrderEvents> postTransition(StateContext<OrderStates, OrderEvents> stateContext) {

							log.info("[XXXX] POST TRANSITION:");
							log.info("[XXXX] STATE CONTEXT " + stateContext);

							return super.postTransition(stateContext);
						}

						@Override
						public void postStateChange(State<OrderStates, OrderEvents> state, Message<OrderEvents> message, Transition<OrderStates, OrderEvents> transition, StateMachine<OrderStates, OrderEvents> stateMachine) {
							log.info("[XXXX] POST STATE CHANGE");

							log.info("[XXXX] State: " + state);
							log.info("[XXXX] Message: " + message);
							log.info("[XXXX] Transition: " + transition);
							log.info("[XXXX] State Machine: " + stateMachine);

							super.postStateChange(state, message, transition, stateMachine);
						}
					});

					/**
					 * This tells the state machine to force itself to go to a separated state
					 * instead of defaulting to it's initial state.
					 */
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
