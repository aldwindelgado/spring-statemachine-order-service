package com.example.orderservice;

import lombok.extern.java.Log;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Log
@Component
public class Runner implements ApplicationRunner {

	private final OrderService orderService;


//	private final StateMachineFactory<OrderStates, OrderEvents> factory;

	public Runner(OrderService orderService, StateMachineFactory<OrderStates, OrderEvents> factory) {
		this.orderService = orderService;
//		this.factory = factory;
	}


	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {

		Order order = this.orderService.create(new Date());

		StateMachine<OrderStates, OrderEvents> paymentStateMachine =
				orderService.pay(order.getId(), UUID.randomUUID().toString());
		log.info("[XXXX - Payment State Machine OBJECT] " + paymentStateMachine);
		log.info("[XXXX - Payment State Machine EXTENDED STATE] " + paymentStateMachine.getExtendedState());
		log.info("[XXXX - Payment State Machine INITIAL STATE] " + paymentStateMachine.getInitialState());

		log.info("[XXXX - Payment State Machine] " + paymentStateMachine.getState().getId().name());
		log.info("[XXXX - Payment ORDER DB Entry] " + orderService.byId(order.getId()));

		StateMachine<OrderStates, OrderEvents> fulfilledStateMachine =
				orderService.fulfill(order.getId());
		log.info("[XXXX - Fulfill State Machine] " + fulfilledStateMachine.getState().getId().name());
		log.info("[XXXX - Fulfill ORDER DB Entry] " + orderService.byId(order.getId()));


//		Long orderId = 666L;
//		StateMachine<OrderStates, OrderEvents> machine = this.factory.getStateMachine(String.valueOf(orderId));
//
//		machine
//				.getExtendedState()
//				.getVariables()
//				.putIfAbsent(
//						"orderId",
//						orderId
//				);
//
//
//		machine.start();
//		log.info("[XXXX -- EXPECTED: SUBMITTED] Current State: " + machine.getState().getId().name());
//		machine.sendEvent(OrderEvents.FULFILL);
//		log.info("[XXXX -- EXPECTED: SUBMITTED] Current State: " + machine.getState().getId().name());
//		machine.sendEvent(OrderEvents.PAY);
//		log.info("[XXXX -- EXPECTED: PAID] Current State: " + machine.getState().getId().name());
//
//		/**
//		 * Probably the best implementation rather than 'sendEvent' function
//		 * This enables us to be flexible enough handling the outside calls
//		 * Through API or whatsoever implementation...
//		 *
//		 * 'sendEvent' is only useful if used inside the program itself...
//		 */
//		Message<OrderEvents> eventsMessage = MessageBuilder
//				.withPayload(OrderEvents.FULFILL)
//				.setHeader("A", "B")
//				.build();
//		machine.sendEvent(eventsMessage);
//
////        machine.sendEvent(OrderEvents.FULFILL);
//		log.info("[XXXX -- EXPECTED: FULFILLED] Current State: " + machine.getState().getId().name());

	}
}
