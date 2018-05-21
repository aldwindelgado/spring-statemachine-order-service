package com.example.orderservice;


import lombok.extern.java.Log;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

enum OrderEvents {
	FULFILL,
	PAY,
	CANCEL
}

enum OrderStates {
	SUBMITTED,
	PAID,
	FULFILLED,
	CANCELLED
}

@Log
@Configuration
@EnableStateMachineFactory
public class SimpleEnumStatemachineConfiguration extends StateMachineConfigurerAdapter<OrderStates, OrderEvents> {

	@Override
	public void configure(StateMachineTransitionConfigurer<OrderStates, OrderEvents> transitions) throws Exception {
		// @formatter:off
        transitions
                .withExternal()
					.source(OrderStates.SUBMITTED)
					.target(OrderStates.PAID)
					.event(OrderEvents.PAY)
                .and()
                .withExternal()
					.source(OrderStates.PAID)
					.target(OrderStates.FULFILLED)
					.event(OrderEvents.FULFILL)
                .and()
                .withExternal()
					.source(OrderStates.SUBMITTED)
					.target(OrderStates.CANCELLED)
					.event(OrderEvents.CANCEL)
                .and()
                .withExternal()
					.source(OrderStates.PAID)
					.target(OrderStates.CANCELLED)
					.event(OrderEvents.CANCEL);
        // @formatter:on
	}

	@Override
	public void configure(StateMachineStateConfigurer<OrderStates, OrderEvents> states) throws Exception {
		states
				.withStates()
				.initial(OrderStates.SUBMITTED)
//				.stateEntry(OrderStates.SUBMITTED, stateContext -> {
//
//					/**
//					 * This will fail if 'orderId' is NULL; the program will throw an 'IllegalStateException'
//					 * followed by 'NullPointerException';
//					 *
//					 * This will return '-1L' if the 'orderId' is NOT PRESENT;
//					 */
//					Long orderId = Long.class.cast(
//							stateContext
//									.getExtendedState()
//									.getVariables()
//									.getOrDefault("orderId", -1L)
//					);
//
//					log.info("[XXXX] - orderId is " + orderId + ".");
//					log.info("[XXXX] - Entering SUBMITTED State!");
//				})
				.state(OrderStates.PAID)
				.end(OrderStates.FULFILLED)
				.end(OrderStates.CANCELLED);
	}

	// Configuration Stuff
	@Override
	public void configure(StateMachineConfigurationConfigurer<OrderStates, OrderEvents> config) throws Exception {
		StateMachineListenerAdapter<OrderStates, OrderEvents> adapter = new StateMachineListenerAdapter<OrderStates, OrderEvents>() {
			@Override
			public void stateChanged(State<OrderStates, OrderEvents> from, State<OrderStates, OrderEvents> to) {
				log.info(String.format("[XXXX] State changed(from: %s, to: %s)", from + "", to + ""));

			}
		};

		config.withConfiguration()
				.autoStartup(false)
				.listener(adapter);
	}
}
